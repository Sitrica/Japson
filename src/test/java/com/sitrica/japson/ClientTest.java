package com.sitrica.japson;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.UnknownHostException;

import com.sitrica.japson.client.JapsonClient;

public class ClientTest {

	static JapsonClient japson;

	public static void setupClient() {
		try {
			japson = new JapsonClient(1337)
					.setPassword("test-password")
					.makeSureConnectionValid()
					.enableDebug()
					.start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		assertTrue(japson.getAddress().getPort() == 1337);
		assertTrue(japson.hasPassword());
		assertTrue(japson.passwordMatches("test-password"));
		assertNotNull(japson);
	}

}
