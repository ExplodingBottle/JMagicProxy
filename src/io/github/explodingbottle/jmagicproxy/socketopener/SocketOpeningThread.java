package io.github.explodingbottle.jmagicproxy.socketopener;

import java.io.IOException;
import java.net.InetAddress;

/**
 * 
 * This class is used to try to initiate a connection.
 * 
 * @author ExplodingBottle
 *
 */
class SocketOpeningThread extends Thread {

	private InetAddress inetAddress;
	private int port;
	private SocketOpeningTool parent;
	private SocketOpener opener;

	SocketOpeningThread(InetAddress inetAddress, int port, SocketOpeningTool parent, SocketOpener opener) {
		this.inetAddress = inetAddress;
		this.port = port;
		this.parent = parent;
		this.opener = opener;
	}

	@Override
	public void run() {
		try {
			parent.receiveSocket(opener.openedSocket(inetAddress, port), this);
		} catch (IOException e) {
			parent.receiveSocket(null, this);
		}
	}

}
