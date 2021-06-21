package com.sitrica.japson.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sitrica.japson.client.packets.HeartbeatPacket;
import com.sitrica.japson.shared.Japson;
import com.sitrica.japson.shared.Packet;
import com.sitrica.japson.shared.ReturnablePacket;

public class JapsonClient extends Japson {

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	protected long HEARTBEAT = 1000L, DELAY = 1000L; // in milliseconds.

	protected final InetSocketAddress address;

	private boolean check, valid = true;
	private final Gson gson;

	public JapsonClient(int port) throws UnknownHostException {
		this(new InetSocketAddress(InetAddress.getLocalHost().getHostName(), port));
	}

	public JapsonClient(InetSocketAddress address) {
		this(address, new GsonBuilder()
				.enableComplexMapKeySerialization()
				.serializeNulls()
				.setLenient()
				.create());
	}

	public JapsonClient(int port, Gson gson) throws UnknownHostException {
		this(new InetSocketAddress(InetAddress.getLocalHost().getHostName(), port), gson);
	}

	public JapsonClient(InetSocketAddress address, Gson gson) {
		this.address = address;
		this.gson = gson;
	}

	public JapsonClient start() {
		executor.scheduleAtFixedRate(() -> {
			try {
				Boolean success = sendPacket(new HeartbeatPacket(password, address.getPort()));
				if (check && success != null && success)
					valid = true;
			} catch (TimeoutException | InterruptedException | ExecutionException e) {
				valid = false;
			}
		}, DELAY, HEARTBEAT, TimeUnit.MILLISECONDS);
		logger.atInfo().log("Started Japson client bound to %s.", address.getAddress().getHostName() + ":" + address.getPort());
		return this;
	}

	@Override
	public JapsonClient setAllowedAddresses(InetAddress... addesses) {
		acceptable.clear();
		acceptable.addAll(Sets.newHashSet(addesses));
		return this;
	}

	@Override
	public JapsonClient setPacketBufferSize(int buffer) {
		this.PACKET_SIZE = buffer;
		return this;
	}

	@Override
	public JapsonClient setPassword(String password) {
		this.password = password;
		return this;
	}

	/**
	 * The amount of milliseconds the heartbeat is set at, must match that of the JapsonClient.
	 * 
	 * @param heartbeat time in milliseconds.
	 * @return The JapsonClient for chaining.
	 */
	public JapsonClient setHeartbeat(long heartbeat) {
		this.HEARTBEAT = heartbeat;
		return this;
	}

	/**
	 * Will ensure that packets are only sent if heartbeats were successful.
	 * 
	 * @return The JapsonClient for chaining.
	 */
	public JapsonClient makeSureConnectionValid() {
		this.check = true;
		return this;
	}

	@Override
	public JapsonClient setTimeout(int timeout) {
		this.TIMEOUT = timeout;
		return this;
	}

	@Override
	public JapsonClient enableDebug() {
		this.debug = true;
		return this;
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	public void shutdown() {
		executor.shutdown();
	}

	public void kill() {
		executor.shutdownNow();
	}

	public <T> T sendPacket(ReturnablePacket<T> packet) throws TimeoutException, InterruptedException, ExecutionException {
		if (check && !valid && !(packet instanceof HeartbeatPacket))
			throw new TimeoutException("No connection to the server. Cancelling sending packet.");
		return super.sendPacket(address, packet, gson);
	}

	public void sendPacket(Packet packet) throws InterruptedException, ExecutionException, TimeoutException {
		super.sendPacket(address, packet, gson);
	}

}
