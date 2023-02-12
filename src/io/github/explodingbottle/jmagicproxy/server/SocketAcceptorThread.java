/*
 *   JMagic Proxy - A HTTP and HTTPS Proxy
 *   Copyright (C) 2023  ExplodingBottle
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.explodingbottle.jmagicproxy.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;
import io.github.explodingbottle.jmagicproxy.proxy.SocketHandlerThread;

/**
 * This Thread is responsible of a {@code ServerSocket} that will listen any
 * incoming connections.
 * 
 * @author ExplodingBottle
 *
 */
public class SocketAcceptorThread extends Thread {

	private ProxyLogger selfLogger;
	private int listenPort;
	private ServerSocket socket;
	private List<SocketHandlerThread> threads;

	/**
	 * Instantiates the thread with a port to listen.
	 * 
	 * @param listenPort The port the server socket must listen to.
	 */
	public SocketAcceptorThread(int listenPort) {
		this.listenPort = listenPort;
		threads = Collections.synchronizedList(new ArrayList<SocketHandlerThread>());
		selfLogger = ProxyMain.getLoggerProvider().createLogger();
	}

	/**
	 * Use this method to close the socket and interrupt the thread.
	 */
	public void closeServerSocket() {
		super.interrupt();
		try {
			if (socket != null)
				socket.close();
		} catch (IOException e) {
			selfLogger.log(LoggingLevel.WARN, "Failed to close the server socket.", e);
		}
	}

	/**
	 * Redefining interrupt to force use of closeServerSocket().
	 */
	public void interrupt() {
		throw new IllegalAccessError("Should not be called, use closeServerSocket() instead.");
	}

	/**
	 * Adds a SocketHandlerThread to the list. {@code threads} shouldn't be used
	 * directly to avoid thread issues.
	 * 
	 * @param sht The thread you want to register.
	 */
	public synchronized void addToList(SocketHandlerThread sht) {
		threads.add(sht);
	}

	/**
	 * Removes a SocketHandlerThread to the list. {@code threads} shouldn't be used
	 * directly to avoid thread issues.
	 * 
	 * @param sht The thread you want to unregister.
	 */
	public synchronized void removeFromList(SocketHandlerThread sht) {
		threads.remove(sht);
	}

	public void run() {
		selfLogger.log(LoggingLevel.INFO, "Starting the Server socket for port " + listenPort);
		try {
			socket = new ServerSocket(listenPort);
		} catch (IOException e) {
			selfLogger.log(LoggingLevel.FATAL, "Failed to open the Server Socket, sending shutdown message.", e);
			ProxyMain.getShutdownThread().start();
			return;
		}
		selfLogger.log(LoggingLevel.INFO, "Server socket is listening.");
		while (!interrupted()) {
			try {
				Socket accepted = socket.accept();
				SocketHandlerThread handler = new SocketHandlerThread(accepted, this);
				addToList(handler);
				handler.start();
			} catch (IOException e) {
				if (!isInterrupted())
					selfLogger.log(LoggingLevel.WARN, "Failed to accept socket.", e);
			}
		}
		selfLogger.log(LoggingLevel.INFO, "Closing handling threads.");
		ArrayList<SocketHandlerThread> t2 = new ArrayList<SocketHandlerThread>();
		threads.forEach(handler -> {
			t2.add(handler);
		});
		t2.forEach(handler -> {
			handler.closeListeningSocket();
		});
		selfLogger.log(LoggingLevel.INFO, "Handling threads were closed.");
		if (socket != null && !socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e) {
				selfLogger.log(LoggingLevel.WARN, "Failed to close the server socket.", e);
			}
		}
	}
}
