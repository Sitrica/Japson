package com.sitrica.japson.client.packets;

import com.google.gson.JsonObject;
import com.sitrica.japson.shared.ReturnablePacket;

public class HeartbeatPacket extends ReturnablePacket<Boolean> {

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

	@Override
	public Boolean getObject(JsonObject object) {
		return object.has("success");
	}

}
