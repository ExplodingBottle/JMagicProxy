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

import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;

/**
 * This class represents the shutdown thread which is useful for hooking ^C
 * requests.
 * 
 * @author ExplodingBottle
 *
 */
public class ShutdownThread extends Thread {

	private boolean isShuttingDown;
	private ProxyLogger logger;

	/**
	 * Constructor of the thread.
	 */
	public ShutdownThread() {
		isShuttingDown = false;
		logger = ProxyMain.getLoggerProvider().createLogger();
	}

	public synchronized void start() {
		if (!isShuttingDown) {
			isShuttingDown = true;
			super.start();
		}
	}

	/**
	 * Returns if the shutdown thread has already been called.
	 * 
	 * @return If the shutdown is set to true.
	 */
	public boolean isShuttingDown() {
		return isShuttingDown;
	}

	public void run() {
		logger.log(LoggingLevel.INFO, "Shutdown thread has been invoked !");
		ProxyMain.shutdown();
	}

}
