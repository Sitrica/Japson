package com.sitrica.japson.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sitrica.japson.client.packets.HeartbeatPacket;
import com.sitrica.japson.shared.Japson;
import com.sitrica.japson.shared.Packet;
import com.sitrica.japson.shared.ReceiverFuture;
import com.sitrica.japson.shared.ReturnablePacket;

public class JapsonClient extends Japson {

	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	protected long HEARTBEAT = 1000L, DELAY = 1000L, EXPIRY = 10; // EXPIRY in minutes, rest in milliseconds.

	private final InetAddress address;
	private final Gson gson;
	private final int port;

	public JapsonClient(int port, String identification) throws UnknownHostException {
		this(InetAddress.getLocalHost(), port, identification);
	}

	public JapsonClient(String host, int port, String identification) throws UnknownHostException {
		this(InetAddress.getByName(host), port, identification);
	}

	public JapsonClient(InetAddress address, int port, String identification) {
		this(address, port, identification, new GsonBuilder()
				.enableComplexMapKeySerialization()
				.serializeNulls()
				.create());
	}

	public JapsonClient(int port, String identification, Gson gson) throws UnknownHostException {
		this(InetAddress.getLocalHost(), port, identification, gson);
	}

	public JapsonClient(String host, int port, String identification, Gson gson) throws UnknownHostException {
		this(InetAddress.getByName(host), port, identification, gson);
	}

	public JapsonClient(InetAddress address, int port, String identification, Gson gson) {
		this.address = address;
		this.port = port;
		this.gson = gson;
		HeartbeatPacket packet = new HeartbeatPacket(identification);
		packets.add(packet);
		executor.scheduleAtFixedRate(() -> sendPacket(packet), DELAY, HEARTBEAT, TimeUnit.MILLISECONDS);
	}

	/**
	 * The amount of minutes to wait before forgetting about a connection.
	 * 
	 * @param expiry time in minutes.
	 * @return The JapsonClient for chaining.
	 */
	public JapsonClient setExpiryMinutes(long expiry) {
		this.EXPIRY = expiry;
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

	public void shutdown() {
		executor.shutdown();
	}

	public void kill() {
		executor.shutdownNow();
	}

	public <T> T sendPacket(ReturnablePacket<T> japsonPacket) throws InterruptedException, ExecutionException, TimeoutException {
		try (DatagramSocket socket = new DatagramSocket()) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.write(japsonPacket.getID());
			out.writeUTF(japsonPacket.toJson(gson));
			byte[] buf = out.toByteArray();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
			socket.send(packet);
			ByteArrayDataInput input = new ReceiverFuture(socket)
					.create(new DatagramPacket(buf, buf.length))
					.get(HEARTBEAT * 5, TimeUnit.MILLISECONDS);
			if (input == null) {
				logger.atSevere().log("Packet with id %s returned null or an incorrect readable object for Japson", japsonPacket.getID());
				return null;
			}
			byte id = input.readByte();
			if (id != japsonPacket.getID()) {
				logger.atSevere().log("Sent returnable packet with id %s, but did not get correct packet id returned", japsonPacket.getID());
				return null;
			}
			String json = input.readUTF();
			if (debug)
				logger.atInfo().log("Sent returnable packet with id %s and recieved \n%d", japsonPacket.getID(), json);
			return gson.fromJson(json, japsonPacket.getType());
		} catch (SocketException socketException) {
			logger.atSevere().withCause(socketException)
					.atMostEvery(15, TimeUnit.SECONDS)
					.log("Socket error: " + socketException.getMessage());
		} catch (IOException exception) {
			logger.atSevere().withCause(exception)
					.atMostEvery(15, TimeUnit.SECONDS)
					.log("IO error: " + exception.getMessage());
		}
		return null;
	}

	public void sendPacket(Packet japsonPacket) {
		try (DatagramSocket socket = new DatagramSocket()) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.write(japsonPacket.getID());
			out.writeUTF(japsonPacket.toJson(gson));
			byte[] buf = out.toByteArray();
			socket.send(new DatagramPacket(buf, buf.length, address, port));
			if (debug)
				logger.atInfo().log("Sent non-returnable packet with id %s", japsonPacket.getID());
		} catch (SocketException socketException) {
			logger.atSevere().withCause(socketException)
					.atMostEvery(15, TimeUnit.SECONDS)
					.log("Socket error: " + socketException.getMessage());
		} catch (IOException exception) {
			logger.atSevere().withCause(exception)
					.atMostEvery(15, TimeUnit.SECONDS)
					.log("IO error: " + exception.getMessage());
		}
	}

}
