package org.openmuc.jasn1.ber.types;

import java.io.IOException;
import java.io.InputStream;

import org.openmuc.jasn1.ber.BerByteArrayOutputStream;
import org.openmuc.jasn1.ber.BerTag;

public interface BerType {

    int encode(BerByteArrayOutputStream os) throws IOException;

    int encode(BerByteArrayOutputStream os, boolean withTag) throws IOException;
    
    int decode(InputStream is) throws IOException;

    int decode(InputStream is, boolean withTag) throws IOException;
    
    BerTag getTag();
}
