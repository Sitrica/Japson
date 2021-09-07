package com.sitrica.japson.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sitrica.japson.shared.Handler;

public class Connections extends Handler {

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final LoadingCache<InetSocketAddress, JapsonConnection> disconnected;
	private final List<JapsonConnection> connections = new ArrayList<>();
	private final JapsonServer japson;

	public Connections(JapsonServer japson) {
		super(0x00);
		this.japson = japson;
		disconnected = CacheBuilder.newBuilder()
				.expireAfterWrite(japson.getConnectionExpiry(), TimeUnit.MINUTES)
				.maximumSize(1000)
				.removalListener(new RemovalListener<InetSocketAddress, JapsonConnection>() {
					@Override
					public void onRemoval(RemovalNotification<InetSocketAddress, JapsonConnection> notification) {
						JapsonConnection connection = notification.getValue();
						// Connection was reacquired.
						if (notification.getCause() == RemovalCause.EXPLICIT)
							return;
						japson.getListeners().forEach(listener -> listener.onForget(connection));
					}
				}).build(new CacheLoader<InetSocketAddress, JapsonConnection>() {
					@Override
					public JapsonConnection load(InetSocketAddress address) throws Exception {
						return getConnection(address)
								.orElseGet(() -> {
									JapsonConnection created = new JapsonConnection(address);
									connections.add(created);
									return created;
								});
					}
				});
		executor.schedule(() -> {
			for (JapsonConnection connection : connections) {
				if (System.currentTimeMillis() - connection.getLastUpdate() < japson.getTimeout())
					continue;
				japson.getListeners().forEach(listener -> listener.onUnresponsive(connection));
				connection.unresponsive();
				if (connection.getUnresponsiveCount() > japson.getMaxReconnectAttempts()) {
					japson.getListeners().forEach(listener -> listener.onDisconnect(connection));
					disconnected.put(connection.getAddress(), connection);
				}
			}
			connections.removeIf(connection -> connection.getUnresponsiveCount() > japson.getMaxReconnectAttempts());
		}, 1, TimeUnit.SECONDS);
	}

	public JapsonConnection addConnection(InetSocketAddress address) {
		return getConnection(address)
				.orElseGet(() -> {
					JapsonConnection connection = new JapsonConnection(address);
					japson.getListeners().forEach(listener -> listener.onAcquiredCommunication(connection));
					connections.add(connection);
					return connection;
				});
	}

	public Optional<JapsonConnection> getConnection(InetSocketAddress address) {
		Optional<JapsonConnection> optional = connections.stream()
				.filter(existing -> existing.getAddress().equals(address))
				.findFirst();
		if (optional.isPresent())
			return optional;
		optional = Optional.ofNullable(disconnected.getIfPresent(address));
		if (!optional.isPresent())
			return Optional.empty();
		JapsonConnection connection = optional.get();
		japson.getListeners().forEach(listener -> listener.onReacquiredCommunication(connection));
		connections.add(connection);
		disconnected.invalidate(address);
		return optional;
	}

	@Override
	public JsonObject handle(InetSocketAddress address, JsonObject json) {
		int port = json.get("port").getAsInt();
		InetSocketAddress server = InetSocketAddress.createUnresolved(address.getHostName(), port);
		if (!japson.hasPassword()) {
			JapsonConnection connection = addConnection(server);
			connection.update();
		} else {
			Optional<JsonElement> optional = Optional.ofNullable(json.get("password"));
			if (!optional.isPresent())
				return null;
			String password = optional.get().getAsString();
			if (!japson.passwordMatches(password)) {
				japson.getLogger().atWarning().log("A packet from %s did not match the correct password!", server.getHostName());
				return null;
			}
			JapsonConnection connection = addConnection(server);
			connection.update();
		}
		JsonObject returning = new JsonObject();
		returning.addProperty("success", true);
		return returning;
	}

	public void shutdown() {
		executor.shutdown();
	}

	public void kill() {
		executor.shutdownNow();
	}

	public class JapsonConnection {

		private long updated = System.currentTimeMillis();
		private final InetSocketAddress address;
		private int fails = 0;

		public JapsonConnection(InetSocketAddress address) {
			this.address = address;
		}

		public int getUnresponsiveCount() {
			return fails;
		}

		public InetSocketAddress getAddress() {
			return address;
		}

		public long getLastUpdate() {
			return updated;
		}

		public void unresponsive() {
			fails++;
		}

		public void update() {
			updated = System.currentTimeMillis();
			japson.getListeners().forEach(listener -> listener.onHeartbeat(this));
		}

	}

}
