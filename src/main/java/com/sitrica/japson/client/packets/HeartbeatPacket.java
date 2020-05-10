package com.sitrica.japson.client.packets;

import com.google.gson.Gson;
import com.sitrica.japson.shared.Packet;

public class HeartbeatPacket extends Packet {

	private final String identification;

	public HeartbeatPacket(String identification) {
		super((byte)0x00);
		this.identification = identification;
	}

	@Override
	public String toJson(Gson gson) {
		object.addProperty("identification", identification);
		return gson.toJson(object);
	}

	/**
	 * @return The defined name to identify this Japson client instance.
	 */
	public String getIdentification() {
		return identification;
	}

}
