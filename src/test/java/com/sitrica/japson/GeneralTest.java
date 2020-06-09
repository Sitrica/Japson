package com.sitrica.japson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sitrica.japson.shared.ReturnablePacket;

@TestMethodOrder(OrderAnnotation.class)
public class GeneralTest {

	@Test
	@Order(1)
	public void startServer() {
		ServerTest.setupServer();
	}

	@Test
	@Order(2)
	public void startClient() {
		ClientTest.setupClient();
	}

	@Test
	@Order(3)
	public void sendPacket() {
		String value = "testing Japson";
		String returned = null;
		try {
			returned = ClientTest.japson.sendPacket(new ReturnablePacket<String>(0x01) {
				@Override
				public String toJson(Gson gson) {
					object.addProperty("value", value);
					return gson.toJson(object);
				}
				@Override
				public String getObject(JsonObject object) {
					return object.get("value").getAsString();
				}
			});
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
		}
		assertNotNull(returned);
		assertEquals(returned, value);
		ClientTest.japson.getLogger().atInfo().log("All tests were successful! Returned value was as expected: %s", returned);
	}

}
