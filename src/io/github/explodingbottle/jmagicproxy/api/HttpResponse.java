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
 * This class encapsulates a HTTP response without the content.
 * 
 * @author ExplodingBottle
 *
 */
public class HttpResponse {

	private String version;
	private int responseCode;
	private String responseMessage;
	private TreeMap<String, String> headers;

	/**
	 * The constructor of a HTTP Response. Let's take for example HTTP/1.1 200 OK
	 * 
	 * @param version         Corresponds to HTTP/1.1
	 * @param responseCode    Corresponds to 200
	 * @param responseMessage Corresponds to OK
	 * @param headers         Corresponds to the lines like Connection: Keep-Alive
	 */
	public HttpResponse(String version, int responseCode, String responseMessage, TreeMap<String, String> headers) {
		this.version = version;
		this.responseCode = responseCode;
		this.responseMessage = responseMessage;
		// Sanitizing
		TreeMap<String, String> headersSan = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		headers.forEach((s1, s2) -> {
			headersSan.put(s1, s2);
		});
		this.headers = headersSan;
	}

	public String toString() {
		String b = "{Message=" + responseMessage + ";ResponseCode=" + responseCode + ";Version=" + version
				+ ";Headers={";
		StringBuilder strb = new StringBuilder(b);
		headers.forEach((header, val) -> {
			strb.append(header + "=" + val + ";");
		});
		strb.append("}}");
		return strb.toString();
	}

	/**
	 * Creates a response line under the format HTTP/1.1 200 OK
	 * 
	 * @return the request line.
	 */
	public String toHttpResponseLine() {
		return version + " " + responseCode + " " + responseMessage;
	}

	/**
	 * Creates a full response block.
	 * 
	 * @return the created response block.
	 */
	public String toHttpResponseBlock() {
		StringBuilder builder = new StringBuilder(toHttpResponseLine() + "\r\n");
		getHeaders().forEach((hKey, hVal) -> {
			builder.append(hKey + ": " + hVal + "\r\n");
		});
		builder.append("\r\n");
		return builder.toString();
	}

	/**
	 * Returns a new HTTP Response from the parsed lines.
	 * 
	 * @param builder Contains the parsed lines.
	 * @return An instance of HttpResponse which is linked to the builder.
	 */
	public static HttpResponse createFromHeaderBlock(StringBuilder builder) throws MalformedParsableContent {
		String responseMessage = "";
		Integer responseCode = null;
		String httpVersion = null;
		String lines[] = builder.toString().replace("\r", "").split("\n");
		if (lines.length <= 0)
			throw new MalformedParsableContent("Totally invalid response block.");
		TreeMap<String, String> headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		if (lines.length <= 0) {
			throw new MalformedParsableContent("No content.");
		}
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (i == 0) { // Special case, it's the first line..
				String[] splitedFL = line.split(" ");
				if (splitedFL.length >= 3) {
					httpVersion = splitedFL[0];
					if (!httpVersion.startsWith("HTTP/")) {
						throw new MalformedParsableContent("Response version doesn't starts with HTTP.");
					}
					try {
						responseCode = new Integer(Integer.parseInt(splitedFL[1]));
					} catch (Exception e) {
						throw new MalformedParsableContent(
								"Response code is not a valid number, " + responseCode + ".");
					}
					for (int k = 2; k < splitedFL.length; k++) {
						if (k == splitedFL.length - 1) {
							responseMessage += splitedFL[k];
						} else {
							responseMessage += splitedFL[k] + " ";
						}
					}
				} else {
					throw new MalformedParsableContent("First line doesn't have at least 3 chunks.");
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
		return new HttpResponse(httpVersion, responseCode, responseMessage, headers);
	}

	/**
	 * This function will return the HTTP response message.
	 * 
	 * @return the HTTP response message.
	 */
	public String getResponseMessage() {
		return responseMessage;
	}

	/**
	 * This function will return the response code.
	 * 
	 * @return the response code.
	 */
	public int getResponseCode() {
		return responseCode;
	}

	/**
	 * This function will return the HTTP Version.
	 * 
	 * @return the HTTP Version.
	 */
	public String getHttpVersion() {
		return version;
	}

	/**
	 * This function will return the list of headers.
	 * 
	 * @return the list containing the headers and their values.
	 */
	public TreeMap<String, String> getHeaders() {
		return headers;
	}

}
