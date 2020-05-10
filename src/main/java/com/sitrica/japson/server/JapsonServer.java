package com.sitrica.japson.server;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sitrica.japson.shared.Japson;

public class JapsonServer extends Japson {

	private static final ExecutorService executor = Executors.newCachedThreadPool();

	private long TIMEOUT = 3000L, HEARTBEAT = 1000L, DISCONNECT = 5;
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
		this.address = address;
		this.port = port;
		connections = new Connections(this);
		handlers.add(connections);
		executor.submit(new SocketHandler(this, socket));
	}

	public JapsonServer setDisconnectAttempts(long disconnect) {
		this.DISCONNECT = disconnect;
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

	public InetAddress getAddress() {
		return address;
	}

	public long getHeartbeat() {
		return HEARTBEAT;
	}

	public long getTimeout() {
		return TIMEOUT;
	}

	public int getPort() {
		return port;
	}

}
