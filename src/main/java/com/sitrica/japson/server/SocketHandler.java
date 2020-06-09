package com.sitrica.japson.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sitrica.japson.shared.ReceiverFuture;

public class SocketHandler implements Runnable {

	private static final ExecutorService executor = Executors.newSingleThreadExecutor();
	private static final int UDP_VALID_PACKET_SIZE  = 1024;

	private final DatagramSocket socket;
	private final JapsonServer japson;

	private boolean running = true;

	public SocketHandler(JapsonServer japson, DatagramSocket socket) {
		this.japson = japson;
		this.socket = socket;
	}

	void shutdown() {
		executor.shutdown();
		running = false;
	}

	void kill() {
		executor.shutdownNow();
		running = false;
	}

	@Override
	public void run() {
		while (running) {
			try {
				byte[] buf = new byte[UDP_VALID_PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				ByteArrayDataInput input = new ReceiverFuture(japson.getLogger(), japson, socket)
						.create(packet)
						.get();
				if (input == null) {
					japson.getLogger().atSevere().log("Packet received was null or an incorrect readable object for Japson");
					return;
				}
				int id = input.readInt();
				String json = input.readUTF();
				if (json == null) {
					japson.getLogger().atSevere().log("Recieved packet with id %s and the json was null.", id);
					return;
				}
				if (japson.isDebug())
					japson.getLogger().atInfo().log("Recieved packet with id %s and data %s", id, json);
				// Handle
				JsonObject object = JsonParser.parseString(json).getAsJsonObject();
				japson.getHandlers().stream()
						.filter(handler -> handler.getID() == id)
						.map(handler -> handler.handle(packet.getAddress(), packet.getPort(), object))
						.filter(data -> data != null)
						.findFirst()
						.ifPresent(data -> {
							ByteArrayDataOutput out = ByteStreams.newDataOutput();
							out.writeInt(id);
							out.writeUTF(data);
							byte[] returnBuf = out.toByteArray();
							try {
								socket.send(new DatagramPacket(returnBuf, returnBuf.length, packet.getAddress(), packet.getPort()));
							} catch (IOException e) {
								japson.getLogger().atSevere().withCause(e).log("Failed to send return data %s.", data);
							}
						});
			} catch (InterruptedException | ExecutionException e) {
				japson.getListeners().forEach(listener -> listener.onShutdown());
			}
		}
	}

}
