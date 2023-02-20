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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;

/**
 * This class is used to determine if sites should be using the special
 * certificate or if we just let the tunnel work as intended.
 * 
 * @author ExplodingBottle
 *
 */
public class SSLSortEngine {

	private SSLSortMode mode;
	private String list;
	private ProxyLogger logger;

	/**
	 * Builds up the sort engine using parameters.
	 * 
	 * @param mode Defines the sort mode.
	 * @param list Defines the list of exclusions or inclusions depending on the
	 *             mode.
	 */
	public SSLSortEngine(SSLSortMode mode, String list) {
		this.mode = mode;
		this.list = list;
		logger = ProxyMain.getLoggerProvider().createLogger();
		logger.log(LoggingLevel.INFO, "Created a SSL sort engine for list " + list + " using the " + mode + " mode.");
	}

	/**
	 * This method will decide if the address should be using the custom
	 * certificates or if a direct connection only should be created.
	 * 
	 * @param host The target host.
	 * @return True if we use the custom certificates or false for a direct
	 *         connection.
	 */
	public boolean shouldUseCustomPipe(String host) {
		if (mode == SSLSortMode.NONE)
			return false;
		String[] patterns = list.split(";");
		for (int i = 0; i < patterns.length; i++) {
			patterns[i] = patterns[i].replace(".", "\\.").replace("*", ".*");
			Pattern found = Pattern.compile(patterns[i]);
			Matcher matcher = found.matcher(host);
			if (matcher.matches()) {
				return mode == SSLSortMode.INCLUDE;
			}
		}
		return mode == SSLSortMode.EXCLUDE;
	}

}
