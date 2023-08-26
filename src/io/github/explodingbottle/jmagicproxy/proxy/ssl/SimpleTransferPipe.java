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
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;

/**
 * Simple Transfer Pipe is used for SSL, where we have an input stream and an
 * output stream.
 * 
 * @author ExplodingBottle
 *
 */
class SimpleTransferPipe extends Thread {

	private InputStream input;
	private OutputStream output;

	private byte[] buffer;

	private ProxyLogger logger;

	private SSLComunicator communicator;

	/**
	 * This is the constructor of the transfer pipe.
	 * 
	 * @param input  Represents the input that will feed the output.
	 * @param output Represents the output that will be feed.
	 */
	public SimpleTransferPipe(InputStream input, OutputStream output) {
		this.input = input;
		this.output = output;
		buffer = new byte[HardcodedConfig.returnBufferSize()];
		logger = ProxyMain.getLoggerProvider().createLogger();
	}

	/**
	 * This is the constructor of the transfer pipe.
	 * 
	 * @param input        Represents the input that will feed the output.
	 * @param output       Represents the output that will be feed.
	 * @param communicator Represents the parent SSL communicator (if any).
	 */
	public SimpleTransferPipe(InputStream input, OutputStream output, SSLComunicator communicator) {
		this(input, output);
		this.communicator = communicator;
	}

	public void run() {
		try {
			int readedLength = input.read(buffer, 0, buffer.length);
			while (readedLength != -1 && !interrupted()) {
				output.write(buffer, 0, readedLength);
				readedLength = input.read(buffer, 0, buffer.length);
			}
		} catch (IOException e) {
			if (!isInterrupted()) {
				logger.log(LoggingLevel.WARN, "A tranfer failed for SimpleTransferPipe.", e);
			}
		}
		if (communicator != null) {
			communicator.stopCommunicator();
		}
	}

}
