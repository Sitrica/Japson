package com.sitrica.japson.shared;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.CompletableFuture;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

public class ReceiverFuture extends CompletableFuture<ByteArrayDataInput> {

	private final DatagramSocket socket;

	public ReceiverFuture(DatagramSocket socket) {
		this.socket = socket;
	}

	public CompletableFuture<ByteArrayDataInput> create(DatagramPacket packet) {
		return CompletableFuture.supplyAsync(() -> {
			String json = null;
			while (json == null) {
				try {
					socket.receive(packet);
					byte[] data = packet.getData();
					if (data == null || data.length == 0)
						continue;
					return ByteStreams.newDataInput(data);
				} catch (IOException e) {}
			}
			return null;
		});
	}

}
