package com.sshtools.javardp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketIO implements IO {
	private Socket socket;
	
	public SocketIO(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void closeIO() throws IOException {
		socket.close();		
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}
}
