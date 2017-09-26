/*
 * Secure.java Component: ProperJavaRDP Revision: $Revision: 1.1 $ Author:
 * $Author: brett $ Date: $Date: 2011/11/28 14:13:42 $ Copyright (c) 2005
 * Propero Limited Purpose: Secure layer of communication
 */
package com.sshtools.javardp.layers;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.IContext;
import com.sshtools.javardp.Licence;
import com.sshtools.javardp.OrderException;
import com.sshtools.javardp.Packet;
import com.sshtools.javardp.RdesktopCryptoException;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.SecurityType;
import com.sshtools.javardp.State;
import com.sshtools.javardp.Utilities;
import com.sshtools.javardp.io.IO;
import com.sshtools.javardp.rdp5.VChannel;
import com.sshtools.javardp.rdp5.VChannels;

public class Secure implements Layer<Rdp> {
	static Logger logger = LoggerFactory.getLogger(Secure.class);
	/* constants for the secure layer */
	public static final int SEC_ENCRYPT = 0x0008;
	public static final int SEC_LOGON_INFO = 0x0040;
	public static final int SEC_LICENCE_NEG = 0x0080;
	public static final int SEC_MAX_MODULUS_SIZE = 256;
	public static final int SEC_MODULUS_SIZE = 64;
	public static final int SEC_PADDING_SIZE = 8;
	public static final int SEC_RANDOM_SIZE = 32;
	private static final byte[] pad_54 = { 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
			54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54 };
	private static final byte[] pad_92 = { 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92,
			92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92 };
	public static final int SEC_CLIENT_RANDOM = 0x0001;
	public static final int SEC_EXPONENT_SIZE = 4;
	public static final int SEC_RSA_MAGIC = 0x31415352; /* RSA1 */
	public static final int SEC_TAG_CLI_4 = 0xc004;
	public static final int SEC_TAG_CLI_CHANNELS = 0xc003;
	public static final int SEC_TAG_CLI_CRYPT = 0xc002;
	public static final int SEC_TAG_CLI_INFO = 0xc001;
	public static final int SEC_TAG_KEYSIG = 0x0008;
	public static final int SEC_TAG_PUBKEY = 0x0006;
	public static final int SEC_TAG_SRV_3 = 0x0c03;
	public static final int SEC_TAG_SRV_CHANNELS = 0x0c03;
	public static final int SEC_TAG_SRV_CRYPT = 0x0c02;
	public static final int SEC_TAG_SRV_INFO = 0x0c01;
	public static final int SEC_40BIT_ENCRYPTION = 0x00000001;
	public static final int SEC_128BIT_ENCRYPTION = 0x00000002;
	public static final int SEC_56BIT_ENCRYPTION = 0x00000008;
	public static final int SEC_FIPS_ENCRYPTION = 0x00000010;
	// private String hostname=null;
	// private String username=null;
	private VChannels channels;
	private byte[] clientRandom = new byte[SEC_RANDOM_SIZE];
	private int dec_count = 0;
	private int earlyCaps;
	private int enc_count = 0;
	private byte[] exponent = null;
	private Licence licence;
	private MCS mcsLayer = null;
	private MessageDigest md5;
	private byte[] modulus = null;
	private Cipher rc4_dec = null;
	// private RC4 rc4_dec = null;
	private Cipher rc4_enc = null;
	private Cipher rc4_update = null;
	private byte[] sec_crypted_random = null;
	private byte[] decryptKey = null;
	private byte[] decryptUpdateKey = null;
	private byte[] encryptKey = null;
	private byte[] encryptUpdateKey = null;
	private byte[] macKey = null;
	private int server_public_key_len = 0;
	private byte[] serverRandom = null;
	private MessageDigest sha1 = null;
	private State state;
	private Rdp rdp;

	/**
	 * Initialise Secure layer of communications
	 *
	 * @param channels Virtual channels for this connection
	 * @throws RdesktopCryptoException
	 */
	public Secure(IContext context, State state, VChannels channels, Rdp rdp) {
		licence = new Licence(state, context, this);
		this.channels = channels;
		this.state = state;
		this.rdp = rdp;
		mcsLayer = new MCS(context, state, channels, this);
		try {
			rc4_enc = Cipher.getInstance("RC4");
		} catch (NoSuchAlgorithmException nsae) {
			throw new IllegalStateException("Cannot initialise RC4 decoder.", nsae);
		} catch (NoSuchPaddingException nsae) {
			throw new IllegalStateException("Cannot initialise RC4 decoder.", nsae);
		}
		try {
			rc4_dec = Cipher.getInstance("RC4");
		} catch (NoSuchAlgorithmException nsae) {
			throw new IllegalStateException("Cannot initialise RC4 decoder.", nsae);
		} catch (NoSuchPaddingException nsae) {
			throw new IllegalStateException("Cannot initialise RC4 decoder.", nsae);
		}
		try {
			rc4_update = Cipher.getInstance("RC4");
		} catch (NoSuchAlgorithmException nsae) {
			throw new IllegalStateException("Cannot initialise RC4 update.", nsae);
		} catch (NoSuchPaddingException nsae) {
			throw new IllegalStateException("Cannot initialise RC4 update.", nsae);
		}
		try {
			sha1 = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException nsae) {
			throw new IllegalStateException("Cannot initialise MD5.", nsae);
		}
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException nsae) {
			throw new IllegalStateException("Cannot initialise MD5.", nsae);
		}
		macKey = new byte[16];
		encryptUpdateKey = new byte[16];
		decryptUpdateKey = new byte[16];
		sec_crypted_random = new byte[64];
		/*
		 * Inform all the channels we now have a securelayer so they can be used
		 */
		for (VChannel ch : rdp.getChannels()) {
			ch.start(context, state, this);
		}
	}

	/**
	 * Connect to server
	 *
	 * @param host Address of server to connect to
	 * @param port Port to connect to
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws RdesktopException
	 * @throws SocketException
	 * @throws CryptoException
	 * @throws OrderException
	 */
	public void connect(IO io) throws UnknownHostException, IOException, RdesktopException, SocketException, OrderException {
		Packet mcs_data = this.sendMcsData();
		logger.info(String.format("Requested server bpp is %d", state.getServerBpp()));
		mcsLayer.connect(io, mcs_data);
		this.processMcsData(mcs_data);
		if (state.getSecurityType() == SecurityType.STANDARD) {
			this.establishKey();
		} else {
			logger.info("Not sending key as encryption has been disabled.");
		}
	}

	/**
	 * Decrypt provided data using RC4 algorithm
	 *
	 * @param data Data to decrypt
	 * @return Decrypted data
	 * @throws RdesktopCryptoException
	 * @throws CryptoException
	 */
	public byte[] decrypt(byte[] data) throws RdesktopCryptoException {
		try {
			byte[] buffer = null;
			if (logger.isDebugEnabled())
				logger.debug(String.format("Decrypting %d bytes", data.length));
			if (this.dec_count == 4096) {
				decryptKey = this.update(decryptKey, decryptUpdateKey);
				this.rc4_dec.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptKey, "RC4"));
				logger.debug("Packet dec_count=" + dec_count);
				this.dec_count = 0;
			}
			buffer = this.rc4_dec.update(data);
			this.dec_count++;
			return buffer;
		} catch (InvalidKeyException ike) {
			throw new RdesktopCryptoException("Failed to update key.", ike);
		}
		// catch (IllegalBlockSizeException e) {
		// throw new RdesktopCryptoException("Failed to update key.", e);
		// } catch (BadPaddingException e) {
		// throw new RdesktopCryptoException("Failed to update key.", e);
		// }
	}

	/**
	 * Decrypt specified number of bytes from provided data using RC4 algorithm
	 *
	 * @param data Data to decrypt
	 * @param length Number of bytes to decrypt (from start of array)
	 * @return Decrypted data
	 * @throws RdesktopCryptoException
	 * @throws CryptoException
	 */
	public byte[] decrypt(byte[] data, int length) throws RdesktopCryptoException {
		try {
			byte[] buffer = null;
			if (logger.isDebugEnabled())
				logger.debug(String.format("Decrypting %d bytes (fixed length)", data.length));
			if (this.dec_count == 4096) {
				decryptKey = this.update(decryptKey, decryptUpdateKey);
				this.rc4_dec.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptKey, "RC4"));
				logger.debug("Packet dec_count=" + dec_count);
				this.dec_count = 0;
			}
			buffer = this.rc4_dec.update(data, 0, length);
			this.dec_count++;
			return buffer;
		} catch (InvalidKeyException ike) {
			throw new RdesktopCryptoException("Failed to update key.", ike);
		}
	}

	/**
	 * Close connection
	 */
	public void disconnect() {
		mcsLayer.disconnect();
	}

	/**
	 * Encrypt provided data using the RC4 algorithm
	 *
	 * @param data Data to encrypt
	 * @return Encrypted data
	 * @throws RdesktopCryptoException
	 */
	public byte[] encrypt(byte[] data) throws RdesktopCryptoException {
		try {
			byte[] buffer = null;
			if (this.enc_count == 4096) {
				encryptKey = this.update(encryptKey, encryptUpdateKey);
				this.rc4_enc.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptKey, "RC4"));
				this.enc_count = 0;
			}
			buffer = this.rc4_enc.update(data);
			this.enc_count++;
			return buffer;
		} catch (InvalidKeyException ike) {
			throw new RdesktopCryptoException("Failed to update key.", ike);
		}
	}

	/**
	 * Encrypt specified number of bytes from provided data using RC4 algorithm
	 *
	 * @param data Data to encrypt
	 * @param length Number of bytes to encrypt (from start of array)
	 * @return Encrypted data
	 * @throws RdesktopCryptoException
	 */
	public byte[] encrypt(byte[] data, int length) throws RdesktopCryptoException {
		try {
			byte[] buffer = null;
			if (this.enc_count == 4096) {
				encryptKey = this.update(encryptKey, encryptUpdateKey);
				this.rc4_enc.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptKey, "RC4"));
				this.enc_count = 0;
			}
			buffer = this.rc4_enc.update(data, 0, length);
			this.enc_count++;
			return buffer;
		} catch (InvalidKeyException ike) {
			throw new RdesktopCryptoException("Failed to update key.", ike);
		}
	}

	public void establishKey() throws RdesktopException, IOException {
		Packet buffer;
		int flags = SEC_CLIENT_RANDOM;
		if (state.isReadCert()) {
			// RDP5-style encryption, use old code for now
			int length = SEC_MODULUS_SIZE + SEC_PADDING_SIZE;
			buffer = this.init(flags, 76);
			buffer.setLittleEndian32(length);
			buffer.copyFromByteArray(this.sec_crypted_random, 0, buffer.getPosition(), SEC_MODULUS_SIZE);
			buffer.incrementPosition(SEC_MODULUS_SIZE);
		} else {
			int length = server_public_key_len + SEC_PADDING_SIZE;
			buffer = this.init(flags, length + 4);
			buffer.setLittleEndian32(length);
			buffer.copyFromByteArray(this.sec_crypted_random, 0, buffer.getPosition(), server_public_key_len);
			buffer.incrementPosition(server_public_key_len);
		}
		buffer.incrementPosition(SEC_PADDING_SIZE);
		buffer.markEnd();
		logger.info("Sending client key");
		this.send(buffer, flags);
	}

	/**
	 * Generate encryption keys of applicable size for connection
	 *
	 * @param sessionKeyEncryptionMethod Size of keys to generate (1 if 40-bit
	 *            encryption, 2 for 128-bit, 4 for 56-bit, 16 for FIPS)
	 * @throws RdesktopCryptoException
	 */
	public void generateInitialKeys() throws RdesktopCryptoException {
		int sessionKeyEncryptionMethod = state.getSessionKeyEncryptionMethod();
		// MS-RDPBCGR 5.3.5
		if (sessionKeyEncryptionMethod == Secure.SEC_FIPS_ENCRYPTION)
			throw new RdesktopCryptoException("FIPS not supported.");
		byte[] preMasterSecret = Utilities.concatenateBytes(Utilities.padBytes(clientRandom, 24),
				Utilities.padBytes(serverRandom, 24));
		byte[] masterSecret = Utilities.concatenateBytes(Utilities.padBytes(saltedHash(preMasterSecret, new byte[] { 0x41 }), 16),
				Utilities.padBytes(saltedHash(preMasterSecret, new byte[] { 0x42, 0x42 }), 16),
				Utilities.padBytes(saltedHash(preMasterSecret, new byte[] { 0x43, 0x43, 0x43 }), 16));
		byte[] sessionKeyBlob = Utilities.concatenateBytes(Utilities.padBytes(saltedHash(masterSecret, new byte[] { 0x58 }), 16),
				Utilities.padBytes(saltedHash(masterSecret, new byte[] { 0x59, 0x59 }), 16),
				Utilities.padBytes(saltedHash(masterSecret, new byte[] { 0x5A, 0x5A, 0x5A }), 16));
		byte[] macKey128 = Utilities.padBytes(sessionKeyBlob, 16);
		byte[] decryptKey = finalHash(Utilities.slice(sessionKeyBlob, 16, 32));
		byte[] encryptKey = finalHash(Utilities.slice(sessionKeyBlob, 32, 48));
		if (sessionKeyEncryptionMethod == Secure.SEC_40BIT_ENCRYPTION) {
			logger.info("40 Bit Encryption enabled");
			macKey = reduceEntropy40Bit(macKey128);
			decryptKey = reduceEntropy40Bit(decryptKey);
			encryptKey = reduceEntropy40Bit(encryptKey);
		} else if (sessionKeyEncryptionMethod == Secure.SEC_56BIT_ENCRYPTION) {
			logger.info("56 Bit Encryption enabled");
			macKey = reduceEntropy56Bit(macKey128);
			decryptKey = reduceEntropy56Bit(decryptKey);
			encryptKey = reduceEntropy56Bit(encryptKey);
		} else if (sessionKeyEncryptionMethod == Secure.SEC_128BIT_ENCRYPTION) {
			macKey = macKey128;
			logger.info("128 Bit Encryption enabled");
		} else {
			throw new RdesktopCryptoException("TODO FIPS not supported.");
		}
		System.arraycopy(decryptKey, 0, decryptUpdateKey, 0, 16);
		System.arraycopy(encryptKey, 0, encryptUpdateKey, 0, 16);
		try {
			rc4_enc.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptKey, "RC4"));
			rc4_dec.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptKey, "RC4"));
			// rc4_dec.engineInitDecrypt(decryptKey);
		} catch (InvalidKeyException ike) {
			throw new RdesktopCryptoException("Failed to update key.", ike);
		}
		this.encryptKey = encryptKey;
		this.decryptKey = decryptKey;
	}

	private byte[] reduceEntropy40Bit(byte[] d) {
		return Utilities.concatenateBytes(new byte[] { (byte) 0xd1, 0x26, (byte) 0x9e },
				Utilities.slice(Utilities.padBytes(d, 8), 3, 8));
	}

	private byte[] reduceEntropy56Bit(byte[] d) {
		return Utilities.concatenateBytes(new byte[] { (byte) 0xd1 }, Utilities.slice(Utilities.padBytes(d, 8), 1, 8));
	}

	/*
	 * public X509Certificate readCert(int length, Packet data){ byte[] buf =
	 * new byte[length];
	 * data.copyToByteArray(buf,0,data.getPosition(),buf.length);
	 * data.incrementPosition(length); for(int i = 0; i < buf.length; i++){
	 * buf[i] = (byte) (buf[i] & 0xFF); } ByteArrayInputStream bIn = new
	 * ByteArrayInputStream(buf); X509Certificate cert = null;
	 * CertificateFactory cf = null; try { cf =
	 * CertificateFactory.getInstance("X.509"); cert =
	 * (X509Certificate)cf.generateCertificate(bIn); } catch
	 * (CertificateException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } bIn.reset(); return cert; }
	 */
	public void generateRandom() {
		/*
		 * try{ SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		 * random.nextBytes(this.client_random); }
		 * catch(NoSuchAlgorithmException
		 * e){logger.warn("No Such Random Algorithm");}
		 */
	}

	@Deprecated
	public byte[] hash16(byte[] in, byte[] salt1, byte[] salt2, int in_position) throws RdesktopCryptoException {
		md5.update(in, in_position, 16);
		md5.update(salt1, 0, 32);
		md5.update(salt2, 0, 32);
		return md5.digest();
	}

	private byte[] finalHash(byte[] k) {
		md5.update(k);
		md5.update(clientRandom);
		md5.update(serverRandom);
		return md5.digest();
	}

	private byte[] saltedHash(byte[] s, byte[] i) {
		sha1.update(i);
		sha1.update(s);
		sha1.update(this.clientRandom);
		sha1.update(this.serverRandom);
		md5.update(s);
		md5.update(sha1.digest());
		return md5.digest();
	}

	@Deprecated
	public byte[] hash48(byte[] in, byte[] salt1, byte[] salt2, int salt) throws RdesktopCryptoException {
		byte[] shasig = new byte[20];
		byte[] pad = new byte[4];
		byte[] out = new byte[48];
		int i = 0;
		for (i = 0; i < 3; i++) {
			for (int j = 0; j <= i; j++) {
				pad[j] = (byte) (salt + i);
			}
			sha1.update(pad, 0, i + 1);
			sha1.update(in, 0, 48);
			sha1.update(salt1, 0, 32);
			sha1.update(salt2, 0, 32);
			shasig = sha1.digest();
			md5.update(in, 0, 48);
			md5.update(shasig, 0, 20);
			System.arraycopy(md5.digest(), 0, out, i * 16, 16);
		}
		return out;
	}

	/**
	 * Intialise a packet at the Secure layer
	 *
	 * @param flags Encryption flags
	 * @param length Length of packet
	 * @return Intialised packet
	 * @throws RdesktopException
	 */
	public Packet init(int flags, int length) throws RdesktopException {
		int headerlength = 0;
		Packet buffer;
		if (!state.isLicenceIssued())
			headerlength = ((flags & SEC_ENCRYPT) != 0) ? 12 : 4;
		else
			headerlength = ((flags & SEC_ENCRYPT) != 0) ? 12 : 0;
		buffer = mcsLayer.init(length + headerlength);
		buffer.pushLayer(Packet.SECURE_HEADER, headerlength);
		// buffer.setHeader(Packet.SECURE_HEADER);
		// buffer.incrementPosition(headerlength);
		// buffer.setStart(buffer.getPosition());
		return buffer;
	}

	/**
	 * Read encryption information from a Secure layer PDU, obtaining and
	 * storing level of encryption and any keys received
	 *
	 * MS-RDPBCGR 2.2.1.4.3 Server Security Data (TS_UD_SC_SEC1)
	 *
	 * @param data Packet to read encryption information from
	 * @return Session Key Encryption Method
	 * @throws RdesktopException
	 *
	 */
	private void parseServerSecurityData(Packet data) throws RdesktopException {
		logger.debug("Secure.parseCryptInfo");
		int random_length = 0, RSA_info_length = 0;
		int tag = 0, length = 0;
		int next_tag = 0, end = 0;
		int serverSessionKeyEncryptionMethod = 0;
		serverSessionKeyEncryptionMethod = data.getLittleEndian32(); // 0 = None
																		// (Enhanced),
																		// 1 =
																		// 40-Bit,
																		// 2 =
																		// 128
																		// Bit,
																		// 8 =
																		// 56
																		// bits,
																		// 16 =
																		// FIPS
		int encryptionLevel = data.getLittleEndian32(); // 1 = low, 2 = medium,
														// 3 = high, 4 = FIPS
		state.setSessionKeyEncryptionMethod(0);
		logger.debug(String.format("Key size %d, encryption level %d", serverSessionKeyEncryptionMethod, encryptionLevel));
		if (encryptionLevel == 0) { // no encryption
			return;
		}
		//
		// 1. Low: All data sent from the client to the server is protected by
		// encryption based on the maximum
		// key strength supported by the client.
		// 2. Client Compatible: All data sent between the client and the server
		// is protected by encryption
		// based on the maximum key strength supported by the client.
		// 3. High: All data sent between the client and server is protected by
		// encryption based on the server's
		// maximum key strength.
		// 4. FIPS: All data sent between the client and server is protected
		// using Federal Information
		// Processing Standard 140-1 validated encryption methods.
		int sessionKeyEncryptionMethod = state.getOptions().getBestSessionKeyEncryptionMethod();
		if (encryptionLevel == 3) {
			sessionKeyEncryptionMethod = serverSessionKeyEncryptionMethod;
		}
		if (encryptionLevel > 1) {
			logger.info("Server will be sending encrypted packets");
		}
		if (encryptionLevel > 0) {
			logger.info("Client will be sending encrypted packets");
		}
		random_length = data.getLittleEndian32();
		RSA_info_length = data.getLittleEndian32();
		if (random_length != SEC_RANDOM_SIZE) {
			throw new RdesktopException("Wrong Size of Random! Got" + random_length + "expected" + SEC_RANDOM_SIZE);
		}
		this.serverRandom = new byte[random_length];
		data.copyToByteArray(this.serverRandom, 0, data.getPosition(), random_length);
		data.incrementPosition(random_length);
		end = data.getPosition() + RSA_info_length;
		if (end > data.getEnd()) {
			logger.debug("Reached end of crypt info prematurely ");
			return;
		}
		// data.incrementPosition(12); // unknown bytes
		int flags = data.getLittleEndian32(); // in_uint32_le(s, flags); // 1 =
		// RDP4-style, 0x80000002 =
		// X.509
		logger.debug("Flags = 0x" + Integer.toHexString(flags));
		if ((flags & 1) != 0) {
			logger.debug(("We're going for the RDP4-style encryption"));
			data.incrementPosition(8); // in_uint8s(s, 8); // unknown
			while (data.getPosition() < data.getEnd()) {
				tag = data.getLittleEndian16();
				length = data.getLittleEndian16();
				next_tag = data.getPosition() + length;
				switch (tag) {
				case (Secure.SEC_TAG_PUBKEY):
					if (!parsePublicKey(data)) {
						return;
					}
					break;
				case (Secure.SEC_TAG_KEYSIG):
					// Microsoft issued a key but we don't care
					break;
				default:
					throw new RdesktopException("Unimplemented decrypt tag " + tag);
				}
				data.setPosition(next_tag);
			}
			if (data.getPosition() == data.getEnd()) {
				state.setSessionKeyEncryptionMethod(sessionKeyEncryptionMethod);
			} else {
				logger.warn("End not reached!");
			}
		} else {
			int nocerts = data.getLittleEndian32(); // number of certificates
			logger.debug(String.format("We're going for the RDP5-style encryption (%d certfificates)", nocerts));
			int cacert_len = data.getLittleEndian32();
			data.incrementPosition(cacert_len);
			int cert_len = data.getLittleEndian32();
			data.incrementPosition(cert_len);
			state.setReadCert(true);
			state.setSessionKeyEncryptionMethod(sessionKeyEncryptionMethod);
		}
	}

	/**
	 * Read in a public key from a provided Secure layer PDU, and store in
	 * this.exponent and this.modulus
	 *
	 * @param data Secure layer PDU containing key data
	 * @return True if key successfully read
	 * @throws RdesktopException
	 */
	public boolean parsePublicKey(Packet data) throws RdesktopException {
		int magic = 0, modulus_length = 0;
		magic = data.getLittleEndian32();
		if (magic != SEC_RSA_MAGIC) {
			throw new RdesktopException("Wrong magic! Expected" + SEC_RSA_MAGIC + "got:" + magic);
		}
		modulus_length = data.getLittleEndian32() - SEC_PADDING_SIZE;
		if (modulus_length < 64 || modulus_length > SEC_MAX_MODULUS_SIZE) {
			throw new RdesktopException("Bad server public key size (" + (modulus_length * 8) + " bites)");
		}
		data.incrementPosition(8); // unknown modulus bits
		this.exponent = new byte[SEC_EXPONENT_SIZE];
		data.copyToByteArray(this.exponent, 0, data.getPosition(), SEC_EXPONENT_SIZE);
		data.incrementPosition(SEC_EXPONENT_SIZE);
		this.modulus = new byte[modulus_length];
		data.copyToByteArray(this.modulus, 0, data.getPosition(), modulus_length);
		data.incrementPosition(modulus_length);
		data.incrementPosition(SEC_PADDING_SIZE);
		this.server_public_key_len = modulus_length;
		if (data.getPosition() <= data.getEnd()) {
			return true;
		} else {
			return false;
		}
	}

	private void processServerSecurityData(Packet data) throws RdesktopException {
		parseServerSecurityData(data);
		if (state.getSessionKeyEncryptionMethod() == 0) {
			if (state.getSecurityType() == SecurityType.STANDARD) {
				logger.info("Disabling encryption, server is not using it.");
				state.setSecurityType(SecurityType.NONE);
			}
			return;
		}
		// this.client_random = this.generateRandom(SEC_RANDOM_SIZE);
		logger.debug("readCert = " + state.isReadCert());
		if (state.isReadCert()) { /*
									 * Which means we should use RDP5-style
									 * encryption
									 */
			// *** reverse the client random
			// this.reverse(this.client_random);
			// *** load the server public key into the stored data for
			// encryption
			/*
			 * this.exponent =
			 * this.server_public_key.getPublicExponent().toByteArray();
			 * this.modulus = this.server_public_key.getModulus().toByteArray();
			 * System.out.println("Exponent: " +
			 * server_public_key.getPublicExponent());
			 * System.out.println("Modulus: " + server_public_key.getModulus());
			 */
			// *** perform encryption
			// this.sec_crypted_random = RSA_public_encrypt(this.client_random,
			// this.server_public_key);
			// this.RSAEncrypt(SEC_RANDOM_SIZE);
			// this.RSAEncrypt(SEC_RANDOM_SIZE);
			// *** reverse the random data back
			// this.reverse(this.sec_crypted_random);
		} else {
			this.generateRandom();
			this.RSAEncrypt(SEC_RANDOM_SIZE, server_public_key_len);
		}
		this.generateInitialKeys();
	}

	/**
	 * Handle MCS info from server (server info, encryption info and channel
	 * information)
	 *
	 * @param mcs_data Data received from server
	 */
	public void processMcsData(Packet mcs_data) throws RdesktopException {
		logger.debug("Secure.processMcsData");
		int tag = 0, len = 0, length = 0, nexttag = 0;
		mcs_data.incrementPosition(21); // header (T.124 stuff, probably)
		len = mcs_data.get8();
		if ((len & 0x00000080) != 0) {
			len = mcs_data.get8();
		}
		while (mcs_data.getPosition() < mcs_data.getEnd()) {
			tag = mcs_data.getLittleEndian16();
			length = mcs_data.getLittleEndian16();
			if (length <= 4)
				return;
			nexttag = mcs_data.getPosition() + length - 4;
			switch (tag) {
			case (Secure.SEC_TAG_SRV_INFO):
				processSrvInfo(mcs_data);
				break;
			case (Secure.SEC_TAG_SRV_CRYPT):
				this.processServerSecurityData(mcs_data);
			case (Secure.SEC_TAG_SRV_CHANNELS):
				/*
				 * FIXME: We should parse this information and use it to map
				 * RDP5 channels to MCS channels
				 */
				break;
			default:
				throw new RdesktopException("Not implemented! Tag:" + tag + "not recognized!");
			}
			mcs_data.setPosition(nexttag);
		}
	}

	/**
	 * Receive a Secure layer PDU from the MCS layer
	 *
	 * @return Packet representing received Secure PDU
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 * @throws OrderException
	 */
	public Packet receive() throws RdesktopException, IOException, OrderException {
		// // TODO really not sure about this but it gets us further
		// if (state.isNegotiated() && state.isSsl() && !state.isEncryption()) {
		// Packet buffer = null;
		// while (true) {
		// int[] channel = new int[1];
		// buffer = mcsLayer.receive(channel);
		// if (buffer == null)
		// return null;
		// if (channel[0] != MCS.MCS_GLOBAL_CHANNEL) {
		// channels.channel_process(buffer, channel[0]);
		// continue;
		// }
		// buffer.setStart(buffer.getPosition());
		// return buffer;
		// }
		// }
		int sec_flags = 0;
		Packet buffer = null;
		while (true) {
			int[] channel = new int[1];
			buffer = mcsLayer.receive(channel);
			if (buffer == null)
				return null;
			buffer.setHeader(Packet.SECURE_HEADER);
			if (state.getSecurityType() == SecurityType.STANDARD || (!state.isLicenceIssued())) {
				sec_flags = buffer.getLittleEndian32();
				if (!state.isLicenceIssued() && (sec_flags & SEC_LICENCE_NEG) != 0) {
					licence.process(buffer);
					continue;
				}
				if (state.getSecurityType() == SecurityType.STANDARD && (sec_flags & SEC_ENCRYPT) != 0) {
					buffer.incrementPosition(8); // signature
					byte[] data = new byte[buffer.size() - buffer.getPosition()];
					buffer.copyToByteArray(data, 0, buffer.getPosition(), data.length);
					byte[] packet = this.decrypt(data);
					buffer.copyFromByteArray(packet, 0, buffer.getPosition(), packet.length);
					// buffer.setStart(buffer.getPosition());
					// return buffer;
				}
			}
			if (channel[0] != MCS.MCS_GLOBAL_CHANNEL) {
				channels.channel_process(buffer, channel[0]);
				continue;
			}
			buffer.setStart(buffer.getPosition());
			return buffer;
		}
	}

	/**
	 * Reverse the values in the provided array
	 *
	 * @param data Array as passed reversed on return
	 */
	public void reverse(byte[] data) {
		int i = 0, j = 0;
		byte temp = 0;
		for (i = 0, j = data.length - 1; i < j; i++, j--) {
			temp = data[i];
			data[i] = data[j];
			data[j] = temp;
		}
	}

	public void reverse(byte[] data, int length) {
		int i = 0, j = 0;
		byte temp = 0;
		for (i = 0, j = length - 1; i < j; i++, j--) {
			temp = data[i];
			data[i] = data[j];
			data[j] = temp;
		}
	}

	public void RSAEncrypt(int length, int modulus_size) throws RdesktopException {
		byte[] inr = new byte[length];
		// int outlength = 0;
		BigInteger mod = null;
		BigInteger exp = null;
		BigInteger x = null;
		this.reverse(this.exponent);
		this.reverse(this.modulus);
		System.arraycopy(this.clientRandom, 0, inr, 0, length);
		this.reverse(inr);
		if ((this.modulus[0] & 0x80) != 0) {
			byte[] temp = new byte[this.modulus.length + 1];
			System.arraycopy(this.modulus, 0, temp, 1, this.modulus.length);
			temp[0] = 0;
			mod = new BigInteger(temp);
		} else {
			mod = new BigInteger(this.modulus);
		}
		if ((this.exponent[0] & 0x80) != 0) {
			byte[] temp = new byte[this.exponent.length + 1];
			System.arraycopy(this.exponent, 0, temp, 1, this.exponent.length);
			temp[0] = 0;
			exp = new BigInteger(temp);
		} else {
			exp = new BigInteger(this.exponent);
		}
		if ((inr[0] & 0x80) != 0) {
			byte[] temp = new byte[inr.length + 1];
			System.arraycopy(inr, 0, temp, 1, inr.length);
			temp[0] = 0;
			x = new BigInteger(temp);
		} else {
			x = new BigInteger(inr);
		}
		BigInteger y = x.modPow(exp, mod);
		this.sec_crypted_random = y.toByteArray();
		if ((this.sec_crypted_random[0] & 0x80) != 0) {
			throw new RdesktopException("Wrong Sign! Expected positive Integer!");
		}
		if (this.sec_crypted_random.length > SEC_MAX_MODULUS_SIZE) {
			logger.warn("sec_crypted_random too big!"); /* FIXME */
		}
		this.reverse(this.sec_crypted_random);
		byte[] temp = new byte[SEC_MAX_MODULUS_SIZE];
		if (this.sec_crypted_random.length < modulus_size) {
			System.arraycopy(this.sec_crypted_random, 0, temp, 0, this.sec_crypted_random.length);
			for (int i = this.sec_crypted_random.length; i < temp.length; i++) {
				temp[i] = 0;
			}
			this.sec_crypted_random = temp;
		}
	}

	/**
	 * Send secure data on the global channel
	 *
	 * @param sec_data Data to send
	 * @param flags Encryption flags
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 */
	public void send(Packet sec_data, int flags) throws RdesktopException, IOException {
		send_to_channel(sec_data, flags, MCS.MCS_GLOBAL_CHANNEL);
	}

	/**
	 * Prepare data as a Secure PDU and pass down to the MCS layer
	 *
	 * @param sec_data Data to send
	 * @param flags Encryption flags
	 * @param channel Channel over which to send data
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 */
	public void send_to_channel(Packet sec_data, int flags, int channel) throws RdesktopException, IOException {
		int datalength = 0;
		byte[] signature = null;
		byte[] data;
		byte[] buffer;
		sec_data.setPosition(sec_data.getHeader(Packet.SECURE_HEADER));
		if (!state.isLicenceIssued() || (flags & SEC_ENCRYPT) != 0) {
			sec_data.setLittleEndian32(flags);
		}
		if ((flags & SEC_ENCRYPT) != 0) {
			flags &= ~SEC_ENCRYPT;
			datalength = sec_data.getEnd() - sec_data.getPosition() - 8;
			data = new byte[datalength];
			buffer = null;
			sec_data.copyToByteArray(data, 0, sec_data.getPosition() + 8, datalength);
			signature = this.sign(this.macKey, 8, this.macKey.length, data, datalength);
			buffer = this.encrypt(data, datalength);
			sec_data.copyFromByteArray(signature, 0, sec_data.getPosition(), 8);
			sec_data.copyFromByteArray(buffer, 0, sec_data.getPosition() + 8, datalength);
		}
		// McsLayer.send(sec_data);
		mcsLayer.send_to_channel(sec_data, channel);
	}

	/**
	 * Construct MCS data, including channel, encryption and display options
	 *
	 * @return Packet populated with MCS data
	 */
	public Packet sendMcsData() {
		logger.debug("Secure.sendMcsData");
		Packet buffer = new Packet(512);
		int hostlen = 2 * (state.getWorkstationName() == null ? 0 : state.getWorkstationName().length());
		if (hostlen > 30) {
			hostlen = 30;
		}
		int length = 158;
		if (state.isRDP5())
			length += 80 + 12 + 4;
		if (state.isRDP5() && (channels.num_channels() > 0))
			length += channels.num_channels() * 12 + 8;
		buffer.setBigEndian16(5); /* unknown */
		buffer.setBigEndian16(0x14);
		buffer.set8(0x7c);
		buffer.setBigEndian16(1);
		buffer.setBigEndian16(length | 0x8000); // remaining length
		buffer.setBigEndian16(8); // length?
		buffer.setBigEndian16(16);
		buffer.set8(0);
		buffer.setLittleEndian16(0xc001);
		buffer.set8(0);
		buffer.setLittleEndian32(0x61637544); // "Duca" ?!
		buffer.setBigEndian16(length - 14 | 0x8000); // remaining length
		// Client information
		buffer.setLittleEndian16(SEC_TAG_CLI_INFO);
		buffer.setLittleEndian16(state.isRDP5() ? 216 : 136); // length
		buffer.setLittleEndian16(state.isRDP5() ? 4 : 1);
		buffer.setLittleEndian16(8);
		buffer.setLittleEndian16(state.getWidth());
		buffer.setLittleEndian16(state.getHeight());
		buffer.setLittleEndian16(0xca01);
		buffer.setLittleEndian16(0xaa03);
		buffer.setLittleEndian32(state.getOptions().getKeymap().getMapCode());
		buffer.setLittleEndian32(state.isRDP5() ? 2600 : 419); // or 0ece //
		// client
		// build? we
		// are 2600
		// compatible
		// :-)
		/* Unicode name of client, padded to 32 bytes */
		buffer.outUnicodeString(state.getWorkstationName().toUpperCase(), hostlen);
		buffer.incrementPosition(30 - hostlen);
		buffer.setLittleEndian32(4);
		buffer.setLittleEndian32(0);
		buffer.setLittleEndian32(12);
		buffer.incrementPosition(64); /* reserved? 4 + 12 doublewords */
		buffer.setLittleEndian16(0xca01); // out_uint16_le(s, 0xca01);
		buffer.setLittleEndian16(state.isRDP5() ? 1 : 0);
		if (state.isRDP5()) {
			buffer.setLittleEndian32(0); // out_uint32(s, 0);
			boolean thirtyTwoBitColor = false;
			int bpp = state.getServerBpp();
			if (bpp >= 32) {
				bpp = 24;
				thirtyTwoBitColor = true;
			}
			buffer.setLittleEndian16(bpp); // out_uint8(s,
											// g_server_bpp);
			switch (bpp) {
			case 32:
				buffer.setLittleEndian16(0x0008);
				break;
			case 24:
				buffer.setLittleEndian16(0x0001);
				break;
			case 15:
				buffer.setLittleEndian16(0x0004);
				break;
			default:
				buffer.setLittleEndian16(0x0002);
				break;
			}
			// early caps
			// RNS_UD_CS_SUPPORT_ERRINFO_PDU Indicates that the client supports
			// the Set Error Info 0x0001
			// RNS_UD_CS_SUPPORT_STATUSINFO_PDU Indicates that the client
			// supports the Server Status Info PDU (section 2.2.5.2). 0x0004
			// RNS_UD_CS_SUPPORT_MONITOR_LAYOUT_PDU 0x0040 - TODO
			// RNS_UD_CS_VALID_CONNECTION_TYPE 0x0020
			// RNS_UD_CS_SUPPORT_NETCHAR_AUTODETECT 0x0080 - TODO
			// RNS_UD_CS_WANT_32BPP_SESSION 0x0002
			// int earlyCaps = 0x0001 | 0x0040 | 0x0020 | 0x0080 TODO need to
			// support autodetect PDUs;
			// int earlyCaps = 0x0001 | 0x0040 | 0x0020 | 0x0080;
			// int earlyCaps = 0x0001 | 0x0020;
			int earlyCaps = 0x0001 | 0x0004;
			if (thirtyTwoBitColor)
				earlyCaps = earlyCaps | 0x0002;
			buffer.setLittleEndian16(earlyCaps);
			buffer.incrementPosition(64);
			if (state.getOptions().getConnectionType() == Rdp.CONNECTION_TYPE_AUTODETECT && (earlyCaps & 0x0080) == 0)
				throw new IllegalStateException("TODO Cannot use autodetect with support for detection PDUs.");
			buffer.set8(state.getOptions().getConnectionType());
			buffer.incrementPosition(1); // pad
			buffer.setLittleEndian32(state.getSecurityType().getMask());
			buffer.setLittleEndian16(SEC_TAG_CLI_4); // out_uint16_le(s,
			// SEC_TAG_CLI_4);
			buffer.setLittleEndian16(12); // out_uint16_le(s, 12);
			buffer.setLittleEndian32(state.getOptions().isConsoleSession() ? 0xb : 0xd); // out_uint32_le(s,
			// g_console_session
			// ?
			// 0xb
			// :
			// 9);
			buffer.setLittleEndian32(0); // out_uint32(s, 0);
		}
		sendClientSecurityData(buffer);
		if (state.isRDP5() && (channels.num_channels() > 0)) {
			logger.debug(("num_channels is " + channels.num_channels()));
			buffer.setLittleEndian16(SEC_TAG_CLI_CHANNELS); // out_uint16_le(s,
			// SEC_TAG_CLI_CHANNELS);
			buffer.setLittleEndian16(channels.num_channels() * 12 + 8); // out_uint16_le(s,
			// g_num_channels
			// * 12
			// + 8);
			// //
			// length
			buffer.setLittleEndian32(channels.num_channels()); // out_uint32_le(s,
			// g_num_channels);
			// // number of
			// virtual
			// channels
			for (int i = 0; i < channels.num_channels(); i++) {
				logger.debug(("Requesting channel " + channels.channel(i).name()));
				buffer.out_uint8p(channels.channel(i).name(), 8); // out_uint8a(s,
				// g_channels[i].name,
				// 8);
				buffer.setBigEndian32(channels.channel(i).flags()); // out_uint32_be(s,
				// g_channels[i].flags);
			}
		}
		buffer.markEnd();
		return buffer;
	}

	private void sendClientSecurityData(Packet buffer) {
		logger.debug(
				String.format("Request encryption method %d for session keys", state.getOptions().getSessionKeyEncryptionMethod()));
		buffer.setLittleEndian16(SEC_TAG_CLI_CRYPT);
		buffer.setLittleEndian16(12); // length
		buffer.setLittleEndian32(state.getOptions().getSessionKeyEncryptionMethod());
		buffer.setLittleEndian32(0);
		// Client encryption settings //
		// buffer.setLittleEndian16(SEC_TAG_CLI_CRYPT);
		// buffer.setLittleEndian16(state.isRDP5() ? 12 : 8); // length
		// // if(Options.use_rdp5) buffer.setLittleEndian32(Options.encryption ?
		// // 0x1b : 0); // 128-bit encryption supported
		// // else
		// buffer.setLittleEndian32(state.isEncryption() ?
		// (state.getOptions().isConsoleSession() ? 0xb : 0x3) : 0);
		// if (state.isRDP5())
		// buffer.setLittleEndian32(0); // unknown
		//
	}

	/**
	 * Write a 32-bit integer value to an array of bytes, length 4
	 *
	 * @param data Modified by method to be a 4-byte array representing the
	 *            parameter value
	 * @param value Integer value to return as a little-endian 32-bit value
	 */
	public void setLittleEndian32(byte[] data, int value) {
		data[3] = (byte) ((value >>> 24) & 0xff);
		data[2] = (byte) ((value >>> 16) & 0xff);
		data[1] = (byte) ((value >>> 8) & 0xff);
		data[0] = (byte) (value & 0xff);
	}

	/**
	 * Generate MD5 signature
	 *
	 * @param session_key Key with which to sign data
	 * @param length Length of signature
	 * @param keylen Length of key
	 * @param data Data to sign
	 * @param datalength Length of data to sign
	 * @return Signature for data
	 * @throws RdesktopCryptoException
	 * @throws CryptoException
	 */
	public byte[] sign(byte[] session_key, int length, int keylen, byte[] data, int datalength) throws RdesktopCryptoException {
		byte[] shasig = new byte[20];
		byte[] md5sig = new byte[16];
		byte[] lenhdr = new byte[4];
		byte[] signature = new byte[length];
		this.setLittleEndian32(lenhdr, datalength);
		sha1.reset();
		sha1.update(session_key, 0, keylen/* length */);
		sha1.update(pad_54, 0, 40);
		sha1.update(lenhdr, 0, 4);
		sha1.update(data, 0, datalength);
		shasig = sha1.digest();
		sha1.reset();
		md5.reset();
		md5.update(session_key, 0, keylen/* length */);
		md5.update(pad_92, 0, 48);
		md5.update(shasig, 0, 20);
		md5sig = md5.digest();
		md5.reset();
		System.arraycopy(md5sig, 0, signature, 0, length);
		return signature;
	}

	/**
	 * @param key
	 * @param update_key
	 * @return
	 * @throws RdesktopCryptoException
	 */
	public byte[] update(byte[] key, byte[] update_key) throws RdesktopCryptoException {
		int keylength = key.length;
		byte[] shasig = new byte[20];
		byte[] update = new byte[keylength]; // changed from 8 - rdesktop
		// 1.2.0
		byte[] thekey = new byte[key.length];
		sha1.reset();
		sha1.update(update_key, 0, keylength);
		sha1.update(pad_54, 0, 40);
		sha1.update(key, 0, keylength); // changed from 8 - rdesktop 1.2.0
		shasig = sha1.digest();
		sha1.reset();
		md5.reset();
		md5.update(update_key, 0, keylength); // changed from 8 - rdesktop
		// 1.2.0
		md5.update(pad_92, 0, 48);
		md5.update(shasig, 0, 20);
		thekey = md5.digest();
		md5.reset();
		System.arraycopy(thekey, 0, update, 0, keylength);
		try {
			rc4_update.init(Cipher.DECRYPT_MODE, new SecretKeySpec(update, "RC4"));
			thekey = rc4_update.doFinal(thekey, 0, keylength);
			if (state.getSessionKeyEncryptionMethod() == Secure.SEC_40BIT_ENCRYPTION) {
				thekey = reduceEntropy40Bit(thekey);
			} else if (state.getSessionKeyEncryptionMethod() == Secure.SEC_56BIT_ENCRYPTION) {
				thekey = reduceEntropy56Bit(thekey);
			}
			return thekey;
		} catch (InvalidKeyException ike) {
			throw new RdesktopCryptoException("Failed to update key.", ike);
		} catch (IllegalBlockSizeException e) {
			throw new RdesktopCryptoException("Failed to update key.", e);
		} catch (BadPaddingException e) {
			throw new RdesktopCryptoException("Failed to update key.", e);
		}
	}

	/**
	 * Read server info from packet, specifically the RDP version of the server
	 *
	 * @param mcs_data Packet to read
	 */
	private void processSrvInfo(Packet mcs_data) {
		state.setServerRdpVersion(mcs_data.getLittleEndian32() & 0x0000FFFF); // in_uint16_le(s,
		// g_server_rdp_version);
		logger.debug(("Server RDP version is " + state.getServerRdpVersion()));
		if (state.isNegotiated()) {
			int protocol = mcs_data.getLittleEndian32();
			SecurityType serverType = SecurityType.fromMask(protocol);
			if (serverType != state.getSecurityType())
				throw new IllegalStateException(String.format("Client wants %s but server did not agree and wants %s.",
						state.getSecurityType(), serverType));
			// TODO windows 2012 seems to be returning rubbish here?
			earlyCaps = mcs_data.getLittleEndian32();
			logger.info("Early server capabilities is " + earlyCaps);
		}
	}

	@Override
	public Rdp getParent() {
		return rdp;
	}
}
