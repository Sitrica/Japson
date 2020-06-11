package com.sitrica.japson.shared;

import java.net.InetAddress;

import com.google.gson.JsonObject;

public abstract class Handler {

	private final int id;

	public Handler(int id) {
		this.id = id;
	}

	public int getID() {
		return id;
	}

	/**
	 * Handle data from an incoming packet matching the id.
	 * 
	 * @param address The InetAddress of the incoming packet.
	 * @param post The port of the address from the incoming packet.
	 * @param json The incoming JsonObject from the packet.
	 * @return JsonObject of Json for the packet on the client to read. Return null for no response.
	 */
	public abstract JsonObject handle(InetAddress address, int port, JsonObject json);

}
