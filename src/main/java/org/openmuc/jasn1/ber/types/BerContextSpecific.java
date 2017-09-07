/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jasn1.ber.types;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.openmuc.jasn1.ber.BerByteArrayOutputStream;
import org.openmuc.jasn1.ber.BerInputStream;
import org.openmuc.jasn1.ber.BerLength;
import org.openmuc.jasn1.ber.BerTag;

public class BerContextSpecific implements Serializable, BerType {
	private static final long serialVersionUID = 1L;
	public byte[] code = null;
	public BerType value = null;
	private BerTag tag;
	{
		tag = new BerTag(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 0);
	}

	public BerContextSpecific() {
	}

	public BerContextSpecific(byte[] code, int tagNumber) {
		this.code = code;
		tag = new BerTag(this.tag.tagClass, this.tag.primitive, tagNumber);
	}

	public BerContextSpecific(BerType value, int tagNumber) {
		this.value = value;
		tag = new BerTag(this.tag.tagClass, this.tag.primitive, tagNumber);
	}
	
	public void setTagNumber(int tagNumber) {
		tag = new BerTag(this.tag.tagClass, this.tag.primitive, tagNumber);
	}

	@Override
	public int encode(BerByteArrayOutputStream os) throws IOException {
		return encode(os, true);
	}

	@Override
	public int encode(BerByteArrayOutputStream os, boolean withTag) throws IOException {
		if (code != null) {
			for (int i = code.length - 1; i >= 0; i--) {
				os.write(code[i]);
			}
			if (withTag) {
				return tag.encode(os) + code.length;
			}
			return code.length;
		}
		int codeLength = value.encode(os, true);
		codeLength += BerLength.encodeLength(os, codeLength);
		if (withTag) {
			codeLength += tag.encode(os);
		}
		return codeLength;
	}

	@Override
	public int decode(InputStream is) throws IOException {
		return decode(is, true);
	}

	@Override
	@SuppressWarnings("resource")
	public int decode(InputStream is, boolean withTag) throws IOException {
		int codeLength = 0;
		if (withTag) {
			codeLength += tag.decodeAndCheck(is);
		}
		BerLength length = new BerLength();
		codeLength += length.decode(is);
		int len = length.val;
		codeLength += len;
		int r;
		int off = 0;
		byte[] data = new byte[len];
		while (off < len && (r = is.read(data, off, len - off)) != -1) {
			off += r;
		}
		value = new BerInputStream(new ByteArrayInputStream(data)).next();
		return codeLength;
	}

	public void encodeAndSave(int encodingSizeGuess) throws IOException {
		BerByteArrayOutputStream os = new BerByteArrayOutputStream(encodingSizeGuess);
		encode(os, false);
		code = os.getArray();
	}

	@Override
	public String toString() {
		return "" + value;
	}

	@Override
	public BerTag getTag() {
		return tag;
	}
}
