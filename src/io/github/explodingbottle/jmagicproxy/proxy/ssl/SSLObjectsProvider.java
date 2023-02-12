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
package io.github.explodingbottle.jmagicproxy.proxy.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.github.explodingbottle.jmagicproxy.ProxyMain;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;

/**
 * This class has to handle the work of loading KeyStores and prepare factories.
 * 
 * @author ExplodingBottle
 *
 */
public class SSLObjectsProvider {

	private File keyStoreFile;
	private String password;
	private String keystoreType;

	private SSLServerSocketFactory factoryServer;
	private SSLSocketFactory factoryOutgoing;

	private ProxyLogger providerLogger;

	/**
	 * Constructs the provider using arguments.
	 * 
	 * @param keyStoreFile Represents where to go find the keystore.
	 * @param password     Represents where to go find the keystore's password.
	 * @param keystoreType Represents the file type of the keystore.
	 */
	public SSLObjectsProvider(File keyStoreFile, String password, String keystoreType) {
		this.keyStoreFile = keyStoreFile;
		this.password = password;
		this.keystoreType = keystoreType;
		providerLogger = ProxyMain.getLoggerProvider().createLogger();
	}

	/**
	 * This function will initialize factories.
	 */
	public void getFactoriesReady() {
		providerLogger.log(LoggingLevel.INFO, "Getting factories ready...");
		X509TrustManager[] trustManagers = new X509TrustManager[] { new AcceptAllCertificatesTrustManager() };
		try {
			SSLContext freeContext = SSLContext.getInstance("SSL");
			freeContext.init(null, trustManagers, SecureRandom.getInstanceStrong());
			factoryOutgoing = freeContext.getSocketFactory();
			providerLogger.log(LoggingLevel.INFO, "Outgoing factory is ready.");
		} catch (GeneralSecurityException e) {
			providerLogger.log(LoggingLevel.WARN, "Failed to instantiate the outgoing factory.", e);
		}
		try {
			KeyStore trustStore = KeyStore.getInstance(keystoreType);
			KeyStore keyStore = KeyStore.getInstance(keystoreType);

			FileInputStream keyStorePath = new FileInputStream(keyStoreFile);
			FileInputStream trustStorePath = new FileInputStream(keyStoreFile);
			trustStore.load(trustStorePath, password.toCharArray());
			keyStore.load(keyStorePath, password.toCharArray());
			trustStorePath.close();
			keyStorePath.close();

			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(trustStore);
			keyManagerFactory.init(keyStore, password.toCharArray());

			SSLContext context = SSLContext.getInstance("SSL");
			context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(),
					SecureRandom.getInstanceStrong());
			factoryServer = context.getServerSocketFactory();
			providerLogger.log(LoggingLevel.INFO, "Server factory is ready.");

		} catch (Exception e) {
			providerLogger.log(LoggingLevel.WARN, "Failed to instantiate the server factory.", e);
		}
		providerLogger.log(LoggingLevel.INFO, "Factories are now ready.");
	}

	/**
	 * Will returns the Server Factory, which is used to establish connections
	 * between the client and the proxy.
	 * 
	 * @return The Server Factory.
	 */
	public SSLServerSocketFactory getFactoryServer() {
		return factoryServer;
	}

	/**
	 * Will returns the Outgoing Factory, which is used to establish connections
	 * between the proxy and servers from the internet.
	 * 
	 * @return The Outgoing Factory.
	 */
	public SSLSocketFactory getFactoryOutgoing() {
		return factoryOutgoing;
	}
}
