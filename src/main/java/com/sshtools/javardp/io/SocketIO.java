package com.sshtools.javardp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.security.cert.Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketIO implements IO {
	static Logger logger = LoggerFactory.getLogger(SocketIO.class);
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

	@Override
	public byte[] getPublicKey() {
		try {
			if ((socket instanceof SSLSocket)) {
				return getPkFromSocket((SSLSocket) socket);
			}
		} catch (SSLPeerUnverifiedException sslpue) {
			logger.warn("Failed to get public key.", sslpue);
		} catch (IllegalArgumentException sslpue) {
			logger.warn("Failed to get public key.", sslpue);
		}
		return new byte[0];
	}

	public static byte[] getPkFromSocket(SSLSocket socket) throws SSLPeerUnverifiedException {
		SSLSession sesh = socket.getSession();
		Certificate[] certificates = sesh.getPeerCertificates();
		int j = certificates.length;
		for (int i = 0; i < j; i++) {
			PublicKey publicKey = certificates[i].getPublicKey();
			if (publicKey != null) {
				byte[] tmp = publicKey.getEncoded();
				byte[] pk = new byte[tmp.length - 24];
				System.arraycopy(tmp, 24, pk, 0, pk.length);
				return pk;
			}
		}
		throw new IllegalArgumentException("No public keys.");
	}

	@Override
	public String getAddress() {
		return socket.getInetAddress().getHostName();
	}
}
