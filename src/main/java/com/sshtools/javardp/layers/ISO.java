/*
 * ISO.java Component: ProperJavaRDP Revision: $Revision: 1.1 $ Author: $Author:
 * brett $ Date: $Date: 2011/11/28 14:13:42 $ Copyright (c) 2005 Propero Limited
 * Purpose: ISO layer of communication
 */
package com.sshtools.javardp.layers;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.IContext;
import com.sshtools.javardp.OrderException;
import com.sshtools.javardp.Packet;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.SecurityType;
import com.sshtools.javardp.State;
import com.sshtools.javardp.io.IO;

public class ISO implements Layer<MCS> {
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
	private State state;
	private Transport transport;
	private MCS mcs;

	/**
	 * Construct ISO object, initialises hex dump
	 * 
	 * @param context context
	 * @param state state
	 * @param mcs mcs
	 */
	public ISO(IContext context, State state, MCS mcs) {
		this.context = context;
		this.state = state;
		this.mcs = mcs;
		transport = new Transport(state, this);
	}

	/**
	 * Connect to a server
	 * 
	 * @param io IO
	 * @throws IOException on error
	 * @throws RdesktopException on error
	 */
	public void connect(IO io) throws IOException, RdesktopException {
		transport.connect(io);
		int[] code = new int[1];
		// this.in = new InputStreamReader(rdpsock.getInputStream());
		logger.info(String.format("Connecting with %s enabled.", state.getOptions().getSecurityTypes()));
		send_connection_request();
		Packet neg = receiveMessage(code);
		if (code[0] != CONNECTION_CONFIRM) {
			throw new RdesktopException("Expected CC got:" + Integer.toHexString(code[0]).toUpperCase());
		}
		if (state.isRDP5() && neg.getPosition() < neg.capacity()) {
			int result = neg.get8();
			neg.get8(); // negotiation flags
			neg.incrementPosition(2); // length, always 8
			if (result == 0x02) {
				int selectedProtocol = neg.getLittleEndian32();
				state.setNegotiated();
				List<SecurityType> supportedTypes = Arrays.asList(SecurityType.fromMasks(selectedProtocol));
				logger.info(String.format("Server supports: %s", supportedTypes));
				SecurityType wanted = state.getSecurityType();
				if (!supportedTypes.contains(wanted)) {
					List<SecurityType> weSupport = new ArrayList<>(state.getOptions().getSecurityTypes());
					weSupport.retainAll(supportedTypes);
					if (weSupport.isEmpty())
						throw new RdesktopException("We do not support any of the security protocols the server wants to use.");
					SecurityType alt = weSupport.get(weSupport.size() - 1);
					logger.warn(String.format("Server does not support requested security type %s, downgrading to %s.", wanted, alt));
					state.setSecurityType(alt);
				}
			}
			else {
				// 2.2.1.2.2 RDP Negotiation Failure (RDP_NEG_FAILURE)
				switch(neg.getLittleEndian32()) {
				case 0x0000001:
					throw new IOException("SSL_REQUIRED_BY_SERVER. The server requires that the client support Enhanced RDP Security (section 5.4) with either TLS 1.0, 1.1 or 1.2 (section 5.4.5.1) or CredSSP (section 5.4.5.2). If only CredSSP was requested then the server only supports TLS. ");
				case 0x0000002:
					throw new IOException("SSL_NOT_ALLOWED_BY_SERVER. The server is configured to only use Standard RDP Security mechanisms (section 5.3) and does not support any External Security Protocols (section 5.4.5).");
				case 0x0000003:
					throw new IOException("SSL_CERT_NOT_ON_SERVER. The server does not possess a valid authentication certificate and cannot initialize the External Security Protocol Provider (section 5.4.5).");
				case 0x0000004:
					throw new IOException("INCONSISTENT_FLAGS. The list of requested security protocols is not consistent with the current security protocol in effect. This error is only possible when the Direct Approach (sections 5.4.2.2 and 1.3.1.2) is used and an External Security Protocol (section 5.4.5) is already being used.");
				case 0x0000005:
					throw new IOException("HYBRID_REQUIRED_BY_SERVER. The server requires that the client support Enhanced RDP Security (section 5.4) with CredSSP (section 5.4.5.2).");
				case 0x0000006:
					throw new IOException("SSL_WITH_USER_AUTH_REQUIRED_BY_SERVER. The server requires that the client support Enhanced RDP Security (section 5.4) with TLS 1.0, 1.1 or 1.2 (section 5.4.5.1) and certificate-based client authentication.");
				default:
					throw new IOException(String.format("Unknown error code 0x%0000000x."));
				}
			}
		} else {
			if (state.isRDP5()) {
				logger.info("Downgrading to RDP4 because no negotiation.");
				state.setRDP5(false);
				state.setServerBpp(8);
			}
			if (state.getSecurityType().ordinal() > SecurityType.STANDARD.ordinal()) {
				logger.warn(String.format("Downgrading security type to %s from %s because no negotiation.", SecurityType.STANDARD, state.getSecurityType()));
				state.setSecurityType(SecurityType.STANDARD);
			}
		}
		if (state.getSecurityType().isSSL()) {
			logger.info("Protocol requires SSL, instantiating.");
			transport.startSsl();
		}
	}

	public Transport getTransport() {
		return transport;
	}

	/**
	 * Disconnect from an RDP session, closing all sockets
	 */
	public void disconnect() {
		if (!transport.isConnected())
			return;
		try {
			sendMessage(DISCONNECT_REQUEST);
			transport.disconnect();
		} catch (IOException e) {
			transport.disconnect();
		}
	}

	/**
	 * Initialise an ISO PDU
	 * 
	 * @param length Desired length of PDU
	 * @return Packet configured as ISO PDU, ready to write at higher level
	 */
	public Packet init(int length) {
		Packet data = new Packet(length + 7);// getMemory(length+7);
		data.incrementPosition(7);
		data.setStart(data.getPosition());
		return data;
	}

	/**
	 * Receive a data transfer message from the server
	 * 
	 * @return Packet containing message (as ISO PDU)
	 * @throws IOException on error
	 * @throws RdesktopException  on error
	 */
	public Packet receive() throws IOException, RdesktopException {
		int[] type = new int[1];
		Packet buffer = receiveMessage(type);
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
	 * @throws RdesktopException on error
	 * @throws IOException on error
	 */
	public void send(Packet buffer) throws RdesktopException, IOException {
		if (buffer.getEnd() < 0) {
			throw new RdesktopException("No End Mark!");
		} else {
			int length = buffer.getEnd();
			// RdpPacket data = this.getMemory(length+7);
			buffer.setPosition(0);
			buffer.set8(PROTOCOL_VERSION); // Version
			buffer.set8(0); // reserved
			buffer.setBigEndian16(length); // length of packet
			buffer.set8(2); // length of header
			buffer.set8(DATA_TRANSFER);
			buffer.set8(EOT);
			transport.sendPacket(buffer);
		}
	}

	/**
	 * Send the server a connection request, detailing client protocol version
	 * 
	 * See MS-RDPBCGR - 2.2.1.1 Client X.224 Connection Request PDU
	 * 
	 * @throws IOException
	 */
	void send_connection_request() throws IOException {
		String cookie = state.getCookie();
		if (cookie != null && cookie.length() > 9)
			cookie = cookie.substring(0, 9);
		int length = 11 + (StringUtils.isNotBlank(state.getCookie()) ? ("Cookie: mstshash=".length() + cookie.length() + 2) : 0)
				+ 8;
		if (state.isRDP5()) {
			length += 8;
		}
		Packet buffer = new Packet(length);
		buffer.set8(PROTOCOL_VERSION); // send Version Info
		buffer.set8(0); // reserved byte
		buffer.setBigEndian16(length); // Length
		buffer.set8(length - 5); // Length of Header
		buffer.set8(CONNECTION_REQUEST);
		buffer.setBigEndian16(0); // Destination reference ( 0 at CC and DR)
		buffer.setBigEndian16(0); // source reference should be a reasonable
		// address we use 0
		buffer.set8(0); // service class
		if (StringUtils.isNotBlank(cookie)) {
			if (logger.isDebugEnabled())
				logger.debug("Including username");
			buffer.out_uint8p("Cookie: mstshash=", "Cookie: mstshash=".length());
			buffer.out_uint8p(cookie, cookie.length());
			buffer.set8(0x0d); // terminator for cookie 1
			buffer.set8(0x0a); // terminator for cookie 2
		}
		/* Negotiation request. See MS-RDPBCGR - 2.2.1.1.1 RDP Negotiation Request (RDP_NEG_REQ) */
		if (state.isRDP5()) {
			buffer.set8(0x01);
			buffer.set8(0); // 0x01 for admin mode, 0x02 for redirected auth,
							// 0x08 for correlation info present
			buffer.setLittleEndian16(8);
			//buffer.setLittleEndian32(state.getSecurityType().getMask());
			buffer.setLittleEndian32(state.getOptions().getSecurityTypesMask());
		}
		/*
		 * // Authentication request? buffer.setLittleEndian16(0x01);
		 * buffer.setLittleEndian16(0x08); // Do we try to use SSL?
		 * buffer.set8(Options.use_ssl? 0x01 : 0x00);
		 * buffer.incrementPosition(3);
		 */
		transport.sendPacket(buffer);
	}

	private Packet receiveMessage(int[] type) throws IOException, RdesktopException {
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
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 */
	private Packet receiveMessageex(int[] type, int[] rdpver) throws IOException, RdesktopException {
		if (logger.isDebugEnabled())
			logger.debug("ISO.receiveMessage");
		Packet s = null;
		int length, version;
		next_packet: while (true) {
			if (logger.isDebugEnabled())
				logger.debug("next_packet");
			s = transport.receivePacket(null, 4);
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
			s = transport.receivePacket(s, length - 4);
			if (s == null)
				return null;
			if ((version & 3) == 0) {
				if (logger.isDebugEnabled())
					logger.debug("Processing rdp5 packet of " + length);
				mcs.getParent().getParent().rdp5_process(s, (version & 0x80) != 0);
				continue next_packet;
			} else
				break;
		}
		s.get8();
		type[0] = s.get8();
		if (type[0] == DATA_TRANSFER) {
			if (logger.isDebugEnabled())
				logger.debug("Data Transfer Packet of " + length);
			s.incrementPosition(1); // eot
			return s;
		}
		if (logger.isDebugEnabled())
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
		Packet buffer = new Packet(11);// getMemory(11);
		buffer.set8(PROTOCOL_VERSION); // send Version Info
		buffer.set8(0); // reserved byte
		buffer.setBigEndian16(11); // Length
		buffer.set8(6); // Length of Header
		buffer.set8(type); // where code = CR or DR
		buffer.setBigEndian16(0); // Destination reference ( 0 at CC and DR)
		buffer.setBigEndian16(0); // source reference should be a reasonable
		// address we use 0
		buffer.set8(0); // service class
		transport.sendPacket(buffer);
	}

	@Override
	public MCS getParent() {
		return mcs;
	}
}
