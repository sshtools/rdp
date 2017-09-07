package com.sshtools.javardp.layers.nla;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.HexDump;
import com.sshtools.javardp.Packet;
import com.sshtools.javardp.RdesktopCryptoException;
import com.sshtools.javardp.State;
import com.sshtools.javardp.Utilities;
import com.sshtools.javardp.crypto.MD4;

public class NTLMState implements NTLM {
	static Logger logger = LoggerFactory.getLogger(NTLMState.class);
	//
	public final static String STANDARD_ENCODING = "Cp850";
	public final static String UNICODE_ENCODING = "UTF-16LE";
	//
	private State state;
	private byte[] challenge;
	private SecureRandom random = new SecureRandom();
	private int flags;
	private byte[] sessionKey = NTLM.NULL_BYTES;
	private String target;
	private NTLMVersion clientVersion = new NTLMVersion();
	private NTLMAVPairs avPairs;
	private byte[] kexKey;
	private byte[] randomSessionKey;
	private byte[] exchangedKey;
	private byte[] clientSignKey;
	private int sequence;
	private Cipher seal;
	private byte[] clientSealKey;
	private static final byte[] S8 = { (byte) 0x4b, (byte) 0x47, (byte) 0x53, (byte) 0x21, (byte) 0x40, (byte) 0x23, (byte) 0x24,
			(byte) 0x25 };
	static final long MILLISECONDS_BETWEEN_1970_AND_1601 = 11644473600000L;

	public NTLMState(State state) {
		this(state,
				NTLMSSP_NEGOTIATE_56 | NTLMSSP_NEGOTIATE_KEY_EXCH | NTLMSSP_NEGOTIATE_128 | NTLMSSP_NEGOTIATE_VERSION
						| NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY | NTLMSSP_NEGOTIATE_ALWAYS_SIGN | NTLMSSP_NEGOTIATE_NTLM
						| NTLMSSP_NEGOTIATE_LM_KEY | NTLMSSP_NEGOTIATE_SEAL | NTLMSSP_NEGOTIATE_SIGN | NTLMSSP_REQUEST_TARGET
						| NTLM_NEGOTIATE_OEM | NTLMSSP_NEGOTIATE_UNICODE);
	}

	public NTLMState(State state, int flags) {
		this.state = state;
		this.flags = flags;
	}

	public int nextSequence() {
		return sequence++;
	}

	public NTLMAVPairs getAvPairs() {
		return avPairs;
	}

	public void setAvPairs(NTLMAVPairs avPairs) {
		this.avPairs = avPairs;
	}

	public void dumpFlags() {
		List<String> f = new ArrayList<>();
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_56) != 0) {
			f.add("NTLMSSP_NEGOTIATE_56");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_KEY_EXCH) != 0) {
			f.add("NTLMSSP_NEGOTIATE_KEY_EXCH");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_128) != 0) {
			f.add("NTLMSSP_NEGOTIATE_128");
		}
		if ((flags & NTLM.NTLMSSP_RESERVED_1) != 0) {
			f.add("NTLMSSP_RESERVED_1");
		}
		if ((flags & NTLM.NTLMSSP_RESERVED_2) != 0) {
			f.add("NTLMSSP_RESERVED_2");
		}
		if ((flags & NTLM.NTLMSSP_RESERVED_3) != 0) {
			f.add("NTLMSSP_RESERVED_3");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_VERSION) != 0) {
			f.add("NTLMSSP_NEGOTIATE_VERSION");
		}
		if ((flags & NTLM.NTLMSSP_RESERVED_4) != 0) {
			f.add("NTLMSSP_RESERVED_4");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_TARGET_INFO) != 0) {
			f.add("NTLMSSP_NEGOTIATE_TARGET_INFO");
		}
		if ((flags & NTLM.NTLMSSP_REQUEST_NON_NT_SESSION_KEY) != 0) {
			f.add("NTLMSSP_REQUEST_NON_NT_SESSION_KEY");
		}
		if ((flags & NTLM.NTLMSSP_RESERVED_5) != 0) {
			f.add("NTLMSSP_RESERVED_5");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_IDENTIFY) != 0) {
			f.add("NTLMSSP_NEGOTIATE_IDENTIFY");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY) != 0) {
			f.add("NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY");
		}
		if ((flags & NTLM.NTLMSSP_RESERVED_6) != 0) {
			f.add("NTLMSSP_RESERVED_6");
		}
		if ((flags & NTLM.NTLMSSP_TARGET_TYPE_SERVER) != 0) {
			f.add("NTLMSSP_TARGET_TYPE_SERVER");
		}
		if ((flags & NTLM.NTLMSSP_TARGET_TYPE_DOMAIN) != 0) {
			f.add("NTLMSSP_TARGET_TYPE_DOMAIN");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_ALWAYS_SIGN) != 0) {
			f.add("NTLMSSP_NEGOTIATE_ALWAYS_SIGN");
		}
		if ((flags & NTLM.NTLMSSP_RESERVED_7) != 0) {
			f.add("NTLMSSP_RESERVED_7");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED) != 0) {
			f.add("NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED) != 0) {
			f.add("NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED");
		}
		if ((flags & NTLM.NTLMSSP_ANONYMOUS) != 0) {
			f.add("NTLMSSP_ANONYMOUS");
		}
		if ((flags & NTLM.NTLMSSP_RESERVED_8) != 0) {
			f.add("NTLMSSP_RESERVED_8");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_NTLM) != 0) {
			f.add("NTLMSSP_NEGOTIATE_NTLM");
		}
		if ((flags & NTLM.NTLMSSP_RESERVED_9) != 0) {
			f.add("NTLMSSP_RESERVED_9");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_LM_KEY) != 0) {
			f.add("NTLMSSP_NEGOTIATE_LM_KEY");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_DATAGRAM) != 0) {
			f.add("NTLMSSP_NEGOTIATE_DATAGRAM");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_SEAL) != 0) {
			f.add("NTLMSSP_NEGOTIATE_SEAL");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_SIGN) != 0) {
			f.add("NTLMSSP_NEGOTIATE_SIGN");
		}
		if ((flags & NTLM.NTLMSSP_RESERVED_10) != 0) {
			f.add("NTLMSSP_RESERVED_10");
		}
		if ((flags & NTLM.NTLMSSP_REQUEST_TARGET) != 0) {
			f.add("NTLMSSP_REQUEST_TARGET");
		}
		if ((flags & NTLM.NTLM_NEGOTIATE_OEM) != 0) {
			f.add("NTLM_NEGOTIATE_OEM");
		}
		if ((flags & NTLM.NTLMSSP_NEGOTIATE_UNICODE) != 0) {
			f.add("NTLMSSP_NEGOTIATE_UNICODE");
		}
		logger.debug(
				String.format("Flags: %d (decimal) %x (hex) %s (bin) %s (flags)", flags, flags, Integer.toBinaryString(flags), f));
	}

	public byte[] encryptMessage(byte[] bytes) throws RdesktopCryptoException {
		int seq = nextSequence();
		try {
			Mac mac = Mac.getInstance("HmacMD5");
			mac.init(new SecretKeySpec(clientSignKey, "HmacMD5"));
			byte[] seqbytes = Utilities.intToBytes(seq);
			mac.update(seqbytes);
			mac.update(bytes);
			byte[] digest = Utilities.padBytes(mac.doFinal(), 8);
			byte[] dataEnc = seal.update(bytes);
			return Utilities.concatenateBytes(Utilities.intToBytes(1), seal.update(digest), seqbytes, dataEnc);
		} catch (NoSuchAlgorithmException nsae) {
			throw new RdesktopCryptoException("Failed to encrypt message.", nsae);
		} catch (InvalidKeyException e) {
			throw new RdesktopCryptoException("Failed to encrypt message.", e);
		}
	}

	public void initSeal(byte[] key) throws RdesktopCryptoException {
		try {
			seal = Cipher.getInstance("RC4");
			seal.init(1, new SecretKeySpec(key, "RC4"));
			this.clientSealKey = key;
		} catch (NoSuchPaddingException pspe) {
			throw new RdesktopCryptoException("Failed to initialise seal.", pspe);
		} catch (NoSuchAlgorithmException e) {
			throw new RdesktopCryptoException("Failed to initialise seal.", e);
		} catch (InvalidKeyException e) {
			throw new RdesktopCryptoException("Failed to initialise seal.", e);
		}
	}

	public byte[] getClientSignKey() {
		return clientSignKey;
	}

	public void setClientSignKey(byte[] clientSignKey) {
		this.clientSignKey = clientSignKey;
	}

	public byte[] getClientSealKey() {
		return clientSealKey;
	}

	public byte[] getExchangedKey() {
		return exchangedKey;
	}

	public void setExchangedKey(byte[] exchangedKey) {
		this.exchangedKey = exchangedKey;
	}

	public NTLMVersion getClientVersion() {
		return clientVersion;
	}

	public void setClientVersion(NTLMVersion clientVersion) {
		this.clientVersion = clientVersion;
	}

	public byte[] hashNTLM(char[] password) {
		try {
			MD4 md4 = new MD4();
			md4.update((password == null ? "" : new String(password)).getBytes(UNICODE_ENCODING));
			return md4.digest();
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Unsupported encoding.", e);
		}
	}

	public byte[] hashNTLMV2(String domain, String user, char[] password) {
		try {
			Mac mac = Mac.getInstance("HmacMD5");
			SecretKeySpec secretKeySpec = new SecretKeySpec(hashNTLM(password), "HmacMD5");
			mac.init(secretKeySpec);
			mac.update((StringUtils.defaultIfBlank(user, "").toUpperCase() + StringUtils.defaultIfBlank(domain, ""))
					.getBytes(UNICODE_ENCODING));
			return mac.doFinal();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to hash NTLMv2.", e);
		}
	}

	public byte[] getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(byte[] sessionKey) {
		this.sessionKey = sessionKey;
	}

	public byte[] getKexKey() {
		return kexKey;
	}

	public void setKexKey(byte[] kexKey) {
		this.kexKey = kexKey;
	}

	public byte[] getEncodedStringBytes(String str) {
		try {
			if (str == null || str.length() == 0)
				return NTLM.NULL_BYTES;
			return (flags & NTLM.NTLMSSP_NEGOTIATE_UNICODE) != 0 ? str.getBytes(NTLMState.UNICODE_ENCODING)
					: str.getBytes(NTLMState.STANDARD_ENCODING);
		} catch (UnsupportedEncodingException uee) {
			throw new IllegalStateException("Unsupported encoding.");
		}
	}

	public byte[] getRandomSessionKey() {
		if (randomSessionKey == null) {
			randomSessionKey = new byte[16];
			random.nextBytes(randomSessionKey);
		}
		return randomSessionKey;
	}

	public void setRandomSessionKey(byte[] randomSessionKey) {
		this.randomSessionKey = randomSessionKey;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		if (flags != this.flags) {
			this.flags = flags;
			if (state.getOptions().isDebugHexdump()) {
				logger.debug("Flags changed.");
				dumpFlags();
			}
		}
	}

	public SecureRandom getRandom() {
		return random;
	}

	public void setRandom(SecureRandom random) {
		this.random = random;
	}

	public State getState() {
		return state;
	}

	public byte[] getChallenge() {
		return challenge;
	}

	public void setChallenge(byte[] challenge) {
		this.challenge = challenge;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public byte[] getLMv2Response(byte[] ntlmv2Hash, byte[] nonce) {
		try {
			Mac mac = Mac.getInstance("HmacMD5");
			SecretKeySpec secretKeySpec = new SecretKeySpec(ntlmv2Hash, "HmacMD5");
			mac.init(secretKeySpec);
			mac.update(challenge);
			mac.update(nonce);
			byte[] result = mac.doFinal();
			byte[] response = new byte[24];
			System.arraycopy(result, 0, response, 0, result.length);
			System.arraycopy(nonce, 0, response, 16, 8);
			return response;
		} catch (Exception ex) {
			throw new IllegalStateException("LMv2 response failed.", ex);
		}
	}

	/**
	 * Generate the Unicode MD4 hash for the password associated with these
	 * credentials.
	 * 
	 * This is from jCIFS (LGPL)
	 * 
	 * @throws RdesktopCryptoException
	 */
	public byte[] getNTLMResponse(char[] password) throws RdesktopCryptoException {
		if (password == null)
			return null;
		byte[] uni = null;
		byte[] p21 = new byte[21];
		byte[] p24 = new byte[24];
		try {
			uni = new String(password).getBytes(NTLMState.UNICODE_ENCODING);
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException("Invalid encoding.", uee);
		}
		MD4 md4 = new MD4();
		md4.update(uni);
		try {
			md4.digest(p21, 0, 16);
		} catch (Exception ex) {
			throw new RuntimeException("Invalid digest.", ex);
		}
		E(p21, challenge, p24);
		return p24;
	}

	/**
	 * Generate the ANSI DES hash for the password associated with these
	 * credentials.
	 * 
	 * This is from jCIFS (LGPL)
	 * 
	 * @throws RdesktopCryptoException
	 */
	public byte[] getPreNTLMResponse(char[] password) throws RdesktopCryptoException {
		if (password == null)
			return null;
		byte[] p14 = new byte[14];
		byte[] p21 = new byte[21];
		byte[] p24 = new byte[24];
		byte[] passwordBytes;
		try {
			passwordBytes = new String(password).toUpperCase().getBytes(NTLMState.STANDARD_ENCODING);
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException("Invalid encoding.", uee);
		}
		int passwordLength = passwordBytes.length;
		// Only encrypt the first 14 bytes of the password for Pre 0.12 NT LM
		if (passwordLength > 14) {
			passwordLength = 14;
		}
		System.arraycopy(passwordBytes, 0, p14, 0, passwordLength);
		E(p14, S8, p21);
		E(p21, challenge, p24);
		return p24;
	}

	public byte[] getNTLM2Response(byte[] nTOWFv1, byte[] clientChallenge) throws RdesktopCryptoException {
		byte[] sessionHash = new byte[8];
		try {
			MessageDigest md5;
			md5 = MessageDigest.getInstance("MD5");
			md5.update(challenge);
			md5.update(clientChallenge, 0, 8);
			System.arraycopy(md5.digest(), 0, sessionHash, 0, 8);
		} catch (GeneralSecurityException gse) {
			throw new RuntimeException("MD5", gse);
		}
		byte[] key = new byte[21];
		System.arraycopy(nTOWFv1, 0, key, 0, 16);
		byte[] ntResponse = new byte[24];
		E(key, sessionHash, ntResponse);
		return ntResponse;
	}

	public byte[] getNTLMv2Blob(byte[] nonce) throws IOException {
		long nanos1601 = (System.currentTimeMillis() + MILLISECONDS_BETWEEN_1970_AND_1601) * 10000L;
		Packet targetInfo = avPairs == null ? null : avPairs.write();
		byte[] targetData = targetInfo == null ? NTLM.NULL_BYTES : targetInfo.getBytes();
		HexDump.encode(targetData, "AV Pairs to send");
		// Blob
		Packet p = new Packet(targetData.length + 28 + 4);
		p.set8(0x01); // resp
		p.set8(0x01); // hi resp type
		p.setLittleEndian16(0); // reserved
		p.setLittleEndian32(0); // reserved
		p.setLittleEndian64(nanos1601); // timestamp
		p.setArray(nonce); // client nonce
		p.setLittleEndian32(0); // reserved
		p.setArray(targetData);
		if (targetData.length == 0)
			p.setLittleEndian32(0); // AV_PAIR MsvAvEOL
		return p.getBytes();
	}

	public byte[] getNTLMv2Response(byte[] responseKeyNT, byte[] ntlmv2Blob) {
		try {
			Mac mac = Mac.getInstance("HmacMD5");
			SecretKeySpec secretKeySpec = new SecretKeySpec(responseKeyNT, "HmacMD5");
			mac.init(secretKeySpec);
			mac.update(challenge);
			mac.update(ntlmv2Blob);
			return Utilities.concatenateBytes(mac.doFinal(), ntlmv2Blob);
		} catch (Exception ex) {
			throw new IllegalStateException("LMv2 response failed.", ex);
		}
	}

	/*
	 * Accepts key multiple of 7 Returns enc multiple of 8 Multiple is the same
	 * like: 21 byte key gives 24 byte result
	 * 
	 * This is from jCIFS (LGPL)
	 */
	private static void E(byte[] key, byte[] data, byte[] e) throws RdesktopCryptoException {
		byte[] key7 = new byte[7];
		byte[] e8 = new byte[8];
		try {
			for (int i = 0; i < key.length / 7; i++) {
				System.arraycopy(key, i * 7, key7, 0, 7);
				Cipher des = Cipher.getInstance("DES/ECB/NoPadding");
				SecretKey dk = new SecretKeySpec(key7, "DES");
				des.init(1, dk);
				des.doFinal(data, 0, 8, e8);
				System.arraycopy(e8, 0, e, i * 8, 8);
			}
		} catch (NoSuchAlgorithmException nsae) {
			throw new RdesktopCryptoException("Failed to encrypt using DES.", nsae);
		} catch (ShortBufferException e1) {
			throw new RdesktopCryptoException("Failed to encrypt using DES.", e1);
		} catch (IllegalBlockSizeException e1) {
			throw new RdesktopCryptoException("Failed to encrypt using DES.", e1);
		} catch (BadPaddingException e1) {
			throw new RdesktopCryptoException("Failed to encrypt using DES.", e1);
		} catch (NoSuchPaddingException e1) {
			throw new RdesktopCryptoException("Failed to encrypt using DES.", e1);
		} catch (InvalidKeyException e1) {
			throw new RdesktopCryptoException("Failed to encrypt using DES.", e1);
		}
	}

	public String getDecodedString(byte[] data) {
		try {
			return new String(data, ((flags & NTLM.NTLMSSP_NEGOTIATE_UNICODE) != 0) ? UNICODE_ENCODING : STANDARD_ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Could not decode string.", e);
		}
	}
}
