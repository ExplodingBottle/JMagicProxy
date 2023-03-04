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
package io.github.explodingbottle.jmagicproxy.implementation;

import java.util.TreeMap;

import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.api.ConnectionDirective;
import io.github.explodingbottle.jmagicproxy.api.ConnectionType;
import io.github.explodingbottle.jmagicproxy.api.HttpMethod;
import io.github.explodingbottle.jmagicproxy.api.HttpRequestHeader;
import io.github.explodingbottle.jmagicproxy.api.HttpResponse;
import io.github.explodingbottle.jmagicproxy.api.IncomingTransferDirective;
import io.github.explodingbottle.jmagicproxy.api.ProxyPlugin;
import io.github.explodingbottle.jmagicproxy.api.SSLControlDirective;
import io.github.explodingbottle.jmagicproxy.api.SSLControlInformations;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;
import io.github.explodingbottle.jmagicproxy.proxy.ssl.SSLSortEngine;

/**
 * This is a required basic implementation of the proxy, else it won't work.
 * 
 * @author ExplodingBottle
 *
 */
public class BasicProxy extends ProxyPlugin {

	private ProxyLogger logger;

	public BasicProxy() {
		logger = ProxyMain.getLoggerProvider().createLogger();
	}

	/**
	 * Will strip the http://google.com part from URLS.
	 * 
	 * @param host The host with the part to strip.
	 * @return the stripped host.
	 */
	String stripHostFromRequest(String host) {
		return host.replaceFirst("http:\\/\\/[^\\/]+", "").replaceFirst("https:\\/\\/[^\\/]+", "");
	}

	@Override
	public ConnectionDirective onReceiveProxyRequest(HttpRequestHeader request) {
		String host = request.getHost();
		HttpMethod method = request.getMethod();
		HttpRequestHeader httpReq = null;
		boolean isSSL = method == HttpMethod.CONNECT; // This is not the best method, as other kind of protocols could
														// also use CONNECT.
		String realHost = null;
		int realPort = 80;

		boolean isDirect = false;

		if (method == HttpMethod.CONNECT) {
			String[] splitedHostURL = host.split(":");
			if (splitedHostURL.length == 2) {
				realHost = splitedHostURL[0];
				try {
					realPort = Integer.parseInt(splitedHostURL[1]);
				} catch (NumberFormatException exc) {
					logger.log(LoggingLevel.WARN,
							"Failed to cast port number, no connection directive will be returned.", exc);
					return null;
				}
			} else {
				realHost = host;
				realPort = 443;
			}
			SSLSortEngine engine = ProxyMain.getSSLSortEngine();
			isDirect = !engine.shouldUseCustomPipe(realHost);
			if (isDirect) {
				logger.log(LoggingLevel.INFO, "SSLSortEngine decided that " + realHost + ":" + realPort
						+ " will be using direct connection.");
			}
		} else {
			String[] splitedHost = host.split("/");
			if (splitedHost.length >= 3) {
				String realSmallURLPart = splitedHost[2];
				String[] splitedHostURL = realSmallURLPart.split(":");
				if (splitedHostURL.length == 2) {
					realHost = splitedHostURL[0];
					try {
						realPort = Integer.parseInt(splitedHostURL[1]);
					} catch (NumberFormatException exc) {
						logger.log(LoggingLevel.WARN,
								"Failed to cast port number, no connection directive will be returned.", exc);
						return null;
					}
				} else {
					realHost = realSmallURLPart;
				}
				TreeMap<String, String> modifiedHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
				modifiedHeaders.putAll(request.getHeaders());
				String proxyConnection = modifiedHeaders.get("Proxy-Connection");
				logger.log(LoggingLevel.INFO, "Proxy-Connection header was set to " + proxyConnection + ".");
				modifiedHeaders.remove("Proxy-Connection");
				httpReq = HttpRequestHeader.createFromParameters(method, stripHostFromRequest(host),
						request.getHttpVersion(), modifiedHeaders);
			} else {
				logger.log(LoggingLevel.WARN, "Malformed input URL, no connection directive will be returned.");
				return null;
			}
		}
		return new ConnectionDirective(realHost, realPort, isSSL, httpReq, isDirect);
	}

	@Override
	public String returnPluginName() {
		return "Basic Proxy Plugin";
	}

	@Override
	public IncomingTransferDirective onReceiveServerAnswer(HttpResponse response) {
		TreeMap<String, String> headers = response.getHeaders();
		ConnectionType defaultConType = null;
		if (response.getHttpVersion().split("/")[1].equals("1.1")) { // Keep-Alive by default after HTTP/1.1
			defaultConType = ConnectionType.KEEPALIVE;
		} else if (response.getHttpVersion().split("/")[1].equals("1.0")) {
			defaultConType = ConnectionType.CLOSE;
		}
		if (headers.containsKey("Connection")) {
			logger.log(LoggingLevel.INFO, "Detected a Connection header with " + headers.get("Connection"));
			if ("Close".equalsIgnoreCase(headers.get("Connection")))
				defaultConType = ConnectionType.CLOSE;
			if ("Keep-Alive".equalsIgnoreCase(headers.get("Connection"))) {
				defaultConType = ConnectionType.KEEPALIVE;
			}
		}
		return new IncomingTransferDirective(response, defaultConType);
	}

	@Override
	public SSLControlDirective onReceiveProxyRequestSSL(SSLControlInformations request) {
		return new SSLControlDirective(request.getOriginalHost(), request.getOriginalPort(), true,
				request.getRequest());
	}

	@Override
	public HttpResponse onReceiveServerSSLAnswer(HttpResponse response) {
		return response;
	}

	@Override
	public byte[] getModifiedAnswerForServer(byte[] original, ConnectionDirective linkedDirective) {
		return original;
	}

	@Override
	public byte[] getModifiedAnswerForClient(byte[] original, ConnectionDirective linkedDirective,
			IncomingTransferDirective additionalInformations) {
		return original;
	}

	@Override
	public byte[] getModifiedAnswerForServerSSL(byte[] original, SSLControlDirective linkedDirective) {
		return original;
	}

	@Override
	public byte[] getModifiedAnswerForClientSSL(byte[] original, SSLControlDirective linkedDirective,
			HttpResponse additionalInformations) {
		return original;
	}

}
