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
package io.github.explodingbottle.jmagicproxy.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.api.ConnectionDirective;
import io.github.explodingbottle.jmagicproxy.api.ConnectionType;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;
import io.github.explodingbottle.jmagicproxy.proxy.ssl.SSLComunicator;
import io.github.explodingbottle.jmagicproxy.socketopener.SocketOpeningTool;
import io.github.explodingbottle.jmagicproxy.socketopener.StandardSocketOpener;

/**
 * This class contains code to handle directives.
 * 
 * @author ExplodingBottle
 *
 */
public class ConnectionDirectiveHandler {

	/*
	 * This code is a way better than the draft I've wrote, oof... Before, when the
	 * socket(server-proxy) would've closed, it would've closed the
	 * socket(client-proxy) too, causing errors on client.
	 */

	private ConnectionDirective directive;
	private SocketHandlerThread handlerThread;

	private Socket referenceSocket;

	private InputStream inputStream;
	private OutputStream outputStream;

	private ProxyLogger logger;
	private boolean closed;

	private ConnectionType connectionType;

	private SimpleInputOutputPipeThread pipeThread;

	private SSLComunicator sslCommunicator;

	private List<byte[]> toflush;
	private List<Integer> offsetFlush;
	private List<Integer> lengthFlush;

	private boolean readyToFlush;

	/**
	 * Constructor for this class which takes the connection directive and the
	 * handler thread.
	 * 
	 * @param directive     The Connection Directive to process.
	 * @param handlerThread The thread which initiated the process.
	 */
	public ConnectionDirectiveHandler(ConnectionDirective directive, SocketHandlerThread handlerThread) {
		this.directive = directive;
		this.handlerThread = handlerThread;
		logger = ProxyMain.getLoggerProvider().createLogger();
		connectionType = ConnectionType.CLOSE;
		closed = false;
		toflush = Collections.synchronizedList(new ArrayList<byte[]>());
		offsetFlush = Collections.synchronizedList(new ArrayList<Integer>());
		lengthFlush = Collections.synchronizedList(new ArrayList<Integer>());
		readyToFlush = false;
	}

	/**
	 * Sets the connection type.
	 * 
	 * @param connectionType The connection type.
	 */
	public void setConnectionType(ConnectionType connectionType) {
		logger.log(LoggingLevel.INFO, "Connection type has been changed to " + connectionType);
		this.connectionType = connectionType;
	}

	/**
	 * Gets the connection type.
	 * 
	 * return The connection type.
	 */
	public ConnectionType getConnectionType() {
		return connectionType;
	}

	void signalThreadClose() {
		closeSocket();
	}

	/**
	 * Returns the connection directive.
	 * 
	 * @return The connection directive.
	 */
	public ConnectionDirective getDirective() {
		return directive;
	}

	/**
	 * Sets the connection directive.
	 * 
	 * @param directive The connection directive.
	 */
	public void setDirective(ConnectionDirective directive) {
		if (directive == null) {
			this.directive = directive;
		} else {
			if (directive.isSSL() == this.directive.isSSL()
					&& directive.isUsingFile() == this.directive.isUsingFile()) {
				this.directive = directive;
			} else {
				logger.log(LoggingLevel.WARN, "Trying to set a directive using too different methods.");
				return;
			}
		}
	}

	/**
	 * Used to write the directive related content.
	 */
	public void rewriteDirectiveLine() {
		if (directive.isUsingFile() || directive.isSSL()) {
			logger.log(LoggingLevel.WARN, "Trying to use Keep-Alive with unsupported methods.");
			return;
		}
		try {
			outputStream.write(directive.getOutcomingRequest().toHttpRequestBlock().getBytes());
		} catch (IOException e) {
			logger.log(LoggingLevel.WARN, "Failed to write directive content.", e);
		}

	}

	/**
	 * This function will open the outgoing socket or a file depending from the
	 * connection directive.
	 */
	public void openSocket() {
		if (closed) {
			logger.log(LoggingLevel.WARN,
					"Attempted to open outgoing socket on a closed connection directive handler.");
			return;
		}
		if (directive.isRemoteConnect()) {
			if (directive.isSSL()) {
				logger.log(LoggingLevel.INFO, "Opening outgoing socket for " + directive.getHost() + ":"
						+ directive.getPort() + " with SSL.");
				sslCommunicator = new SSLComunicator(handlerThread.getOutputStream(), this, directive.getHost(),
						directive.getPort());
				sslCommunicator.startConnection();
				readyToFlush = true;
				registerToWaitingQueue(null, 0, 0, true);
			} else {
				logger.log(LoggingLevel.INFO, "Opening outgoing socket for " + directive.getHost() + ":"
						+ directive.getPort() + " with request " + directive.getOutcomingRequest().toHttpRequestLine());
				SocketOpeningTool openingTool = new SocketOpeningTool(directive.getHost(), directive.getPort(),
						new StandardSocketOpener(), (s, status) -> {
							if (s == null) {
								try {
									if (!status) {
										handlerThread.getOutputStream()
												.write(new String("HTTP/1.1 504 Gateway Timeout\r\n").getBytes());
									} else {
										handlerThread.getOutputStream()
												.write(new String("HTTP/1.1 502 Bad Gateway\r\n").getBytes());
									}
									handlerThread.getOutputStream()
											.write(new String("Connection: Close\r\n\r\n").getBytes());
								} catch (IOException e) {
									logger.log(LoggingLevel.WARN, "Failed to tell the client that an error occured.",
											e);
								}
								logger.log(LoggingLevel.WARN, "Failed to open the outgoing socket.");
								closeSocket();
							} else {
								try {
									referenceSocket = s;
									inputStream = referenceSocket.getInputStream();
									outputStream = referenceSocket.getOutputStream();
									rewriteDirectiveLine();
									pipeThread = new SimpleInputOutputPipeThread(inputStream,
											handlerThread.getOutputStream(), this);
									pipeThread.start();
									logger.log(LoggingLevel.INFO, "Outgoing socket opened.");
									readyToFlush = true;
									registerToWaitingQueue(null, 0, 0, true);
								} catch (IOException e) {
									logger.log(LoggingLevel.WARN, "Failed to open the outgoing socket.", e);
									closeSocket();
								}
							}
						});
				try {

					openingTool.run();
				} catch (Exception e) {
					logger.log(LoggingLevel.WARN, "Failed to open the outgoing socket.", e);
					closeSocket();
				}
			}
		} else {
			pipeThread = new SimpleInputOutputPipeThread(null, handlerThread.getOutputStream(), this);
			pipeThread.start();
			logger.log(LoggingLevel.INFO, "Won't create a remote connection due to the nature of the directive.");
		}

	}

	private void internalFlush() {
		if (readyToFlush) {
			for (int i = 0; i < toflush.size(); i++) {
				byte[] buffer = toflush.get(i);
				int offset = offsetFlush.get(i);
				int length = lengthFlush.get(i);
				if (sslCommunicator != null) {
					sslCommunicator.feedOutput(buffer, offset, length);
				} else if (outputStream != null) {
					try {
						if (outputStream != null) {
							outputStream.write(buffer, offset, length);
						}
					} catch (IOException e) {
						logger.log(LoggingLevel.WARN, "Failed to write to the outgoing stream.", e);
					}
				}
			}
			toflush.clear();
			offsetFlush.clear();
			lengthFlush.clear();
		}
	}

	private synchronized void registerToWaitingQueue(byte[] buffer, int offset, int length, boolean flushOnly) {
		if (!flushOnly) {
			toflush.add(buffer);
			offsetFlush.add(offset);
			lengthFlush.add(length);
		}
		internalFlush();
	}

	/**
	 * This function is used to tell the outgoing stream informations.
	 * 
	 * @param buffer The buffer you want to send.
	 * @param offset The offset for the buffer.
	 * @param length The size of the buffer to read and send.
	 */
	public void feedOutput(byte[] buffer, int offset, int length) {
		byte[] copy = new byte[buffer.length];
		for (int i = 0; i < buffer.length; i++) {
			copy[i] = buffer[i];
		}
		registerToWaitingQueue(copy, offset, length, false);
	}

	/**
	 * Used to know if the handler is closed.
	 * 
	 * @return Returns if the connection directive handler is closed.
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * This function is used to close the outgoing connection.
	 */
	public void closeSocket() {
		if (!closed) {
			closed = true;
			ProxyMain.getPluginsManager().notifyDirectiveClose(directive);
			if (pipeThread != null) {
				pipeThread.interrupt();
			}
			if (sslCommunicator != null) {
				sslCommunicator.stopCommunicator();
			}
			try {
				if (referenceSocket != null) {
					referenceSocket.close();
					referenceSocket = null;
				}
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to close the socket coming from outside.", e);
			}

			if (directive.isRemoteConnect()) {
				if (directive.isSSL()) {
					logger.log(LoggingLevel.INFO, "Closed outgoing socket for " + directive.getHost() + ":"
							+ directive.getPort() + " with SSL.");
				} else {
					logger.log(LoggingLevel.INFO,
							"Closed outgoing socket for " + directive.getHost() + ":" + directive.getPort()
									+ " with request " + directive.getOutcomingRequest().toHttpRequestLine());
				}
			} else {
				logger.log(LoggingLevel.INFO, "Closed non-remote-connect handler.");
			}
			if (connectionType == ConnectionType.CLOSE) {
				logger.log(LoggingLevel.INFO, "Closing listening thread as the handler thread is in Close mode.");
				handlerThread.closeListeningSocket();
			}
		}
	}
}
