package com.sitrica.japson.shared;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Sets;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

public abstract class Japson {

	protected final FluentLogger logger = FluentLogger.forEnclosingClass();
	protected final Set<InetAddress> acceptable = new HashSet<>();
	protected final Set<Handler> handlers = new HashSet<>();

	protected int PACKET_SIZE = 1024; // UDP standard
	protected int TIMEOUT = 2000; // milliseconds
	protected String password;
	protected boolean debug;

	public Japson registerHandlers(Handler... handlers) {
		Sets.newHashSet(handlers).stream()
				.filter(handler -> !this.handlers.stream().anyMatch(existing -> existing.getID() == handler.getID()))
				.forEach(handler -> this.handlers.add(handler));
		return this;
	}

	public abstract Japson setAllowedAddresses(InetAddress... addesses);

	public abstract Japson setPacketBufferSize(int buffer);

	public abstract Japson setPassword(String password);

	/**
	 * Set the timeout in milliseconds to wait for returnable packets.
	 * 
	 * @param timeout The time in Milliseconds to set as the timeout.
	 * @return Japson instance for chaining.
	 */
	public abstract Japson setTimeout(int timeout);

	public abstract Japson enableDebug();

	public boolean passwordMatches(String password) {
		return this.password.equals(password);
	}

	public boolean isAllowed(InetAddress address) {
		if (acceptable.isEmpty())
			return true;
		return acceptable.contains(address);
	}

	public Set<Handler> getHandlers() {
		return handlers;
	}

	public FluentLogger getLogger() {
		return logger;
	}

	public boolean hasPassword() {
		return password != null;
	}

	public boolean isDebug() {
		return debug;
	}

	public <T> T sendPacket(InetAddress address, int port, ReturnablePacket<T> packet) throws TimeoutException, InterruptedException, ExecutionException {
		return sendPacket(address, port, packet, new GsonBuilder()
				.enableComplexMapKeySerialization()
				.serializeNulls()
				.setLenient()
				.create());
	}

	public <T> T sendPacket(InetAddress address, int port, ReturnablePacket<T> japsonPacket, Gson gson) throws TimeoutException, InterruptedException, ExecutionException {
		return CompletableFuture.supplyAsync(() -> {
			try (DatagramSocket socket = new DatagramSocket()) {
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeInt(japsonPacket.getID());
				out.writeUTF(gson.toJson(japsonPacket.toJson()));
				byte[] buf = out.toByteArray();
				socket.setSoTimeout(TIMEOUT);
				socket.send(new DatagramPacket(buf, buf.length, address, port));
				// Reset the byte buffer
				buf = new byte[PACKET_SIZE];
				ByteArrayDataInput input = new ReceiverFuture(logger, this, socket)
						.create(new DatagramPacket(buf, buf.length))
						.get(TIMEOUT, TimeUnit.MILLISECONDS);
				socket.close();
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
			} catch (InterruptedException | ExecutionException | TimeoutException exception) {
				// Already handled seperate.
			}
			return null;
		}).get(TIMEOUT, TimeUnit.MILLISECONDS);
	}

	public void sendPacket(InetAddress address, int port, Packet japsonPacket) throws InterruptedException, ExecutionException, TimeoutException {
		sendPacket(address, port, japsonPacket, new GsonBuilder()
				.enableComplexMapKeySerialization()
				.serializeNulls()
				.setLenient()
				.create());
	}

	public void sendPacket(InetAddress address, int port, Packet japsonPacket, Gson gson) throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture.runAsync(() -> {
			try (DatagramSocket socket = new DatagramSocket()) {
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeInt(japsonPacket.getID());
				out.writeUTF(gson.toJson(japsonPacket.toJson()));
				byte[] buf = out.toByteArray();
				socket.setSoTimeout(TIMEOUT);
				socket.send(new DatagramPacket(buf, buf.length, address, port));
				if (debug)
					logger.atInfo().log("Sent non-returnable packet with id %s", japsonPacket.getID());
				socket.close();
			} catch (SocketException socketException) {
				logger.atSevere().withCause(socketException)
						.atMostEvery(15, TimeUnit.SECONDS)
						.log("Socket error: " + socketException.getMessage());
			} catch (IOException exception) {
				logger.atSevere().withCause(exception)
						.atMostEvery(15, TimeUnit.SECONDS)
						.log("IO error: " + exception.getMessage());
			}
		}).get(TIMEOUT, TimeUnit.MILLISECONDS);
	}

}
