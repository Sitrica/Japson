package com.sitrica.japson;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
		assertNotNull(japson);
	}

}
