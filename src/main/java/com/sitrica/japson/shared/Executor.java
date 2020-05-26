package com.sitrica.japson.shared;

import java.net.InetAddress;

import com.google.gson.JsonObject;

public abstract class Executor extends Handler {

	public Executor(byte id) {
		super(id);
	}

	/**
	 * A void executor used from an incoming packet matching the id.
	 * 
	 * @param address The InetAddress of the incoming packet.
	 * @param post The port of the address from the incoming packet.
	 * @param json The incoming JsonObject from the packet.
	 * @return String of Json for the packet on the client to read. Return null for no response.
	 */
	public abstract void execute(InetAddress address, int port, JsonObject json);

	@Override
	public String handle(InetAddress address, int port, JsonObject json) {
		execute(address, port, json);
		return null;
	}

}
