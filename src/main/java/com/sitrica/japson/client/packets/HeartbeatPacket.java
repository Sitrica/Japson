package com.sitrica.japson.client.packets;

import com.google.gson.JsonObject;
import com.sitrica.japson.shared.Packet;

public class HeartbeatPacket extends Packet {

	private final String password;
	private final int port;

	public HeartbeatPacket(String password, int port) {
		super(0x00);
		this.password = password;
		this.port = port;
	}

	@Override
	public JsonObject toJson() {
		JsonObject object = new JsonObject();
		if (password != null)
			object.addProperty("password", password);
		object.addProperty("port", port);
		return object;
	}

}
