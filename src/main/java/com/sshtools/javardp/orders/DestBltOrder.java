/* DestBltOrder.java
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

public class DestBltOrder implements Order {

	private int cx = 0;
	private int cy = 0;
	private int opcode = 0;
	private int x = 0;
	private int y = 0;

	public DestBltOrder() {
	}

	public int getCX() {
		return this.cx;
	}

	public int getCY() {
		return this.cy;
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
		cx = 0;
		cy = 0;
		opcode = 0;
	}

	public void setCX(int cx) {
		this.cx = cx;
	}

	public void setCY(int cy) {
		this.cy = cy;
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
