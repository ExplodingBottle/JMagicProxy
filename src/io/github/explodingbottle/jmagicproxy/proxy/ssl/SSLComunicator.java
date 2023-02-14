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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.TreeMap;

import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.api.ConnectionType;
import io.github.explodingbottle.jmagicproxy.api.HttpResponse;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;
import io.github.explodingbottle.jmagicproxy.properties.PropertyKey;
import io.github.explodingbottle.jmagicproxy.proxy.ConnectionDirectiveHandler;

/**
 * 
 * This class is responsible of handling SSL Connections
 * 
 * @author ExplodingBottle
 *
 */
public class SSLComunicator {

	private OutputStream output;

	private InputStream inputOutgoing;
	private OutputStream outputOutgoing;

	private ProxyLogger logger;

	String originalHost;
	int originalPort;

	private Socket transferSocket;

	private SSLCommunicationServer server;

	private ConnectionDirectiveHandler parent;

	private SimpleTransferPipe transferPipeOutToIn;

	/**
	 * Constructs a SSL Communicator by using the input and output.
	 * 
	 * @param input  The input stream.
	 * @param output The output stream.
	 */
	public SSLComunicator(OutputStream output, ConnectionDirectiveHandler parent, String originalHost,
			int originalPort) {
		this.output = output;
		this.parent = parent;
		this.originalHost = originalHost;
		this.originalPort = originalPort;
		logger = ProxyMain.getLoggerProvider().createLogger();
	}

	public void startConnection() {
		if (!ProxyMain.getPropertiesProvider().getAsBoolean(PropertyKey.PROXY_SSL_ENABLED)) {
			try {
				HttpResponse hrqh = new HttpResponse("HTTP/1.1", 503, "Service Unavailable",
						new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
				output.write((hrqh.toHttpResponseLine() + "\r\n\r\n").getBytes());
				logger.log(LoggingLevel.WARN,
						"An attempt to connect with SSL has been caught while SSL being disabled.");
				stopCommunicator();
				return;
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to write the response line.", e);
				stopCommunicator();
				return;
			}
		}
		server = new SSLCommunicationServer(this);
		Integer serverPort = server.prepareServerSocket();
		try {
			if (serverPort != null) {
				HttpResponse hrqh = new HttpResponse("HTTP/1.1", 200, "Connection Established",
						new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
				output.write((hrqh.toHttpResponseLine() + "\r\n\r\n").getBytes());
			} else {
				HttpResponse hrqh = new HttpResponse("HTTP/1.1", 500, "Internal Server Error",
						new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
				output.write((hrqh.toHttpResponseLine() + "\r\n\r\n").getBytes());
				stopCommunicator();
				return;
			}
		} catch (IOException e) {
			logger.log(LoggingLevel.WARN, "Failed to write the response line.", e);
			stopCommunicator();
			return;
		}
		try {
			server.start();
			logger.log(LoggingLevel.INFO,
					"SSL incoming-side comunication has been started and is waiting for an accept on port " + serverPort
							+ ".");
			transferSocket = new Socket(InetAddress.getLoopbackAddress(), serverPort);
			inputOutgoing = transferSocket.getInputStream();
			outputOutgoing = transferSocket.getOutputStream();
		} catch (IOException e) {
			logger.log(LoggingLevel.WARN, "Failed to start SSL communication.", e);
		}
		if (transferSocket != null) {
			transferPipeOutToIn = new SimpleTransferPipe(inputOutgoing, output);
			transferPipeOutToIn.start();
		}
		logger.log(LoggingLevel.INFO, "SSL transfer is ready for port " + serverPort + ".");

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
			if (outputOutgoing != null)
				outputOutgoing.write(buffer, offset, length);
		} catch (IOException e) {
			logger.log(LoggingLevel.WARN, "Failed to write to the outgoing stream.", e);
		}
	}

	/**
	 * This is used to close the SSL Communication.
	 */
	public void stopCommunicator() {
		if (parent != null) {
			if (server != null)
				server.interrupt();
			if (transferPipeOutToIn != null)
				transferPipeOutToIn.interrupt();
			if (transferSocket != null)
				try {
					transferSocket.close();
				} catch (IOException e) {
					logger.log(LoggingLevel.WARN, "Failed to close the simple SSL transfer socket.", e);
				}
			if (parent != null) {
				parent.setConnectionType(ConnectionType.CLOSE); // In SSL, the tunnel must ALWAYS be closed.
				parent.closeSocket();
				parent = null;
			}
		}
	}

}
