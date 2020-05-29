package com.sitrica.japson;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.UnknownHostException;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.sitrica.japson.client.JapsonClient;

public class ClientTest {

	static JapsonClient japson;

	@Tag("gradle")
	@Test
	@Order(2)
	public void setupClient() {
		try {
			japson = new JapsonClient(1337);
			japson.enableDebug();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		assertNotNull(japson);
		System.out.println("Client searching on " + japson.getAddress().getHostAddress() + ":" + japson.getPort());
	}

}
