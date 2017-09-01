/* OrderState.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Storage of current order state, which may consist of one of each of a number of
 *          order types.
 */
package com.sshtools.javardp;

import com.sshtools.javardp.orders.BoundsOrder;
import com.sshtools.javardp.orders.DeskSaveOrder;
import com.sshtools.javardp.orders.DestBltOrder;
import com.sshtools.javardp.orders.LineOrder;
import com.sshtools.javardp.orders.MemBltOrder;
import com.sshtools.javardp.orders.PatBltOrder;
import com.sshtools.javardp.orders.PolyLineOrder;
import com.sshtools.javardp.orders.RectangleOrder;
import com.sshtools.javardp.orders.ScreenBltOrder;
import com.sshtools.javardp.orders.Text2Order;
import com.sshtools.javardp.orders.TriBltOrder;

class OrderState {
	private BoundsOrder bounds = null;
	private DeskSaveOrder desksave = null;

	private DestBltOrder destblt = null;
	private LineOrder line = null;
	private MemBltOrder memblt = null;
	private int order_type = 0;
	private PatBltOrder patblt = null;
	private PolyLineOrder polyline = null;
	private RectangleOrder rect = null;
	private ScreenBltOrder screenblt = null;
	private Text2Order text2 = null;
	private TriBltOrder triblt = null;

	/**
	 * Initialise this OrderState object, initialise one of each type of order
	 */
	public OrderState() {
		bounds = new BoundsOrder();
		destblt = new DestBltOrder();
		patblt = new PatBltOrder();
		screenblt = new ScreenBltOrder();
		line = new LineOrder();
		rect = new RectangleOrder();
		desksave = new DeskSaveOrder();
		memblt = new MemBltOrder();
		triblt = new TriBltOrder();
		polyline = new PolyLineOrder();
		text2 = new Text2Order();
	}

	/**
	 * Retrieve the bounds order stored within this state
	 * 
	 * @return BoundsOrder from this state
	 */
	public BoundsOrder getBounds() {
		return this.bounds;
	}

	/**
	 * Retrieve the desktop save order stored within this state
	 * 
	 * @return DeskSaveOrder from this state
	 */
	public DeskSaveOrder getDeskSave() {
		return this.desksave;
	}

	/**
	 * Retrieve the dest blt order stored within this state
	 * 
	 * @return DestBltOrder from this state
	 */
	public DestBltOrder getDestBlt() {
		return this.destblt;
	}

	/**
	 * Retrieve the line order stored within this state
	 * 
	 * @return LineOrder from this state
	 */
	public LineOrder getLine() {
		return this.line;
	}

	/**
	 * Retrieve the memory blit order stored within this state
	 * 
	 * @return MemBltOrder from this state
	 */
	public MemBltOrder getMemBlt() {
		return this.memblt;
	}

	/**
	 * Get the id of the current order type
	 * 
	 * @return Order type id
	 */
	public int getOrderType() {
		return this.order_type;
	}

	/**
	 * Retrieve the pattern blit order stored within this state
	 * 
	 * @return PatBltOrder from this state
	 */
	public PatBltOrder getPatBlt() {
		return this.patblt;
	}

	/**
	 * Retrieve the multi-point line order stored within this state
	 * 
	 * @return PolyLineOrder from this state
	 */
	public PolyLineOrder getPolyLine() {
		return this.polyline;
	}

	/**
	 * Retrieve the rectangle order stored within this state
	 * 
	 * @return RectangleOrder from this state
	 */
	public RectangleOrder getRectangle() {
		return this.rect;
	}

	/**
	 * Retrieve the screen blit order stored within this state
	 * 
	 * @return ScreenBltOrder from this state
	 */
	public ScreenBltOrder getScreenBlt() {
		return this.screenblt;
	}

	/**
	 * Retrieve the text2 order stored within this state
	 * 
	 * @return Text2Order from this state
	 */
	public Text2Order getText2() {
		return this.text2;
	}

	/**
	 * Retrieve the tri blit order stored within this state
	 * 
	 * @return TriBltOrder from this state
	 */
	public TriBltOrder getTriBlt() {
		return this.triblt;
	}

	/**
	 * Reset all orders within this order state
	 */
	public void reset() {
		bounds.reset();
		destblt.reset();
		patblt.reset();
		screenblt.reset();
		line.reset();
		rect.reset();
		desksave.reset();
		memblt.reset();
		triblt.reset();
		polyline.reset();
		text2.reset();
	}

	/**
	 * Set the id of the current order type
	 * 
	 * @param order_type Type id to set for current order
	 */
	public void setOrderType(int order_type) {
		this.order_type = order_type;
	}
}
