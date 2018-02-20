package com.sshtools.javardp.layers.nla;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.openmuc.jasn1.ber.BerByteArrayOutputStream;
import org.openmuc.jasn1.ber.BerInputStream;
import org.openmuc.jasn1.ber.types.BerInteger;
import org.openmuc.jasn1.ber.types.BerOctetString;
import org.openmuc.jasn1.ber.types.BerSequence;
import org.openmuc.jasn1.ber.types.BerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.HexDump;
import com.sshtools.javardp.Packet;
import com.sshtools.javardp.RdesktopCryptoException;
import com.sshtools.javardp.State;
import com.sshtools.javardp.layers.Transport;

public class NLA {
	static Logger logger = LoggerFactory.getLogger(NLA.class);
	private State state;
	private Transport transport;

	interface TSCredentialType extends BerPayload {
	}

	class TSPasswordCreds implements TSCredentialType {
		private String domainName;
		private String userName;
		private char[] password;

		@Override
		public BerType write() throws IOException {
			return new BerSequence(new BerOctetString(domainName), new BerOctetString(userName), new BerOctetString(password));
		}
	}

	class TSCredentials implements BerPayload {
		private int credType;
		private TSCredentialType credentials;

		@Override
		public BerType write() throws IOException {
			return new BerSequence(new BerInteger(credType), credentials.write());
		}
	}

	public NLA(State state, Transport transport) {
		this.state = state;
		this.transport = transport;
	}

	@SuppressWarnings("resource")
	public void start() throws IOException, RdesktopCryptoException {
		
		// MS-NLMP - 1.3.1.1 NTLM Connection-Oriented Call Flow
		
		/*
		 * Over the encrypted TLS channel, the SPNEGO, Kerberos, or NTLM
		 * handshake between the client and server completes authentication and
		 * establishes an encryption key that is used by the SPNEGO
		 * confidentiality services, as specified in [RFC4178]. All SPNEGO
		 * tokens or Kerberos/NTLM messages as well as the underlying encryption
		 * algorithms are opaque to the calling application (the CredSSP client
		 * and CredSSP server). The wire protocol for SPNEGO, Kerberos, and NTLM
		 * is specified in [MS-SPNG], [MS-KILE], and [MS-NLMP], respectively.
		 * The SPNEGO tokens or Kerberos/NTLM messages exchanged between the
		 * client and the server are encapsulated in the negoTokens field of the
		 * TSRequest structure (section 2.2.1). Both the client and the server
		 * useOver the encrypted TLS channel, the SPNEGO, Kerberos, or NTLM
		 * handshake between the client and server completes authentication and
		 * establishes an encryption key that is used by the SPNEGO
		 * confidentiality services, as specified in [RFC4178]. All SPNEGO
		 * tokens or Kerberos/NTLM messages as well as the underlying encryption
		 * algorithms are opaque to the calling application (the CredSSP client
		 * and CredSSP server). The wire protocol for SPNEGO, Kerberos, and NTLM
		 * is specified in [MS-SPNG], [MS-KILE], and [MS-NLMP], respectively.
		 * The SPNEGO tokens or Kerberos/NTLM messages exchanged between the
		 * client and the server are encapsulated in the negoTokens field of the
		 * TSRequest structure (section 2.2.1). Both the client and the server
		 * use
		 */
		NTLMState ntlm = new NTLMState(state);
		
		
		
		byte[] negotiateData = new NTLMNegotiate(ntlm).write().getBytes();
		HexDump.encode(negotiateData, "NEG DATA");
		TSRequest req = new TSRequest(negotiateData);
		BerType send = req.write();
		BerByteArrayOutputStream bos = new BerByteArrayOutputStream();
		send.encode(bos, true);
		
		// MS-NLMP - 2.2.1.1 NEGOTIATE_MESSAGE		
		logger.info("Sending NTLM Negotiate");
		ntlm.dumpFlags();
		transport.sendPacket(new Packet(bos.getArray()));
		
		// MS-NLMP - 2.2.1.2 CHALLENGE_MESSAGE
		req.read(new BerInputStream(transport.getIn()).next());
		NTLMResponse response = new NTLMResponse(ntlm);
		byte[] responseData = req.getNegoData();
		logger.info("Received NTLM Challenge");
		HexDump.encode(responseData, "NTLM Challenge");
		response.read(new NTLMPacket(responseData).setPosition(0));
		
		/*
		 * Build the authentication response but don't set it yet as we may need
		 * to create a signature and then set the MIC
		 * 
		 * MS-NLMP - 2.2.1.3 AUTHENTICATE_MESSAGE
		 */
		NTLMAuthenticate auth = new NTLMAuthenticate(ntlm);
		byte[] authData = auth.write().getBytes();
		

		HexDump.encode(authData, "AUTH DATA");
		
		/* Configure targetInfo block for response */
		if (ntlm.getAvPairs().getTimestamp() > 0) {
			ntlm.getAvPairs().setFlags(ntlm.getAvPairs().getFlags() | 0x02);
			/* MIC */
			try {
				Mac mac = Mac.getInstance("HmacMD5");
				mac.init(new SecretKeySpec(ntlm.getExportedSessionKey(), "HmacMD5"));
				mac.update(authData);
				mac.update(negotiateData);
				mac.update(responseData);
				byte[] mic = mac.doFinal();
				auth.setMIC(mic);
				/* Public Key */
				byte[] publicKey = transport.getIo().getPublicKey();
				if (publicKey != null)
					req.setPubKeyAuth(ntlm.encryptMessage(ntlm.getClientSealKey(), publicKey, ntlm.getClientSeal()));
				/* Replace existing authData with a new one that has a MIC */
				authData = auth.write().getBytes();
			} catch (NoSuchAlgorithmException nsae) {
				throw new RdesktopCryptoException("Failed to create MIC.", nsae);
			} catch (InvalidKeyException e) {
				throw new RdesktopCryptoException("Failed to create MIC.", e);
			}
		}
		else {
			ntlm.getAvPairs().setTimestamp(System.currentTimeMillis());
		}
		ntlm.getAvPairs().setChannelHash(new byte[16]);
		ntlm.getAvPairs().setTargetName("TERMSRV/" + transport.getIo().getAddress());
		req.setNegoData(authData);
		send = req.write();
		bos = new BerByteArrayOutputStream();
		send.encode(bos, true);
		logger.info("Sending NTLM Authenticate");
		byte[] authPacketData = bos.getArray();
		HexDump.encode(authPacketData, "AUTH PACKET DATA");
		transport.sendPacket(new Packet(authPacketData));
		logger.info("Receiving NTLM Authenticate Response");
		ByteArrayOutputStream bbos = new ByteArrayOutputStream();
		try {
			int r;
			while ((r = transport.getIn().read()) != -1) {
				bbos.write(r);
			}
		} catch (IOException ioe) {
		}
		HexDump.encode(bbos.toByteArray(), "GOT BACK");
		 req.read(new BerInputStream(transport.getIn()).next());
		 response.read(new NTLMPacket(responseData).setPosition(0));
	}
}
