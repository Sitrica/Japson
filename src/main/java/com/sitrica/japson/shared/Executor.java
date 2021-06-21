package com.sitrica.japson.shared;

import java.net.InetSocketAddress;

import com.google.gson.JsonObject;

public abstract class Executor extends Handler {

	public Executor(int id) {
		super(id);
	}

	/**
	 * A void executor used from an incoming packet matching the id.
	 * 
	 * @param address The InetSocketAddress of the incoming packet.
	 * @param json The incoming JsonObject from the packet.
	 * @return String of Json for the packet on the client to read. Return null for no response.
	 */
	public abstract void execute(InetSocketAddress address, JsonObject json);

	@Override
	public final JsonObject handle(InetSocketAddress address, JsonObject json) {
		execute(address, json);
		return null;
	}

}
