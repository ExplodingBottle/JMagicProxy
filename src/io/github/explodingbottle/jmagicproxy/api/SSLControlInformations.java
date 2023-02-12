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
package io.github.explodingbottle.jmagicproxy.api;

/**
 * This class represents the informations that will be passed to the proxy for a
 * incoming SSL connection.
 * 
 * @author ExplodingBottle
 *
 */
public class SSLControlInformations {

	private HttpRequestHeader request;
	private String originalHost;
	private int originalPort;

	/**
	 * Builds a control information object.
	 * 
	 * @param request      The request that is decoded.
	 * @param originalHost The original clear-text host.
	 * @param originalPort The original clear-text port.
	 */
	public SSLControlInformations(HttpRequestHeader request, String originalHost, int originalPort) {
		this.request = request;
		this.originalHost = originalHost;
		this.originalPort = originalPort;
	}

	/**
	 * Returns the HTTP request header.
	 * 
	 * @return the HTTP request header.
	 */
	public HttpRequestHeader getRequest() {
		return request;
	}

	/**
	 * Sets the HTTP request header.
	 * 
	 * @param request the HTTP request header.
	 */
	public void setRequest(HttpRequestHeader request) {
		this.request = request;
	}

	/**
	 * Returns the original host.
	 * 
	 * @return the original host.
	 */
	public String getOriginalHost() {
		return originalHost;
	}

	/**
	 * Sets the original host.
	 * 
	 * @param originalHost the original host.
	 */
	public void setOriginalHost(String originalHost) {
		this.originalHost = originalHost;
	}

	/**
	 * Returns the original port.
	 * 
	 * @return the original port.
	 */
	public int getOriginalPort() {
		return originalPort;
	}

	/**
	 * Sets the original port.
	 * 
	 * @param originalPort the original port.
	 */
	public void setOriginalPort(int originalPort) {
		this.originalPort = originalPort;
	}

}
