package com.sitrica.japson.shared;

import java.lang.reflect.Type;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class ReturnablePacket<T> extends Packet {

	private JsonElement returned;

	public ReturnablePacket(byte id) {
		super(id);
	}

	public void setReturnedJsonElement(JsonElement returned) {
		this.returned = returned;
	}

	public JsonElement getReturnedJsonElement() {
		return returned;
	}

	public abstract T getObject(JsonObject object);

	@SuppressWarnings("serial")
	public Type getType() {
		return new TypeToken<T>(getClass()){}.getType();
	}

}
