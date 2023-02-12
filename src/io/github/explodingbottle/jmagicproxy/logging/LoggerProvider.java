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
package io.github.explodingbottle.jmagicproxy.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * This class is responsible of providing {@code ProxyLogger} classes, useful
 * for logging.
 * 
 * @author ExplodingBottle
 */
public class LoggerProvider {

	private OutputStream output;
	private ProxyLogger selfLogger;

	private boolean useLoggingFile;
	private ArrayList<String> messageCache;

	/**
	 * This creates a {@code LoggerProvider} instance. If enabling log file support,
	 * don't forget to close it with {@code closeLogStream}.
	 * 
	 * @param useLoggingFile If set to false, {@code openLogStream} and
	 *                       {@code closeLogStream} won't be usable and no support
	 *                       for storing messages in a file will be enabled.
	 */
	public LoggerProvider(boolean useLoggingFile) {
		selfLogger = createLogger();
		this.useLoggingFile = useLoggingFile;
		if (useLoggingFile)
			messageCache = new ArrayList<String>();
	}

	/**
	 * Opens an {@code OutputStream} linked to the {@code outputFile} parameter.<br>
	 * 
	 * <b>NOTE: This function won't work if useLoggingFile was set to
	 * {@code false}.</b>
	 * 
	 * @param outputFile The place where you will store your log file.
	 */
	public void openLogStream(File outputFile) {
		if (!useLoggingFile)
			return;
		if (output == null) {
			try {
				output = new FileOutputStream(outputFile);
				flushMessageCache();
			} catch (FileNotFoundException e) {
				selfLogger.log(LoggingLevel.WARN, "Failed to open the logging output.", e);
			}
		}
		selfLogger.log(LoggingLevel.INFO, "Log file has been opened.");
	}

	/**
	 * Closes the {@code OutputStream} which was previously opened.<br>
	 * 
	 * <b>NOTE: This function won't work if useLoggingFile was set to
	 * {@code false}.</b>
	 * 
	 */
	public void closeLogStream() {
		if (!useLoggingFile)
			return;
		if (output != null) {
			try {
				OutputStream backup = output;
				output = null;
				backup.close();
			} catch (IOException e) {
				selfLogger.log(LoggingLevel.WARN, "Failed to close the logging output.", e);
			}
		}
		selfLogger.log(LoggingLevel.INFO, "Log file has been closed.");
	}

	/**
	 * Creates and returns a brand new logger which has the class name of the
	 * caller.
	 * 
	 * @return The created logger.
	 */
	public ProxyLogger createLogger() {
		String lg = null;
		StackTraceElement[] trace = new Throwable().getStackTrace(); // This is a strange way I've found to go up in the
																		// stack trace, like in Lua.
		if (trace.length >= 2) {
			StackTraceElement parent = trace[1];
			lg = parent.getClassName();
		}
		return new ProxyLogger(this, lg);

	}

	private synchronized void flushMessageCache() {
		if (!useLoggingFile)
			return;
		if (output != null) {
			messageCache.forEach(msg -> {
				try {
					output.write((msg + "\r\n").getBytes());
				} catch (IOException e) {
					selfLogger.log(LoggingLevel.WARN, "Failed to write a log message.", e);
				}
			});
			messageCache.clear();
		}
	}

	// Maybe write() will be done through different threads, synchronized is
	// super-important here.
	synchronized void write(String message, LoggingLevel lt) {
		if (lt == LoggingLevel.INFO) {
			System.out.println(message);
		} else {
			System.err.println(message);
		}
		if (!useLoggingFile)
			return;
		messageCache.add(message);
		flushMessageCache();
	}

}
