package com.sshtools.javardp.graphics;

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
import java.awt.image.IndexColorModel;

public interface Display {

	void addKeyListener(KeyListener keyListener);

	void addMouseListener(MouseListener mouseListener);

	void addMouseMotionListener(MouseMotionListener mouseMotionListener);

	void addMouseWheelListener(MouseWheelListener mouseWheelListener);

	/**
	 * Force a colour to its true RGB representation (extracting from colour
	 * model if indexed colour)
	 * 
	 * @param color color
	 * @return color
	 */
	int checkColor(int color);

	RdpCursor createCursor(String name, Point hotspot, Image data);

	Rectangle getBounds();

	BufferedImage getBufferedImage();

	Graphics getDisplayGraphics();

	int getDisplayHeight();

	int getDisplayWidth();

	Point getLocationOnScreen();

	int getRGB(int x, int y);

	int[] getRGB(int x, int y, int cx, int cy, int[] data, int offset, int width);

	BufferedImage getSubimage(int x, int y, int width, int height);
	
	void init(RdesktopCanvas canvas);

	void removeKeyListener(KeyListener keyListener);

	void removeMouseListener(MouseListener mouseListener);

	void removeMouseMotionListener(MouseMotionListener mouseMotionListener);

	void removeMouseWheelListener(MouseWheelListener mouseWheelListener);

	void repaint();

	void repaint(int x, int y, int cx, int cy);

	void resizeDisplay(Dimension dimension);

	void setCursor(RdpCursor cursor);

	/**
	 * Set the colour model for this Image
	 * 
	 * @param cm
	 *            Colour model for use with this image
	 */
	void setIndexColorModel(IndexColorModel cm);

	void setRGB(int x, int y, int color);

	void setRGB(int x, int y, int cx, int cy, int[] data, int offset, int w);

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

	boolean getLockingKeyState(int vk);

}