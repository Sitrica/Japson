package com.sitrica.japson.client.packets;

import com.google.gson.Gson;
import com.sitrica.japson.shared.Packet;

public class HeartbeatPacket extends Packet {

	public HeartbeatPacket() {
		super((byte)0x00);
	}

	@Override
	public String toJson(Gson gson) {
		return gson.toJson(object);
	}

}
