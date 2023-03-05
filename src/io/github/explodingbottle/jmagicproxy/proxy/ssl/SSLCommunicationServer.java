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
import java.net.BindException;
import java.net.InetAddress;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import io.github.explodingbottle.jmagicproxy.HardcodedConfig;
import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.api.HttpRequestHeader;
import io.github.explodingbottle.jmagicproxy.api.MalformedParsableContent;
import io.github.explodingbottle.jmagicproxy.api.SSLControlDirective;
import io.github.explodingbottle.jmagicproxy.api.SSLControlInformations;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;
import io.github.explodingbottle.jmagicproxy.properties.PropertyKey;

/**
 * This class is responsible of handling a SSL connection through a socket.
 * 
 * @author ExplodingBottle
 *
 */
public class SSLCommunicationServer extends Thread {

	private SSLServerSocket server;
	private ProxyLogger logger;

	private InputStream heartInput; // Just a strange naming, heart like coming from the inside, also like very
									// intense feelings.
	private OutputStream heartOutput;

	private SSLSocket acceptedSocket;

	private SSLComunicator communicator;

	private SSLDirectiveHandler outgoingHandler;

	private byte[] buffer;

	private boolean canParseHeader;
	private int toReadBeforeParse;

	/**
	 * This constructor is used to create the server
	 * 
	 * @param communicator Represents the parent.
	 */
	public SSLCommunicationServer(SSLComunicator communicator) {
		this.communicator = communicator;
		logger = ProxyMain.getLoggerProvider().createLogger();
		buffer = new byte[HardcodedConfig.returnBufferSize()];
		canParseHeader = true;
		toReadBeforeParse = 0;
	}

	public void interrupt() {
		super.interrupt();
		if (server != null) {
			if (outgoingHandler != null)
				outgoingHandler.finishHandler(true);
			try {
				if (acceptedSocket != null)
					acceptedSocket.close();
				if (server != null)
					server.close();
				server = null;
				if (communicator != null) {
					communicator.stopCommunicator();
				}
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to close the SSL server.", e);
			}
			logger.log(LoggingLevel.INFO, "Stopped the SSL Communication server.");
		} else {
			logger.log(LoggingLevel.WARN, "Communication server is already closed.");
		}

	}

	/**
	 * This function will setup the server socket and return the used port.
	 * 
	 * @return The port associated to the server socket.
	 */
	public Integer prepareServerSocket() {
		if (server != null) {
			logger.log(LoggingLevel.WARN, "Trying to open a SSL Server Socket twice.");
			return null;
		}
		SSLObjectsProvider obProv = ProxyMain.getSSLObjectsProvider();
		logger.log(LoggingLevel.INFO, "Searching an available port.");
		int testPort = ProxyMain.getPropertiesProvider().getAsInteger(PropertyKey.PROXY_SSL_SCAN_STARTING_PORT);
		while (server == null)
			try {
				server = (SSLServerSocket) obProv.getFactoryServer().createServerSocket(testPort, 0,
						InetAddress.getLoopbackAddress());
			} catch (BindException e) {
				testPort++;
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to open a socket for a different reason than a BindException.",
						e);
				break;
			}
		if (server != null) {
			logger.log(LoggingLevel.INFO,
					"A SSL Communication Server has been started on local port " + server.getLocalPort() + ".");
		} else {
			logger.log(LoggingLevel.WARN, "SSL Communication Server won't be started due to an error.");
			return null;
		}
		return testPort;
	}

	/**
	 * Returns the heart output.
	 * 
	 * @return the heart output.
	 */
	public OutputStream getHeartOutput() {
		return heartOutput;
	}

	private StringBuilder lastReadBlock;
	private StringBuilder lastReadLine;

	// Yes, I borrowed it again, this is a bad practice...
	private Integer handleLineRead(int readLength) {
		if (!canParseHeader) {
			return null;
		}
		int offset = 0;
		if (toReadBeforeParse != 0) {
			toReadBeforeParse -= readLength;
			if (toReadBeforeParse < 0) {
				lastReadBlock = new StringBuilder();
				lastReadLine = new StringBuilder();
				offset = toReadBeforeParse * -1;
				toReadBeforeParse = 0;
			}
		}
		if (toReadBeforeParse > 0) {
			lastReadBlock = new StringBuilder();
			lastReadLine = new StringBuilder();
			return null;
		}
		Integer toRet = null;
		if (lastReadBlock == null)
			lastReadBlock = new StringBuilder();
		if (lastReadLine == null)
			lastReadLine = new StringBuilder();
		for (int it = offset; it < readLength; it++) {
			byte r = buffer[it];
			lastReadBlock.append((char) r);
			lastReadLine.append((char) r);
			if ((char) r == '\n') {
				String readLine = lastReadLine.toString();
				if (readLine.trim().isEmpty()) {
					try {
						HttpRequestHeader httpRequestHeader = HttpRequestHeader.createFromHeaderBlock(lastReadBlock);
						SSLControlDirective directive = ProxyMain.getPluginsManager()
								.getSSLControlDirective(new SSLControlInformations(httpRequestHeader,
										communicator.originalHost, communicator.originalPort));
						if (directive != null) {
							if (directive.getOutcomingRequest() != null
									&& directive.getOutcomingRequest().getHeaders().get("Content-Length") != null) {
								toReadBeforeParse = Integer
										.parseInt(directive.getOutcomingRequest().getHeaders().get("Content-Length"));
							}
							if (outgoingHandler != null) {
								outgoingHandler.finishHandler(false);
							}
							outgoingHandler = new SSLDirectiveHandler(directive, this);
							outgoingHandler.openSocket();
							toRet = it + 1;
							lastReadBlock = new StringBuilder();
							lastReadLine = new StringBuilder();
							break;
						}
					} catch (MalformedParsableContent e) {
					}
					lastReadBlock = new StringBuilder();
				}
				lastReadLine = new StringBuilder();

			}
		}
		return toRet;
	}

	public void run() {
		if (server != null) {
			try {
				try {
					if (acceptedSocket != null)
						acceptedSocket.close();
				} catch (IOException e) {
					logger.log(LoggingLevel.WARN, "Failed to close the previous SSL socket.", e);
				}
				acceptedSocket = (SSLSocket) server.accept();
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to accept a SSL socket.", e);
				interrupt();
				return;
			}
			logger.log(LoggingLevel.INFO, "SSL Socket has been successfully accepted.");
			try {
				acceptedSocket.startHandshake();
				heartInput = acceptedSocket.getInputStream();
				heartOutput = acceptedSocket.getOutputStream();
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to process the handshake of the SSL socket.", e);
				interrupt();
				return;
			}
			logger.log(LoggingLevel.INFO, "Handshake successfully performed.");

			try {
				int read = heartInput.read(buffer, 0, buffer.length);
				while (!interrupted() && read != -1) {
					Integer offset = handleLineRead(read);
					if (offset != null) {
						if (outgoingHandler != null) {
							byte[] realData = new byte[read - offset];
							for (int i = 0; i < read - offset; i++) {
								realData[i] = buffer[i + offset];
							}
							realData = ProxyMain.getPluginsManager().getModifiedData(3,
									outgoingHandler.getControlDirective(), realData, null);
							outgoingHandler.feedOutput(realData, 0, realData.length);
						}
					} else {
						if (outgoingHandler != null) {
							byte[] realData = new byte[read];
							for (int i = 0; i < read; i++) {
								realData[i] = buffer[i];
							}
							realData = ProxyMain.getPluginsManager().getModifiedData(3,
									outgoingHandler.getControlDirective(), realData, null);
							outgoingHandler.feedOutput(realData, 0, realData.length);
						}
					}
					read = heartInput.read(buffer, 0, buffer.length);
				}
			} catch (IOException e) {
				if (!isInterrupted()) {
					logger.log(LoggingLevel.WARN, "SSL Communicator Server transfer error.", e);
				}
			}
			logger.log(LoggingLevel.INFO, "SSL Communicator Server will now shutdown.");
			interrupt();

		} else {
			logger.log(LoggingLevel.WARN,
					"SSL Communication Server is attempting to start but has no SSL Server Socket.");
		}
	}

}
