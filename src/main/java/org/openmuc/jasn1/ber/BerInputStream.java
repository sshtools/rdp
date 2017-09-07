package org.openmuc.jasn1.ber;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openmuc.jasn1.ber.types.BerBitString;
import org.openmuc.jasn1.ber.types.BerBoolean;
import org.openmuc.jasn1.ber.types.BerContextSpecific;
import org.openmuc.jasn1.ber.types.BerDate;
import org.openmuc.jasn1.ber.types.BerDateTime;
import org.openmuc.jasn1.ber.types.BerDuration;
import org.openmuc.jasn1.ber.types.BerEmbeddedPdv;
import org.openmuc.jasn1.ber.types.BerEnum;
import org.openmuc.jasn1.ber.types.BerGeneralizedTime;
import org.openmuc.jasn1.ber.types.BerInteger;
import org.openmuc.jasn1.ber.types.BerNull;
import org.openmuc.jasn1.ber.types.BerObjectIdentifier;
import org.openmuc.jasn1.ber.types.BerOctetString;
import org.openmuc.jasn1.ber.types.BerReal;
import org.openmuc.jasn1.ber.types.BerSequence;
import org.openmuc.jasn1.ber.types.BerTime;
import org.openmuc.jasn1.ber.types.BerTimeOfDay;
import org.openmuc.jasn1.ber.types.BerType;
import org.openmuc.jasn1.ber.types.BerUtcTime;

public class BerInputStream extends FilterInputStream {
	private boolean eof;

	public BerInputStream(InputStream in) {
		super(in);
	}

	@SuppressWarnings("unchecked")
	public <T extends BerType> T next(Class<T> typeClass) throws IOException {
		BerType t = next();
		if (typeClass.isAssignableFrom(t.getClass()))
			return (T) t;
		throw new IllegalArgumentException("Unexpected type");
	}

	public BerType next() throws IOException {
		BerTag tag = new BerTag();
		try {
			tag.decode(this);
		} catch (EOFException eofe) {
			if (eof)
				throw eofe;
			eof = true;
			return null;
		}
		BerType type = null;
		switch (tag.tagClass) {
		case BerTag.CONTEXT_CLASS:
			type = new BerContextSpecific();
			type.getTag().tagNumber = tag.tagNumber;
			type.decode(this, false);
			break;
		case BerTag.UNIVERSAL_CLASS:
			switch (tag.tagNumber) {
			case BerTag.BIT_STRING_TAG:
				type = new BerBitString();
				type.decode(this, false);
				break;
			case BerTag.BOOLEAN_TAG:
				type = new BerBoolean();
				type.decode(this, false);
				break;
			case BerTag.DATE_TAG:
				type = new BerDate();
				type.decode(this, false);
				break;
			case BerTag.DATE_TIME_TAG:
				type = new BerDateTime();
				type.decode(this, false);
				break;
			case BerTag.DURATION_TAG:
				type = new BerDuration();
				type.decode(this, false);
				break;
			case BerTag.EMBEDDED_PDV_TAG:
				type = new BerEmbeddedPdv();
				type.decode(this, false);
				break;
			case BerTag.ENUMERATED_TAG:
				type = new BerEnum();
				type.decode(this, false);
				break;
			case BerTag.GENERALIZED_TIME_TAG:
				type = new BerGeneralizedTime();
				type.decode(this, false);
				break;
			case BerTag.INTEGER_TAG:
				type = new BerInteger();
				type.decode(this, false);
				break;
			case BerTag.NULL_TAG:
				type = new BerNull();
				type.decode(this, false);
				break;
			case BerTag.OBJECT_IDENTIFIER_TAG:
				type = new BerObjectIdentifier();
				type.decode(this, false);
				break;
			case BerTag.OCTET_STRING_TAG:
				type = new BerOctetString();
				type.decode(this, false);
				break;
			case BerTag.REAL_TAG:
				type = new BerReal();
				type.decode(this, false);
				break;
			case BerTag.SEQUENCE_TAG:
				type = new BerSequence();
				type.decode(this, false);
				break;
			case BerTag.TIME_TAG:
				type = new BerTime();
				type.decode(this, false);
				break;
			case BerTag.TIME_OF_DAY_TAG:
				type = new BerTimeOfDay();
				type.decode(this, false);
				break;
			case BerTag.UTC_TIME_TAG:
				type = new BerUtcTime();
				type.decode(this, false);
				break;
			default:
				throw new UnsupportedOperationException(String.format("Unknown tag %d (%x)", tag.tagNumber, tag.tagNumber));
			}
			break;
		default:
			throw new UnsupportedOperationException(String.format("Unknown tag %d (%x)", tag.tagClass, tag.tagClass));
		}
		return type;
	}
}
