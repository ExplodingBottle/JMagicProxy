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
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import io.github.explodingbottle.jmagicproxy.HardcodedConfig;
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

	private byte[] fileReadingBuffer;
	private Map<Object, FileInputStream> readingFiles;
	private List<Object> toFinishDirectives;

	public BasicProxy() {
		logger = ProxyMain.getLoggerProvider().createLogger();
		readingFiles = Collections.synchronizedMap(new HashMap<Object, FileInputStream>());
		toFinishDirectives = Collections.synchronizedList(new ArrayList<Object>());
		fileReadingBuffer = new byte[HardcodedConfig.returnBufferSize()];
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

	private byte[] doReadingJob(Object linkedDirective) {
		if (toFinishDirectives.contains(linkedDirective)) {
			toFinishDirectives.remove(linkedDirective);
			return null;
		}
		boolean isUsingFile = false;
		File inFile = null;
		if (linkedDirective instanceof ConnectionDirective) {
			ConnectionDirective cd = ((ConnectionDirective) linkedDirective);
			isUsingFile = cd.isUsingFile();
			inFile = cd.getFileInput();
		}
		if (linkedDirective instanceof SSLControlDirective) {
			SSLControlDirective sslCd = ((SSLControlDirective) linkedDirective);
			isUsingFile = sslCd.isUsingFile();
			inFile = sslCd.getFileInput();
		}
		if (isUsingFile) {
			if (readingFiles.containsKey(linkedDirective)) {
				try {
					int read = readingFiles.get(linkedDirective).read(fileReadingBuffer, 0, fileReadingBuffer.length);
					if (read != -1) {
						byte[] b = new byte[read];
						for (int i = 0; i < read; i++) {
							b[i] = fileReadingBuffer[i];
						}
						return b;
					} else {
						readingFiles.get(linkedDirective).close();
						readingFiles.remove(linkedDirective);
					}
				} catch (IOException e) {
					logger.log(LoggingLevel.WARN, "Failed a file read.", e);
					try {
						readingFiles.get(linkedDirective).close();
						readingFiles.remove(linkedDirective);
					} catch (IOException e1) {
						logger.log(LoggingLevel.WARN, "Failed a file close.", e1);
					}
				}
			} else {
				if (!inFile.exists() || inFile.isDirectory()) {
					TreeMap<String, String> headers = new TreeMap<String, String>();
					headers.put("Connection", "Close");
					HttpResponse response = new HttpResponse("HTTP/1.1", 404, "Not Found", headers);
					toFinishDirectives.add(linkedDirective);
					return response.toHttpResponseBlock().getBytes();
				}
				logger.log(LoggingLevel.INFO, "Opening outgoing file input stream for " + inFile);
				try {
					FileInputStream inputStream = new FileInputStream(inFile);
					TreeMap<String, String> headers = new TreeMap<String, String>();
					headers.put("Connection", "Keep-Alive");
					SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
					f.setTimeZone(TimeZone.getTimeZone("GMT"));
					String lM = f.format(new Date(inFile.lastModified()));
					headers.put("Last-Modified", lM);
					headers.put("Content-Type", "application/octet-stream");
					headers.put("Content-Length", "" + inFile.length());
					HttpResponse response = new HttpResponse("HTTP/1.1", 200, "OK", headers);
					logger.log(LoggingLevel.INFO, "Outgoing fake socket opened (using file).");
					readingFiles.put(linkedDirective, inputStream);
					return response.toHttpResponseBlock().getBytes();

				} catch (IOException e) {
					TreeMap<String, String> headers = new TreeMap<String, String>();
					headers.put("Connection", "Close");
					HttpResponse response = new HttpResponse("HTTP/1.1", 500, "Internal Server Error", headers);
					toFinishDirectives.add(linkedDirective);
					logger.log(LoggingLevel.WARN, "Failed to make a fake connection using a file.", e);
					return response.toHttpResponseBlock().getBytes();
				}
			}
		}
		return null;
	}

	@Override
	public byte[] getRawBytesToClient(ConnectionDirective linkedDirective,
			IncomingTransferDirective additionalInformations) {
		return doReadingJob(linkedDirective);
	}

	@Override
	public byte[] getRawBytesToClientSSL(SSLControlDirective linkedDirective, HttpResponse additionalInformations) {
		return doReadingJob(linkedDirective);
	}

	private void filesCleanup(Object directive) {
		FileInputStream linkedStream = readingFiles.get(directive);
		if (linkedStream != null) {
			try {
				linkedStream.close();
			} catch (IOException e) {
				logger.log(LoggingLevel.WARN, "Failed to close a file stream for cleanup.");
			}
		}
		readingFiles.remove(directive);
		toFinishDirectives.remove(directive);
	}

	@Override
	public void onDirectiveClosed(ConnectionDirective directive) {
		filesCleanup(directive);
	}

	@Override
	public void onDirectiveClosedSSL(SSLControlDirective directive) {
		filesCleanup(directive);
	}

}
