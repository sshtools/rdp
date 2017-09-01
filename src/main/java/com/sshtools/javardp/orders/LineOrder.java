/* LineOrder.java
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

public class LineOrder implements Order {

	Pen pen = null;
	private int bgcolor = 0;
	private int endx = 0;
	private int endy = 0;
	private int mixmode = 0;
	private int opcode = 0;
	private int startx = 0;
	private int starty = 0;

	public LineOrder() {
		pen = new Pen();
	}

	public int getBackgroundColor() {
		return this.bgcolor;
	}

	public int getEndX() {
		return this.endx;
	}

	public int getEndY() {
		return this.endy;
	}

	public int getMixmode() {
		return this.mixmode;
	}

	public int getOpcode() {
		return this.opcode;
	}

	public Pen getPen() {
		return this.pen;
	}

	public int getStartX() {
		return this.startx;
	}

	public int getStartY() {
		return this.starty;
	}

	public void reset() {
		mixmode = 0;
		startx = 0;
		starty = 0;
		endx = 0;
		endy = 0;
		bgcolor = 0;
		opcode = 0;
		pen.reset();
	}

	public void setBackgroundColor(int bgcolor) {
		this.bgcolor = bgcolor;
	}

	public void setEndX(int endx) {
		this.endx = endx;
	}

	public void setEndY(int endy) {
		this.endy = endy;
	}

	public void setMixmode(int mixmode) {
		this.mixmode = mixmode;
	}

	public void setOpcode(int opcode) {
		this.opcode = opcode;
	}

	public void setStartX(int startx) {
		this.startx = startx;
	}

	public void setStartY(int starty) {
		this.starty = starty;
	}
}
