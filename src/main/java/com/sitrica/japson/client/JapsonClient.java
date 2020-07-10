package com.sitrica.japson.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.flogger.FluentLogger;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.sitrica.japson.client.packets.HeartbeatPacket;
import com.sitrica.japson.shared.Japson;
import com.sitrica.japson.shared.Packet;
import com.sitrica.japson.shared.ReceiverFuture;
import com.sitrica.japson.shared.ReturnablePacket;

public class JapsonClient extends Japson {

	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	protected long HEARTBEAT = 1000L, DELAY = 1000L; // in milliseconds.

	private final Gson gson;

	public JapsonClient(int port) throws UnknownHostException {
		this(InetAddress.getLocalHost(), port);
	}

	public JapsonClient(String host, int port) throws UnknownHostException {
		this(InetAddress.getByName(host), port);
	}

	public JapsonClient(InetAddress address, int port) {
		this(address, port, new GsonBuilder()
				.enableComplexMapKeySerialization()
				.serializeNulls()
				.setLenient()
				.create());
	}

	public JapsonClient(int port, Gson gson) throws UnknownHostException {
		this(InetAddress.getLocalHost(), port, gson);
	}

	public JapsonClient(String host, int port, Gson gson) throws UnknownHostException {
		this(InetAddress.getByName(host), port, gson);
	}

	public JapsonClient(InetAddress address, int port, Gson gson) {
		super(address, port);
		this.gson = gson;
		HeartbeatPacket packet = new HeartbeatPacket(password, port);
		executor.scheduleAtFixedRate(() -> sendPacket(packet), DELAY, HEARTBEAT, TimeUnit.MILLISECONDS);
		if (debug)
			logger.atInfo().log("Started Japson client bound to %s.", address.getHostAddress() + ":" + port);
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

	public FluentLogger getLogger() {
		return logger;
	}

	public void shutdown() {
		executor.shutdown();
	}

	public void kill() {
		executor.shutdownNow();
	}

	public <T> T sendPacket(ReturnablePacket<T> japsonPacket) throws TimeoutException, InterruptedException, ExecutionException {
		return CompletableFuture.supplyAsync(() -> {
			try (DatagramSocket socket = new DatagramSocket()) {
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeInt(japsonPacket.getID());
				out.writeUTF(gson.toJson(japsonPacket.toJson()));
				byte[] buf = out.toByteArray();
				DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
				socket.send(packet);
				// Reset the byte buffer
				buf = new byte[PACKET_SIZE];
				ByteArrayDataInput input = new ReceiverFuture(logger, this, socket)
						.create(new DatagramPacket(buf, buf.length))
						.get();
				if (input == null) {
					logger.atSevere().log("Packet with id %s returned null or an incorrect readable object for Japson", japsonPacket.getID());
					return null;
				}
				int id = input.readInt();
				if (id != japsonPacket.getID()) {
					logger.atSevere().log("Sent returnable packet with id %s, but did not get correct packet id returned", japsonPacket.getID());
					return null;
				}
				String json = input.readUTF();
				if (debug)
					logger.atInfo().log("Sent returnable packet with id %s and recieved %s", japsonPacket.getID(), json);
				return japsonPacket.getObject(JsonParser.parseString(json).getAsJsonObject());
			} catch (SocketException socketException) {
				logger.atSevere().withCause(socketException)
						.atMostEvery(15, TimeUnit.SECONDS)
						.log("Socket error: " + socketException.getMessage());
			} catch (IOException exception) {
				logger.atSevere().withCause(exception)
						.atMostEvery(15, TimeUnit.SECONDS)
						.log("IO error: " + exception.getMessage());
			} catch (InterruptedException | ExecutionException exception) {
				logger.atSevere().withCause(exception)
						.atMostEvery(15, TimeUnit.SECONDS)
						.log("Timeout: " + exception.getMessage());
			}
			return null;
		}).get(HEARTBEAT * 3, TimeUnit.MILLISECONDS);
	}

	public void sendPacket(Packet japsonPacket) {
		CompletableFuture.runAsync(() -> {
			try (DatagramSocket socket = new DatagramSocket()) {
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeInt(japsonPacket.getID());
				out.writeUTF(gson.toJson(japsonPacket.toJson()));
				byte[] buf = out.toByteArray();
				socket.setSoTimeout((int)(HEARTBEAT * 3));
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
		});
	}

}
