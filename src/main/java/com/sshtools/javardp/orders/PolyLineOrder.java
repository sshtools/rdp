/* PolyLineOrder.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:40 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: 
 */
package com.sshtools.javardp.orders;

public class PolyLineOrder implements Order {

	byte[] data = new byte[256];
	private int datasize = 0;
	private int fgcolor = 0;
	private int flags = 0;
	private int lines = 0;
	private int opcode = 0;
	private int x = 0;
	private int y = 0;

	public PolyLineOrder() {
	}

	public byte[] getData() {
		return this.data;
	}

	public int getDataSize() {
		return this.datasize;
	}

	public int getFlags() {
		return this.flags;
	}

	public int getForegroundColor() {
		return this.fgcolor;
	}

	public int getLines() {
		return this.lines;
	}

	public int getOpcode() {
		return this.opcode;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public void reset() {
		x = 0;
		y = 0;
		flags = 0;
		fgcolor = 0;
		lines = 0;
		datasize = 0;
		opcode = 0;
		data = new byte[256];
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public void setDataSize(int datasize) {
		this.datasize = datasize;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public void setForegroundColor(int fgcolor) {
		this.fgcolor = fgcolor;
	}

	public void setLines(int lines) {
		this.lines = lines;
	}

	public void setOpcode(int opcode) {
		this.opcode = opcode;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}
}
