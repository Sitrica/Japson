package com.sitrica.japson.shared;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.flogger.FluentLogger;

public abstract class Japson {

	protected static final FluentLogger logger = FluentLogger.forEnclosingClass();
	protected final Set<InetAddress> acceptable = new HashSet<>();
	protected final Set<Handler> handlers = new HashSet<>();

	protected final InetAddress address;
	protected final int port;

	protected String password;
	protected boolean debug;

	protected Japson(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}

	public Japson registerHandlers(Handler... handlers) {
		Sets.newHashSet(handlers).stream()
				.filter(handler -> !this.handlers.stream().anyMatch(existing -> existing.getID() == handler.getID()))
				.forEach(handler -> this.handlers.add(handler));
		return this;
	}

	public Japson setAllowedAddresses(InetAddress... addesses) {
		acceptable.clear();
		acceptable.addAll(Sets.newHashSet(addesses));
		return this;
	}

	public boolean passwordMatches(String password) {
		return this.password.equals(password);
	}

	public boolean isAllowed(InetAddress address) {
		if (acceptable.isEmpty())
			return true;
		return acceptable.contains(address);
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Set<Handler> getHandlers() {
		return handlers;
	}

	public FluentLogger getLogger() {
		return logger;
	}

	public InetAddress getAddress() {
		return address;
	}

	public boolean hasPassword() {
		return password != null;
	}

	public Japson enableDebug() {
		this.debug = true;
		return this;
	}

	public boolean isDebug() {
		return debug;
	}

	public int getPort() {
		return port;
	}

}
