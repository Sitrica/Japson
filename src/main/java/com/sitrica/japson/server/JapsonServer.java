package com.sitrica.japson.server;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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

	private final ExecutorService executor = Executors.newCachedThreadPool();
	protected final Set<Listener> listeners = new HashSet<>();
	private final SocketHandler handler;

	protected final InetSocketAddress address;

	private long RECONNECT = 5, EXPIRY = 10; // EXPIRY in minutes, RECONNECT is amount of trys.
	private final Connections connections;
	private final DatagramSocket socket;

	private final Gson gson;

	public JapsonServer(int port) throws UnknownHostException, SocketException {
		this(new InetSocketAddress(InetAddress.getLocalHost().getHostName(), port));
	}

	public JapsonServer(InetSocketAddress address) throws SocketException {
		this(address, new GsonBuilder()
				.enableComplexMapKeySerialization()
				.serializeNulls()
				.setLenient()
				.create());
	}

	public JapsonServer(int port, Gson gson) throws UnknownHostException, SocketException {
		this(new InetSocketAddress(InetAddress.getLocalHost().getHostName(), port), gson);
	}

	public JapsonServer(InetSocketAddress address, Gson gson) throws SocketException {
		this.address = address;
		this.gson = gson;
		this.socket = new DatagramSocket(address);
		socket.setSoTimeout(TIMEOUT);
		connections = new Connections(this);
		handlers.add(connections);
		handler = new SocketHandler(PACKET_SIZE, this, socket);
		executor.execute(handler);
		logger.atInfo().log("Started Japson server bound to %s.", address.getHostName() + ":" + address.getPort());
	}

	@Override
	public JapsonServer setAllowedAddresses(InetAddress... addesses) {
		acceptable.clear();
		acceptable.addAll(Sets.newHashSet(addesses));
		return this;
	}

	public JapsonServer setMaxReconnectAttempts(long reconnect) {
		this.RECONNECT = reconnect;
		return this;
	}

	public JapsonServer registerListeners(Listener... listeners) {
		this.listeners.addAll(Sets.newHashSet(listeners));
		return this;
	}

	/**
	 * The amount of minutes to wait before forgetting about a connection.
	 * 
	 * @param minutes Time in minutes.
	 * @return The JapsonServer for chaining.
	 */
	public JapsonServer setConnectionExpiry(long minutes) {
		this.EXPIRY = minutes;
		return this;
	}

	@Override
	public JapsonServer setPacketBufferSize(int buffer) {
		this.PACKET_SIZE = buffer;
		return this;
	}

	public Japson registerListener(Listener listener) {
		return registerListeners(listener);
	}

	@Override
	public JapsonServer setPassword(String password) {
		this.password = password;
		return this;
	}

	@Override
	public JapsonServer setTimeout(int timeout) {
		this.TIMEOUT = timeout;
		return this;
	}

	public long getMaxReconnectAttempts() {
		return RECONNECT;
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	public Connections getConnections() {
		return connections;
	}

	public Set<Listener> getListeners() {
		return listeners;
	}

	public long getConnectionExpiry() {
		return EXPIRY;
	}

	@Override
	public JapsonServer enableDebug() {
		this.debug = true;
		return this;
	}

	public FluentLogger getLogger() {
		return logger;
	}

	public long getTimeout() {
		return TIMEOUT;
	}

	public void shutdown() {
		connections.shutdown();
		socket.disconnect();
		socket.close();
		handler.stop();
		executor.shutdown();
	}

	public void kill() {
		connections.kill();
		socket.disconnect();
		socket.close();
		handler.stop();
		executor.shutdownNow();
	}

	public Gson getGson() {
		return gson;
	}

}
