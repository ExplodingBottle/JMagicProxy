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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class represents an instance of a logger, which has been created by
 * {@code LoggerProvider}
 * 
 * @author ExplodingBottle
 * @see LoggerProvider
 *
 */
public class ProxyLogger {

	private LoggerProvider provider;
	private String loggerName;

	ProxyLogger(LoggerProvider provider, String loggerName) {
		this.provider = provider;
		this.loggerName = loggerName;
		if (loggerName == null)
			loggerName = "Unknown Class";
	}

	/**
	 * Logs a simple message with no throwable.
	 * 
	 * @param type       Corresponds to the record type.
	 * @param logMessage Corresponds to the log message.
	 */
	public void log(LoggingLevel type, String logMessage) {
		log(type, logMessage, null);
	}

	/**
	 * Logs a simple message with a throwable.
	 * 
	 * @param type       Corresponds to the record type.
	 * @param logMessage Corresponds to the log message.
	 * @param e          Corresponds to the throwable.
	 */
	public void log(LoggingLevel type, String logMessage, Throwable e) {
		LocalDateTime ldt = LocalDateTime.now();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		String message = dtf.format(ldt) + " [" + type.toString() + "](" + loggerName + "): " + logMessage;
		StringBuffer buff = new StringBuffer(message);
		if (e != null) {
			buff.append("\r\n");
			StringWriter strWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(strWriter));
			buff.append(strWriter.toString());
		}
		provider.write(buff.toString(), type);
	}

}
