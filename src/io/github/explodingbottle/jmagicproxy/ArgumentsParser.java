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

import java.util.TreeMap;

/**
 * This class is useful to parse arguments from the {@code main} method.
 * 
 * @author ExplodingBottle
 */
public class ArgumentsParser {

	private TreeMap<String, String> argsParsed;

	/**
	 * Initiates the parser with the arguments. Arguments will also be parsed while
	 * constructing.
	 * 
	 * @param args Arguments provided by the {@code main} method.
	 */
	public ArgumentsParser(String[] args) {
		argsParsed = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		for (String s : args) {
			if (s.startsWith("-")) {
				String[] split = s.split(":");
				String argument = split[0].replaceFirst("-", "");
				if (split.length > 1) {
					String p2 = "";
					for (int i = 1; i < split.length; i++) {
						if (i == split.length - 1)
							p2 += split[i];
						else
							p2 += split[i] + ":";
					}
					argsParsed.put(argument, p2);
				} else {
					argsParsed.put(argument, "");
				}
			}
		}
	}

	/**
	 * Get a parsed arguments by giving its name.
	 * 
	 * @param argName The name of the argument.
	 * @return The value of the argument or {@code null}.
	 */
	public String getArgumentByName(String argName) {
		return argsParsed.get(argName);
	}

}
