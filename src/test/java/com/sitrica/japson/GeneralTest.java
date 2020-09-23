package com.sitrica.japson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.google.gson.JsonObject;
import com.sitrica.japson.shared.Packet;
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
		assertEquals(ClientTest.japson.getAddress(), ServerTest.japson.getAddress());
		String value = "testing Japson", value2 = "testing Japson 2";
		String returned = null, second = null;
		try {
			returned = ClientTest.japson.sendPacket(new ReturnablePacket<String>(0x01) {
				@Override
				public JsonObject toJson() {
					JsonObject object = new JsonObject();
					object.addProperty("value", value);
					return object;
				}
				@Override
				public String getObject(JsonObject object) {
					return object.get("value").getAsString();
				}
			});
			second = ClientTest.japson.sendPacket(new ReturnablePacket<String>(0x02) {
				@Override
				public JsonObject toJson() {
					JsonObject object = new JsonObject();
					object.addProperty("value2", value2);
					return object;
				}
				@Override
				public String getObject(JsonObject object) {
					assertTrue(!object.has("value"));
					return object.get("value2").getAsString();
				}
			});
			ClientTest.japson.sendPacket(new Packet(0x03) {
				@Override
				public JsonObject toJson() {
					JsonObject object = new JsonObject();
					object.addProperty("test", "test");
					return object;
				}
			});
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
		}
		assertNotNull(returned);
		assertEquals(returned, value);
		assertNotNull(second);
		assertEquals(second, value2);
	}

}
