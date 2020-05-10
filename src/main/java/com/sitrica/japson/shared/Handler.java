package com.sitrica.japson.shared;

import java.net.InetAddress;

import com.google.gson.JsonObject;

public abstract class Handler {

	private final byte id;

	public Handler(byte id) {
		this.id = id;
	}

	public byte getID() {
		return id;
	}

	/**
	 * Handle data from an incoming packet matching the id.
	 * 
	 * @param json The incoming JsonObject from the packet.
	 * @return String of Json for the packet on the client to read. Return null for no response.
	 */
	public abstract String handle(InetAddress address, int port, JsonObject json);

}
