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

/**
 * This class is here to store some hard-coded values, such as config file name.
 * 
 * @author ExplodingBottle
 */
public class HardcodedConfig {

	/**
	 * Preventing this class to be instantiated.
	 */
	private HardcodedConfig() {

	}

	/**
	 * Returns the path to the config file.
	 * 
	 * @return The config file name or path.
	 */
	public static String getConfigFileName() {
		return "jmagicproxy.cfg";
	}

	/**
	 * Returns the buffer size that will be used everywhere.
	 * 
	 * @return The buffer size.
	 */
	public static int returnBufferSize() {
		return 65536; // Note: I tried before allowing to change the buffer size but, for example, a
						// buffer size of 16 makes cURL not getting the whole content.
	}
}
