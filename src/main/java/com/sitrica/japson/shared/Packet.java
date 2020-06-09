package com.sitrica.japson.shared;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public abstract class Packet {

	protected static final JsonObject object = new JsonObject();
	private final int id;

	public Packet(int id) {
		this.id = id;
	}

	public int getID() {
		return id;
	}

	public abstract String toJson(Gson gson);

}
