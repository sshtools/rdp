/* TriBltOrder.java
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

public class TriBltOrder extends PatBltOrder {

	private int cache_id = 0;
	private int cache_idx = 0;
	private int color_table = 0;
	private int srcx = 0;
	private int srcy = 0;
	private int unknown = 0;

	public TriBltOrder() {
		super();
	}

	public int getCacheID() {
		return this.cache_id;
	}

	public int getCacheIDX() {
		return this.cache_idx;
	}

	public int getColorTable() {
		return this.color_table;
	}

	public int getSrcX() {
		return this.srcx;
	}

	public int getSrcY() {
		return this.srcy;
	}

	public int getUnknown() {
		return this.unknown;
	}

	@Override
	public void reset() {
		super.reset();
		color_table = 0;
		cache_id = 0;
		cache_idx = 0;
		srcx = 0;
		srcy = 0;
		unknown = 0;
	}

	public void setCacheID(int cache_id) {
		this.cache_id = cache_id;
	}

	public void setCacheIDX(int cache_idx) {
		this.cache_idx = cache_idx;
	}

	public void setColorTable(int color_table) {
		this.color_table = color_table;
	}

	public void setSrcX(int srcx) {
		this.srcx = srcx; // corrected
	}

	public void setSrcY(int srcy) {
		this.srcy = srcy; // corrected
	}

	public void setUnknown(int unknown) {
		this.unknown = unknown;
	}
}
