/* Brush.java
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

public class Brush {

	private byte[] pattern = new byte[8];
	private int style = 0;
	private int xorigin = 0;
	private int yorigin = 0;

	public Brush() {
	}

	public byte[] getPattern() {
		return this.pattern;
	}

	public int getStyle() {
		return this.style;
	}

	public int getXOrigin() {
		return this.xorigin;
	}

	public int getYOrigin() {
		return this.yorigin;
	}

	public void reset() {
		xorigin = 0;
		yorigin = 0;
		style = 0;
		pattern = new byte[8];
	}

	public void setPattern(byte[] pattern) {
		this.pattern = pattern;
	}

	public void setStyle(int style) {
		this.style = style;
	}

	public void setXOrigin(int xorigin) {
		this.xorigin = xorigin;
	}

	public void setYOrigin(int yorigin) {
		this.yorigin = yorigin;
	}
}
