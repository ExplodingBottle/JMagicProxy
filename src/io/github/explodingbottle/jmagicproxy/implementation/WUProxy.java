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
import io.github.explodingbottle.jmagicproxy.api.SSLControlDirective;
import io.github.explodingbottle.jmagicproxy.api.SSLControlInformations;
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
	public SSLControlDirective onReceiveProxyRequestSSL(SSLControlInformations request) {
		SSLControlDirective computed = super.onReceiveProxyRequestSSL(request);
		if (computed.getOutcomingRequest().getHost().toLowerCase().startsWith("/v6/selfupdate/")
				&& computed.getHost().equals("fe2.update.microsoft.com")) {
			logger.log(LoggingLevel.INFO, "Detected a selfupdate to replace line.");
			if (computed.getOutcomingRequest().getHost().toLowerCase().contains("/WSUS3/x86/Other/".toLowerCase())) {
				logger.log(LoggingLevel.INFO, "Using wsus3 special.");
				computed.getOutcomingRequest().setHost(computed.getOutcomingRequest().getHost()
						.replace("/v6/selfupdate/", "/v11/3/windowsupdate/selfupdate/"));
				computed.getOutcomingRequest().getHeaders().put("Host", "ds.download.windowsupdate.com");
				computed.setSSL(false);
				computed.setHost("ds.download.windowsupdate.com");
				computed.setPort(80);
			} else {
				logger.log(LoggingLevel.INFO, "Using default special fallback.");
				computed.getOutcomingRequest().setHost(computed.getOutcomingRequest().getHost()
						.replace("/v6/selfupdate/", "/v11/3/legacy/windowsupdate/selfupdate/"));
			}
		}
		if (computed.getOutcomingRequest().getHost().toLowerCase().startsWith("/v6/reportingwebservice/")
				&& computed.getHost().equals("fe2.update.microsoft.com")) {
			logger.log(LoggingLevel.INFO, "Using statsfe2 special.");
			computed.getOutcomingRequest().setHost(computed.getOutcomingRequest().getHost()
					.replace("/v6/ReportingWebService/", "/ReportingWebService/"));
			computed.setSSL(false);
			computed.setHost("statsfe2.update.microsoft.com");
			computed.setPort(80);
		}
		return computed;
	}

	@Override
	public String returnPluginName() {
		return "WinUpdPlugin"; // On the other I put spaces, here I don't..
	}

}
