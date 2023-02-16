package io.github.explodingbottle.jmagicproxy.socketopener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * 
 * This opener will create a standard socket.
 * 
 * @author ExplodingBottle
 *
 */
public class StandardSocketOpener implements SocketOpener {

	@Override
	public Socket openedSocket(InetAddress host, int port) throws IOException {
		return new Socket(host, port);
	}

}
