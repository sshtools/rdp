package com.sshtools.javardp.layers.nla;

import com.sshtools.javardp.Packet;

public class NTLMPacket extends Packet {
	public NTLMPacket(byte[] data) {
		super(data);
	}

	public NTLMPacket(int capacity) {
		super(capacity);
	}

	public int setOffsetArray(int offset, byte[] data) {
        int length = (data != null) ? data.length : 0;
//        if (length == 0) return;
        setLittleEndian16(length);
        setLittleEndian16(length);
        setLittleEndian32(offset);
        if(length > 0)
        	copyFromByteArray(data, 0, offset, length);
        return data.length;
    }

	public byte[] getOffsetArray() {
		int len = getLittleEndian16();
		getLittleEndian16(); // TODO why?
		int off = getLittleEndian32();
		byte[] b = new byte[len];
		copyToByteArray(b, 0, off, b.length);
		return b;
	}
}
