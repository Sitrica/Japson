package com.sitrica.japson.shared;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.flogger.FluentLogger;
import com.sitrica.japson.server.Listener;

public abstract class Japson {

	protected static final FluentLogger logger = FluentLogger.forEnclosingClass();
	protected final Set<Listener> listeners = new HashSet<>();
	protected final Set<Handler> handlers = new HashSet<>();
	protected final Set<Packet> packets = new HashSet<>();

	protected boolean debug;

	public Japson registerListeners(Listener... listeners) {
		this.listeners.addAll(Sets.newHashSet(listeners));
		return this;
	}

	public Japson registerListener(Listener listener) {
		return registerListeners(listener);
	}

	public Japson registerHandlers(Handler... handlers) {
		Sets.newHashSet(handlers).stream()
				.filter(handler -> !this.handlers.stream().anyMatch(existing -> existing.getID() == handler.getID()))
				.forEach(handler -> this.handlers.add(handler));
		return this;
	}

	public Japson registerPackets(Packet... packets) {
		Sets.newHashSet(packets).stream()
				.filter(packet -> !this.packets.stream().anyMatch(existing -> existing.getID() == packet.getID()))
				.forEach(packet -> this.packets.add(packet));
		return this;
	}

	public Set<Listener> getListeners() {
		return listeners;
	}

	public Set<Handler> getHandlers() {
		return handlers;
	}

	public FluentLogger getLogger() {
		return logger;
	}

	public Japson enableDebug() {
		this.debug = true;
		return this;
	}

	public boolean isDebug() {
		return debug;
	}

}
