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
package io.github.explodingbottle.jmagicproxy.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;

/**
 * This class handles the load of properties from a file.
 * 
 * @author ExplodingBottle
 */
public class PropertiesProvider {

	private File propsFile;
	private ProxyLogger selfLogger;
	private Properties loaded;
	private boolean noOverwrite;

	/**
	 * Constructs a new {@code PropertiesProvider}
	 * 
	 * @param propsFile Corresponds to the file that will be used to find
	 *                  configuration.
	 */
	public PropertiesProvider(File propsFile) {
		this.propsFile = propsFile;
		selfLogger = ProxyMain.getLoggerProvider().createLogger();
		loaded = new Properties();
		noOverwrite = false;
	}

	private void fillWithDefaults() {
		boolean atLeastOneReplace = false;
		for (PropertyKey pk : PropertyKey.values()) {
			if (!loaded.containsKey(pk.getPropertyKeyName())) {
				loaded.put(pk.getPropertyKeyName(), pk.getDefaultValue().toString());
				atLeastOneReplace = true;
			}
		}
		noOverwrite = noOverwrite || !atLeastOneReplace;
	}

	private void failWithMessage(String msg, Throwable e) {
		selfLogger.log(LoggingLevel.WARN, msg + " File won't be overwritten and default values will be used.", e);
		noOverwrite = true;
		fillWithDefaults();
	}

	/**
	 * Get a value as String.
	 * 
	 * @param propKey The key name that you want to get.
	 * @return The value as a String or null if not existing.
	 */
	public String getAsString(PropertyKey propKey) {
		if (propKey.getKeyType() == String.class) {
			return loaded.getProperty(propKey.getPropertyKeyName());
		}
		return null;
	}

	/**
	 * Get a value as Integer.
	 * 
	 * @param propKey The key name that you want to get.
	 * @return The value as an Integer or null if not existing and the default value
	 *         if a parse error occurred.
	 */
	public Integer getAsInteger(PropertyKey propKey) {
		if (propKey.getKeyType() == Integer.class) {
			String found = loaded.getProperty(propKey.getPropertyKeyName());
			try {
				return Integer.parseInt(found);
			} catch (NumberFormatException ignored) {

			}
		}
		return null;
	}

	/**
	 * Get a value as Boolean.
	 * 
	 * @param propKey The key name that you want to get.
	 * @return The value as an Boolean or null if not existing and the default value
	 *         if a parse error occurred.
	 */
	public Boolean getAsBoolean(PropertyKey propKey) {
		if (propKey.getKeyType() == Boolean.class) {
			return Boolean.parseBoolean(loaded.getProperty(propKey.getPropertyKeyName()));
		}
		return null;
	}

	public void saveConfiguration() {
		selfLogger.log(LoggingLevel.INFO, "Saving configuration...");
		if (noOverwrite) {
			selfLogger.log(LoggingLevel.INFO, "No overwrite flag has been triggered, skipping file save.");
			return;
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(propsFile);
		} catch (FileNotFoundException e) {
			selfLogger.log(LoggingLevel.WARN, "Failed to open properties file.");
		}
		if (fos != null) {
			try {
				loaded.store(fos, "JMagicProxy Configuration File");
			} catch (IOException e) {
				selfLogger.log(LoggingLevel.WARN, "Failed to write properties to file.", e);
			}
		}
		if (fos != null) {
			try {
				fos.close();
			} catch (IOException e) {
				selfLogger.log(LoggingLevel.WARN, "Failed to close properties.");
			}
		}
		selfLogger.log(LoggingLevel.INFO, "Configuration saved.");
	}

	/**
	 * Loads the configuration file to the loaded properties.
	 */
	public void loadConfiguration() {
		selfLogger.log(LoggingLevel.INFO, "Loading configuration...");
		if (!propsFile.exists()) {
			fillWithDefaults();
			selfLogger.log(LoggingLevel.INFO, "Configuration not found, a new file will be created, using defaults.");
			return;
		}
		FileInputStream reader = null;
		try {
			reader = new FileInputStream(propsFile);
		} catch (FileNotFoundException e) {
			failWithMessage("Failed to open properties file.", e);
		}
		if (reader != null) {
			try {
				loaded.load(reader);
			} catch (IOException e) {
				failWithMessage("Failed to read properties from file.", e);
			}
		}
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				selfLogger.log(LoggingLevel.WARN, "Failed to close properties.");
			}
		}
		fillWithDefaults();
		selfLogger.log(LoggingLevel.INFO, "Configuration loading done.");
	}

}
