package com.sitrica.japson.server;

import com.sitrica.japson.server.Connections.JapsonConnection;

public interface Listener {

	public void onReacquiredCommunication(JapsonConnection connection);

	public void onAcquiredCommunication(JapsonConnection connection);

	public void onUnresponsive(JapsonConnection connection);

	public void onDisconnect(JapsonConnection connection);

	public void onHeartbeat(JapsonConnection connection);

	public void onForget(JapsonConnection connection);

	public void onShutdown();

}
