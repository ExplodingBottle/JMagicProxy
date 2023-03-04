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

import io.github.explodingbottle.jmagicproxy.HardcodedConfig;
import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.api.ConnectionDirective;
import io.github.explodingbottle.jmagicproxy.api.ConnectionType;
import io.github.explodingbottle.jmagicproxy.api.HttpRequestHeader;
import io.github.explodingbottle.jmagicproxy.api.MalformedParsableContent;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;
import io.github.explodingbottle.jmagicproxy.server.SocketAcceptorThread;

/**
 * This socket handles the communication with the client and the server.
 * 
 * @author ExplodingBottle
 *
 */
public class SocketHandlerThread extends Thread {

	private Socket socket;
	private ProxyLogger logger;

	private InputStream input;
	private OutputStream output;

	private SocketAcceptorThread parent;

	private byte[] buffer;

	private boolean isClosed;

	private ConnectionDirectiveHandler linkedDirectiveHandler;

	/**
	 * Creates a handler thread according to its socket.
	 * 
	 * @param socket The socket to handle.
	 * @param parent The parent which is the SocketAcceptorThread to allow
	 *               unregister when thread dies.
	 */
	public SocketHandlerThread(Socket socket, SocketAcceptorThread parent) {
		this.socket = socket;
		this.parent = parent;
		logger = ProxyMain.getLoggerProvider().createLogger();
		buffer = new byte[HardcodedConfig.returnBufferSize()];
	}

	/**
	 * Use this method to close the socket and interrupt the thread.
	 */
	public void closeListeningSocket() {
		if (!isClosed) {
			isClosed = true;
			super.interrupt();
			if (linkedDirectiveHandler != null)
				try {
					linkedDirectiveHandler.closeSocket();
				} catch (Exception e) {
					logger.log(LoggingLevel.WARN, "Failed to close the directive handler.", e);
				}
			try {
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to close the current socket.", e);
			}
			parent.removeFromList(this);
			logger.log(LoggingLevel.INFO, "Socket from " + socket.getInetAddress() + " and from port "
					+ socket.getPort() + " has been closed.");
		}
	}

	/**
	 * Redefining interrupt to force use of closeListeningSocket().
	 */
	public void interrupt() {
		closeListeningSocket();
	}

	/**
	 * This methods returns the output stream to go to the client.
	 * 
	 * @return The output stream to the client.
	 */
	OutputStream getOutputStream() {
		return output;
	}

	/**
	 * This methods returns the input stream which goes from the client.
	 * 
	 * @return The input stream from the client.
	 */
	InputStream getInputStream() {
		return input;
	}

	private StringBuilder lastReadBlock;
	private StringBuilder lastReadLine;

	private Integer handleLineRead(int readLength) {
		Integer toRet = null;
		if (lastReadBlock == null)
			lastReadBlock = new StringBuilder();
		if (lastReadLine == null)
			lastReadLine = new StringBuilder();
		boolean gotItOnce = false;
		for (int it = 0; it < readLength; it++) {
			byte r = buffer[it];
			lastReadBlock.append((char) r);
			lastReadLine.append((char) r);
			if ((char) r == '\n') {
				String readLine = lastReadLine.toString();
				try {
					HttpRequestHeader.createFromHeaderBlock(lastReadBlock);
					gotItOnce = true;
				} catch (MalformedParsableContent e1) {
					lastReadBlock = new StringBuilder();
					lastReadLine = new StringBuilder();
					return toRet;
				}
				if (readLine.trim().isEmpty()) {
					try {
						HttpRequestHeader httpRequestHeader = HttpRequestHeader.createFromHeaderBlock(lastReadBlock);
						ConnectionDirective directive = ProxyMain.getPluginsManager()
								.getInitialDirectiveByPlugins(httpRequestHeader);
						boolean reuse = false;
						if (linkedDirectiveHandler != null) {
							if (directive != null
									&& directive.getHost()
											.equalsIgnoreCase(linkedDirectiveHandler.getDirective().getHost())
									&& directive.getPort() == linkedDirectiveHandler.getDirective().getPort()
									&& linkedDirectiveHandler.getConnectionType() == ConnectionType.KEEPALIVE
									&& !linkedDirectiveHandler.isClosed())
								reuse = true;
							if (!reuse)
								linkedDirectiveHandler.closeSocket();
						}
						if (directive != null) {
							if (reuse) {
								logger.log(LoggingLevel.INFO, "Keep-Alive connection has been reused for "
										+ directive.getOutcomingRequest().toHttpRequestLine() + ".");
								linkedDirectiveHandler.setDirective(directive);
								linkedDirectiveHandler.rewriteDirectiveLine();
							} else {
								linkedDirectiveHandler = new ConnectionDirectiveHandler(directive, this);
								linkedDirectiveHandler.openSocket();
							}
							lastReadBlock = new StringBuilder();
							lastReadLine = new StringBuilder();
							toRet = it + 1;
							break;
						} else {
							logger.log(LoggingLevel.WARN, "Directive is null, closing socket.");
							closeListeningSocket();
						}
					} catch (MalformedParsableContent e) {
					}
					lastReadBlock = new StringBuilder();
				}
				lastReadLine = new StringBuilder();
			}
		}
		if (!gotItOnce) {
			lastReadBlock = new StringBuilder();
			lastReadLine = new StringBuilder();
		}
		return toRet;
	}

	public void run() {
		logger.log(LoggingLevel.INFO,
				"Now handling a socket from " + socket.getInetAddress() + " and from port " + socket.getPort());
		try {
			input = socket.getInputStream();
			output = socket.getOutputStream();
		} catch (IOException e) {
			logger.log(LoggingLevel.WARN, "Failed to open input or output stream.", e);
		}
		try {
			int readLength = input.read(buffer, 0, buffer.length);
			while (readLength != -1 && !interrupted()) {
				Integer offset = handleLineRead(readLength);

				if (offset != null) {
					if (linkedDirectiveHandler != null) {
						byte[] realData = new byte[readLength - offset];
						for (int i = 0; i < readLength - offset; i++) {
							realData[i] = buffer[i + offset];
						}
						realData = ProxyMain.getPluginsManager().getModifiedData(1,
								linkedDirectiveHandler.getDirective(), realData, null);
						linkedDirectiveHandler.feedOutput(realData, 0, realData.length);
					} else {
						linkedDirectiveHandler.feedOutput(buffer, offset, readLength - offset);
					}
				} else {
					if (linkedDirectiveHandler != null) {
						if (!linkedDirectiveHandler.getDirective().isSSL()) {
							byte[] realData = new byte[readLength];
							for (int i = 0; i < readLength; i++) {
								realData[i] = buffer[i];
							}
							realData = ProxyMain.getPluginsManager().getModifiedData(1,
									linkedDirectiveHandler.getDirective(), realData, null);
							if (linkedDirectiveHandler != null) {
								linkedDirectiveHandler.feedOutput(realData, 0, realData.length);
							}
						} else {
							linkedDirectiveHandler.feedOutput(buffer, 0, readLength);
						}
					}
				}
				readLength = input.read(buffer, 0, buffer.length);
			}
		} catch (Exception e) {
			if (!isInterrupted()) {
				logger.log(LoggingLevel.WARN, "Transfer thread crashed.", e);
			}
		}
		closeListeningSocket();
	}

}
