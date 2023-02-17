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
