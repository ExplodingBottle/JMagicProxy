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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.api.ConnectionDirective;
import io.github.explodingbottle.jmagicproxy.api.ConnectionType;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;
import io.github.explodingbottle.jmagicproxy.proxy.ssl.SSLComunicator;

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
			outputStream.write((directive.getOutcomingRequest().toHttpRequestLine() + "\r\n").getBytes());
			directive.getOutcomingRequest().getHeaders().forEach((hKey, hVal) -> {
				try {
					outputStream.write((hKey + ": " + hVal + "\r\n").getBytes());
				} catch (IOException e) {
					logger.log(LoggingLevel.WARN, "Failed to write a header in the ForEach loop.", e);
				}
			});
			outputStream.write("\r\n".getBytes());
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
		if (!directive.isUsingFile()) {
			if (directive.isSSL()) {
				logger.log(LoggingLevel.INFO, "Opening outgoing socket for " + directive.getHost() + ":"
						+ directive.getPort() + " with SSL.");
				sslCommunicator = new SSLComunicator(handlerThread.getOutputStream(), this, directive.getHost(), directive.getPort());
				sslCommunicator.startConnection();

			} else {
				logger.log(LoggingLevel.INFO, "Opening outgoing socket for " + directive.getHost() + ":"
						+ directive.getPort() + " with request " + directive.getOutcomingRequest().toHttpRequestLine());
				try {
					referenceSocket = new Socket(InetAddress.getByName(directive.getHost()), directive.getPort());
					inputStream = referenceSocket.getInputStream();
					outputStream = referenceSocket.getOutputStream();
					rewriteDirectiveLine();
					pipeThread = new SimpleInputOutputPipeThread(inputStream, handlerThread.getOutputStream(), this);
					pipeThread.start();
					logger.log(LoggingLevel.INFO, "Outgoing socket opened.");
				} catch (Exception e) {
					logger.log(LoggingLevel.WARN, "Failed to open the outgoing socket.", e);
					closeSocket();
				}
			}
		} else {
			logger.log(LoggingLevel.INFO, "Opening outgoing file input stream for " + directive.getFileInput());
			try {
				inputStream = new FileInputStream(directive.getFileInput());
				handlerThread.getOutputStream().write(new String("HTTP/1.1 200 OK\r\n").getBytes());
				handlerThread.getOutputStream().write(new String("Connection: Keep-Alive\r\n").getBytes());
				handlerThread.getOutputStream().write(
						new String("Content-Length: " + directive.getFileInput().length() + "\r\n\r\n").getBytes());
				pipeThread = new SimpleInputOutputPipeThread(inputStream, handlerThread.getOutputStream(), this);
				pipeThread.start();
				logger.log(LoggingLevel.INFO, "Outgoing fake socket opened (using file).");
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to make a fake connection using a file.", e);
				closeSocket();
			}
		}

	}

	/**
	 * This function is used to tell the outgoing stream informations.
	 * 
	 * @param buffer The buffer you want to send.
	 * @param offset The offset for the buffer.
	 * @param length The size of the buffer to read and send.
	 */
	public void feedOutput(byte[] buffer, int offset, int length) {
		try {
			if (outputStream != null) {
				outputStream.write(buffer, offset, length);
			} else if (sslCommunicator != null) {
				sslCommunicator.feedOutput(buffer, offset, length);
			}
		} catch (IOException e) {
			logger.log(LoggingLevel.WARN, "Failed to write to the outgoing stream.", e);
		}
	}

	/**
	 * This function is used to close the outgoing connection.
	 */
	public void closeSocket() {
		if (!closed) {
			closed = true;
			if (pipeThread != null)
				pipeThread.interrupt();
			if (sslCommunicator != null) {
				sslCommunicator.stopCommunicator();
			}
			try {
				if (inputStream != null) {
					inputStream.close();
					inputStream = null;
				}
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to close the input stream coming from outside.", e);
			}
			try {
				if (outputStream != null) {
					outputStream.close();
					outputStream = null;
				}
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to close the output stream coming from outside.", e);
			}
			try {
				if (referenceSocket != null) {
					referenceSocket.close();
					referenceSocket = null;
				}
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to close the socket coming from outside.", e);
			}
			if (!directive.isUsingFile()) {
				if (directive.isSSL()) {
					logger.log(LoggingLevel.INFO, "Closed outgoing socket for " + directive.getHost() + ":"
							+ directive.getPort() + " with SSL.");
				} else {
					logger.log(LoggingLevel.INFO,
							"Closed outgoing socket for " + directive.getHost() + ":" + directive.getPort()
									+ " with request " + directive.getOutcomingRequest().toHttpRequestLine());
				}
			} else {
				logger.log(LoggingLevel.INFO, "Closed outgoing file input stream for " + directive.getFileInput());
			}
			if (connectionType == ConnectionType.CLOSE) {
				logger.log(LoggingLevel.INFO, "Closing listening thread as the handler thread is in Close mode.");
				handlerThread.closeListeningSocket();
			}
		}
	}
}
