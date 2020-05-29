package com.sitrica.japson;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.sitrica.japson.server.JapsonServer;
import com.sitrica.japson.shared.Handler;

public class ServerTest {

	static JapsonServer japson;

	@Tag("gradle")
	@Test
	@Order(1)
	public void setupServer() {
		try {
			japson = new JapsonServer(1337);
			japson.enableDebug();
			japson.registerHandlers(new Handler((byte)0x01) {
				@Override
				public String handle(InetAddress address, int port, JsonObject object) {
					assertNotNull(object);
					assertTrue(object.has("value"));
					assertEquals(port, 1337);
					assertEquals(address, japson.getAddress());
					return object.get("value").getAsString();
				}
			});
		} catch (UnknownHostException | SocketException e) {
			e.printStackTrace();
		}
		assertNotNull(japson);
		System.out.println("Server started on " + japson.getAddress().getHostAddress() + ":" + japson.getPort());
	}

}
