package com.sitrica.japson.shared;

import com.google.gson.JsonObject;

public abstract class Packet {

	private final int id;

	public Packet(int id) {
		this.id = id;
	}

	public int getID() {
		return id;
	}

	public abstract JsonObject toJson();

}
