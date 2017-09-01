package com.sshtools.javardp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class DefaultIO implements IO {
	private InetAddress address;
	private int port;
	private Socket socket;

	public DefaultIO(InetAddress server, int port) {
		this.address = server;
		this.port = port;
	}

	@Override
	public void closeIO() throws IOException {
		try {
			socket.close();
		} finally {
			socket = null;
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		checkConnected();
		return socket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		checkConnected();
		return socket.getOutputStream();
	}

	public Socket getSocket() {
		return socket;
	}

	void checkConnected() throws IOException {
		if (socket == null) {
			socket = new Socket(address, port);
		}
	}
}