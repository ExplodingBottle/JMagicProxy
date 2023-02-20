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

import io.github.explodingbottle.jmagicproxy.HardcodedConfig;
import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.api.ConnectionType;
import io.github.explodingbottle.jmagicproxy.api.HttpResponse;
import io.github.explodingbottle.jmagicproxy.api.IncomingTransferDirective;
import io.github.explodingbottle.jmagicproxy.api.MalformedParsableContent;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;

/**
 * This class represents a Input->Output pipe Thread to allow asynchronous
 * communications.
 *
 * Warning ! The only way to stop this thread is to interrupt it and close the
 * input.
 *
 * @author ExplodingBottle
 *
 */
/*
 * Author's quick note: Never allow that kind of thread to close streams else
 * you'll have a real nightmare.
 */
public class SimpleInputOutputPipeThread extends Thread {

	private InputStream in;
	private OutputStream out;

	private byte[] transferBuffer;

	private ProxyLogger logger;

	private boolean canParseHeader;
	private int toReadBeforeParse;

	private ConnectionDirectiveHandler parent;

	/**
	 * This constructs the pipe.
	 * 
	 * @param input  Where the data will be read.
	 * @param output Where the data will be written.
	 * @param parent Represents the directive handler which instantiated this
	 *               thread.
	 */
	public SimpleInputOutputPipeThread(InputStream input, OutputStream output, ConnectionDirectiveHandler parent) {
		in = input;
		out = output;
		this.parent = parent;
		logger = ProxyMain.getLoggerProvider().createLogger();
		transferBuffer = new byte[HardcodedConfig.returnBufferSize()]; // You must respect buffer size here too !.
		canParseHeader = true;
		toReadBeforeParse = 0;
	}

	private StringBuilder lastReadBlock;
	private StringBuilder lastReadLine;

	// This code has been borrowed from SocketHandlerThread.
	private Integer handleLineRead(int readLength) throws IOException {
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
			byte r = transferBuffer[it];
			lastReadBlock.append((char) r);
			lastReadLine.append((char) r);
			if ((char) r == '\n') {
				String readLine = lastReadLine.toString();
				if (readLine.trim().isEmpty()) {
					try {
						HttpResponse response = HttpResponse.createFromHeaderBlock(lastReadBlock);
						IncomingTransferDirective itd = ProxyMain.getPluginsManager()
								.getIncomingTransferDirective(response);
						if (itd != null) {
							HttpResponse modifiedResponse = itd.getResponse();
							ConnectionType ct = itd.getConnectionType();
							if (ct == ConnectionType.KEEPALIVE) {
								if (modifiedResponse.getHeaders().containsKey("Content-Length")) {
									toReadBeforeParse = Integer
											.parseInt(modifiedResponse.getHeaders().get("Content-Length"));
								}
							} else {
								canParseHeader = false;
							}
							parent.setConnectionType(ct);
							out.write((modifiedResponse.toHttpResponseLine() + "\r\n").getBytes());
							modifiedResponse.getHeaders().forEach((hKey, hVal) -> {
								try {
									out.write((hKey + ": " + hVal + "\r\n").getBytes());
								} catch (IOException e) {
									logger.log(LoggingLevel.WARN, "Failed to write a header in the ForEach loop.", e);
								}
							});
							out.write("\r\n".getBytes());
							toRet = it + 1;
							lastReadBlock = new StringBuilder();
							lastReadLine = new StringBuilder();
							break;
						} else {
							logger.log(LoggingLevel.WARN, "Directive is null, no actions will be taken.");
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

	@Override
	public void run() {
		logger.log(LoggingLevel.INFO, "Signaling pipe startup.");
		try {
			int read = in.read(transferBuffer, 0, transferBuffer.length);
			while (!interrupted() && read != -1) {
				if (!isInterrupted()) {
					Integer offset = handleLineRead(read);
					if (offset != null) {
						out.write(transferBuffer, offset, read - offset);
					} else {
						out.write(transferBuffer, 0, read);
					}
					read = in.read(transferBuffer, 0, transferBuffer.length);
				}
			}
			if (read == -1) {
				logger.log(LoggingLevel.INFO, "Server has terminated the stream. Forcing connection mode to Close.");
				parent.setConnectionType(ConnectionType.CLOSE);
			}
		} catch (IOException e) {
			if (!isInterrupted())
				logger.log(LoggingLevel.WARN, "An unexpected stream closure happened.", e);
		}
		logger.log(LoggingLevel.INFO, "Thread can be interrupted now !");
		parent.signalThreadClose();
	}

}

/*
 * Quick note: I would say that it's better if this thread is created by the
 * ConnectionDirectiveHandler.
 */
