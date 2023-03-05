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

import java.io.File;

/**
 * This class corresponds to a connection directive which will be delivered by a
 * plugin.
 * 
 * @author ExplodingBottle
 *
 */
public class ConnectionDirective {

	private String host;
	private int port;
	private boolean isSSL;
	private HttpRequestHeader outcomingRequest;

	private boolean isDirect;

	private File fileInput;

	private boolean isUsingFile;

	private boolean remoteConnect;

	/**
	 * Builds a Connection Directive.
	 * 
	 * @param host             Represents the host you want to connect to.
	 * @param port             Represents the port you want to access to.
	 * @param isSSL            Represents if there is a need of starting a SSL
	 *                         Server Socket.
	 * @param outcomingRequest Represents the out-coming first line sent to the
	 *                         server. Ignored if isSSL is true.
	 * @param isDirect         Represents if the SSL connection must be through our
	 *                         custom pipe or else direct ( with no modifications ).
	 *                         Only works with HTTPS, isSSL must be true.
	 */
	public ConnectionDirective(String host, int port, boolean isSSL, HttpRequestHeader outcomingRequest,
			boolean isDirect) {
		this.host = host;
		this.port = port;
		this.isSSL = isSSL;
		this.outcomingRequest = outcomingRequest;
		isUsingFile = false;
		this.isDirect = isDirect;
		remoteConnect = true;
	}

	/**
	 * This is a custom constructor for searching content of a file.
	 * 
	 * @param fileInput The file which will be used.
	 */
	public ConnectionDirective(File fileInput) {
		this.fileInput = fileInput;
		isUsingFile = true;
		remoteConnect = false;
	}

	/**
	 * Builds a connection directive without any form of connection.
	 */
	public ConnectionDirective() {
		remoteConnect = false;
	}

	public String toString() {
		if (!isUsingFile)
			return "{Host=" + host + ";Port=" + port + ";IsSSL=" + isSSL + ";OutcomingRequest=" + outcomingRequest
					+ ";IsDirect=" + isDirect + "}";
		else
			return "{UsingFile=true}";
	}

	/**
	 * Used to retrieve the file used to change content.
	 * 
	 * @return The file to replace with.
	 */
	public File getFileInput() {
		return fileInput;
	}

	/**
	 * Used to set the file used to change content.
	 * 
	 * @return The file to replace with.
	 */
	public void setFileInput(File input) {
		fileInput = input;
		if (fileInput != null)
			isUsingFile = true;
	}

	/**
	 * Used to see if the directive is using a file or an URL.
	 * 
	 * @return If the directive uses a file.
	 */
	public boolean isUsingFile() {
		return isUsingFile;
	}

	/**
	 * Gets the host to connect.
	 * 
	 * @return the host to connect.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Sets the host to connect.
	 * 
	 * @param host the host to connect.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Sets if we connect remotely or simulate a connection.
	 * 
	 * @param remoteConnect if we connect remotely or simulate a connection.
	 */
	public void setRemoteConnect(boolean remoteConnect) {
		this.remoteConnect = remoteConnect;
	}

	/**
	 * Gets if we connect remotely or simulate a connection.
	 * 
	 * @return if we connect remotely or simulate a connection.
	 */
	public boolean isRemoteConnect() {
		return remoteConnect;
	}

	/**
	 * Obtains the HTTP Request which is for the destination server.
	 * 
	 * @return the HTTP Request which is for the destination server.
	 */
	public HttpRequestHeader getOutcomingRequest() {
		return outcomingRequest;
	}

	/**
	 * Changes the HTTP Request which is for the destination server.
	 * 
	 * @param outcomingRequest the HTTP Request which is for the destination server.
	 */
	public void setOutcomingRequest(HttpRequestHeader outcomingRequest) {
		this.outcomingRequest = outcomingRequest;
	}

	/**
	 * Gets the port to connect.
	 * 
	 * @return the port to connect.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Sets the port to connect.
	 * 
	 * @param port the port to connect.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Gets if the connection will be in SSL.
	 * 
	 * @return True if the connection be in SSL.
	 */
	public boolean isSSL() {
		return isSSL;
	}

	/**
	 * Gets if the connection will be direct.
	 * 
	 * @return True if the connection be direct.
	 */
	public boolean isDirect() {
		return isDirect;
	}

	/**
	 * Sets if the connection will be direct.
	 * 
	 * @param isDirect corresponds to if the connection will be direct.
	 */
	public void setDirect(boolean isDirect) {
		this.isDirect = isDirect;
	}

	/**
	 * Sets if the connection will be in SSL.
	 * 
	 * @param isSSL corresponds to the SSL status of the connection.
	 */
	public void setSSL(boolean isSSL) {
		this.isSSL = isSSL;
	}

}
