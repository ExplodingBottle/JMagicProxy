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
package io.github.explodingbottle.jmagicproxy.proxy.ssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.api.SSLControlDirective;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;

/**
 * The main goal of this class is to assure an outgoing SSL connection.
 * 
 * @author ExplodingBottle
 *
 */
public class SSLDirectiveHandler {

	private SSLControlDirective directive;
	private ProxyLogger selfLogger;

	private Socket outgoingSocket;
	private SSLCommunicationServer parent;

	private InputStream inputStream;
	private OutputStream outputStream;

	private SSLInputOutputPipeThread ioPipe;

	private boolean isClosed;

	/**
	 * Builds a handler using a directive.
	 * 
	 * @param directive The directive to use.
	 */
	public SSLDirectiveHandler(SSLControlDirective directive, SSLCommunicationServer parent) {
		this.directive = directive;
		this.parent = parent;
		selfLogger = ProxyMain.getLoggerProvider().createLogger();
		isClosed = false;
	}

	/**
	 * Used to write the directive related content.
	 */
	public void rewriteDirectiveLine() {
		if (directive.isUsingFile()) {
			selfLogger.log(LoggingLevel.WARN, "Trying to use Keep-Alive with unsupported methods.");
			return;
		}
		try {
			//System.err.println(directive.getOutcomingRequest().toHttpRequestLine());
			outputStream.write((directive.getOutcomingRequest().toHttpRequestLine() + "\r\n").getBytes());
			directive.getOutcomingRequest().getHeaders().forEach((hKey, hVal) -> {
				try {
					outputStream.write((hKey + ": " + hVal + "\r\n").getBytes());
				} catch (IOException e) {
					selfLogger.log(LoggingLevel.WARN, "Failed to write a header in the ForEach loop.", e);
				}
			});
			outputStream.write("\r\n".getBytes());
		} catch (IOException e) {
			selfLogger.log(LoggingLevel.WARN, "Failed to write directive content.", e);
		}

	}

	/**
	 * This function will open the outgoing connection.
	 */
	public void openSocket() {
		selfLogger.log(LoggingLevel.INFO, "Opening an outgoing connection for SSL for host " + directive.getHost() + ":"
				+ directive.getPort() + ".");
		SSLObjectsProvider obProv = ProxyMain.getSSLObjectsProvider();
		if (directive.isUsingFile()) {
			selfLogger.log(LoggingLevel.INFO,
					"The connection will be using a file located at " + directive.getFileInput() + ".");
			try {
				inputStream = new FileInputStream(directive.getFileInput());
				parent.getHeartOutput().write(new String("HTTP/1.1 200 OK\r\n").getBytes());
				parent.getHeartOutput().write(new String("Connection: Keep-Alive\r\n").getBytes());
				parent.getHeartOutput().write(
						new String("Content-Length: " + directive.getFileInput().length() + "\r\n\r\n").getBytes());
				ioPipe = new SSLInputOutputPipeThread(inputStream, parent.getHeartOutput(), this);
				ioPipe.start();
			} catch (IOException e) {
				selfLogger.log(LoggingLevel.WARN, "Failed to make a fake connection using a file.", e);
				finishHandler(true);
			}
		} else {
			if (directive.isSSL()) {
				selfLogger.log(LoggingLevel.INFO, "The connection will be using outgoing SSL");
				try {
					outgoingSocket = obProv.getFactoryOutgoing()
							.createSocket(InetAddress.getByName(directive.getHost()), directive.getPort());
					((SSLSocket) outgoingSocket).startHandshake();
					inputStream = outgoingSocket.getInputStream();
					outputStream = outgoingSocket.getOutputStream();
					rewriteDirectiveLine();
					ioPipe = new SSLInputOutputPipeThread(inputStream, parent.getHeartOutput(), this);
					ioPipe.start();
				} catch (IOException e) {
					selfLogger.log(LoggingLevel.WARN, "Failed to open the outgoing SSL socket.", e);
					finishHandler(true);
				}
			} else {
				selfLogger.log(LoggingLevel.INFO, "The connection will be using outgoing standard HTTP.");
				try {
					outgoingSocket = new Socket(InetAddress.getByName(directive.getHost()), directive.getPort());
					inputStream = outgoingSocket.getInputStream();
					outputStream = outgoingSocket.getOutputStream();
					rewriteDirectiveLine();
					ioPipe = new SSLInputOutputPipeThread(inputStream, parent.getHeartOutput(), this);
					ioPipe.start();
				} catch (IOException e) {
					selfLogger.log(LoggingLevel.WARN, "Failed to open the outgoing standard socket.", e);
					finishHandler(true);
				}
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
			if (outputStream != null)
				outputStream.write(buffer, offset, length);
		} catch (IOException e) {
			selfLogger.log(LoggingLevel.WARN, "Failed to write to the outgoing stream.", e);
		}
	}

	/**
	 * This function will close any outgoing connections.
	 */
	public void finishHandler(boolean shouldInterrupt) { // TODO: Correctly implement shouldInterrupt
		if (!isClosed) {
			selfLogger.log(LoggingLevel.INFO, "Finishing handler with shouldInterrupt=" + shouldInterrupt);
			isClosed = true;
			if (ioPipe != null)
				ioPipe.interrupt();
			try {
				if (inputStream != null) {
					inputStream.close();
					inputStream = null;
				}
			} catch (IOException e) {
				selfLogger.log(LoggingLevel.WARN, "Failed to close the input stream coming to outside.", e);
			}
			try {
				if (outputStream != null) {
					outputStream.close();
					outputStream = null;
				}
			} catch (IOException e) {
				selfLogger.log(LoggingLevel.WARN, "Failed to close the output stream coming to outside.", e);
			}
			try {
				if (outgoingSocket != null) {
					outgoingSocket.close();
					outgoingSocket = null;
				}
			} catch (IOException e) {
				selfLogger.log(LoggingLevel.WARN, "Failed to close the socket coming from outside.", e);
			}
			if (shouldInterrupt) {
				if (!directive.isUsingFile()) {
					if (directive.isSSL()) {
						selfLogger.log(LoggingLevel.INFO, "Closed outgoing socket for " + directive.getHost() + ":"
								+ directive.getPort() + " with SSL.");
					} else {
						selfLogger.log(LoggingLevel.INFO,
								"Closed outgoing socket for " + directive.getHost() + ":" + directive.getPort()
										+ " with request " + directive.getOutcomingRequest().toHttpRequestLine());
					}
				} else {
					selfLogger.log(LoggingLevel.INFO,
							"Closed outgoing file input stream for " + directive.getFileInput());
				}
				selfLogger.log(LoggingLevel.INFO, "Closing SSL thread as the handler thread is in Close mode.");
				parent.interrupt();
			}
		} else {
			selfLogger.log(LoggingLevel.WARN, "Closing SSL but it is already closed !!.");
		}
	}

}
