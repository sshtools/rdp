/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jasn1.ber.types;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.openmuc.jasn1.ber.BerByteArrayOutputStream;
import org.openmuc.jasn1.ber.BerInputStream;
import org.openmuc.jasn1.ber.BerLength;
import org.openmuc.jasn1.ber.BerTag;

public class BerSequence implements Serializable, BerType, Iterable<BerType> {
	private static final long serialVersionUID = 1L;
	public final static BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.CONSTRUCTED, BerTag.SEQUENCE_TAG);
	public byte[] code = null;
	public List<BerType> value = new ArrayList<>();

	public BerSequence() {
	}

	public BerSequence(byte[] code) {
		this.code = code;
	}

	public BerSequence(BerType... value) {
		this(new ArrayList<BerType>(Arrays.asList(value)));
	}

	public BerSequence(List<BerType> value) {
		this.value = value;
	}

	@SuppressWarnings("unchecked")
	public <T extends BerType> T get(Class<T> typeClass, int tagNumber) {
		int i = 0;
		for (BerType t : value) {
			if(!(t instanceof BerContextSpecific) && i == tagNumber)
				return (T)t;
			if (t instanceof BerContextSpecific && t.getTag().tagNumber == tagNumber)
				return (T) ((BerContextSpecific) t).value;
			i++;
		}
		return null;
	}

	public BerSequence add(BerType type) {
		this.value.add(type);
		return this;
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
		byte[] data = getData();
		int codeLength = data.length;
		os.write(data);
		codeLength += BerLength.encodeLength(os, codeLength);
		if (withTag) {
			codeLength += tag.encode(os);
		}
		return codeLength;
	}

	public byte[] getData() throws IOException {
		BerByteArrayOutputStream bb = new BerByteArrayOutputStream(1, true);
		for (int i = value.size() - 1; i >= 0; i--) {
			value.get(i).encode(bb, true);
		}
		return bb.getArray();
	}

	@Override
	public int decode(InputStream is) throws IOException {
		return decode(is, true);
	}

	@Override
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
		@SuppressWarnings("resource")
		BerInputStream bin = new BerInputStream(new ByteArrayInputStream(data));
		off = 0;
		value = new ArrayList<>();
		BerType t = null;
		do {
			t = bin.next();
			if (t != null)
				value.add(t);
		} while (t != null);
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
	public Iterator<BerType> iterator() {
		return value.iterator();
	}

	@Override
	public BerTag getTag() {
		return tag;
	}
}
