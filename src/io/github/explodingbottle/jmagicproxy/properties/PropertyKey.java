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

import io.github.explodingbottle.jmagicproxy.implementation.BasicProxy;

/**
 * 
 * This class is used to determine the different property keys.
 * 
 * @author ExplodingBottle
 *
 */
public enum PropertyKey {
	PROXY_LOGGING_LOGFILETMPL("proxy.logging.logfile", "log&$LNUM$.txt", String.class),
	PROXY_LOGGING_LOGSFOLDER("proxy.logging.logsfolder", "logs", String.class),
	PROXY_SERVER_PORT("proxy.server.port", 8087, Integer.class),
	PROXY_PLUGINS("proxy.plugins", BasicProxy.class.getName(), String.class),
	WUPROXY_REDIRECTJS("proxy.plugin.wuproxy.redirectjs", "content/redirect.js", String.class), // WUProxy Specific
	PROXY_SSL_ENABLED("proxy.ssl.enabled", false, Boolean.class),
	PROXY_SSL_KEYSTORE_PATH("proxy.ssl.keystorepath", "certs/keystore.p12", String.class),
	PROXY_SSL_KEYSTORE_PASSWORD("proxy.ssl.keystorepass", "Password", String.class),
	PROXY_SSL_KEYSTORE_TYPE("proxy.ssl.keystoretype", "pkcs12", String.class),
	PROXY_SSL_WARN_ALGORITHMS("proxy.ssl.warn.algorithms", false, Boolean.class),
	PROXY_SSL_SORT_MODE("proxy.ssl.sortmode", "NONE", String.class),
	PROXY_SSL_SORT_LIST("proxy.ssl.sortlist", "*", String.class),
	WUPROXY_REDIRECT_WUCLIENT("proxy.plugins.wuproxy.redirwuclient", false, Boolean.class),
	PROXY_SSL_SCAN_STARTING_PORT("proxy.ssl.scan.startingport", 9784, Integer.class);

	private String propKey;
	private Object defaultVal;
	private Class<?> type;

	/**
	 * Returns the property key name.
	 * 
	 * @return The property key name.
	 */
	public String getPropertyKeyName() {
		return propKey;
	}

	/**
	 * Returns the property default value as an object.
	 * 
	 * @return The property default value as an object.
	 */
	public Object getDefaultValue() {
		return defaultVal;
	}

	/**
	 * Returns the property key type.
	 * 
	 * @return The property key type.
	 */
	public Class<?> getKeyType() {
		return type;
	}

	/**
	 * Builds a property key.
	 * 
	 * @param propKey    The key name used in the configuration file.
	 * @param defaultVal The default value for the configuration file.
	 * @param type       The type of the value represented by a class such as
	 *                   {@code Boolean} or {@code Integer}.
	 */
	PropertyKey(String propKey, Object defaultVal, Class<?> type) {
		if (defaultVal.getClass() != type) {
			throw new IllegalArgumentException("Default value doesn't match key type.");
		}
		this.propKey = propKey;
		this.defaultVal = defaultVal;
		this.type = type;
	}
}
