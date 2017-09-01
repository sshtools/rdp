/*
 * ISO.java Component: ProperJavaRDP Revision: $Revision: 1.1 $ Author: $Author:
 * brett $ Date: $Date: 2011/11/28 14:13:42 $ Copyright (c) 2005 Propero Limited
 * Purpose: ISO layer of communication
 */
package com.sshtools.javardp.layers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.HexDump;
import com.sshtools.javardp.IContext;
import com.sshtools.javardp.OrderException;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.RdpPacket;
import com.sshtools.javardp.SecurityType;
import com.sshtools.javardp.State;
import com.sshtools.javardp.crypto.CryptoException;
import com.sshtools.javardp.io.DefaultIO;
import com.sshtools.javardp.io.IO;
import com.sshtools.javardp.io.SocketIO;

public class ISO {
	public static final String[] CIPHERS = { "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA",
			"TLS_DHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
			"SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA", "SSL_RSA_WITH_DES_CBC_SHA",
			"SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
			"SSL_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA" };
	static Logger logger = LoggerFactory.getLogger(ISO.class);
	private static final int CONNECTION_CONFIRM = 0xD0;
	/* this for the ISO Layer */
	private static final int CONNECTION_REQUEST = 0xE0;
	private static final int DATA_TRANSFER = 0xF0;
	private static final int DISCONNECT_REQUEST = 0x80;
	private static final int EOT = 0x80;
	private static final int ERROR = 0x70;
	private static final int PROTOCOL_VERSION = 0x03;
	private IContext context;
	private HexDump dump = null;
	private DataInputStream in = null;
	private IO io;
	private DataOutputStream out = null;
	private State state;

	/**
	 * Construct ISO object, initialises hex dump
	 */
	public ISO(IContext context, State state) {
		this.context = context;
		this.state = state;
		dump = new HexDump();
	}

	/**
	 * Connect to a server
	 * 
	 * @param host Address of server
	 * @param port Port to connect to on server
	 * @throws IOException
	 * @throws RdesktopException
	 * @throws OrderException
	 * @throws CryptoException
	 */
	public void connect(IO io) throws IOException, RdesktopException, OrderException, CryptoException {
		this.io = io;
		int[] code = new int[1];
		// this.in = new InputStreamReader(rdpsock.getInputStream());
		this.in = new DataInputStream(new BufferedInputStream(io.getInputStream()));
		this.out = new DataOutputStream(new BufferedOutputStream(io.getOutputStream()));
		send_connection_request();
		RdpPacket neg = receiveMessage(code);
		if (code[0] != CONNECTION_CONFIRM) {
			throw new RdesktopException("Expected CC got:" + Integer.toHexString(code[0]).toUpperCase());
		}
		if (state.isRDP5() && neg.getPosition() < neg.capacity()) {
			if (neg.get8() != 0x02)
				throw new RdesktopException("Was expecting negotiation success response");
			int flags = neg.get8(); // negotiation flags
			neg.incrementPosition(2); // length, always 8
			int selectedProtocol = neg.getLittleEndian32();
			state.setNegotiated();
			SecurityType[] supportedTypes = SecurityType.fromMasks(selectedProtocol);
			List<SecurityType> t = Arrays.asList(supportedTypes);
			logger.info(String.format("Server supports: %s", t));
			SecurityType wanted = state.getSecurityType();
			if(!t.contains(wanted)) {
				int ord = wanted.ordinal();
				ord--;
				SecurityType alt = null;
				if(ord > 0) {
					alt = SecurityType.values()[ord];
				}
				else {
					ord += 2;
					if(ord < SecurityType.values().length)
						alt = SecurityType.values()[ord];
					else 
						alt = SecurityType.STANDARD;
				}
				logger.warn(String.format("Server does not support requested security type %s, downgrading to %s.", wanted, alt));
				state.setSecurityType(alt);
			}
		} else {
			if(state.isRDP5()) {
				logger.info("Downgrading to RDP4 because no negotiation.");
				state.setRDP5(false);
				state.setServerBpp(8);
			}
			if (state.getSecurityType().ordinal() > SecurityType.STANDARD.ordinal()) {
				logger.info("Downgrading security type to %s from %s because no negotiation.");
				state.setSecurityType(SecurityType.STANDARD);
			}
		}
		if (state.getSecurityType().isSSL()) {
			logger.info("Protocol requires SSL, instantiating.");
			try {
				this.io = this.negotiateSSL(this.io);
				this.in = new DataInputStream(this.io.getInputStream());
				this.out = new DataOutputStream(this.io.getOutputStream());
				logger.info("SSL handshake completed.");
			} catch (Exception e) {
				throw new RdesktopException("SSL negotiation failed: " + e.getMessage(), e);
			}
			
			if(state.getSecurityType() == SecurityType.HYBRID) {
				/* Start CredSSP! */
				
			}
		}
	}

	/**
	 * Disconnect from an RDP session, closing all sockets
	 */
	public void disconnect() {
		if (io == null)
			return;
		try {
			sendMessage(DISCONNECT_REQUEST);
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			if (io != null)
				io.closeIO();
		} catch (IOException e) {
			in = null;
			out = null;
			io = null;
			return;
		}
		in = null;
		out = null;
		io = null;
	}

	/**
	 * Initialise an ISO PDU
	 * 
	 * @param length Desired length of PDU
	 * @return Packet configured as ISO PDU, ready to write at higher level
	 */
	public RdpPacket init(int length) {
		RdpPacket data = new RdpPacket(length + 7);// getMemory(length+7);
		data.incrementPosition(7);
		data.setStart(data.getPosition());
		return data;
	}

	/**
	 * Receive a data transfer message from the server
	 * 
	 * @return Packet containing message (as ISO PDU)
	 * @throws IOException
	 * @throws RdesktopException
	 * @throws OrderException
	 * @throws CryptoException
	 */
	public RdpPacket receive() throws IOException, RdesktopException, OrderException, CryptoException {
		int[] type = new int[1];
		RdpPacket buffer = receiveMessage(type);
		if (buffer == null)
			return null;
		if (type[0] != DATA_TRANSFER) {
			throw new RdesktopException("Expected DT got:" + type[0]);
		}
		return buffer;
	}

	/**
	 * Send a packet to the server, wrapped in ISO PDU
	 * 
	 * @param buffer Packet containing data to send to server
	 * @throws RdesktopException
	 * @throws IOException
	 */
	public void send(RdpPacket buffer) throws RdesktopException, IOException {
		if (io == null || out == null)
			return;
		if (buffer.getEnd() < 0) {
			throw new RdesktopException("No End Mark!");
		} else {
			int length = buffer.getEnd();
			byte[] packet = new byte[length];
			// RdpPacket data = this.getMemory(length+7);
			buffer.setPosition(0);
			buffer.set8(PROTOCOL_VERSION); // Version
			buffer.set8(0); // reserved
			buffer.setBigEndian16(length); // length of packet
			buffer.set8(2); // length of header
			buffer.set8(DATA_TRANSFER);
			buffer.set8(EOT);
			buffer.copyToByteArray(packet, 0, 0, buffer.getEnd());
			if (state.getOptions().isDebugHexdump())
				dump.encode(packet, "SEND"/* System.out */);
			out.write(packet);
			out.flush();
		}
	}

	protected IO negotiateSSL(IO io) throws Exception {
		// Socket socket = new IOSocket(io);
		Socket socket = ((DefaultIO) io).getSocket();
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

	/**
	 * Send the server a connection request, detailing client protocol version
	 * 
	 * @throws IOException
	 */
	void send_connection_request() throws IOException {
		String uname = state.getOptions().getUsername();
		if (uname.length() > 9)
			uname = uname.substring(0, 9);
		int length = 11 + (state.getOptions().getUsername().length() > 0 ? ("Cookie: mstshash=".length() + uname.length() + 2) : 0)
				+ 8;
		if (state.isRDP5()) {
			length += 8;
		}
		RdpPacket buffer = new RdpPacket(length);
		byte[] packet = new byte[length];
		buffer.set8(PROTOCOL_VERSION); // send Version Info
		buffer.set8(0); // reserved byte
		buffer.setBigEndian16(length); // Length
		buffer.set8(length - 5); // Length of Header
		buffer.set8(CONNECTION_REQUEST);
		buffer.setBigEndian16(0); // Destination reference ( 0 at CC and DR)
		buffer.setBigEndian16(0); // source reference should be a reasonable
		// address we use 0
		buffer.set8(0); // service class
		if (state.getOptions().getUsername().length() > 0) {
			logger.debug("Including username");
			buffer.out_uint8p("Cookie: mstshash=", "Cookie: mstshash=".length());
			buffer.out_uint8p(uname, uname.length());
			buffer.set8(0x0d); // terminator for cookie 1
			buffer.set8(0x0a); // terminator for cookie 2
		}
		/* Negotiation request */
		if (state.isRDP5()) {
			buffer.set8(0x01);
			buffer.set8(0); // 0x01 for admin mode, 0x02 for redirected auth,
							// 0x08 for correlation info present
			buffer.setLittleEndian16(8);
			buffer.setLittleEndian32(state.getSecurityType().getMask());
		}
		/*
		 * // Authentication request? buffer.setLittleEndian16(0x01);
		 * buffer.setLittleEndian16(0x08); // Do we try to use SSL?
		 * buffer.set8(Options.use_ssl? 0x01 : 0x00);
		 * buffer.incrementPosition(3);
		 */
		buffer.copyToByteArray(packet, 0, 0, packet.length);
		out.write(packet);
		out.flush();
	}

	private RdpPacket receiveMessage(int[] type) throws IOException, RdesktopException, OrderException, CryptoException {
		int[] rdpver = new int[1];
		return receiveMessageex(type, rdpver);
	}

	/**
	 * Receive a message from the server
	 * 
	 * @param type Array containing message type, stored in type[0]
	 * @return Packet object containing data of message
	 * @throws IOException
	 * @throws RdesktopException
	 * @throws OrderException
	 * @throws CryptoException
	 */
	private RdpPacket receiveMessageex(int[] type, int[] rdpver)
			throws IOException, RdesktopException, OrderException, CryptoException {
		logger.debug("ISO.receiveMessage");
		RdpPacket s = null;
		int length, version;
		next_packet: while (true) {
			logger.debug("next_packet");
			s = tcp_recv(null, 4);
			if (s == null)
				return null;
			version = s.get8();
			rdpver[0] = version;
			if (version == 3) {
				s.incrementPosition(1); // pad
				length = s.getBigEndian16();
			} else {
				length = s.get8();
				if ((length & 0x80) != 0) {
					length &= ~0x80;
					length = (length << 8) + s.get8();
				}
			}
			s = tcp_recv(s, length - 4);
			if (s == null)
				return null;
			if ((version & 3) == 0) {
				logger.debug("Processing rdp5 packet of " + length);
				context.getRdp().rdp5_process(s, (version & 0x80) != 0);
				continue next_packet;
			} else
				break;
		}
		s.get8();
		type[0] = s.get8();
		if (type[0] == DATA_TRANSFER) {
			logger.debug("Data Transfer Packet of " + length);
			s.incrementPosition(1); // eot
			return s;
		}
		logger.debug("Message of " + length);
		s.incrementPosition(5); // dst_ref, src_ref, class
		return s;
	}

	/**
	 * Send a self contained iso-pdu
	 * 
	 * @param type one of the following CONNECT_RESPONSE, DISCONNECT_REQUEST
	 * @exception IOException when an I/O Error occurs
	 */
	private void sendMessage(int type) throws IOException {
		RdpPacket buffer = new RdpPacket(11);// getMemory(11);
		byte[] packet = new byte[11];
		buffer.set8(PROTOCOL_VERSION); // send Version Info
		buffer.set8(0); // reserved byte
		buffer.setBigEndian16(11); // Length
		buffer.set8(6); // Length of Header
		buffer.set8(type); // where code = CR or DR
		buffer.setBigEndian16(0); // Destination reference ( 0 at CC and DR)
		buffer.setBigEndian16(0); // source reference should be a reasonable
		// address we use 0
		buffer.set8(0); // service class
		buffer.copyToByteArray(packet, 0, 0, packet.length);
		out.write(packet);
		out.flush();
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
	private RdpPacket tcp_recv(RdpPacket p, int length) throws IOException {
		logger.debug("ISO.tcp_recv");
		RdpPacket buffer = null;
		byte[] packet = new byte[length];
		in.readFully(packet, 0, length);
		// try{ }
		// catch(IOException e){ logger.warn("IOException: " + e.getMessage());
		// return null; }
		if (state.getOptions().isDebugHexdump())
			dump.encode(packet, "RECEIVE" /* System.out */);
		if (p == null) {
			buffer = new RdpPacket(length);
			buffer.copyFromByteArray(packet, 0, 0, packet.length);
			buffer.markEnd(length);
			buffer.setStart(buffer.getPosition());
		} else {
			buffer = new RdpPacket((p.getEnd() - p.getStart()) + length);
			buffer.copyFromPacket(p, p.getStart(), 0, p.getEnd());
			buffer.copyFromByteArray(packet, 0, p.getEnd(), packet.length);
			buffer.markEnd(p.size() + packet.length);
			buffer.setPosition(p.getPosition());
			buffer.setStart(0);
		}
		return buffer;
	}
}
