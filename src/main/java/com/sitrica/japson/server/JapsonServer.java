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
import com.sitrica.japson.shared.Japson;

public class JapsonServer extends Japson {

	private static final ExecutorService executor = Executors.newCachedThreadPool();
	protected final Set<Listener> listeners = new HashSet<>();

	private long TIMEOUT = 3000L, HEARTBEAT = 1000L, DISCONNECT = 5, EXPIRY = 10; // EXPIRY in minutes, DISCONNECT is amount, and rest in milliseconds.;
	private final Connections connections;
	private final DatagramSocket socket;
	private final InetAddress address;
	private final int port;

	public JapsonServer(int port) throws UnknownHostException, SocketException {
		this(InetAddress.getLocalHost(), port);
	}

	public JapsonServer(String host, int port) throws UnknownHostException, SocketException {
		this(InetAddress.getByName(host), port);
	}

	public JapsonServer(InetAddress address, int port) throws SocketException {
		this.socket = new DatagramSocket(port);
		socket.connect(address, port);
		this.address = address;
		this.port = port;
		connections = new Connections(this);
		handlers.add(connections);
		executor.execute(new SocketHandler(this, socket));
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

	public InetAddress getAddress() {
		return address;
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

	public int getPort() {
		return port;
	}

}
