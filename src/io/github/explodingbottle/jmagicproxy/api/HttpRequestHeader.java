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

import java.util.TreeMap;

/**
 * Represents a request header.
 * 
 * @author ExplodingBottle
 */
public class HttpRequestHeader {
	private HttpMethod method;
	private String host;
	private String httpVersion;

	/**
	 * This function will return the HTTP method.
	 * 
	 * @return the HTTP method.
	 */
	public HttpMethod getMethod() {
		return method;
	}

	/**
	 * This function will return the destination host.
	 * 
	 * @return the destination host.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * This function will set the destination host.
	 * 
	 * @param host the destination host.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * This function will return the HTTP Version.
	 * 
	 * @return the HTTP Version.
	 */
	public String getHttpVersion() {
		return httpVersion;
	}

	/**
	 * This function will return the list of headers.
	 * 
	 * @return the list containing the headers and their values.
	 */
	public TreeMap<String, String> getHeaders() {
		return headers;
	}

	private TreeMap<String, String> headers;

	/**
	 * The constructor of a HTTP Request. Let's take for example GET / HTTP/1.1
	 * 
	 * @param method      Corresponds to GET
	 * @param host        Corresponds to /
	 * @param httpVersion Corresponds to HTTP/1.1
	 * @param headers     Corresponds to the lines like Accept: xml/text
	 */
	public HttpRequestHeader(HttpMethod method, String host, String httpVersion, TreeMap<String, String> headers) {
		this.method = method;
		this.host = host;
		this.httpVersion = httpVersion;
		this.headers = headers;
	}

	public String toString() {
		String b = "{Method=" + method + ";Host=" + host + ";Version=" + httpVersion + ";Headers={";
		StringBuilder strb = new StringBuilder(b);
		headers.forEach((header, val) -> {
			strb.append(header + "=" + val + ";");
		});
		strb.append("}}");
		return strb.toString();
	}

	/**
	 * Creates a request line under the format GET / HTTP/1.1
	 * 
	 * @return the request line.
	 */
	public String toHttpRequestLine() {
		return method.toString() + " " + host + " " + httpVersion;
	}

	/**
	 * Creates a full request block.
	 * 
	 * @return the created request block.
	 */
	public String toHttpRequestBlock() {
		StringBuilder builder = new StringBuilder(toHttpRequestLine() + "\r\n");
		getHeaders().forEach((hKey, hVal) -> {
			builder.append(hKey + ": " + hVal + "\r\n");
		});
		builder.append("\r\n");
		return builder.toString();
	}

	/**
	 * Builds a HTTP Request Header using parameters.
	 * 
	 * @param method  The HTTP Method
	 * @param host    The destination host
	 * @param version The HTTP version
	 * @param headers HTTP Headers
	 * @return The built HTTP Request Header.
	 */
	public static HttpRequestHeader createFromParameters(HttpMethod method, String host, String version,
			TreeMap<String, String> headers) {
		if (headers == null)
			headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		return new HttpRequestHeader(method, host, version, headers);
	}

	/**
	 * Returns a new HTTP Request header from the parsed lines.
	 * 
	 * @param builder Contains the parsed lines.
	 * @return An instance of HttpRequestHeader which is linked to the builder.
	 */
	public static HttpRequestHeader createFromHeaderBlock(StringBuilder builder) throws MalformedParsableContent {
		HttpMethod method = null;
		String host = null;
		String httpVersion = null;
		String lines[] = builder.toString().replace("\r", "").split("\n");
		TreeMap<String, String> headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		if (lines.length <= 0) {
			throw new MalformedParsableContent("No content.");
		}
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (i == 0) { // Special case, it's the GET / HTTP/1.1 line
				String[] splitedFL = line.split(" ");
				if (splitedFL.length == 3) {
					try {
						method = HttpMethod.valueOf(splitedFL[0].toUpperCase());
					} catch (IllegalArgumentException e) {
						throw new MalformedParsableContent("First line is not a correct method.");
					}
					if (method == null)
						throw new MalformedParsableContent("First line is not a correct method.");
					host = splitedFL[1];
					httpVersion = splitedFL[2];
					if (!httpVersion.startsWith("HTTP/")) {
						throw new MalformedParsableContent("Request version doesn't starts with HTTP.");
					}
				} else {
					throw new MalformedParsableContent("First line doesn't have 3 chunks.");
				}
			} else {
				if (!line.trim().isEmpty()) {
					String[] splitedHH = line.split(": ");
					if (splitedHH.length >= 2) {
						String rebuiltSecHeader = "";
						for (int k = 1; k < splitedHH.length; k++) {
							if (k == splitedHH.length - 1)
								rebuiltSecHeader += splitedHH[k];
							else
								rebuiltSecHeader += splitedHH[k] + ": ";
						}
						headers.put(splitedHH[0], rebuiltSecHeader);
					}

				}
			}
		}
		return new HttpRequestHeader(method, host, httpVersion, headers);
	}
}
