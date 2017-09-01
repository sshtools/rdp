/* PatBltOrder.java
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

public class PatBltOrder extends DestBltOrder {

	private int bgcolor = 0;
	private Brush brush = null;
	private int fgcolor = 0;

	public PatBltOrder() {
		super();
		brush = new Brush();
	}

	public int getBackgroundColor() {
		return this.bgcolor;
	}

	public Brush getBrush() {
		return this.brush;
	}

	public int getForegroundColor() {
		return this.fgcolor;
	}

	@Override
	public void reset() {
		super.reset();
		bgcolor = 0;
		fgcolor = 0;
		brush.reset();
	}

	public void setBackgroundColor(int bgcolor) {
		this.bgcolor = bgcolor;
	}

	public void setForegroundColor(int fgcolor) {
		this.fgcolor = fgcolor;
	}
}
