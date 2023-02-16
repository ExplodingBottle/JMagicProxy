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
package io.github.explodingbottle.jmagicproxy;

import java.io.File;
import java.io.IOException;

import io.github.explodingbottle.jmagicproxy.api.PluginsManager;
import io.github.explodingbottle.jmagicproxy.logging.LoggerProvider;
import io.github.explodingbottle.jmagicproxy.logging.LoggingLevel;
import io.github.explodingbottle.jmagicproxy.logging.ProxyLogger;
import io.github.explodingbottle.jmagicproxy.properties.PropertiesProvider;
import io.github.explodingbottle.jmagicproxy.properties.PropertyKey;
import io.github.explodingbottle.jmagicproxy.proxy.ssl.SSLObjectsProvider;
import io.github.explodingbottle.jmagicproxy.server.SocketAcceptorThread;

/**
 * This is the main class of the proxy.
 * 
 * @author ExplodingBottle
 */
public class ProxyMain {

	private static LoggerProvider lgp;
	private static PropertiesProvider propsProvider;
	private static PluginsManager pluginsManager;

	/**
	 * Returns the logger provider.
	 * 
	 * @return The logger provider.
	 */
	public static LoggerProvider getLoggerProvider() {
		return lgp;
	}

	/**
	 * Returns the properties provider.
	 * 
	 * @return The properties provider.
	 */
	public static PropertiesProvider getPropertiesProvider() {
		return propsProvider;
	}

	/**
	 * Returns the plugins manager.
	 * 
	 * @return The plugins manager.
	 */
	public static PluginsManager getPluginsManager() {
		return pluginsManager;
	}

	private static ProxyLogger mainLogger;

	private static ShutdownThread shutdownThread;

	/**
	 * Returns the shutdown thread linked to this class.
	 * 
	 * @return The shutdown thread.
	 */
	public static ShutdownThread getShutdownThread() {
		return shutdownThread;
	}

	/**
	 * Returns the SSL objects provider linked to this class.
	 * 
	 * @return The SSL objects provider.
	 */
	public static SSLObjectsProvider getSSLObjectsProvider() {
		return sslObjectsProvider;
	}

	private static SocketAcceptorThread socketAcceptor;

	private static SSLObjectsProvider sslObjectsProvider;

	/**
	 * Initiates a shutdown.
	 */
	synchronized static void shutdown() {
		mainLogger.log(LoggingLevel.INFO, "Recieved shutdown signal.");
		if (socketAcceptor != null)
			socketAcceptor.closeServerSocket();
		propsProvider.saveConfiguration();
		lgp.closeLogStream();
		mainLogger.log(LoggingLevel.INFO, "Proxy has been fully shut down.");
	}

	/**
	 * This is the main method.
	 * 
	 * @param args Represents the command line arguments.
	 */
	public static void main(String[] args) {
		ArgumentsParser agp = new ArgumentsParser(args);
		String config = HardcodedConfig.getConfigFileName();
		String ovc = agp.getArgumentByName("override-config");
		String help = agp.getArgumentByName("help");
		if (help != null) {
			System.out.println("JMagicProxy - Help");
			System.out.println("\t-help\t\tDisplays help.");
			System.out.println("\t-override-config:<cfgf>\t\tOverrides config with cfgf.");
			return;
		}
		if (ovc != null) {
			config = ovc;
		}
		System.out.println("Please report any bugs to https://github.com/ExplodingBottle/JMagicProxy/issues so a fix can be found.");
		System.out.println();
		lgp = new LoggerProvider(true);
		shutdownThread = new ShutdownThread();
		Runtime.getRuntime().addShutdownHook(shutdownThread);
		mainLogger = lgp.createLogger();
		propsProvider = new PropertiesProvider(new File(config));
		propsProvider.loadConfiguration();
		String logPath = propsProvider.getAsString(PropertyKey.PROXY_LOGGING_LOGFILETMPL).replace("&$LNUM$",
				System.currentTimeMillis() + "");
		File logsFolder = new File(propsProvider.getAsString(PropertyKey.PROXY_LOGGING_LOGSFOLDER));
		if (!logsFolder.exists()) {
			if (!logsFolder.mkdirs()) {
				mainLogger.log(LoggingLevel.WARN,
						"Failed to create the logs folder, this may cause issues afterwards.");
			}
		}
		lgp.openLogStream(new File(logsFolder, logPath));
		pluginsManager = new PluginsManager(propsProvider.getAsString(PropertyKey.PROXY_PLUGINS));
		pluginsManager.loadPlugins();
		if (propsProvider.getAsBoolean(PropertyKey.PROXY_SSL_WARN_ALGORITHMS)) {
			DisabledAlgorithmsWarner warner = new DisabledAlgorithmsWarner();
			if (warner.mustWarn()) {
				mainLogger.log(LoggingLevel.WARN,
						"The system has detected that algorithms were present in the jdk.tls.disabledAlgorithms property of java.security. "
								+ "This will cause issues with SSL and old algorithms.");
			}
		}
		if (propsProvider.getAsBoolean(PropertyKey.PROXY_SSL_ENABLED)) {
			mainLogger.log(LoggingLevel.INFO, "SSL is enabled, proceeding to SSL setup.");
			sslObjectsProvider = new SSLObjectsProvider(
					new File(propsProvider.getAsString(PropertyKey.PROXY_SSL_KEYSTORE_PATH)),
					propsProvider.getAsString(PropertyKey.PROXY_SSL_KEYSTORE_PASSWORD),
					propsProvider.getAsString(PropertyKey.PROXY_SSL_KEYSTORE_TYPE));
			sslObjectsProvider.getFactoriesReady();
		} else {
			mainLogger.log(LoggingLevel.INFO, "SSL is not enabled.");
		}
		socketAcceptor = new SocketAcceptorThread(propsProvider.getAsInteger(PropertyKey.PROXY_SERVER_PORT));
		socketAcceptor.start();
		mainLogger.log(LoggingLevel.INFO, "Pressing Backspace in the console will send the shutdown signal.");
		try {
			// If someone has a proper fix, please do a Pull Request.
			while (System.in.available() == 0 && !shutdownThread.isShuttingDown()
					&& !Thread.currentThread().isInterrupted()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					mainLogger.log(LoggingLevel.WARN, "Failed to wait for 1s before doing the System.in check.", e);
				}
			}
		} catch (IOException e) {
			mainLogger.log(LoggingLevel.WARN, "Failed to wait for the shutdown signal coming from the user.", e);
		}
		mainLogger.log(LoggingLevel.INFO, "User is asking to terminate, sending shutdown signal.");
		shutdownThread.start();

	}

}
