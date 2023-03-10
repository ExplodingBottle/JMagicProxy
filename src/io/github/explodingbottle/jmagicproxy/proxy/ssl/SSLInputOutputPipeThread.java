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

import io.github.explodingbottle.jmagicproxy.HardcodedConfig;
import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.api.HttpResponse;
import io.github.explodingbottle.jmagicproxy.api.MalformedParsableContent;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;

/**
 * This class represents a Input->Output pipe Thread to allow asynchronous
 * communications. This thread is customized for SSL.
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
public class SSLInputOutputPipeThread extends Thread {

	private InputStream in;
	private OutputStream out;

	private byte[] transferBuffer;

	private ProxyLogger logger;

	private boolean canParseHeader;
	private int toReadBeforeParse;

	private SSLDirectiveHandler parent;

	private HttpResponse lastRepsonse;

	/**
	 * This constructs the pipe.
	 * 
	 * @param input  Where the data will be read.
	 * @param output Where the data will be written.
	 * @param parent Represents the directive handler which instantiated this
	 *               thread.
	 */
	public SSLInputOutputPipeThread(InputStream input, OutputStream output, SSLDirectiveHandler parent) {
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
						HttpResponse response2 = ProxyMain.getPluginsManager().getModifiedSSLResponse(response);

						if (response2 != null) {
							lastRepsonse = response2;
							if (response2.getHeaders().get("Content-Length") != null) {
								toReadBeforeParse = Integer.parseInt(response2.getHeaders().get("Content-Length"));
							}
							out.write((response2.toHttpResponseLine() + "\r\n").getBytes());
							response2.getHeaders().forEach((hKey, hVal) -> {
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

	public void run() {
		logger.log(LoggingLevel.INFO, "Signaling pipe startup for SSL.");
		try {
			int read = -1;
			if (in != null) {
				read = in.read(transferBuffer, 0, transferBuffer.length);
			} else {
				if (!parent.getControlDirective().isRemoteConnect()) {
					transferBuffer = ProxyMain.getPluginsManager().getModifiedData(4, parent.getControlDirective(),
							null, lastRepsonse);
					if (transferBuffer != null) {
						read = transferBuffer.length;
					} else {
						read = -1;
					}
				}
			}
			logger.log(LoggingLevel.INFO, "SSL Pipe has read for the first time " + read + " bytes.");
			while (!interrupted() && read != -1) {
				Integer offset = handleLineRead(read);

				if (offset != null) {
					byte[] realData = new byte[read - offset];
					for (int i = 0; i < read - offset; i++) {
						realData[i] = transferBuffer[i + offset];
					}
					realData = ProxyMain.getPluginsManager().getModifiedData(4, parent.getControlDirective(), realData,
							lastRepsonse);
					out.write(realData, 0, realData.length);
					// outgoingHandler.feedOutput(realData, 0, realData.length);
					// out.write(transferBuffer, offset, read - offset);
					//out.write(transferBuffer, offset, read - offset);
				} else {
					byte[] realData = new byte[read];
					for (int i = 0; i < read; i++) {
						realData[i] = transferBuffer[i];
					}
					realData = ProxyMain.getPluginsManager().getModifiedData(4, parent.getControlDirective(), realData,
							lastRepsonse);
					out.write(realData, 0, realData.length);
					// out.write(transferBuffer, 0, read);
				}
				if (in != null) {
					read = in.read(transferBuffer, 0, transferBuffer.length);
				} else {
					if (!parent.getControlDirective().isRemoteConnect()) {
						transferBuffer = ProxyMain.getPluginsManager().getModifiedData(4, parent.getControlDirective(),
								null, lastRepsonse);
						if (transferBuffer != null) {
							read = transferBuffer.length;
						} else {
							read = -1;
						}
					}
				}
			}
		} catch (IOException e) {
			if (!isInterrupted())
				logger.log(LoggingLevel.WARN, "An unexpected stream closure happened in SSL pipe.", e);
		}
		parent.finishHandler(true);
	}

}

/*
 * Quick note: I would say that it's better if this thread is created by the
 * ConnectionDirectiveHandler.
 */
