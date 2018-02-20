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
	private byte[] exportedSessionKey;
	private byte[] encryptedRandomSessionKey;
	private byte[] sessionBaseKey = NTLM.NULL_BYTES;
	private byte[] clientSignKey;
	private byte[] clientSealKey;
	private byte[] serverSignKey;
	private byte[] serverSealKey;
	private int sequence;
	private Cipher clientSeal;
	private Cipher serverSeal;
	private static final byte[] S8 = { (byte) 0x4b, (byte) 0x47, (byte) 0x53, (byte) 0x21, (byte) 0x40, (byte) 0x23, (byte) 0x24,
			(byte) 0x25 };
	static final long MILLISECONDS_BETWEEN_1970_AND_1601 = 11644473600000L;

	public NTLMState(State state) {
		this(state,
				NTLMSSP_NEGOTIATE_56 
				| NTLMSSP_NEGOTIATE_KEY_EXCH 
				| NTLMSSP_NEGOTIATE_128 
				//| NTLMSSP_NEGOTIATE_VERSION
				| NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY 
				| NTLMSSP_NEGOTIATE_ALWAYS_SIGN 
				| NTLMSSP_NEGOTIATE_NTLM
				| NTLMSSP_NEGOTIATE_LM_KEY 
				| NTLMSSP_NEGOTIATE_SEAL 
				| NTLMSSP_NEGOTIATE_SIGN 
				| NTLMSSP_REQUEST_TARGET
				| NTLM_NEGOTIATE_OEM 
				| NTLMSSP_NEGOTIATE_UNICODE);
	}

	public NTLMState(State state, int flags) {
		this.state = state;
		this.flags = flags;
	}

	public int nextSequence() {
		return sequence++;
	}

	public Cipher getClientSeal() {
		return clientSeal;
	}

	public void setClientSeal(Cipher clientSeal) {
		this.clientSeal = clientSeal;
	}

	public Cipher getServerSeal() {
		return serverSeal;
	}

	public void setServerSeal(Cipher serverSeal) {
		this.serverSeal = serverSeal;
	}

	public byte[] getServerSignKey() {
		return serverSignKey;
	}

	public void setServerSignKey(byte[] serverSignKey) {
		this.serverSignKey = serverSignKey;
	}

	public byte[] getServerSealKey() {
		return serverSealKey;
	}

	public void setServerSealKey(byte[] serverSealKey) {
		this.serverSealKey = serverSealKey;
	}

	public void setClientSealKey(byte[] clientSealKey) {
		this.clientSealKey = clientSealKey;
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

	public byte[] getExportedSessionKey() {
		return exportedSessionKey;
	}

	public void setExportedSessionKey(byte[] exportedSessionKey) {
		this.exportedSessionKey = exportedSessionKey;
	}

	public byte[] encryptMessage(byte[] key, byte[] bytes, Cipher seal) throws RdesktopCryptoException {
		int seq = nextSequence();
		try {
			Mac mac = Mac.getInstance("HmacMD5");
			mac.init(new SecretKeySpec(key, "HmacMD5"));
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

	public Cipher initSeal(byte[] key) throws RdesktopCryptoException {
		try {
			Cipher seal = Cipher.getInstance("RC4");
			seal.init(1, new SecretKeySpec(key, "RC4"));
			return seal;
		} catch (NoSuchPaddingException pspe) {
			throw new RdesktopCryptoException("Failed to initialise seal.", pspe);
		} catch (NoSuchAlgorithmException e) {
			throw new RdesktopCryptoException("Failed to initialise seal.", e);
		} catch (InvalidKeyException e) {
			throw new RdesktopCryptoException("Failed to initialise seal.", e);
		}
	}

	public void initServerSeal() throws RdesktopCryptoException {
		try {
			serverSeal = Cipher.getInstance("RC4");
			serverSeal.init(1, new SecretKeySpec(getServerSealKey(), "RC4"));
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

	public byte[] getSessionBaseKey() {
		return sessionBaseKey;
	}

	public void setSessionBaseKey(byte[] sessionBaseKey) {
		this.sessionBaseKey = sessionBaseKey;
	}

	public NTLMVersion getClientVersion() {
		return clientVersion;
	}

	public void setClientVersion(NTLMVersion clientVersion) {
		this.clientVersion = clientVersion;
	}

	public byte[] lmowfv1(char[] password, String user, String domain) throws RdesktopCryptoException {
		try {
			byte[] passwordBytes = Utilities.padBytes(new String(password).toUpperCase().getBytes(NTLMState.STANDARD_ENCODING), 14);
			byte[] magic = "KGS!@#$%".getBytes("US-ASCII");
			Cipher des = Cipher.getInstance("DES");
			des.init(1, new SecretKeySpec(Utilities.padBytes(Utilities.slice(passwordBytes, 0, 7), 8), "DES"));
			byte[] d1 = des.doFinal(magic);
			des.init(1, new SecretKeySpec(Utilities.padBytes(Utilities.slice(passwordBytes, 7, 14), 8), "DES"));
			byte[] d2 = des.doFinal(magic);
			return Utilities.concatenateBytes(d1, d2);
		} catch (UnsupportedEncodingException uoe) {
			throw new IllegalStateException("Unsupported encoding.", uoe);
		} catch (NoSuchAlgorithmException nsae) {
			throw new RdesktopCryptoException("Failed to encrypt using DES.", nsae);
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

	public byte[] ntowf(char[] password) {
		try {
			MD4 md4 = new MD4();
			md4.update((password == null ? "" : new String(password)).getBytes(UNICODE_ENCODING));
			return md4.digest();
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Unsupported encoding.", e);
		}
	}

	public byte[] ntowfv2(String domain, String user, char[] password) {
		try {
			Mac mac = Mac.getInstance("HmacMD5");
			SecretKeySpec secretKeySpec = new SecretKeySpec(ntowf(password), "HmacMD5");
			mac.init(secretKeySpec);
			mac.update((StringUtils.defaultIfBlank(user, "").toUpperCase() + StringUtils.defaultIfBlank(domain, ""))
					.getBytes(UNICODE_ENCODING));
			return mac.doFinal();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to hash NTLMv2.", e);
		}
	}

	public byte[] desl(byte[] key, byte[] data) throws RdesktopCryptoException {
		try {
			Cipher des = Cipher.getInstance("DES/ECB/NoPadding");
			des.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Utilities.padBytes(Utilities.slice(key, 0, 7), 8), "DES"));
			byte[] b1 = des.doFinal(data);
			des.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Utilities.padBytes(Utilities.slice(key, 7, 14), 8), "DES"));
			byte[] b2 = des.doFinal(data);
			des.init(Cipher.ENCRYPT_MODE,
					new SecretKeySpec(Utilities.concatenateBytes(Utilities.slice(key, 14, 16), new byte[6]), "DES"));
			byte[] b3 = des.doFinal(data);
			return Utilities.concatenateBytes(b1, b2, b3);
		} catch (NoSuchAlgorithmException nsae) {
			throw new RdesktopCryptoException("Failed to encrypt using DES.", nsae);
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
			return Utilities.concatenateBytes(Utilities.padBytes(mac.doFinal(), 16), Utilities.padBytes(nonce, 8));
		} catch (Exception ex) {
			throw new IllegalStateException("LMv2 response failed.", ex);
		}
	}

	public byte[] getNTLMv2Blob(byte[] nonce) throws IOException {
		long nanos1601 = ((avPairs.getTimestamp() > 0 ? avPairs.getTimestamp() : System.currentTimeMillis())
				+ MILLISECONDS_BETWEEN_1970_AND_1601) * 10000L;
		Packet targetInfo = avPairs == null ? null : avPairs.write();
		byte[] targetData = targetInfo == null ? NTLM.NULL_BYTES : targetInfo.getBytes();
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

	public String getDecodedString(byte[] data) {
		try {
			return new String(data, ((flags & NTLM.NTLMSSP_NEGOTIATE_UNICODE) != 0) ? UNICODE_ENCODING : STANDARD_ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Could not decode string.", e);
		}
	}

	public byte[] getEncryptedRandomSessionKey() {
		return encryptedRandomSessionKey;
	}

	public void setEncryptedRandomSessionKey(byte[] encryptedRandomSessionKey) {
		this.encryptedRandomSessionKey = encryptedRandomSessionKey;
	}
}
