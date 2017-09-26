package com.sshtools.javardp.layers.nla;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.CredentialProvider.CredentialType;
import com.sshtools.javardp.Packet;
import com.sshtools.javardp.RdesktopCryptoException;
import com.sshtools.javardp.Utilities;
import com.sshtools.javardp.crypto.MD4;

public class NTLMAuthenticate implements PacketPayload {
	static Logger logger = LoggerFactory.getLogger(NTLMAuthenticate.class);
	private byte[] lmResponse = NTLM.NULL_BYTES;
	private byte[] ntResponse = NTLM.NULL_BYTES;
	private String domain;
	private String user;
	private char[] password;
	private NTLMState state;
	private byte[] mic;
	private byte[] responseKeyNT;
	private byte[] responseKeyLM;

	public NTLMAuthenticate(NTLMState state) throws IOException, RdesktopCryptoException {
		this.state = state;
		/*
		 * MS-NLMP 3.1.5.1.2 Client Receives a CHALLENGE_MESSAGE from the Server
		 */
		/* Get credentials we need */
		List<String> creds = state.getState().getCredentialProvider().getCredentials("ntlm", 0, CredentialType.values());
		if (creds == null)
			throw new IOException("No credentials to preset.");
		domain = creds.get(0);
		user = creds.get(1);
		password = creds.get(2) == null ? null : creds.get(2).toCharArray();
		/* Compute responses */
		computeResponse();
		/* Calculate key exchange key */
		computeKxKey();
		/* Calculate exported session key and random session key */
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_KEY_EXCH) != 0) {
			byte[] nonce = state.getRandomSessionKey();
			state.setExportedSessionKey(nonce);
			try {
				Cipher rc4 = Cipher.getInstance("RC4");
				rc4.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(state.getKexKey(), "RC4"));
				state.setEncryptedRandomSessionKey(rc4.update(nonce));
			} catch (NoSuchAlgorithmException e) {
				throw new RdesktopCryptoException("Failed to compute encrypted random session key.", e);
			} catch (InvalidKeyException e) {
				throw new RdesktopCryptoException("Failed to compute encrypted random session key.", e);
			} catch (NoSuchPaddingException e) {
				throw new RdesktopCryptoException("Failed to compute encrypted random session key.", e);
			}
		} else {
			state.setExportedSessionKey(state.getKexKey());
			state.setEncryptedRandomSessionKey(NTLM.NULL_BYTES);
		}
		/* Client and Server Sign key */
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY) != 0) {
			try {
				/* 3.4.5.2 SIGNKEY */
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				md5.update(state.getExportedSessionKey());
				md5.update("session key to client-to-server signing key magic constant\0".getBytes("US-ASCII"));
				state.setClientSignKey(md5.digest());
				md5 = MessageDigest.getInstance("MD5");
				md5.update(state.getExportedSessionKey());
				md5.update("session key to server-to-client signing key magic constant\0".getBytes("US-ASCII"));
				state.setServerSignKey(md5.digest());
			} catch (NoSuchAlgorithmException e) {
				throw new RdesktopCryptoException("Failed to compute sign/seal keys.", e);
			}
		} else {
			state.setClientSignKey(NTLM.NULL_BYTES);
			state.setServerSignKey(NTLM.NULL_BYTES);
		}
		/* Client and server Seal key */
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY) != 0) {
			try {
				/* 3.4.5.3 SEALKEY */
				byte[] k;
				if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_128) != 0) {
					k = state.getExportedSessionKey();
				} else if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_56) != 0) {
					k = Utilities.padBytes(state.getExportedSessionKey(), 7);
				} else {
					k = Utilities.padBytes(state.getExportedSessionKey(), 5);
				}
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				md5.update(k);
				md5.update("session key to client-to-server signing key magic constant\0".getBytes("US-ASCII"));
				state.setClientSealKey(md5.digest());
				md5 = MessageDigest.getInstance("MD5");
				md5.update(k);
				md5.update("session key to server-to-client signing key magic constant\0".getBytes("US-ASCII"));
				state.setServerSealKey(md5.digest());
			} catch (NoSuchAlgorithmException e) {
				throw new RdesktopCryptoException("Failed to compute sign/seal keys.", e);
			}
		}
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_LM_KEY) != 0
				|| ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_DATAGRAM) != 0 && state.getClientVersion() != null
						&& state.getClientVersion().getRevision() >= NTLMVersion.NTLMSSP_REVISION_W2K3)) {
			if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_56) != 0) {
				state.setClientSealKey(Utilities.concatenateBytes(Utilities.padBytes(state.getExportedSessionKey(), 7),
						new byte[] { (byte) 0xa0 }));
				state.setServerSealKey(Utilities.concatenateBytes(Utilities.padBytes(state.getExportedSessionKey(), 7),
						new byte[] { (byte) 0xa0 }));
			} else {
				state.setClientSealKey(Utilities.concatenateBytes(Utilities.padBytes(state.getExportedSessionKey(), 5),
						new byte[] { (byte) 0xe5, (byte) 0x38, (byte) 0xb0 }));
				state.setServerSealKey(Utilities.concatenateBytes(Utilities.padBytes(state.getExportedSessionKey(), 5),
						new byte[] { (byte) 0xe5, (byte) 0x38, (byte) 0xb0 }));
			}
		} else {
			state.setClientSealKey(state.getExportedSessionKey());
			state.setServerSealKey(state.getExportedSessionKey());
		}
		state.setClientSeal(state.initSeal(state.getClientSealKey()));
		state.setServerSeal(state.initSeal(state.getServerSealKey()));
		//
		// Set MIC to HMAC_MD5(ExportedSessionKey, ConcatenationOf(
		// NEGOTIATE_MESSAGE, CHALLENGE_MESSAGE, AUTHENTICATE_MESSAGE))
		// Set AUTHENTICATE_MESSAGE.MIC to MIC
	}

	/**
	 * Compute the NTLMv1 or NTLMv2 responses. At the end {@link #lmResponse},
	 * {@link #ntResponse} and {@link NTLMState#getSessionKey()} will be set.
	 * 
	 * @throws IOException
	 * @throws RdesktopCryptoException
	 */
	void computeResponse() throws IOException, RdesktopCryptoException {
		byte[] clientChallenge = new byte[8];
		state.getRandom().nextBytes(clientChallenge);
		try {
			switch (state.getState().getOptions().getLMCompatibility()) {
			case 0:
			case 1:
				responseKeyNT = state.ntowf(password);
				responseKeyLM = state.lmowfv1(password, user, domain);
				if (StringUtils.isBlank(user)) {
					ntResponse = NTLM.NULL_BYTES;
					lmResponse = new byte[1];
				} else {
					if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY) != 0) {
						MessageDigest md5 = MessageDigest.getInstance("MD5");
						md5.update(state.getChallenge());
						md5.update(clientChallenge);
						ntResponse = state.desl(responseKeyNT, Utilities.padBytes(md5.digest(), 8));
						lmResponse = Utilities.concatenateBytes(state.getChallenge(), new byte[16]);
					} else {
						ntResponse = state.desl(responseKeyNT, state.getChallenge());
						if (state.getState().getOptions().getLMCompatibility() == 0)
							lmResponse = state.desl(responseKeyLM, state.getChallenge());
						else
							lmResponse = ntResponse;
					}
				}
				MD4 md4 = new MD4();
				md4.update(responseKeyNT);
				state.setSessionBaseKey(md4.digest());
				break;
			case 2:
				throw new UnsupportedOperationException();
				// byte[] responseKeyNT = state.ntowf(password);
				// lmResponse = ntResponse = state.getNTLMResponse(password);
				// md4 = new MD4();
				// md4.update(responseKeyNT);
				// state.setSessionBaseKey(md4.digest());
				// break;
			case 3:
			case 4:
			case 5:
				responseKeyNT = state.ntowfv2(domain, user, password);
				byte[] blob = state.getNTLMv2Blob(clientChallenge);
				Mac mac = Mac.getInstance("HmacMD5");
				mac.init(new SecretKeySpec(responseKeyNT, "HmacMD5"));
				mac.update(state.getChallenge());
				mac.update(blob);
				byte[] ntproof = mac.doFinal();
				if (StringUtils.isBlank(user)) {
					ntResponse = Utilities.concatenateBytes(new byte[16], blob);
					lmResponse = Utilities.concatenateBytes(new byte[16], clientChallenge);
				} else {
					ntResponse = Utilities.concatenateBytes(Utilities.padBytes(ntproof, 16), blob);
					mac.update(state.getChallenge());
					mac.update(clientChallenge);
					lmResponse = Utilities.concatenateBytes(Utilities.padBytes(mac.doFinal(), 16), clientChallenge);
				}
				/*
				 * 3.1.5.1.2 Client Receives a CHALLENGE_MESSAGE from the Server
				 * 
				 * If NTLM v2 authentication is used and the CHALLENGE_MESSAGE
				 * TargetInfo field (section 2.2.1.2) has an MsvAvTimestamp
				 * present, the client SHOULD NOT send the LmChallengeResponse
				 * and SHOULD send Z(24) instead.<45>
				 */
				if(state.getAvPairs().getTimestamp() > 0) {
					lmResponse = new byte[24];
				}
				
				mac.update(ntproof);
				byte[] sessionKeyBytes = mac.doFinal();
				state.setSessionBaseKey(Utilities.padBytes(sessionKeyBytes, 16));
				break;
			default:
				throw new UnsupportedOperationException("Unknown LM compatibility level.");
			}
		} catch (InvalidKeyException ike) {
			throw new RdesktopCryptoException("Failed to create authentication response.", ike);
		} catch (NoSuchAlgorithmException e) {
			throw new RdesktopCryptoException("Failed to create authentication response.", e);
		}
	}

	/**
	 * Compute key exchange key (MS-NLMP 3.4.5.1)
	 * 
	 * @throws RdesktopCryptoException
	 */
	void computeKxKey() throws RdesktopCryptoException {
		switch (state.getState().getOptions().getLMCompatibility()) {
		case 0:
		case 1:
			if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY) != 0) {
				try {
					Cipher des = Cipher.getInstance("DES");
					des.init(Cipher.ENCRYPT_MODE,
							new SecretKeySpec(Utilities.padBytes(Utilities.slice(responseKeyLM, 0, 7), 8), "DES"));
					byte[] d1 = des.doFinal(Utilities.padBytes(lmResponse, 8));
					des.init(Cipher.ENCRYPT_MODE,
							new SecretKeySpec(Utilities.concatenateBytes(new byte[] { responseKeyLM[7] },
									new byte[] { (byte) 0xbd, (byte) 0xbd, (byte) 0xbd, (byte) 0xbd, (byte) 0xbd, (byte) 0xbd }),
									"DES"));
					byte[] d2 = des.doFinal(Utilities.padBytes(lmResponse, 8));
					state.setKexKey(Utilities.concatenateBytes(d1, d2));
				} catch (NoSuchAlgorithmException e) {
					throw new RdesktopCryptoException("Failed to compute Key Exchange Key.", e);
				} catch (InvalidKeyException e) {
					throw new RdesktopCryptoException("Failed to create Key Exchange Key.", e);
				} catch (IllegalBlockSizeException e) {
					throw new RdesktopCryptoException("Failed to create Key Exchange Key.", e);
				} catch (BadPaddingException e) {
					throw new RdesktopCryptoException("Failed to create Key Exchange Key.", e);
				} catch (NoSuchPaddingException e) {
					throw new RdesktopCryptoException("Failed to create Key Exchange Key.", e);
				}
			} else {
				if ((state.getFlags() & NTLM.NTLMSSP_REQUEST_NON_NT_SESSION_KEY) != 0) {
					state.setKexKey(
							Utilities.concatenateBytes(Utilities.padBytes(state.lmowfv1(password, user, domain), 8), new byte[8]));
				} else {
					state.setKexKey(state.getSessionBaseKey());
				}
			}
			break;
		case 2:
			// TODO
			throw new UnsupportedOperationException();
		case 3:
		case 4:
		case 5:
			state.setKexKey(state.getSessionBaseKey());
			break;
		default:
			throw new UnsupportedOperationException("Unknown LM compatibility level.");
		}
	}

	@Override
	public Packet write() throws IOException {
		byte[] domainBytes = state.getEncodedStringBytes(domain);
		byte[] wsBytes = state.getEncodedStringBytes(state.getState().getWorkstationName());
		byte[] userBytes = state.getEncodedStringBytes(user);
		int pktlen = 80 + domainBytes.length + userBytes.length + lmResponse.length + ntResponse.length
				+ state.getSessionBaseKey().length;
		int off = 80;
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_VERSION) != 0) {
			off += 8;
			pktlen += 8 + wsBytes.length;
		}
		NTLMPacket packet = new NTLMPacket(pktlen);
		packet.copyFromByteArray(NTLM.SIG, 0, 0, NTLM.SIG.length);
		packet.incrementPosition(NTLM.SIG.length);
		packet.setLittleEndian32(3);
		off += packet.setOffsetArray(off, lmResponse);
		off += packet.setOffsetArray(off, ntResponse);
		off += packet.setOffsetArray(off, domainBytes);
		off += packet.setOffsetArray(off, userBytes);
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_VERSION) != 0)
			/*
			 * TODO Check this. Seems a bit weird. in MS-NLMP, section 3.1.5.1.2
			 * it states ..
			 * 
			 * "If the NTLMSSP_NEGOTIATE_VERSION flag is set by the client
			 * application, the Version field MUST be set to the current version
			 * (section 2.2.2.10), and the Workstation field MUST be set to
			 * NbMachineName."
			 * 
			 * .. however it's not exactly clear if a zero sized workstation
			 * block should be used instead. Section 2.2.1.3 seems to imply that
			 * is should, but this doesn't mention NTLMSSP_NEGOTIATE_VERSION
			 * which is surprising.
			 */
			off += packet.setOffsetArray(off, wsBytes);
		else
			off += packet.setOffsetArray(off, NTLM.NULL_BYTES);
		off += packet.setOffsetArray(off, state.getSessionBaseKey());
		packet.setLittleEndian32(state.getFlags());
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_VERSION) != 0)
			packet.setPacket(state.getClientVersion().write());
		if (mic == null)
			packet.fill(16); // MIC
		else if (mic.length != 16) {
			throw new IllegalStateException("MIC must be 16 bytes if it is set.");
		} else
			packet.setArray(mic);
		packet.setPosition(off);
		return packet;
	}

	public void setMIC(byte[] mic) {
		this.mic = mic;
	}

	@Override
	public void read(Packet packet) throws IOException {
		throw new UnsupportedOperationException();
	}
}