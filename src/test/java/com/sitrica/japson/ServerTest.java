package com.sitrica.japson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sitrica.japson.server.JapsonServer;
import com.sitrica.japson.shared.Handler;

public class ServerTest {

	static JapsonServer japson;

	public static void setupServer() {
		Gson gson = new Gson();
		try {
			japson = new JapsonServer(1337);
			japson.enableDebug();
			japson.registerHandlers(new Handler(0x01) {
				@Override
				public String handle(InetAddress address, int port, JsonObject object) {
					assertNotNull(object);
					assertTrue(object.has("value"));
					assertEquals(address, japson.getAddress());
					String value = object.get("value").getAsString();
					assertEquals(value, "testing Japson");
					JsonObject returnJson = new JsonObject();
					returnJson.addProperty("value", value);
					return gson.toJson(returnJson);
				}
			});
		} catch (UnknownHostException | SocketException e) {
			e.printStackTrace();
		}
		assertNotNull(japson);
		System.out.println("Server started on " + japson.getAddress().getHostAddress() + ":" + japson.getPort());
	}

}
