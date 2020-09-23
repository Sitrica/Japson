package com.sitrica.japson.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
	private final Set<Listener> listeners = new HashSet<>();
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
						listeners.forEach(listener -> listener.onForget(connection));
					}
				}).build(new CacheLoader<InetSocketAddress, JapsonConnection>() {
					@Override
					public JapsonConnection load(InetSocketAddress address) throws Exception {
						return getConnection(address.getAddress(), address.getPort())
								.orElseGet(() -> {
									JapsonConnection created = new JapsonConnection(address.getAddress(), address.getPort());
									connections.add(created);
									return created;
								});
					}
				});
		listeners.addAll(japson.getListeners());
		executor.schedule(() -> {
			for (JapsonConnection connection : connections) {
				if (System.currentTimeMillis() - connection.getLastUpdate() < japson.getTimeout())
					continue;
				listeners.forEach(listener -> listener.onUnresponsive(connection));
				connection.unresponsive();
				if (connection.getUnresponsiveCount() > japson.getMaxReconnectAttempts()) {
					listeners.forEach(listener -> listener.onDisconnect(connection));
					disconnected.put(InetSocketAddress.createUnresolved(connection.getAddress().getHostName(), connection.getPort()), connection);
				}
			}
			connections.removeIf(connection -> connection.getUnresponsiveCount() > japson.getMaxReconnectAttempts());
		}, 1, TimeUnit.SECONDS);
	}

	public JapsonConnection addConnection(InetAddress address, int port) {
		return getConnection(address, port)
				.orElseGet(() -> {
					JapsonConnection connection = new JapsonConnection(address, port);
					listeners.forEach(listener -> listener.onAcquiredCommunication(connection));
					connections.add(connection);
					return connection;
				});
	}

	public Optional<JapsonConnection> getConnection(InetAddress address, int port) {
		Optional<JapsonConnection> optional = connections.stream()
				.filter(existing -> existing.getAddress().equals(address))
				.filter(existing -> existing.getPort() == port)
				.findFirst();
		if (optional.isPresent())
			return optional;
		InetSocketAddress socketAddress = InetSocketAddress.createUnresolved(address.getHostName(), port);
		optional = Optional.ofNullable(disconnected.getIfPresent(socketAddress));
		if (!optional.isPresent())
			return Optional.empty();
		JapsonConnection connection = optional.get();
		listeners.forEach(listener -> listener.onReacquiredCommunication(connection));
		connections.add(connection);
		disconnected.invalidate(socketAddress);
		return optional;
	}

	@Override
	public JsonObject handle(InetAddress address, int packetPort, JsonObject json) {
		int port = json.get("port").getAsInt();
		if (!japson.hasPassword()) {
			JapsonConnection connection = addConnection(address, port);
			connection.update();
		} else {
			Optional<JsonElement> optional = Optional.ofNullable(json.get("password"));
			if (!optional.isPresent())
				return null;
			String password = optional.get().getAsString();
			if (!japson.passwordMatches(password)) {
				japson.getLogger().atWarning().log("A packet from %s did not match the correct password!", address.getHostName());
				return null;
			}
			JapsonConnection connection = addConnection(address, port);
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
		private final InetAddress address;
		private final int port;
		private int fails = 0;

		public JapsonConnection(InetAddress address, int port) {
			this.address = address;
			this.port = port;
		}

		public int getUnresponsiveCount() {
			return fails;
		}

		public InetAddress getAddress() {
			return address;
		}

		public long getLastUpdate() {
			return updated;
		}

		public void unresponsive() {
			fails++;
		}

		public int getPort() {
			return port;
		}

		public void update() {
			updated = System.currentTimeMillis();
		}

	}

}
