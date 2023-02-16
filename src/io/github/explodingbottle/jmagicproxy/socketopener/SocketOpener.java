package io.github.explodingbottle.jmagicproxy.socketopener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * This class represents a Socket opener.
 * 
 * @author ExplodingBottle
 *
 */
public interface SocketOpener {

	/**
	 * Opens a socket using parameters.
	 * 
	 * @param host The host to connect to.
	 * @param port The port to connect to.
	 * @return The opened socket.
	 * @throws IOException If there was any issues while opening the socket.
	 */
	public Socket openedSocket(InetAddress host, int port) throws IOException;

}
