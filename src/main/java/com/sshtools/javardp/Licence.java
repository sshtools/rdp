/* Licence.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Handles request, receipt and processing of
 *          licences
 */
// Created on 02-Jul-2003
package com.sshtools.javardp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.CredentialProvider.CredentialType;
import com.sshtools.javardp.layers.Secure;

public class Licence {
	static Logger logger = LoggerFactory.getLogger(Licence.class);
	private static final int LICENCE_HWID_SIZE = 20;
	private static final int LICENCE_SIGNATURE_SIZE = 16;
	private static final int LICENCE_TAG_CHALLENGE = 0x02;
	private static final int LICENCE_TAG_RESPONSE = 0x15;
	/*
	 * private static final int LICENCE_TAG_DEMAND = 0x0201; private static
	 * final int LICENCE_TAG_AUTHREQ = 0x0202; private static final int
	 * LICENCE_TAG_ISSUE = 0x0203; private static final int LICENCE_TAG_REISSUE
	 * = 0x0204; // rdesktop 1.2.0 private static final int LICENCE_TAG_PRESENT
	 * = 0x0212; // rdesktop 1.2.0 private static final int LICENCE_TAG_REQUEST
	 * = 0x0213; private static final int LICENCE_TAG_AUTHRESP = 0x0215; private
	 * static final int LICENCE_TAG_RESULT = 0x02ff;
	 */
	private static final int LICENCE_TAG_DEMAND = 0x01;
	private static final int LICENCE_TAG_HOST = 0x0010;
	private static final int LICENCE_TAG_ISSUE = 0x03;
	private static final int LICENCE_TAG_PRESENT = 0x12;
	private static final int LICENCE_TAG_REISSUE = 0x04;
	private static final int LICENCE_TAG_REQUEST = 0x13;
	private static final int LICENCE_TAG_ERROR = 0xff;
	private static final int LICENCE_TAG_USER = 0x000f;
	/* constants for the licence negotiation */
	private static final int LICENCE_TOKEN_SIZE = 10;
	private IContext context;
	private byte[] in_token = null, in_sig = null;
	private byte[] licence_key = null;
	private byte[] licence_sign_key = null;
	private Secure secure = null;
	private State state;

	public Licence(State state, IContext context, Secure s) {
		this.state = state;
		this.context = context;
		secure = s;
		licence_key = new byte[16];
		licence_sign_key = new byte[16];
	}

	public byte[] generate_hwid() throws UnsupportedEncodingException {
		byte[] hwid = new byte[LICENCE_HWID_SIZE];
		secure.setLittleEndian32(hwid, 2);
		byte[] name = state.getWorkstationName().getBytes("US-ASCII");
		if (name.length > LICENCE_HWID_SIZE - 4) {
			System.arraycopy(name, 0, hwid, 4, LICENCE_HWID_SIZE - 4);
		} else {
			System.arraycopy(name, 0, hwid, 4, name.length);
		}
		return hwid;
	}

	/**
	 * Generate a set of encryption keys
	 * 
	 * @param client_key Array in which to store client key
	 * @param server_key Array in which to store server key
	 * @param client_rsa Array in which to store RSA data
	 * @throws RdesktopCryptoException on error
	 */
	public void generate_keys(byte[] client_key, byte[] server_key, byte[] client_rsa) throws RdesktopCryptoException {
		byte[] session_key = new byte[48];
		byte[] temp_hash = new byte[48];
		temp_hash = secure.hash48(client_rsa, client_key, server_key, 65);
		session_key = secure.hash48(temp_hash, server_key, client_key, 65);
		System.arraycopy(session_key, 0, this.licence_sign_key, 0, 16);
		this.licence_key = secure.hash16(session_key, client_key, server_key, 16);
	}

	/**
	 * Handle an authorisation request, based on a licence signature (store
	 * signatures in this Licence object
	 * 
	 * @param data Packet containing details of request
	 * @return True if signature is read successfully
	 * @throws RdesktopException on error
	 */
	public boolean parse_authreq(Packet data) throws RdesktopException {
		int tokenlen = 0;
		data.incrementPosition(6); // unknown
		tokenlen = data.getLittleEndian16();
		if (tokenlen != LICENCE_TOKEN_SIZE) {
			throw new RdesktopException("Wrong Tokenlength!");
		}
		this.in_token = new byte[tokenlen];
		data.copyToByteArray(this.in_token, 0, data.getPosition(), tokenlen);
		data.incrementPosition(tokenlen);
		this.in_sig = new byte[LICENCE_SIGNATURE_SIZE];
		data.copyToByteArray(this.in_sig, 0, data.getPosition(), LICENCE_SIGNATURE_SIZE);
		data.incrementPosition(LICENCE_SIGNATURE_SIZE);
		if (data.getPosition() == data.getEnd()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Present a licence to the server
	 * 
	 * @param client_random client random
	 * @param rsa_data rsa data
	 * @param licence_data license data
	 * @param licence_size license size
	 * @param hwid hwid
	 * @param signature signature
	 * @throws RdesktopException on error
	 * @throws IOException on error
	 * @throws BadPaddingException on error
	 * @throws IllegalBlockSizeException on error
	 * @throws InvalidKeyException on error
	 */
	public void present(byte[] client_random, byte[] rsa_data, byte[] licence_data, int licence_size, byte[] hwid, byte[] signature)
			throws RdesktopException, IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		int sec_flags = Secure.SEC_LICENCE_NEG;
		int length = /* rdesktop is 16 not 20, but this must be wrong?! */
				20 + Secure.SEC_RANDOM_SIZE + Secure.SEC_MODULUS_SIZE + Secure.SEC_PADDING_SIZE + licence_size + LICENCE_HWID_SIZE
						+ LICENCE_SIGNATURE_SIZE;
		Packet s = secure.init(sec_flags, length + 4);
		s.set8(LICENCE_TAG_PRESENT);
		s.set8(2); // version
		s.setLittleEndian16(length);
		s.setLittleEndian32(1);
		s.setLittleEndian16(0);
		s.setLittleEndian16(0x0201);
		s.copyFromByteArray(client_random, 0, s.getPosition(), Secure.SEC_RANDOM_SIZE);
		s.incrementPosition(Secure.SEC_RANDOM_SIZE);
		s.setLittleEndian16(0);
		s.setLittleEndian16((Secure.SEC_MODULUS_SIZE + Secure.SEC_PADDING_SIZE));
		s.copyFromByteArray(rsa_data, 0, s.getPosition(), Secure.SEC_MODULUS_SIZE);
		s.incrementPosition(Secure.SEC_MODULUS_SIZE);
		s.incrementPosition(Secure.SEC_PADDING_SIZE);
		s.setLittleEndian16(1);
		s.setLittleEndian16(licence_size);
		s.copyFromByteArray(licence_data, 0, s.getPosition(), licence_size);
		s.incrementPosition(licence_size);
		s.setLittleEndian16(1);
		s.setLittleEndian16(LICENCE_HWID_SIZE);
		s.copyFromByteArray(hwid, 0, s.getPosition(), LICENCE_HWID_SIZE);
		s.incrementPosition(LICENCE_HWID_SIZE);
		s.copyFromByteArray(signature, 0, s.getPosition(), LICENCE_SIGNATURE_SIZE);
		s.incrementPosition(LICENCE_SIGNATURE_SIZE);
		s.markEnd();
		secure.send(s, sec_flags);
	}

	/**
	 * Process and handle licence data from a packet
	 * 
	 * @param data Packet containing licence data
	 * @throws RdesktopException on error
	 * @throws IOException on error
	 */
	public void process(Packet data) throws RdesktopException, IOException {
		int tag = 0;
		tag = data.get8();
		data.incrementPosition(3); // version, length
		switch (tag) {
		case (LICENCE_TAG_DEMAND):
			this.process_demand(data);
			break;
		case (LICENCE_TAG_CHALLENGE):
			processChallenge(data);
			break;
		case (LICENCE_TAG_ISSUE):
			this.process_issue(data);
			break;
		case (LICENCE_TAG_REISSUE):
			if (logger.isDebugEnabled())
				logger.debug("Presented licence was accepted!");
			break;
		case (LICENCE_TAG_ERROR):
			try {
				processError(data);
			} catch (RdesktopLicenseException rle) {
				switch (rle.getState()) {
				case RdesktopLicenseException.LICENSE_STATE_NO_TRANSITION:
					logger.info("License OK");
					state.setLicenceIssued(true);
					break;
				default:
					throw rle;
				}
			}
			break;
		default:
			logger.warn("got licence tag: " + tag);
		}
	}

	/**
	 * Process a licensing error
	 * 
	 * @param data Packet containing error details
	 * @throws RdesktopException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws CryptoException
	 */
	private void processError(Packet data) throws RdesktopException, UnsupportedEncodingException, IOException {
		int dwErrorCode = data.getLittleEndian32();
		int dwStateTransition = data.getLittleEndian32();
		throw new RdesktopLicenseException(dwErrorCode, dwStateTransition);
	}

	/**
	 * Process an authorisation request
	 * 
	 * @param data Packet containing request details
	 * @throws RdesktopException
	 * @throws IOException
	 */
	private void processChallenge(Packet data) throws RdesktopException, IOException {
		byte[] out_token = new byte[LICENCE_TOKEN_SIZE];
		byte[] decrypt_token = new byte[LICENCE_TOKEN_SIZE];
		byte[] crypt_hwid = new byte[LICENCE_HWID_SIZE];
		byte[] sealed_buffer = new byte[LICENCE_TOKEN_SIZE + LICENCE_HWID_SIZE];
		byte[] out_sig = new byte[LICENCE_SIGNATURE_SIZE];
		Cipher rc4_licence = createCipher();
		byte[] crypt_key = null;
		/* parse incoming packet and save encrypted token */
		if (parse_authreq(data) != true) {
			throw new RdesktopException("Authentication Request was corrupt!");
		}
		System.arraycopy(this.in_token, 0, out_token, 0, LICENCE_TOKEN_SIZE);
		/* decrypt token. It should read TEST in Unicode */
		crypt_key = new byte[this.licence_key.length];
		System.arraycopy(this.licence_key, 0, crypt_key, 0, this.licence_key.length);
		try {
			rc4_licence.init(Cipher.DECRYPT_MODE, new SecretKeySpec(crypt_key, "RC4"));
			rc4_licence.doFinal(this.in_token, 0, LICENCE_TOKEN_SIZE, decrypt_token, 0);
			/* construct HWID */
			byte[] hwid = this.generate_hwid();
			/* generate signature for a buffer of token and HWId */
			System.arraycopy(decrypt_token, 0, sealed_buffer, 0, LICENCE_TOKEN_SIZE);
			System.arraycopy(hwid, 0, sealed_buffer, LICENCE_TOKEN_SIZE, LICENCE_HWID_SIZE);
			out_sig = secure.sign(this.licence_sign_key, 16, 16, sealed_buffer, sealed_buffer.length);
			out_sig = new byte[LICENCE_SIGNATURE_SIZE]; // set to 0
			/* now crypt the hwid */
			System.arraycopy(this.licence_key, 0, crypt_key, 0, this.licence_key.length);
			rc4_licence.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(crypt_key, "RC4"));
			rc4_licence.update(hwid, 0, LICENCE_HWID_SIZE, crypt_hwid, 0);
			this.sendResponse(out_token, crypt_hwid, out_sig);
		} catch (InvalidKeyException ike) {
			throw new RdesktopCryptoException("Failed to process license.", ike);
		} catch (IllegalBlockSizeException e) {
			throw new RdesktopCryptoException("Failed to process license.", e);
		} catch (BadPaddingException e) {
			throw new RdesktopCryptoException("Failed to process license.", e);
		} catch (ShortBufferException e) {
			throw new RdesktopCryptoException("Failed to process license.", e);
		}
	}

	private Cipher createCipher() {
		Cipher rc4_licence;
		try {
			rc4_licence = Cipher.getInstance("RC4");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("No RC4.", e);
		} catch (NoSuchPaddingException e) {
			throw new IllegalStateException("No RC4.", e);
		}
		return rc4_licence;
	}

	/**
	 * Process a demand for a licence. Find a license and transmit to server, or
	 * request new licence
	 * 
	 * @param data Packet containing details of licence demand
	 * @throws RdesktopException on error
	 * @throws IOException on error
	 */
	public void process_demand(Packet data) throws RdesktopException, IOException {
		byte[] null_data = new byte[Secure.SEC_MODULUS_SIZE];
		byte[] server_random = new byte[Secure.SEC_RANDOM_SIZE];
		byte[] host = state.getWorkstationName().getBytes("US-ASCII");
		List<String> creds = state.getCredentialProvider().getCredentials("license", 0, CredentialType.USERNAME);
		byte[] user = StringUtils.defaultIfBlank(creds == null ? null : creds.get(0), "").getBytes("US-ASCII");
		/* retrieve the server random */
		data.copyToByteArray(server_random, 0, data.getPosition(), server_random.length);
		data.incrementPosition(server_random.length);
		/* Null client keys are currently used */
		this.generate_keys(null_data, server_random, null_data);
		try {
			if (!state.getOptions().isBuiltInLicence() && state.getOptions().isLoadLicence()) {
				byte[] licence_data = load_licence();
				if ((licence_data != null) && (licence_data.length > 0)) {
					if (logger.isDebugEnabled())
						logger.debug("licence_data.length = " + licence_data.length);
					/* Generate a signature for the HWID buffer */
					byte[] hwid = generate_hwid();
					byte[] signature = secure.sign(this.licence_sign_key, 16, 16, hwid, hwid.length);
					/* now crypt the hwid */
					Cipher rc4_licence = createCipher();
					byte[] crypt_key = new byte[this.licence_key.length];
					byte[] crypt_hwid = new byte[LICENCE_HWID_SIZE];
					System.arraycopy(this.licence_key, 0, crypt_key, 0, this.licence_key.length);
					rc4_licence.init(Cipher.DECRYPT_MODE, new SecretKeySpec(crypt_key, "RC4"));
					rc4_licence.doFinal(hwid, 0, LICENCE_HWID_SIZE, crypt_hwid, 0);
					present(null_data, null_data, licence_data, licence_data.length, crypt_hwid, signature);
					if (logger.isDebugEnabled())
						logger.debug("Presented stored licence to server!");
					return;
				}
			}
			this.send_request(null_data, null_data, user, host);
		} catch (InvalidKeyException ike) {
			throw new RdesktopCryptoException("Failed to process license.", ike);
		} catch (IllegalBlockSizeException e) {
			throw new RdesktopCryptoException("Failed to process license.", e);
		} catch (BadPaddingException e) {
			throw new RdesktopCryptoException("Failed to process license.", e);
		} catch (ShortBufferException e) {
			throw new RdesktopCryptoException("Failed to process license.", e);
		}
	}

	/**
	 * Handle a licence issued by the server, save to disk if
	 * Options.save_licence
	 * 
	 * @param data Packet containing issued licence
	 * @throws RdesktopCryptoException on error
	 * @throws IOException on error
	 */
	public void process_issue(Packet data) throws IOException, RdesktopCryptoException {
		int length = 0;
		int check = 0;
		Cipher rc4_licence = createCipher();
		byte[] key = new byte[this.licence_key.length];
		System.arraycopy(this.licence_key, 0, key, 0, this.licence_key.length);
		data.incrementPosition(2); // unknown
		length = data.getLittleEndian16();
		if (data.getPosition() + length > data.getEnd()) {
			return;
		}
		try {
			rc4_licence.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "RC4"));
			byte[] buffer = new byte[length];
			data.copyToByteArray(buffer, 0, data.getPosition(), length);
			rc4_licence.doFinal(buffer, 0, length, buffer, 0);
			data.copyFromByteArray(buffer, 0, data.getPosition(), length);
			check = data.getLittleEndian16();
			if (check != 0) {
				// return;
			}
			state.setLicenceIssued(true);
			/*
			 * data.incrementPosition(2); // in_uint8s(s, 2); // pad
			 * 
			 * // advance to fourth string length = 0; for (int i = 0; i < 4;
			 * i++) { data.incrementPosition(length); // in_uint8s(s, length);
			 * length = data.getLittleEndian32(length); // in_uint32_le(s,
			 * length); if (!(data.getPosition() + length <= data.getEnd()))
			 * return; }
			 */
			if (logger.isDebugEnabled())
				logger.debug("Server issued Licence");
			if (state.getOptions().isSaveLicence())
				save_licence(data, length - 2);
		} catch (InvalidKeyException ike) {
			throw new RdesktopCryptoException("Failed to process license.", ike);
		} catch (ShortBufferException e) {
			throw new RdesktopCryptoException("Failed to process license.", e);
		} catch (IllegalBlockSizeException e) {
			throw new RdesktopCryptoException("Failed to process license.", e);
		} catch (BadPaddingException e) {
			throw new RdesktopCryptoException("Failed to process license.", e);
		}
	}

	/**
	 * Respond to authorisation request, with token, hwid and signature, send
	 * response to server
	 * 
	 * @param token Token data
	 * @param crypt_hwid HWID for encryption
	 * @param signature Signature data
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws CryptoException
	 */
	private void sendResponse(byte[] token, byte[] crypt_hwid, byte[] signature)
			throws RdesktopException, IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		int sec_flags = Secure.SEC_LICENCE_NEG;
		int length = 58;
		Packet data = null;
		data = secure.init(sec_flags, length + 2);
		data.set8(LICENCE_TAG_RESPONSE);
		data.set8(2); // version
		data.setLittleEndian16(length);
		data.setLittleEndian16(1);
		data.setLittleEndian16(LICENCE_TOKEN_SIZE);
		data.copyFromByteArray(token, 0, data.getPosition(), LICENCE_TOKEN_SIZE);
		data.incrementPosition(LICENCE_TOKEN_SIZE);
		data.setLittleEndian16(1);
		data.setLittleEndian16(LICENCE_HWID_SIZE);
		data.copyFromByteArray(crypt_hwid, 0, data.getPosition(), LICENCE_HWID_SIZE);
		data.incrementPosition(LICENCE_HWID_SIZE);
		data.copyFromByteArray(signature, 0, data.getPosition(), LICENCE_SIGNATURE_SIZE);
		data.incrementPosition(LICENCE_SIGNATURE_SIZE);
		data.markEnd();
		secure.send(data, sec_flags);
	}

	/**
	 * Send a request for a new licence, or to approve a stored licence
	 * 
	 * @param client_random client random
	 * @param rsa_data rcs data
	 * @param username username
	 * @param hostname hostname
	 * @throws RdesktopException on error
	 * @throws IOException on error
	 * @throws BadPaddingException on error
	 * @throws IllegalBlockSizeException on error
	 * @throws InvalidKeyException on error
	 */
	public void send_request(byte[] client_random, byte[] rsa_data, byte[] username, byte[] hostname)
			throws RdesktopException, IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		int sec_flags = Secure.SEC_LICENCE_NEG;
		int userlen = (username.length == 0 ? 0 : username.length + 1);
		int hostlen = (hostname.length == 0 ? 0 : hostname.length + 1);
		int length = 128 + userlen + hostlen;
		Packet buffer = secure.init(sec_flags, length);
		buffer.set8(LICENCE_TAG_REQUEST);
		buffer.set8(2); // version
		buffer.setLittleEndian16(length);
		buffer.setLittleEndian32(1);
		if (state.getOptions().isBuiltInLicence() && (!state.getOptions().isLoadLicence())
				&& (!state.getOptions().isSaveLicence())) {
			if (logger.isDebugEnabled())
				logger.debug("Using built-in Windows Licence");
			buffer.setLittleEndian32(0x03010000);
		} else {
			if (logger.isDebugEnabled())
				logger.debug("Requesting licence");
			buffer.setLittleEndian32(0xff010000);
		}
		buffer.copyFromByteArray(client_random, 0, buffer.getPosition(), Secure.SEC_RANDOM_SIZE);
		buffer.incrementPosition(Secure.SEC_RANDOM_SIZE);
		buffer.setLittleEndian16(0);
		buffer.setLittleEndian16(Secure.SEC_MODULUS_SIZE + Secure.SEC_PADDING_SIZE);
		buffer.copyFromByteArray(rsa_data, 0, buffer.getPosition(), Secure.SEC_MODULUS_SIZE);
		buffer.incrementPosition(Secure.SEC_MODULUS_SIZE);
		buffer.incrementPosition(Secure.SEC_PADDING_SIZE);
		buffer.setLittleEndian16(LICENCE_TAG_USER);
		buffer.setLittleEndian16(userlen);
		if (username.length != 0) {
			buffer.copyFromByteArray(username, 0, buffer.getPosition(), userlen - 1);
		}
		// else {
		// buffer.copyFromByteArray(username, 0, buffer.getPosition(), userlen);
		// }
		buffer.incrementPosition(userlen);
		buffer.setLittleEndian16(LICENCE_TAG_HOST);
		buffer.setLittleEndian16(hostlen);
		if (hostname.length != 0) {
			buffer.copyFromByteArray(hostname, 0, buffer.getPosition(), hostlen - 1);
		} else {
			buffer.copyFromByteArray(hostname, 0, buffer.getPosition(), hostlen);
		}
		buffer.incrementPosition(hostlen);
		buffer.markEnd();
		secure.send(buffer, sec_flags);
	}

	/**
	 * Load a licence from disk
	 * 
	 * @return Raw byte data for stored licence
	 * @throws IOException
	 */
	byte[] load_licence() throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("load_licence");
		// String home = "/root"; // getenv("HOME");
		return context.loadLicense();
	}

	/**
	 * Save a licence to disk
	 * 
	 * @param data Packet containing licence data
	 * @param length Length of licence
	 * @throws IOException
	 */
	void save_licence(Packet data, int length) throws IOException {
		if (logger.isDebugEnabled())
			logger.debug("save_licence");
		int len;
		int startpos = data.getPosition();
		data.incrementPosition(2); // Skip first two bytes
		/* Skip three strings */
		for (int i = 0; i < 3; i++) {
			len = data.getLittleEndian32();
			data.incrementPosition(len);
			/*
			 * Make sure that we won't be past the end of data after reading the
			 * next length value
			 */
			if (data.getPosition() + 4 - startpos > length) {
				logger.warn("Error in parsing licence key.");
				return;
			}
		}
		len = data.getLittleEndian32();
		if (logger.isDebugEnabled())
			logger.debug("save_licence: len=" + len);
		if (data.getPosition() + len - startpos > length) {
			logger.warn("Error in parsing licence key.");
			return;
		}
		byte[] databytes = new byte[len];
		data.copyToByteArray(databytes, 0, data.getPosition(), len);
		context.saveLicense(databytes);
		/*
		 * String dirpath = Options.licence_path;//home+"/.rdesktop"; String
		 * filepath = dirpath +"/licence."+Options.hostname;
		 * 
		 * File file = new File(dirpath); file.mkdir(); try{ FileOutputStream fd
		 * = new FileOutputStream(filepath);
		 * 
		 * // write to the licence file byte[] databytes = new byte[len];
		 * data.copyToByteArray(databytes,0,data.getPosition(),len);
		 * fd.write(databytes); fd.close(); logger.info("Stored licence at " +
		 * filepath); } catch(FileNotFoundException
		 * e){logger.info("save_licence: file path not valid!");}
		 * catch(IOException e){logger.warn("IOException in save_licence");}
		 */
	}
}
