/* RdpPacket.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Encapsulates data from a single received packet.
 *          Provides methods for reading from and writing to
 *          an individual packet at all relevant levels.
 */
// Created on 03-Sep-2003
package com.sshtools.javardp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Packet {
	public static final int CHANNEL_HEADER = 4;
	/* constants for Packet */
	public static final int MCS_HEADER = 1;
	public static final int RDP_HEADER = 3;
	public static final int SECURE_HEADER = 2;
	static Logger logger = LoggerFactory.getLogger(Packet.class);
	protected int channel = -1;
	protected int end = -1;
	protected int mcs = -1;
	protected int rdp = -1;
	protected int secure = -1;
	protected int start = -1;
	private ByteBuffer bb = null;
	private int size = 0;

	public Packet(int capacity) {
		bb = ByteBuffer.allocateDirect(capacity);
		size = capacity;
	}

	public Packet(byte[] data) {
		bb = ByteBuffer.allocateDirect(data.length);
		size = data.length;
		bb.put(data);
	}

	public byte[] getBytes() {
		byte[] packet = new byte[getEnd()];
		copyToByteArray(packet, 0, 0, packet.length);
		return packet;
	}

	/**
	 * Retrieve capacity of this packet
	 * 
	 * @return Packet capacity (in bytes)
	 */
	public int capacity() {
		return bb.capacity();
	}

	/**
	 * Copy data to this packet from an array of bytes
	 * 
	 * @param array Array of bytes containing source data
	 * @param array_offset Offset into array for start of data
	 * @param mem_offset Offset into packet for start of data
	 * @param len Length of data to be copied
	 */
	public void copyFromByteArray(byte[] array, int array_offset, int mem_offset, int len) {
		if ((array_offset >= array.length) || (array_offset + len > array.length) || (mem_offset + len > bb.capacity())) {
			throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
		}
		// store position
		int oldpos = getPosition();
		setPosition(mem_offset);
		bb.put(array, array_offset, len);
		// restore position
		setPosition(oldpos);
	}

	/**
	 * Copy data to this packet from another packet
	 * 
	 * @param src Source packet
	 * @param srcOffset Offset into source packet for start of data
	 * @param dstOffset Offset into this packet (destination) for start of data
	 * @param len Length of data to be copied
	 */
	public void copyFromPacket(Packet src, int srcOffset, int dstOffset, int len) {
		int oldsrcpos = src.getPosition();
		int oldpos = getPosition();
		src.setPosition(srcOffset);
		setPosition(dstOffset);
		for (int i = 0; i < len; i++)
			bb.put((byte) src.get8());
		src.setPosition(oldsrcpos);
		setPosition(oldpos);
	}

	/**
	 * Copy data from this packet to an array of bytes
	 * 
	 * @param array Array of bytes to which data should be copied
	 * @param array_offset Offset into array for start of data
	 * @param mem_offset Offset into packet for start of data
	 * @param len Length of data to be copied
	 */
	public void copyToByteArray(byte[] array, int array_offset, int mem_offset, int len) {
		if ((array_offset >= array.length))
			throw new ArrayIndexOutOfBoundsException("Array offset beyond end of array!");
		if (array_offset + len > array.length)
			throw new ArrayIndexOutOfBoundsException("Not enough bytes in array to copy!");
		if (mem_offset + len > bb.capacity())
			throw new ArrayIndexOutOfBoundsException("Memory accessed out of Range!");
		int oldpos = getPosition();
		setPosition(mem_offset);
		bb.get(array, array_offset, len);
		setPosition(oldpos);
	}

	/**
	 * Copy data from this packet to another packet
	 * 
	 * @param dst Destination packet
	 * @param srcOffset Offset into this packet (source) for start of data
	 * @param dstOffset Offset into destination packet for start of data
	 * @param len Length of data to be copied
	 */
	public void copyToPacket(Packet dst, int srcOffset, int dstOffset, int len) {
		int olddstpos = dst.getPosition();
		int oldpos = getPosition();
		dst.setPosition(dstOffset);
		setPosition(srcOffset);
		for (int i = 0; i < len; i++)
			dst.set8(bb.get());
		dst.setPosition(olddstpos);
		setPosition(oldpos);
	}

	/**
	 * Read an 8-bit integer value from the packet (at current read/write
	 * position)
	 * 
	 * @return Value read from packet
	 */
	public int get8() {
		if (bb.position() >= bb.capacity()) {
			throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
		}
		return bb.get() & 0xff; // treat as unsigned byte
	}

	/**
	 * Read an 8-bit integer value from a specified offset in the packet
	 * 
	 * @param where Offset to read location
	 * @return Value read from packet
	 */
	public int get8(int where) {
		if (where < 0 || where >= bb.capacity()) {
			throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
		}
		return bb.get(where) & 0xff; // treat as unsigned byte
	}

	/**
	 * Read a 2-byte, big-endian integer value from the packet (at current
	 * read/write position)
	 * 
	 * @return Value read from packet
	 */
	public int getBigEndian16() {
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getShort();
	}

	/**
	 * Read a 2-byte, big-endian integer value from a specified offset in the
	 * packet
	 * 
	 * @param where Offset to read location
	 * @return Value read from packet
	 */
	public int getBigEndian16(int where) {
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getShort(where);
	}

	/**
	 * Read a 3-byte, big-endian integer value from the packet (at current
	 * read/write position)
	 * 
	 * @return Value read from packet
	 */
	public int getBigEndian32() {
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getInt();
	}

	/**
	 * Read a 3-byte, big-endian integer value from a specified offset in the
	 * packet
	 * 
	 * @param where Offset to read location
	 * @return Value read from packet
	 */
	public int getBigEndian32(int where) {
		bb.order(ByteOrder.BIG_ENDIAN);
		return bb.getInt(where);
	}

	/**
	 * Retrieve location of packet end
	 * 
	 * @return Position of packet end (as byte offset from start)
	 */
	public int getEnd() {
		return this.end == -1 ? capacity() : this.end;
	}

	/**
	 * Get location of the header for a specific communications layer
	 * 
	 * @param header ID of header type
	 * @return Location of header, as byte offset from start of packet
	 * @throws RdesktopException on error
	 */
	public int getHeader(int header) throws RdesktopException {
		switch (header) {
		case Packet.MCS_HEADER:
			return this.mcs;
		case Packet.SECURE_HEADER:
			return this.secure;
		case Packet.RDP_HEADER:
			return this.rdp;
		case Packet.CHANNEL_HEADER:
			return this.channel;
		default:
			throw new RdesktopException("Wrong Header!");
		}
	}

	/**
	 * Read a 2-byte, little-endian integer value from the packet (at current
	 * read/write position)
	 * 
	 * @return Value read from packet
	 */
	public int getLittleEndian16() {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getShort();
	}

	/**
	 * Read a 2-byte, little-endian integer value from a specified offset in the
	 * packet
	 * 
	 * @param where Offset to read location
	 * @return Value read from packet
	 */
	public int getLittleEndian16(int where) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getShort(where);
	}

	/**
	 * Read a 8-byte, little-endian integer value from the packet (at current
	 * read position)
	 * 
	 * @return Value read from packet
	 */
	public long getLittleEndian64() {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getLong();
	}

	/**
	 * Read a 4-byte, little-endian integer value from the packet (at current
	 * read position)
	 * 
	 * @return Value read from packet
	 */
	public int getLittleEndian32() {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt();
	}

	/**
	 * Read a 4-byte, little-endian integer value from the packet (at current
	 * read position)
	 * 
	 * @return Value read from packet
	 */
	public long getUnsignedLittleEndian32() {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt() & 0x00000000ffffffffL;
	}

	/**
	 * Read a 4-byte, little-endian integer value from a specified offset in the
	 * packet
	 * 
	 * @param where Offset to read location
	 * @return Value read from packet
	 */
	public int getLittleEndian32(int where) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt(where);
	}

	/**
	 * Read a 8-byte, little-endian integer value from a specified offset in the
	 * packet
	 * 
	 * @param where Offset to read location
	 * @return Value read from packet
	 */
	public long getLittleEndian64(int where) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getLong(where);
	}

	/**
	 * Read a 4-byte, little-endian integer value from a specified offset in the
	 * packet
	 * 
	 * @param where Offset to read location
	 * @return Value read from packet
	 */
	public long getUnsignedLittleEndian32(int where) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt(where) & 0x00000000ffffffffL;
	}

	/**
	 * Retrieve offset to current read/write position
	 * 
	 * @return Current read/write position (as byte offset from start)
	 */
	public int getPosition() {
		return bb.position();
	}

	/**
	 * Retrieve start location of this packet
	 * 
	 * @return Start location of packet (as byte offset from location 0)
	 */
	public int getStart() {
		return this.start;
	}

	/**
	 * Advance the read/write position
	 * 
	 * @param length Number of bytes to advance read position by
	 */
	public void incrementPosition(int length) {
		if (length > bb.capacity() || length + bb.position() > bb.capacity() || length < 0) {
			throw new ArrayIndexOutOfBoundsException();
		}
		bb.position(bb.position() + length);
	}

	/**
	 * Mark current read/write position as end of packet
	 */
	public void markEnd() {
		this.end = getPosition();
	}

	/**
	 * Mark specified position as end of packet
	 * 
	 * @param position New end position (as byte offset from start)
	 */
	public void markEnd(int position) {
		if (position > capacity()) {
			throw new ArrayIndexOutOfBoundsException("Mark > size!");
		}
		this.end = position;
	}

	/**
	 * Write an ASCII string to this packet at current read/write position
	 * 
	 * @param str String to be written
	 * @param length Length in bytes to be occupied by string (may be longer
	 *            than string itself)
	 */
	public void out_uint8p(String str, int length) {
		byte[] bStr = str.getBytes();
		this.copyFromByteArray(bStr, 0, this.getPosition(), bStr.length);
		this.incrementPosition(length);
	}

	/**
	 * Add a unicode string to this packet at the current read/write position
	 * 
	 * @param str String to write as unicode to packet
	 * @param len Desired length of output unicode string
	 */
	public void outUnicodeString(String str, int len) {
		int i = 0, j = 0;
		if (str.length() != 0) {
			char[] name = str.toCharArray();
			while (i < len) {
				this.setLittleEndian16((short) name[j++]);
				i += 2;
			}
			this.setLittleEndian16(0); // Terminating Null Character
		} else {
			this.setLittleEndian16(0);
		}
	}

	/**
	 * Reserve space within this packet for writing of headers for a specific
	 * communications layer. Move read/write position ready for adding data for
	 * a higher communications layer.
	 * 
	 * @param header ID of header type
	 * @param increment Required size to be reserved for header
	 * @throws RdesktopException on error
	 */
	public void pushLayer(int header, int increment) throws RdesktopException {
		this.setHeader(header);
		this.incrementPosition(increment);
		// this.setStart(this.getPosition());
	}

	public void reset(int length) {
		// logger.info("RdpPacket_Localised.reset(" + length + "), capacity = "
		// + bb.capacity());
		this.end = 0;
		this.start = 0;
		if (bb.capacity() < length)
			bb = ByteBuffer.allocateDirect(length);
		size = length;
		bb.clear();
	}

	/**
	 * Write 8-bit value to packet at current read/write position
	 * 
	 * @param what Value to write to packet
	 */
	public void set8(int what) {
		if (bb.position() >= bb.capacity()) {
			throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
		}
		bb.put((byte) what);
	}

	/**
	 * Write 8-bit value to packet at specified offset
	 * 
	 * @param where Offset in packet to write location
	 * @param what Value to write to packet
	 */
	public void set8(int where, int what) {
		if (where < 0 || where >= bb.capacity()) {
			throw new ArrayIndexOutOfBoundsException("memory accessed out of Range!");
		}
		bb.put(where, (byte) what);
	}

	/**
	 * Write a 2-byte, big-endian integer value to packet at current read/write
	 * position
	 * 
	 * @param what Value to write to packet
	 */
	public void setBigEndian16(int what) {
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.putShort((short) what);
	}

	/**
	 * Write a 2-byte, big-endian integer value to packet at specified offset
	 * 
	 * @param where Offset in packet to write location
	 * @param what Value to write to packet
	 */
	public void setBigEndian16(int where, int what) {
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.putShort(where, (short) what);
	}

	/**
	 * Write a 3-byte, big-endian integer value to packet at current read/write
	 * position
	 * 
	 * @param what Value to write to packet
	 */
	public void setBigEndian32(int what) {
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.putInt(what);
	}

	/**
	 * Write a 3-byte, big-endian integer value to packet at specified offset
	 * 
	 * @param where Offset in packet to write location
	 * @param what Value to write to packet
	 */
	public void setBigEndian32(int where, int what) {
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.putInt(where, what);
	}

	/**
	 * Set current read/write position as the start of a layer header
	 * 
	 * @param header ID of header type
	 * @throws RdesktopException on error
	 */
	public void setHeader(int header) throws RdesktopException {
		switch (header) {
		case Packet.MCS_HEADER:
			this.mcs = this.getPosition();
			break;
		case Packet.SECURE_HEADER:
			this.secure = this.getPosition();
			break;
		case Packet.RDP_HEADER:
			this.rdp = this.getPosition();
			break;
		case Packet.CHANNEL_HEADER:
			this.channel = this.getPosition();
			break;
		default:
			throw new RdesktopException("Wrong Header!");
		}
	}

	/**
	 * Write a 2-byte, little-endian integer value to packet at current
	 * read/write position
	 * 
	 * @param what Value to write to packet
	 */
	public void setLittleEndian16(int what) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putShort((short) what);
	}

	/**
	 * Write a 2-byte, little-endian integer value to packet at specified offset
	 * 
	 * @param where Offset in packet to write location
	 * @param what Value to write to packet
	 */
	public void setLittleEndian16(int where, int what) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putShort(where, (short) what);
	}

	/**
	 * Write a 4-byte, little-endian integer value to packet at current
	 * read/write position
	 * 
	 * @param what Value to write to packet
	 */
	public void setLittleEndian32(int what) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(what);
	}

	/**
	 * Write a 8-byte, little-endian integer value to packet at current
	 * read/write position
	 * 
	 * @param what Value to write to packet
	 */
	public void setLittleEndian64(long what) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putLong(what);
	}

	/**
	 * Write a 4-byte, little-endian integer value to packet at specified offset
	 * 
	 * @param where Offset in packet to write location
	 * @param what Value to write to packet
	 */
	public void setLittleEndian32(int where, int what) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(where, what);
	}

	/**
	 * Write a 8-byte, little-endian integer value to packet at specified offset
	 * 
	 * @param where Offset in packet to write location
	 * @param what Value to write to packet
	 */
	public void setLittleEndian64(int where, long what) {
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putLong(where, what);
	}

	/**
	 * Set current read/write position
	 * 
	 * @param position New read/write position (as byte offset from start)
	 * @return packet
	 */
	public Packet setPosition(int position) {
		if (position > bb.capacity() || position < 0) {
			throw new ArrayIndexOutOfBoundsException(
					String.format("Set position to %d failed in a capacity of %d", position, bb.capacity()));
		}
		bb.position(position);
		return this;
	}

	/**
	 * Set start position of this packet
	 * 
	 * @param position New start position (as byte offset from location 0)
	 */
	public void setStart(int position) {
		this.start = position;
	}

	/**
	 * Retrieve size of this packet
	 * 
	 * @return Packet size
	 */
	public int size() {
		return size;
		// return bb.capacity(); //this.end - this.start;
	}

	/**
	 * Fill a number of bytes with zero
	 * 
	 * @param len bytes to fill
	 */
	public void fill(int len) {
		fill(len, 0);
	}

	/**
	 * Fill a number of bytes with a value
	 * 
	 * @param len bytes to fill
	 * @param val byte value
	 */
	public void fill(int len, int val) {
		for (int i = 0; i < len; i++) {
			bb.put((byte) val);
		}
	}

	/**
	 * Write the data from another packet.
	 * 
	 * @param packet packet
	 * @return length
	 */
	public int setPacket(Packet packet) {
		byte[] d = packet.getBytes();
		copyFromByteArray(d, 0, bb.position(), d.length);
		incrementPosition(d.length);
		return d.length;
	}

	@Override
	public String toString() {
		return HexDump.dump(getBytes(), "Packet" + hashCode());
	}

	public int setArray(byte[] data) {
		copyFromByteArray(data, 0, bb.position(), data.length);
		incrementPosition(data.length);
		return data.length;
	}
}
