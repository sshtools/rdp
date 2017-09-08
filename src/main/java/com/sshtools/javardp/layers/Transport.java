package com.sshtools.javardp.layers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.HexDump;
import com.sshtools.javardp.Packet;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.SecurityType;
import com.sshtools.javardp.State;
import com.sshtools.javardp.io.IO;
import com.sshtools.javardp.io.IOSocket;
import com.sshtools.javardp.io.SocketIO;
import com.sshtools.javardp.layers.nla.NLA;

public class Transport implements Layer<ISO> {
	static Logger logger = LoggerFactory.getLogger(Transport.class);
	public static final String[] CIPHERS = { "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
			"TLS_DHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
			"SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA", "SSL_RSA_WITH_DES_CBC_SHA",
			"SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
			"SSL_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA" };
	private State state;
	private DataInputStream in = null;
	private IO io;
	private DataOutputStream out = null;
	private ISO iso;

	public Transport(State state, ISO iso) {
		this.state = state;
		this.iso = iso;
	}

	public void sendPacket(Packet buffer) throws IOException {
		byte[] packet = new byte[buffer.getEnd()];
		buffer.copyToByteArray(packet, 0, 0, packet.length);
		if (state.getOptions().isDebugHexdump())
			HexDump.encode(packet, "SEND"/* System.out */);
		out.write(packet);
		out.flush();
	}

	public State getState() {
		return state;
	}

	public DataInputStream getIn() {
		return in;
	}

	public IO getIo() {
		return io;
	}

	public DataOutputStream getOut() {
		return out;
	}

	public void connect(IO io) throws IOException {
		this.io = io;
		this.in = new DataInputStream(new BufferedInputStream(io.getInputStream()));
		this.out = new DataOutputStream(new BufferedOutputStream(io.getOutputStream()));
	}

	public void startSsl() throws IOException, RdesktopException {
		try {
			this.io = this.negotiateSSL(this.io);
			this.in = new DataInputStream(this.io.getInputStream());
			this.out = new DataOutputStream(this.io.getOutputStream());
			logger.info("SSL handshake completed.");
		} catch (Exception e) {
			throw new RdesktopException("SSL negotiation failed: " + e.getMessage(), e);
		}
		if (state.getSecurityType() == SecurityType.HYBRID) {
			/* Start CredSSP! */
			NLA nla = new NLA(state, this);
			nla.start();
		}
	}

	/**
	 * Receive a specified number of bytes from the server, and store in a
	 * packet
	 * 
	 * @param p Packet to append data to, null results in a new packet being
	 *            created
	 * @param length Length of data to read
	 * @return Packet containing read data, appended to original data if
	 *         provided
	 * @throws IOException
	 */
	public Packet receivePacket(Packet p, int length) throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("receivePacket");
		Packet buffer = null;
		byte[] packet = new byte[length];
		in.readFully(packet, 0, length);
		if (state.getOptions().isDebugHexdump())
			HexDump.encode(packet, "RECEIVE" /* System.out */);
		if (p == null) {
			buffer = new Packet(length);
			buffer.copyFromByteArray(packet, 0, 0, packet.length);
			buffer.markEnd(length);
			buffer.setStart(buffer.getPosition());
		} else {
			buffer = new Packet((p.getEnd() - p.getStart()) + length);
			buffer.copyFromPacket(p, p.getStart(), 0, p.getEnd());
			buffer.copyFromByteArray(packet, 0, p.getEnd(), packet.length);
			buffer.markEnd(p.size() + packet.length);
			buffer.setPosition(p.getPosition());
			buffer.setStart(0);
		}
		return buffer;
	}

	protected IO negotiateSSL(IO io) throws Exception {
		Socket socket = new IOSocket(io);
		X509TrustManager tm = new X509TrustManager() {
			LinkedList<X509Certificate> listCert = new LinkedList<>();

			@Override
			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				throw new UnsupportedOperationException();
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				X509Certificate[] arrayOfX509Certificate;
				int j = (arrayOfX509Certificate = chain).length;
				for (int i = 0; i < j; i++) {
					X509Certificate x509Certificate = arrayOfX509Certificate[i];
					this.listCert.add(x509Certificate);
				}
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return this.listCert.toArray(new X509Certificate[this.listCert.size()]);
			}
		};
		SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(null, new X509TrustManager[] { tm }, null);
		SSLSocketFactory socketFactory = sc.getSocketFactory();
		logger.info("Initialising SSL");
		SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(socket, socket.getInetAddress().getHostName(),
				socket.getPort(), true);
		sslSocket.setEnabledCipherSuites(CIPHERS);
		logger.info("Starting SSL handshake");
		sslSocket.startHandshake();
		logger.info("Completed SSL handshake");
		return new SocketIO(sslSocket);
	}

	public boolean isConnected() {
		return io != null;
	}

	public void disconnect() {
		if (io == null)
			return;
		if (in != null) {
			try {
				in.close();
			} catch (IOException ioe) {
			}
		}
		if (out != null) {
			try {
				out.close();
			} catch (IOException ioe) {
			}
		}
		if (io != null) {
			try {
				io.closeIO();
			} catch (IOException ioe) {
			} finally {
				io = null;
			}
		}
	}

	@Override
	public ISO getParent() {
		return iso;
	}
}
