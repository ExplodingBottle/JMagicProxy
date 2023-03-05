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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.api.ConnectionDirective;
import io.github.explodingbottle.jmagicproxy.api.HttpRequestHeader;
import io.github.explodingbottle.jmagicproxy.api.HttpResponse;
import io.github.explodingbottle.jmagicproxy.api.IncomingTransferDirective;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;

/**
 * This class is a custom proxy plugin used as an example for the new intercept
 * features.
 * 
 * Add it in the proxy configuration, with
 * io.github.explodingbottle.jmagicproxy.implementation.ServerProxy
 * 
 * This example will intercept and replace HTTP requests going to
 * http://explodingbottle.github.io/jmagicproxy/intercepttest
 * 
 * @author ExplodingBottle
 *
 */
public class ServerProxy extends BasicProxy {

	private ProxyLogger logger;
	private List<ConnectionDirective> toSpoof;

	public ServerProxy() {
		logger = ProxyMain.getLoggerProvider().createLogger();
		toSpoof = Collections.synchronizedList(new ArrayList<ConnectionDirective>()); // Keep in mind that it can be
																						// called from multiple threads.
	}

	// Here we tell the proxy core that we won't connect to the remote host.
	@Override
	public ConnectionDirective onReceiveProxyRequest(HttpRequestHeader request) {
		ConnectionDirective parentDirective = super.onReceiveProxyRequest(request);
		if (parentDirective.getHost().equalsIgnoreCase("explodingbottle.github.io")
				&& parentDirective.getOutcomingRequest().getHost().equalsIgnoreCase("/jmagicproxy/intercepttest")) {
			logger.log(LoggingLevel.INFO, "Intercepted a sample InterceptTest request.");
			parentDirective.setRemoteConnect(false);
			toSpoof.add(parentDirective);
		}
		return parentDirective;
	}

	// Here we give our own answer.
	@Override
	public byte[] getRawBytesToClient(ConnectionDirective linkedDirective,
			IncomingTransferDirective additionalInformations) {
		if (toSpoof.contains(linkedDirective)) {
			toSpoof.remove(linkedDirective);
			TreeMap<String, String> headers = new TreeMap<String, String>();
			headers.put("Connection", "Close");
			HttpResponse response = new HttpResponse("HTTP/1.1", 200, "OK", headers);
			logger.log(LoggingLevel.INFO, "Rewrote the sample InterceptTest request.");
			return (response.toHttpResponseBlock() + "Hello from JMagic Proxy !").getBytes();
		}
		return super.getRawBytesToClient(linkedDirective, additionalInformations);
	}

	// Code to try to prevent memory leaks.
	@Override
	public void onDirectiveClosed(ConnectionDirective directive) {
		logger.log(LoggingLevel.INFO, "Received a HTTP directive closure signal.");
		super.onDirectiveClosed(directive);
		toSpoof.remove(directive);
	}

}
