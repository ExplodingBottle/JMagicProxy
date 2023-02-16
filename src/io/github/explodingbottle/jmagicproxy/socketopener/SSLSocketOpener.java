package io.github.explodingbottle.jmagicproxy.socketopener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

/**
 * 
 * This opener will create a SSL socket.
 * 
 * @author ExplodingBottle
 * 
 */
public class SSLSocketOpener implements SocketOpener {

	private SSLSocketFactory socketFactory;

	/**
	 * This constructor will prepare the opener using a SSL Socket Factory.
	 * 
	 * @param socketFactory Represents the SSL Socket Factory that will be used.
	 */
	public SSLSocketOpener(SSLSocketFactory socketFactory) {
		this.socketFactory = socketFactory;
	}

	@Override
	public Socket openedSocket(InetAddress host, int port) throws IOException {
		return socketFactory.createSocket(host, port);
	}

}
