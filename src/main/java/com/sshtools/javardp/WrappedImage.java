/* WrappedImage.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Adds functionality to the BufferedImage class, allowing
 *          manipulation of colour indices, making the RGB values
 *          invisible (in the case of Indexed Colour only).
 */
package com.sshtools.javardp;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.graphics.Display;
import com.sshtools.javardp.graphics.RdesktopCanvas;
import com.sshtools.javardp.graphics.RdpCursor;

public class WrappedImage extends JComponent implements Display, Scrollable {
	static Logger logger = LoggerFactory.getLogger(WrappedImage.class);
	private static final long serialVersionUID = 1L;
	private BufferedImage bi = null;
	private RdesktopCanvas canvas;
	private IndexColorModel cm = null;

	public WrappedImage(int width, int height, int imgType) {
		bi = new BufferedImage(width, height, imgType);
	}

	public WrappedImage(int width, int height, int imgType, IndexColorModel cm) {
		bi = new BufferedImage(width, height, imgType);
		this.cm = cm;
	}

	/**
	 * Force a colour to its true RGB representation (extracting from colour
	 * model if indexed colour)
	 * 
	 * @param color
	 * @return
	 */
	@Override
	public int checkColor(int color) {
		if (cm != null)
			return cm.getRGB(color);
		return color;
	}

	@Override
	public RdpCursor createCursor(String name, Point hotspot, Image data) {
		return new AWTRdpCursor(hotspot, name, data);
	}

	@Override
	public BufferedImage getBufferedImage() {
		return bi;
	}

	@Override
	public Graphics getDisplayGraphics() {
		return bi.getGraphics();
	}

	@Override
	public int getDisplayHeight() {
		return bi.getHeight();
	}

	@Override
	public int getDisplayWidth() {
		return bi.getWidth();
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return this.getPreferredSize();
	}

	@Override
	public Dimension getPreferredSize() {
		return bi == null ? new Dimension(0, 0) : new Dimension(bi.getWidth(), bi.getHeight());
	}

	@Override
	public int getRGB(int x, int y) {
		if (cm == null)
			return bi.getRGB(x, y);
		else {
			int pix = bi.getRGB(x, y) & 0xFFFFFF;
			int[] vals = { (pix >> 16) & 0xFF, (pix >> 8) & 0xFF, (pix) & 0xFF };
			int out = cm.getDataElement(vals, 0);
			if (cm.getRGB(out) != bi.getRGB(x, y))
				logger.info("Did not get correct colour value for color (" + Integer.toHexString(pix) + "), got (" + cm.getRGB(out)
						+ ") instead");
			return out;
		}
	}

	@Override
	public int[] getRGB(int x, int y, int cx, int cy, int[] data, int offset, int width) {
		return bi.getRGB(x, y, cx, cy, data, offset, width);
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return (orientation == SwingConstants.VERTICAL) ? visibleRect.height / 10 : visibleRect.width / 10;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return (orientation == SwingConstants.VERTICAL) ? visibleRect.height / 10 : visibleRect.width / 10;
	}

	@Override
	public BufferedImage getSubimage(int x, int y, int width, int height) {
		return bi.getSubimage(x, y, width, height);
	}

	@Override
	public void init(RdesktopCanvas canvas) {
		this.canvas = canvas;
		enableEvents(AWTEvent.FOCUS_EVENT_MASK | AWTEvent.ACTION_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
				| AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
		enableEvents(AWTEvent.KEY_EVENT_MASK);
		setFocusable(true);
		setFocusTraversalKeysEnabled(false);
	}

	@Override
	public void paintComponent(Graphics g) {
		update(g);
	}

	@Override
	public void resizeDisplay(Dimension dimension) {
		BufferedImage bim = bi;
		bi = new BufferedImage(Math.max(dimension.width, 1), Math.max(dimension.height, 1), bi.getType());
		bi.getGraphics().drawImage(bim, 0, 0, null);
	}

	@Override
	public void setCursor(RdpCursor cursor) {
		setCursor(cursor == null ? null : ((AWTRdpCursor) cursor).getCursor());
	}

	/**
	 * Set the colour model for this Image
	 * 
	 * @param cm Colour model for use with this image
	 */
	@Override
	public void setIndexColorModel(IndexColorModel cm) {
		this.cm = cm;
	}

	@Override
	public void setRGB(int x, int y, int color) {
		if (cm != null)
			color = cm.getRGB(color);
		bi.setRGB(x, y, color);
	}

	@Override
	public void setRGB(int x, int y, int cx, int cy, int[] data, int offset, int w) {
		if (cm != null && data != null && data.length > 0) {
			for (int i = 0; i < data.length; i++)
				data[i] = cm.getRGB(data[i]);
		}
		bi.setRGB(x, y, cx, cy, data, offset, w);
	}

	/**
	 * Apply a given array of colour values to an area of pixels in the image,
	 * do not convert for colour model
	 * 
	 * @param x x-coordinate for left of area to set
	 * @param y y-coordinate for top of area to set
	 * @param cx width of area to set
	 * @param cy height of area to set
	 * @param data array of pixel colour values to apply to area
	 * @param offset offset to pixel data in data
	 * @param w width of a line in data (measured in pixels)
	 */
	@Override
	public void setRGBNoConversion(int x, int y, int cx, int cy, int[] data, int offset, int w) {
		bi.setRGB(x, y, cx, cy, data, offset, w);
	}

	@Override
	public void update(Graphics g) {
		Rectangle r = g.getClipBounds();
		if (logger.isDebugEnabled())
			logger.info("Update " + r + " for " + getDisplayWidth() + "x" + getDisplayHeight());
		int w = r.width;
		int h = r.height;
		if (r.x + w > getDisplayWidth()) {
			w -= r.x + w - getDisplayWidth();
		}
		if (r.y + h > getDisplayHeight()) {
			h -= r.y + h - getDisplayHeight();
		}
		g.drawImage(getSubimage(r.x, r.y, w, h), r.x, r.y, null);
	}

	class AWTRdpCursor extends RdpCursor {
		private Cursor cursor;

		public AWTRdpCursor(Point hotspot, String name, Image data) {
			super(hotspot, name, data);
		}

		Cursor getCursor() {
			if (cursor == null) {
				cursor = Toolkit.getDefaultToolkit().createCustomCursor(getData(), getHotspot(), getName());
			}
			return cursor;
		}
	}
}
