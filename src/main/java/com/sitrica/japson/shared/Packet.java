package com.sitrica.japson.shared;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public abstract class Packet {

	protected static final JsonObject object = new JsonObject();
	private final byte id;

	public Packet(byte id) {
		this.id = id;
	}

	public byte getID() {
		return id;
	}

	public abstract String toJson(Gson gson);

}
