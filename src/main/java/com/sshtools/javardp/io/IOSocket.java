package com.sshtools.javardp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOptions;

public class IOSocket extends Socket {
	public IOSocket(final IO io) throws IOException {
		super(new SocketImpl() {
			Boolean noDelay = Boolean.FALSE;

			@Override
			public Object getOption(int optID) throws SocketException {
				if (optID == SocketOptions.TCP_NODELAY) {
					return noDelay;
				} else
					throw new IllegalArgumentException("Unknown option " + optID);
			}

			@Override
			public void setOption(int optID, Object value) throws SocketException {
				if (optID == SocketOptions.TCP_NODELAY) {
					this.noDelay = (Boolean) value;
				} else
					throw new IllegalArgumentException("Unknown option " + optID);
			}

			@Override
			protected void accept(SocketImpl s) throws IOException {
				throw new UnsupportedOperationException();
			}

			@Override
			protected int available() throws IOException {
				int r = io.getInputStream().available();
				System.out.println("r> " + r);
				return r;
			}

			@Override
			protected void bind(InetAddress host, int port) throws IOException {
				throw new UnsupportedOperationException();
			}

			@Override
			protected void close() throws IOException {
				io.closeIO();
			}

			@Override
			protected void connect(InetAddress address, int port) throws IOException {
				this.address = address;
				this.port = port;
			}

			@Override
			protected void connect(SocketAddress address, int timeout) throws IOException {
				if (address instanceof InetSocketAddress) {
					this.address = ((InetSocketAddress) address).getAddress();
					this.port = ((InetSocketAddress) address).getPort();
				} else
					throw new UnsupportedOperationException();
			}

			@Override
			protected void connect(String host, int port) throws IOException {
				throw new UnsupportedOperationException();
			}

			@Override
			protected void create(boolean stream) throws IOException {
				if (!stream)
					throw new UnsupportedOperationException();
			}

			@Override
			protected InputStream getInputStream() throws IOException {
				return io.getInputStream();
			}

			@Override
			protected OutputStream getOutputStream() throws IOException {
				return io.getOutputStream();
			}

			@Override
			protected void listen(int backlog) throws IOException {
			}

			@Override
			protected void sendUrgentData(int data) throws IOException {
			}
		});
		connect(new InetSocketAddress(InetAddress.getLocalHost(), 0));
	}
}
