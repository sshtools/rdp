package com.sshtools.javardp;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.ImageProducer;
import java.awt.image.IndexColorModel;

public interface Display {

	void init(RdesktopCanvas canvas);

	int getDisplayWidth();

	int getDisplayHeight();

	BufferedImage getBufferedImage();

	Graphics getDisplayGraphics();

	BufferedImage getSubimage(int x, int y, int width, int height);

	/**
	 * Force a colour to its true RGB representation (extracting from colour
	 * model if indexed colour)
	 * 
	 * @param color
	 * @return
	 */
	int checkColor(int color);

	/**
	 * Set the colour model for this Image
	 * 
	 * @param cm
	 *            Colour model for use with this image
	 */
	void setIndexColorModel(IndexColorModel cm);

	void setRGB(int x, int y, int color);

	/**
	 * Apply a given array of colour values to an area of pixels in the image,
	 * do not convert for colour model
	 * 
	 * @param x
	 *            x-coordinate for left of area to set
	 * @param y
	 *            y-coordinate for top of area to set
	 * @param cx
	 *            width of area to set
	 * @param cy
	 *            height of area to set
	 * @param data
	 *            array of pixel colour values to apply to area
	 * @param offset
	 *            offset to pixel data in data
	 * @param w
	 *            width of a line in data (measured in pixels)
	 */
	void setRGBNoConversion(int x, int y, int cx, int cy, int[] data, int offset, int w);

	void setRGB(int x, int y, int cx, int cy, int[] data, int offset, int w);

	int[] getRGB(int x, int y, int cx, int cy, int[] data, int offset, int width);

	int getRGB(int x, int y);

	void resizeDisplay(Dimension dimension);

	void repaint(int x, int y, int cx, int cy);
	
	RdpCursor createCursor(String name, Point hotspot, Image data);

	void setCursor(RdpCursor cursor);

	Rectangle getBounds();

	Point getLocationOnScreen();

	void addMouseListener(MouseListener mouseListener);

	void removeMouseListener(MouseListener mouseListener);

	void addMouseMotionListener(MouseMotionListener mouseMotionListener);

	void removeMouseMotionListener(MouseMotionListener mouseMotionListener);

	void addMouseWheelListener(MouseWheelListener mouseWheelListener);

	void removeMouseWheelListener(MouseWheelListener mouseWheelListener);

	void addKeyListener(KeyListener keyListener);

	void removeKeyListener(KeyListener keyListener);

	void repaint();

}