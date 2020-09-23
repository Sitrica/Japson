package com.sitrica.japson.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutionException;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sitrica.japson.shared.ReceiverFuture;

public class SocketHandler implements Runnable {

	private final DatagramSocket socket;
	private final JapsonServer japson;
	private final int packetSize;

	private boolean running = true;

	public SocketHandler(int packetSize, JapsonServer japson, DatagramSocket socket) {
		this.packetSize = packetSize;
		this.japson = japson;
		this.socket = socket;
	}

	@Override
	public void run() {
		while (running) {
			if (socket.isClosed())
				break;
			try {
				byte[] buf = new byte[packetSize];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				ByteArrayDataInput input = new ReceiverFuture(japson.getLogger(), japson, socket)
						.create(packet)
						.get();
				if (input == null) {
					japson.getLogger().atSevere().log("Packet received was null or an incorrect readable object for Japson");
					return;
				}
				int id = input.readInt();
				String data = input.readUTF();
				if (data == null) {
					japson.getLogger().atSevere().log("Received packet with id %s and the json was null.", id);
					return;
				}
				if (japson.isDebug() && (japson.getIgnoredPackets().isEmpty() || !japson.getIgnoredPackets().contains(id)))
					japson.getLogger().atInfo().log("Received packet with id %s and data %s", id, data);
				// Handle
				JsonObject object = JsonParser.parseString(data).getAsJsonObject();
				japson.getHandlers().stream()
						.filter(handler -> handler.getID() == id)
						.map(handler -> handler.handle(packet.getAddress(), packet.getPort(), object))
						.filter(jsonObject -> jsonObject != null)
						.findFirst()
						.ifPresent(jsonObject -> {
							ByteArrayDataOutput out = ByteStreams.newDataOutput();
							String json = japson.getGson().toJson(jsonObject);
							out.writeInt(id);
							out.writeUTF(json);
							byte[] returnBuf = out.toByteArray();
							try {
								if (socket.isClosed())
									return;
								socket.send(new DatagramPacket(returnBuf, returnBuf.length, packet.getAddress(), packet.getPort()));
								if (japson.isDebug())
									japson.getLogger().atInfo().log("Returning data %s as packet id %s", json, id);
							} catch (IOException e) {
								japson.getLogger().atSevere().withCause(e).log("Failed to send return data %s.", json);
							}
						});
			} catch (InterruptedException | ExecutionException e) {
				japson.getListeners().forEach(listener -> listener.onShutdown());
			}
		}
	}

	public void stop() {
		running = false;
	}

}
