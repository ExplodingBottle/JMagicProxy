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
package io.github.explodingbottle.jmagicproxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;

/**
 * This class is very useful to find issues that may be related to the
 * java.security file with disabled algorithms.
 * 
 * @author ExplodingBottle
 *
 */
public class DisabledAlgorithmsWarner {

	private Properties javaSecurity;
	private ProxyLogger logger;

	/**
	 * Gets ready the warner.
	 * 
	 */
	public DisabledAlgorithmsWarner() {
		logger = ProxyMain.getLoggerProvider().createLogger();
		javaSecurity = new Properties();
		FileInputStream is = null;
		try {
			is = new FileInputStream(
					new File(new File(new File(System.getProperty("java.home"), "lib"), "security"), "java.security"));
		} catch (FileNotFoundException e) {
			logger.log(LoggingLevel.WARN,
					"Failed to open the input stream to check java.security. No warnings will be emitted.", e);
		}
		if (is != null) {
			try {
				javaSecurity.load(is);
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to load the properties contained in java.security.", e);
			}
			try {
				is.close();
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to close the java.security file.", e);
			}
		}

	}

	/**
	 * This function will say if the user must be warned about the
	 * jdk.tls.disabledAlgorithms property.
	 * 
	 * @return if the user must be warned about jdk.tls.disabledAlgorithms.
	 */
	public boolean mustWarn() {
		String found = javaSecurity.getProperty("jdk.tls.disabledAlgorithms");
		if (found != null && !found.trim().isEmpty())
			return true;
		return false;
	}

}
