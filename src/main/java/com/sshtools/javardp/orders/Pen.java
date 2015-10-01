/* Pen.java
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

public class Pen {

	private int style = 0;
	private int width = 0;
	private int color = 0;

	public Pen() {
	}

	public int getStyle() {
		return this.style;
	}

	public int getWidth() {
		return this.width;
	}

	public int getColor() {
		return this.color;
	}

	public void setStyle(int style) {
		this.style = style;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public void reset() {
		style = 0;
		width = 0;
		color = 0;
	}
}
