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
 * This class encapsulates the incoming transfer directive, which allows
 * modifications to the headers and a lot more.
 * 
 * @author ExplodingBottle
 *
 */
public class IncomingTransferDirective {

	private HttpResponse response;
	private ConnectionType newConnectionType;

	/**
	 * This is the constructor of the directive.
	 * 
	 * @param response          Represents the modified or original data to send to
	 *                          the client.
	 * @param newConnectionType Represents if the proxy must act with a connection
	 *                          mode of Keep-Alive or Close.
	 */
	public IncomingTransferDirective(HttpResponse response, ConnectionType newConnectionType) {
		this.response = response;
		this.newConnectionType = newConnectionType;
	}

	/**
	 * This function returns the HTTP response.
	 * 
	 * @return the HTTP response.
	 */
	public HttpResponse getResponse() {
		return response;
	}

	/**
	 * This function returns the connection type.
	 * 
	 * @return the connection type.
	 */
	public ConnectionType getConnectionType() {
		return newConnectionType;
	}
}
