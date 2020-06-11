package com.sitrica.japson.server;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.Sets;
import com.google.common.flogger.FluentLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sitrica.japson.shared.Japson;

public class JapsonServer extends Japson {

	private static final ExecutorService executor = Executors.newCachedThreadPool();
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();
	protected final Set<Listener> listeners = new HashSet<>();

	private long TIMEOUT = 3000L, HEARTBEAT = 1000L, DISCONNECT = 5, EXPIRY = 10; // EXPIRY in minutes, DISCONNECT is amount, and rest in milliseconds.;
	private final Connections connections;
	private final DatagramSocket socket;

	private final Gson gson;

	public JapsonServer(int port) throws UnknownHostException, SocketException {
		this(InetAddress.getLocalHost(), port);
	}

	public JapsonServer(String host, int port) throws UnknownHostException, SocketException {
		this(InetAddress.getByName(host), port);
	}

	public JapsonServer(InetAddress address, int port) throws SocketException {
		this(address, port, new GsonBuilder()
				.enableComplexMapKeySerialization()
				.serializeNulls()
				.setLenient()
				.create());
	}

	public JapsonServer(int port, Gson gson) throws UnknownHostException, SocketException {
		this(InetAddress.getLocalHost(), port, gson);
	}

	public JapsonServer(String host, int port, Gson gson) throws UnknownHostException, SocketException {
		this(InetAddress.getByName(host), port, gson);
	}

	public JapsonServer(InetAddress address, int port, Gson gson) throws SocketException {
		super(address, port);
		this.gson = gson;
		this.socket = new DatagramSocket(port);
		connections = new Connections(this);
		handlers.add(connections);
		executor.execute(new SocketHandler(this, socket));
		if (debug)
			logger.atInfo().log("Started Japson server bound to %s.", address.getHostAddress() + ":" + port);
	}

	public JapsonServer setDisconnectAttempts(long disconnect) {
		this.DISCONNECT = disconnect;
		return this;
	}

	public Japson registerListeners(Listener... listeners) {
		this.listeners.addAll(Sets.newHashSet(listeners));
		return this;
	}

	public Japson registerListener(Listener listener) {
		return registerListeners(listener);
	}

	/**
	 * The amount of minutes to wait before forgetting about a connection.
	 * 
	 * @param expiry time in minutes.
	 * @return The JapsonServer for chaining.
	 */
	public JapsonServer setExpiryMinutes(long expiry) {
		this.EXPIRY = expiry;
		return this;
	}

	public JapsonServer setHeartbeat(long heartbeat) {
		this.HEARTBEAT = heartbeat;
		return this;
	}

	public JapsonServer setTimeout(long timeout) {
		this.TIMEOUT = timeout;
		return this;
	}

	public long getMaxDisconnectAttempts() {
		return DISCONNECT;
	}

	public Connections getConnections() {
		return connections;
	}

	public Set<Listener> getListeners() {
		return listeners;
	}

	public FluentLogger getLogger() {
		return logger;
	}

	public long getHeartbeat() {
		return HEARTBEAT;
	}

	public long getTimeout() {
		return TIMEOUT;
	}

	public long getExpiry() {
		return EXPIRY;
	}

	public void shutdown() {
		executor.shutdown();
	}

	public Gson getGson() {
		return gson;
	}

}
