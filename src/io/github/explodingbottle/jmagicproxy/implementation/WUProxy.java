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

import java.io.File;

import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.api.ConnectionDirective;
import io.github.explodingbottle.jmagicproxy.api.HttpRequestHeader;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;
import io.github.explodingbottle.jmagicproxy.properties.PropertyKey;

/**
 * This is a custom implementation of BasicProxy to fix Windows Update related
 * issues.
 * 
 * @author ExplodingBottle
 *
 */
public class WUProxy extends BasicProxy {

	private ProxyLogger logger;

	public WUProxy() {
		logger = ProxyMain.getLoggerProvider().createLogger();
	}

	private static final String DLREP1_REPLACEMENT = "/v9/windowsupdate/a/selfupdate/";
	private static final String DLREP2_REPLACEMENT = "/v11/3/windowsupdate/selfupdate/";
	private static final String MU_DLREP1_REPLACEMENT = "/v9/microsoftupdate/a/selfupdate/";
	private static final String MU_DLREP2_REPLACEMENT = "/v9/1/microsoftupdate/b/selfupdate/";

	@Override
	public ConnectionDirective onReceiveProxyRequest(HttpRequestHeader request) {
		ConnectionDirective computed = super.onReceiveProxyRequest(request);

		if (computed.getHost().equalsIgnoreCase("www.update.microsoft.com")) {
			computed.setHost("fe2.update.microsoft.com");
			if (computed.getOutcomingRequest() != null) {
				computed.getOutcomingRequest().getHeaders().put("Host", "fe2.update.microsoft.com");
			}
			logger.log(LoggingLevel.INFO, "Replaced to a fe2 request.");
		}
		if (ProxyMain.getPropertiesProvider().getAsBoolean(PropertyKey.WUPROXY_REDIRECT_WUCLIENT)) {
			if (request.getHost().toLowerCase().contains(DLREP2_REPLACEMENT)) {
				computed.setHost("download.windowsupdate.com");
				computed.setPort(80);
				String preHost = computed.getOutcomingRequest().getHost();
				computed.getOutcomingRequest()
						.setHost(preHost.toLowerCase().replace(DLREP2_REPLACEMENT, DLREP1_REPLACEMENT));
				if (computed.getOutcomingRequest() != null) {
					computed.getOutcomingRequest().getHeaders().put("Host", "download.windowsupdate.com");
				}
				logger.log(LoggingLevel.INFO,
						"Replaced " + preHost + " by " + computed.getOutcomingRequest().getHost() + ".");
			}
		}

		if (ProxyMain.getPropertiesProvider().getAsBoolean(PropertyKey.WUPROXY_REDIRECT_WUCLIENT)) {
			if (request.getHost().toLowerCase().contains(MU_DLREP2_REPLACEMENT)) {
				computed.setHost("download.windowsupdate.com");
				computed.setPort(80);
				String preHost = computed.getOutcomingRequest().getHost();
				computed.getOutcomingRequest()
						.setHost(preHost.toLowerCase().replace(MU_DLREP2_REPLACEMENT, MU_DLREP1_REPLACEMENT));
				if (computed.getOutcomingRequest() != null) {
					computed.getOutcomingRequest().getHeaders().put("Host", "download.windowsupdate.com");
				}
				logger.log(LoggingLevel.INFO,
						"Replaced " + preHost + " by " + computed.getOutcomingRequest().getHost() + ".");
			}
		}

		if (request.getHost().toLowerCase().contains("/windowsupdate/v6/shared/js/redirect.js")
				|| request.getHost().toLowerCase().contains("/microsoftupdate/v6/shared/js/redirect.js")) {
			logger.log(LoggingLevel.INFO, "Found a redirect.js request.");
			File file = new File(ProxyMain.getPropertiesProvider().getAsString(PropertyKey.WUPROXY_REDIRECTJS));
			if (file.exists()) {
				return new ConnectionDirective(file);
			} else {
				logger.log(LoggingLevel.WARN, "The redirect.js file located in the configuration file is missing.");
			}
		}
		return computed;
	}

	@Override
	public String returnPluginName() {
		return "WinUpdPlugin"; // On the other I put spaces, here I don't..
	}

}
