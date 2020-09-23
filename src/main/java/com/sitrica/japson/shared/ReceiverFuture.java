package com.sitrica.japson.shared;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.flogger.FluentLogger;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

public class ReceiverFuture extends CompletableFuture<ByteArrayDataInput> {

	private final DatagramSocket socket;
	private final FluentLogger logger;
	private final Japson japson;

	public ReceiverFuture(FluentLogger logger, Japson japson, DatagramSocket socket) {
		this.logger = logger;
		this.socket = socket;
		this.japson = japson;
	}

	public CompletableFuture<ByteArrayDataInput> create(DatagramPacket packet) {
		return CompletableFuture.supplyAsync(() -> {
			while (true) {
				if (socket.isClosed())
					return null;
				try {
					socket.receive(packet);
					byte[] data = packet.getData();
					if (data == null || data.length == 0) {
						if (japson.isDebug())
							logger.atInfo().log("Recieved a null packet");
						continue;
					}
					if (!japson.isAllowed(packet.getAddress())) {
						logger.atSevere()
								.atMostEvery(5, TimeUnit.SECONDS)
								.log("Recieved a packet from %s but it was not on the address whitelist.", packet.getAddress().getHostAddress());
						continue;
					}
					return ByteStreams.newDataInput(data);
				} catch (IOException e) {}
			}
		});
	}

}
