package com.sitrica.japson.shared;

import com.google.gson.Gson;

public abstract class Packet {

	private final int id;

	public Packet(int id) {
		this.id = id;
	}

	public int getID() {
		return id;
	}

	public abstract String toJson(Gson gson);

}
