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
package io.github.explodingbottle.jmagicproxy.socketopener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;

/**
 * This class will create a socket by resolving the IP. It is now here because
 * it appeared that some issues were occurring with
 * {@code InetAddress.getByName}.
 * 
 * @author ExplodingBottle
 *
 */
public class SocketOpeningTool {

	private String host;
	private int port;
	private SocketOpener opener;

	private boolean hasBeenFound = false;
	private Consumer<Socket> callback;

	private boolean callBackCalled = false;

	private List<SocketOpeningThread> threads;

	private ProxyLogger logger;

	/**
	 * Builds an opening tool using the parameters.
	 * 
	 * @param host     The host name to try to connect to.
	 * @param port     The port to try to connect to.
	 * @param opener   The opener that will be used in order to open a Socket.
	 * @param callback The callback that will be ran once the socket is ready. Will
	 *                 obtain a socket or {@code null} if every sockets has failed
	 *                 to connect.
	 */
	public SocketOpeningTool(String host, int port, SocketOpener opener, Consumer<Socket> callback) {
		this.host = host;
		this.port = port;
		this.opener = opener;
		this.callback = callback;
		this.logger = ProxyMain.getLoggerProvider().createLogger();
		this.threads = Collections.synchronizedList(new ArrayList<SocketOpeningThread>());
	}

	/**
	 * This method will start the test.
	 * 
	 */
	public void run() {
		InetAddress[] resolvedAddresses;
		try {
			resolvedAddresses = InetAddress.getAllByName(host);
		} catch (UnknownHostException e) {
			logger.log(LoggingLevel.WARN, "Failed to get IPs of an unknown host.", e);
			callback.accept(null);
			return;
		}
		for (InetAddress address : resolvedAddresses) {
			SocketOpeningThread thread = new SocketOpeningThread(address, port, this, opener);
			threads.add(thread);
			thread.start();
		}
		// If someone has a proper fix, please do a Pull Request. This code is
		// technically bad.
		while (!callBackCalled) {
			Thread.yield();
		}
	}

	/**
	 * This function will be called by a started check thread in order to send the
	 * first opened socket.
	 * 
	 * @param received The socket that has been the first opened.
	 * @param thread   The thread from which comes the request.
	 */
	void receiveSocket(Socket received, SocketOpeningThread thread) {
		threads.remove(thread);
		if (received != null) {
			if (!hasBeenFound) {
				hasBeenFound = true;
				logger.log(LoggingLevel.INFO, "We found a socket for connection " + host + ":" + port + " for IP "
						+ received.getInetAddress() + ".");
				callback.accept(received);
				callBackCalled = true;
			} else {
				try {
					received.close();
				} catch (IOException e) {
					logger.log(LoggingLevel.WARN, "Failed to close a connection.", e);
				}
			}
		}
		if (!hasBeenFound && threads.size() == 0) {
			logger.log(LoggingLevel.WARN, "We found NO socket for connection " + host + ":" + port + ".");
			callback.accept(null);
		}
	}

}
