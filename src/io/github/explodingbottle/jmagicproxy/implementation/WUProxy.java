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

	private static final String WUWEB_SITE_REPLACEMENT = "/v11/3/windowsupdate/SelfUpdate/AU/%ARCH%/XP/en/wuweb.cab";
	// private static final String MUWEB_SITE_REPLACEMENT =
	// "/v9/microsoftupdate/a/selfupdate/WSUS3/%ARCH%/Other/muweb.cab";

	private static final String DLREP1_REPLACEMENT = "/v9/windowsupdate/a/selfupdate/";
	private static final String DLREP2_REPLACEMENT = "/v11/3/windowsupdate/selfupdate/";

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
		if (request.getHost().toLowerCase().contains("/windowsupdate/v6/v5controls/")
				|| request.getHost().toLowerCase().contains("/microsoftupdate/v6/v5controls/")) {
			String arch = "unk_arch";
			String query = computed.getOutcomingRequest().getHost();
			if (query.contains("/x86/")) {
				arch = "x86";
			}
			if (query.contains("/x64/")) {
				arch = "x64";
			}
			// Commented because ds.download.windowsupdate.com doesn't seem to contain ia64
			// binaries ( even if the Windows Update website contains them )
			// if (query.contains("/ia64/")) {
			// arch = "ia64";
			// }
			String replacement = null;
			if (query.toLowerCase().contains("/wuweb_site.cab"))
				replacement = WUWEB_SITE_REPLACEMENT;
			// It is not really a good thing to support muweb.
			// if (query.toLowerCase().contains("/muweb_site.cab"))
			// replacement = MUWEB_SITE_REPLACEMENT;
			if (replacement == null) {
				logger.log(LoggingLevel.WARN,
						"Failed to find if it was wuweb_site.cab or muweb_site.cab for " + query + ".");
			} else {
				if (arch.equals("unk_arch")) {
					logger.log(LoggingLevel.WARN, "Failed to parse architecture for " + query + ".");
					logger.log(LoggingLevel.WARN, "wuweb_site.cab or muweb_site.cab request won't be redirected.");
				} else {
					computed.setHost("ds.download.windowsupdate.com");
					computed.setPort(80);
					computed.getOutcomingRequest().setHost(replacement.replace("%ARCH%", arch));
					if (computed.getOutcomingRequest() != null) {
						computed.getOutcomingRequest().getHeaders().put("Host", "ds.download.windowsupdate.com");
					}
					logger.log(LoggingLevel.INFO,
							"Replaced to a wuweb_site.cab request to " + computed.getOutcomingRequest().getHost());
				}
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
	public SSLControlDirective onReceiveProxyRequestSSL(SSLControlInformations request) {
		SSLControlDirective computed = super.onReceiveProxyRequestSSL(request);
		// This has been removed because it is now useless. We can directly use the
		// updates website instead of using the fe2.update.microsoft.com WSUS server
		// gateway.
		/*
		 * if (computed.getOutcomingRequest().getHost().toLowerCase().startsWith(
		 * "/v6/selfupdate/") && computed.getHost().equals("fe2.update.microsoft.com"))
		 * { logger.log(LoggingLevel.INFO, "Detected a selfupdate to replace line."); if
		 * (computed.getOutcomingRequest().getHost().toLowerCase().contains(
		 * "/WSUS3/x86/Other/".toLowerCase())) { logger.log(LoggingLevel.INFO,
		 * "Using wsus3 special.");
		 * computed.getOutcomingRequest().setHost(computed.getOutcomingRequest().getHost
		 * () .replace("/v6/selfupdate/", "/v11/3/windowsupdate/selfupdate/"));
		 * computed.getOutcomingRequest().getHeaders().put("Host",
		 * "ds.download.windowsupdate.com"); computed.setSSL(false);
		 * computed.setHost("ds.download.windowsupdate.com"); computed.setPort(80); }
		 * else { logger.log(LoggingLevel.INFO, "Using default special fallback.");
		 * computed.getOutcomingRequest().setHost(computed.getOutcomingRequest().getHost
		 * () .replace("/v6/selfupdate/", "/v11/3/legacy/windowsupdate/selfupdate/")); }
		 * } if (computed.getOutcomingRequest().getHost().toLowerCase().startsWith(
		 * "/v6/reportingwebservice/") &&
		 * computed.getHost().equals("fe2.update.microsoft.com")) {
		 * logger.log(LoggingLevel.INFO, "Using statsfe2 special.");
		 * computed.getOutcomingRequest().setHost(computed.getOutcomingRequest().getHost
		 * () .replace("/v6/ReportingWebService/", "/ReportingWebService/"));
		 * computed.setSSL(false); computed.setHost("statsfe2.update.microsoft.com");
		 * computed.setPort(80); }
		 */
		return computed;
	}

	@Override
	public String returnPluginName() {
		return "WinUpdPlugin"; // On the other I put spaces, here I don't..
	}

}
