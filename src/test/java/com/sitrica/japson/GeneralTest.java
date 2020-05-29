package com.sitrica.japson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.sitrica.japson.shared.ReturnablePacket;

public class GeneralTest {

	@Tag("gradle")
	@Test
	@Order(3)
	public void packetSend() {
		String value = "testing Japson";
		String returned = null;
		try {
			returned = ClientTest.japson.sendPacket(new ReturnablePacket<String>((byte) 0x01) {
				@Override
				public String toJson(Gson gson) {
					object.addProperty("value", value);
					return gson.toJson(object);
				}
			});
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
		}
		assertNotNull(returned);
		assertEquals(returned, value);
	}

}
