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
import java.io.IOException;
import java.util.Properties;

/**
 * This class is very useful to find issues that may be related to the
 * java.security file with disabled algorithms.
 * 
 * @author ExplodingBottle
 *
 */
public class DisabledAlgorithmsWarner {

	private Properties javaSecurity;

	/**
	 * Gets ready the warner.
	 * 
	 * @throws IOException if an issue happens to load the file.
	 */
	public DisabledAlgorithmsWarner() throws IOException {
		javaSecurity = new Properties();
		FileInputStream is = new FileInputStream(
				new File(new File(new File(System.getProperty("java.home"), "lib"), "security"), "java.security"));
		javaSecurity.load(is);
		is.close();
	}

	public boolean mustWarn() {
		String found = javaSecurity.getProperty("jdk.tls.disabledAlgorithms");
		if (found != null && !found.trim().isEmpty())
			return true;
		return false;
	}

}
