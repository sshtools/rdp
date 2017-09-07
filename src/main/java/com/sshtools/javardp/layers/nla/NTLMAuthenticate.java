package com.sshtools.javardp.layers.nla;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.CredentialProvider.CredentialType;
import com.sshtools.javardp.Packet;
import com.sshtools.javardp.RdesktopCryptoException;
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

	public NTLMAuthenticate(NTLMState state) throws IOException, RdesktopCryptoException {
		this.state = state;
		List<String> creds = state.getState().getCredentialProvider().getCredentials("ntlm", 0, CredentialType.values());
		if (creds == null)
			throw new IOException("No credentials to preset.");
		domain = creds.get(0);
		user = creds.get(1);
		password = creds.get(2) == null ? null : creds.get(2).toCharArray();
		try {
			switch (state.getState().getOptions().getLMCompatibility()) {
			case 0:
			case 1:
				if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY) == 0) {
					lmResponse = state.getPreNTLMResponse(password);
					ntResponse = state.getNTLMResponse(password);
				} else {
					byte[] clientChallenge = new byte[24];
					state.getRandom().nextBytes(clientChallenge);
					java.util.Arrays.fill(clientChallenge, 8, 24, (byte) 0x00);
					byte[] responseKeyNT = state.hashNTLM(password);
					byte[] ntlm2Response = state.getNTLM2Response(responseKeyNT, clientChallenge);
					lmResponse = clientChallenge;
					ntResponse = ntlm2Response;
					if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_SIGN) == NTLM.NTLMSSP_NEGOTIATE_SIGN) {
						byte[] sessionNonce = new byte[16];
						System.arraycopy(state.getChallenge(), 0, sessionNonce, 0, 8);
						System.arraycopy(clientChallenge, 0, sessionNonce, 8, 8);
						MD4 md4 = new MD4();
						md4.update(responseKeyNT);
						byte[] userSessionKey = md4.digest();
						Mac hmac = Mac.getInstance("HmacMD5");
						hmac.init(new SecretKeySpec(userSessionKey, "HmacMD5"));
						hmac.update(sessionNonce);
						byte[] ntlm2SessionKey = hmac.doFinal();
						if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_KEY_EXCH) != 0) {
							byte[] randomKey = state.getRandomSessionKey();
							byte[] exchangedKey = new byte[16];
							Cipher rc4 = Cipher.getInstance("RC4");
							rc4.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(ntlm2SessionKey, "RC4"));
							rc4.update(randomKey, 0, 16, exchangedKey, 0);
							state.setExchangedKey(exchangedKey);
						} else {
							state.setExchangedKey(ntlm2SessionKey);
						}
					}
				}
				break;
			case 2:
				lmResponse = ntResponse = state.getNTLMResponse(password);
				break;
			case 3:
			case 4:
			case 5:
				byte[] ntlmv2Hash = state.hashNTLMV2(domain, user, password);
				byte[] nonce = new byte[8];
				state.getRandom().nextBytes(nonce);
				lmResponse = state.getLMv2Response(ntlmv2Hash, nonce);
				// http://davenport.sourceforge.net/ntlm.html#respondingToTheChallenge
				// A random 8-byte client nonce is created (this is the same
				// client
				// nonce used in the NTLMv2 blob).
				// byte[] ntNonce = new byte[8];
				// state.getRandom().nextBytes(ntNonce);
				byte[] ntNonce = nonce;
				byte[] blob = state.getNTLMv2Blob(ntNonce);
				
				//
				// TODO !!!!!! It appears the blob is not concatenated here. im sure it sure should be !
				//
				ntResponse = state.getNTLMv2Response(ntlmv2Hash, blob);
				
				
				if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_SIGN) == NTLM.NTLMSSP_NEGOTIATE_SIGN) {
					Mac sessionKey = Mac.getInstance("HmacMD5");
					SecretKeySpec secretKeySpec = new SecretKeySpec(ntlmv2Hash, "HmacMD5");
					sessionKey.init(secretKeySpec);
					sessionKey.update(ntResponse);
					byte[] sessionKeyBytes = sessionKey.doFinal();
					state.setSessionKey(sessionKeyBytes);
					if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_KEY_EXCH) != 0) {
						// KEX - TODO is this needed
						byte[] kexKey = new byte[sessionKeyBytes.length];
						System.arraycopy(sessionKeyBytes, 0, kexKey, 0, kexKey.length);
						state.setKexKey(kexKey);
						// Exchanged key
						byte[] randomKey = state.getRandomSessionKey();
						byte[] exchangedKey = new byte[16];
						Cipher rc4 = Cipher.getInstance("RC4");
						rc4.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sessionKeyBytes, "RC4"));
						rc4.update(randomKey, 0, 16, exchangedKey, 0);
						state.setExchangedKey(exchangedKey);
						// Client Key
						byte[] magic = "session key to client-to-server signing key magic constant\0".getBytes("US-ASCII");
						MessageDigest md5 = MessageDigest.getInstance("MD5");
						md5.update(sessionKeyBytes);
						md5.update(magic);
						state.setClientSignKey(md5.digest());
						// Client Seal Key
						magic = "session key to client-to-server sealing key magic constant\0".getBytes("US-ASCII");
						md5.reset();
						md5.update(sessionKeyBytes);
						md5.update(magic);
						state.initSeal(md5.digest());
						
						// TODO more to do for server sealing key
						
					} else
						state.setExchangedKey(state.getSessionKey());
				}
				break;
			default:
				lmResponse = state.getPreNTLMResponse(password);
				ntResponse = state.getNTLMResponse(password);
			}
		} catch (InvalidKeyException ike) {
			throw new RdesktopCryptoException("Failed to create authentication response.", ike);
		} catch (ShortBufferException e) {
			throw new RdesktopCryptoException("Failed to create authentication response.", e);
		} catch (NoSuchAlgorithmException e) {
			throw new RdesktopCryptoException("Failed to create authentication response.", e);
		} catch (NoSuchPaddingException e) {
			throw new RdesktopCryptoException("Failed to create authentication response.", e);
		}
	}

	@Override
	public Packet write() throws IOException {
		
		
		byte[] domainBytes = state.getEncodedStringBytes(domain);
		byte[] wsBytes = state.getEncodedStringBytes(state.getState().getWorkstationName());
		byte[] userBytes = state.getEncodedStringBytes(user);
		int pktlen = 80 + domainBytes.length + userBytes.length + wsBytes.length + lmResponse.length + ntResponse.length
				+ state.getSessionKey().length;
		int off = 80;
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_VERSION) != 0) {
			off += 8;
			pktlen += 8;
		}
		NTLMPacket packet = new NTLMPacket(pktlen);
		packet.copyFromByteArray(NTLM.SIG, 0, 0, NTLM.SIG.length);
		packet.incrementPosition(NTLM.SIG.length);
		packet.setLittleEndian32(3);
		off += packet.setOffsetArray(off, lmResponse);
		off += packet.setOffsetArray(off, ntResponse);
		off += packet.setOffsetArray(off, domainBytes);
		off += packet.setOffsetArray(off, userBytes);
		off += packet.setOffsetArray(off, wsBytes);
		off += packet.setOffsetArray(off, state.getSessionKey());
		packet.setLittleEndian32(state.getFlags());
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_VERSION) != 0)
			packet.setPacket(state.getClientVersion().write());
		if (mic == null)
			packet.fill(16); // MIC
		else if (mic.length != 16) {
			throw new IllegalStateException("MIC must be 16 bytes if it is set.");
		} else
			packet.setArray(mic);
		logger.info(packet.toString() + " : " + packet.getPosition());
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