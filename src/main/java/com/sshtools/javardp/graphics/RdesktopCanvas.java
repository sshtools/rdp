/*
 * RdesktopCanvas.java Component: ProperJavaRDP Revision: $Revision: 1.1 $
 * Author: $Author: brett $ Date: $Date: 2011/11/28 14:13:42 $ Copyright (c)
 * 2005 Propero Limited Purpose: Canvas component, handles drawing requests from
 * server, and passes user input to Input class.
 */
package com.sshtools.javardp.graphics;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.Cache;
import com.sshtools.javardp.IContext;
import com.sshtools.javardp.Input;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.Rdp;
import com.sshtools.javardp.RdpPacket;
import com.sshtools.javardp.State;
import com.sshtools.javardp.WrappedImage;
import com.sshtools.javardp.keymapping.KeyCode;
import com.sshtools.javardp.keymapping.KeyCode_FileBased;
import com.sshtools.javardp.orders.BoundsOrder;
import com.sshtools.javardp.orders.Brush;
import com.sshtools.javardp.orders.DestBltOrder;
import com.sshtools.javardp.orders.LineOrder;
import com.sshtools.javardp.orders.MemBltOrder;
import com.sshtools.javardp.orders.PatBltOrder;
import com.sshtools.javardp.orders.PolyLineOrder;
import com.sshtools.javardp.orders.RectangleOrder;
import com.sshtools.javardp.orders.ScreenBltOrder;
import com.sshtools.javardp.orders.TriBltOrder;

// import org.apache.log4j.NDC;
public class RdesktopCanvas {
	public static final int ROP2_COPY = 0xc;
	static Logger logger = LoggerFactory.getLogger(RdesktopCanvas.class);
	private static final int MIX_OPAQUE = 1;
	private static final int MIX_TRANSPARENT = 0;
	private static final int ROP2_AND = 0x8;
	private static final int ROP2_NXOR = 0x9;
	private static final int ROP2_OR = 0xe;
	private static final int ROP2_XOR = 0x6;
	private static final int TEXT2_IMPLICIT_X = 0x20;
	private static final int TEXT2_VERTICAL = 0x04;
	public KeyCode_FileBased fbKeys = null;
	public KeyCode keys = null;
	public Rdp rdp = null;
	public String sKeys = null;
	// private int[] colors = null; // needed for integer backstore
	protected IndexColorModel colormap = null;
	Display backstore;
	private int bottom = 0;
	private Cache cache = null;
	private IContext context;
	private int height;
	// unsetBusyCursor
	private Input input = null;
	private int left = 0;
	// Graphics backstore_graphics;
	private Cursor previous_cursor = null; // for setBusyCursor and
	private int right = 0;
	private Robot robot = null;
	private RasterOp rop = null;
	private State state;
	// protected int[] backstore_int = null;
	// Clip region
	private int top = 0;

	private int width;

	/**
	 * Initialise this canvas to specified width and height, also initialise
	 * backstore
	 * 
	 * @param width Desired width of canvas
	 * @param height Desired height of canvas
	 */
	public RdesktopCanvas(IContext context, State state) {
		this(context, state, new WrappedImage(state.getWidth(), state.getHeight(), BufferedImage.TYPE_INT_RGB));
	}

	/**
	 * Initialise this canvas to specified width and height, also initialise
	 * backstore
	 * 
	 * @param width Desired width of canvas
	 * @param height Desired height of canvas
	 */
	public RdesktopCanvas(IContext context, State state, Display backstore) {
		super();
		this.context = context;
		this.state = state;
		rop = new RasterOp(state);
		width = state.getWidth();
		height = state.getHeight();
		this.right = width - 1; // changed
		this.bottom = height - 1; // changed
		// Component should return it's dimensions to LayoutManager by itself
		// via getMinimumSize() and getPreferredSize(). Calling setSize() from
		// constructor is considered as a wrong praktice.
		// setSize(width, height);
		this.backstore = backstore;
		// now do input listeners in registerCommLayer() / registerKeyboard()
		backstore.init(this);
		context.init(this);
	}

	public void backingStoreResize(int width, int height, boolean clientInitiated) {
		this.width = width;
		this.height = height;
		backstore.resizeDisplay(new Dimension(width, height));
		context.screenResized(width, height, clientInitiated);
	}

	public RdpCursor createCursor(int nXDst, int nYDst, int nWidth, int nHeight, byte[] andMask, byte[] xorMask, int cache_idx,
			int xorBpp, boolean vFlip) {
		int x, y;
		int xorStep;
		int andStep;
		int xorBit;
		int andBit;
		int xorPixel;
		int andPixel;
		int dstIdx = 0;
		BufferedImage bim;
		andStep = (nWidth + 7) / 8;
		andStep += (andStep % 2);
		if (xorMask == null || (xorMask.length == 0))
			return null;
		switch (xorBpp) {
		case 1:
			if (andMask == null || andMask.length == 0)
				return null;
			xorStep = (nWidth + 7) / 8;
			xorStep += (xorStep % 2);
			if (xorStep * nHeight > xorMask.length)
				return null;
			if (andStep * nHeight > andMask.length)
				return null;
			bim = new BufferedImage(nWidth, nHeight, BufferedImage.TYPE_BYTE_INDEXED, new IndexColorModel(2, 3,
					new byte[] { 0, (byte) (255), 0 }, new byte[] { 0, (byte) 255, 0 }, new byte[] { 0, (byte) 255, 0 }, 2));
			for (y = 0; y < nHeight; y++) {
				byte[] andBits = new byte[andStep];
				byte[] xorBits = new byte[xorStep];
				xorBit = andBit = 0x80;
				if (!vFlip) {
					System.arraycopy(xorMask, xorStep * y, xorBits, 0, xorBits.length);
					System.arraycopy(andMask, andStep * y, andBits, 0, andBits.length);
				} else {
					System.arraycopy(xorMask, xorStep * (nHeight - y - 1), xorBits, 0, xorBits.length);
					System.arraycopy(andMask, andStep * (nHeight - y - 1), andBits, 0, andBits.length);
				}
				int xorBitIdx = 0;
				int andBitIdx = 0;
				for (x = 0; x < nWidth; x++) {
					int color = 0;
					xorPixel = (xorBits[xorBitIdx] & xorBit) != 0 ? 1 : 0;
					if ((xorBit >>= 1) == 0) {
						xorBitIdx++;
						xorBit = 0x80;
					}
					andPixel = (andBits[andBitIdx] & andBit) != 0 ? 1 : 0;
					if ((andBit >>= 1) == 0) {
						andBitIdx++;
						andBit = 0x80;
					}
					if (andPixel == 0 && xorPixel == 0)
						color = 0; /* black */
					else if (andPixel == 0 && xorPixel != 0)
						color = 1; /* white */
					else if (andPixel != 0 && xorPixel == 0)
						color = 2; /* transparent */
					else if (andPixel != 0 && xorPixel != 0)
						color = getInvertedColor(x, y) & 0x01; /* inverted */
					bim.getRaster().getDataBuffer().setElem(dstIdx++, color);
				}
			}
			break;
		case 8:
		case 16:
		case 24:
		case 32:
			int xorBytesPerPixel = xorBpp >> 3;
			int elemIdx = 0;
			xorStep = nWidth * xorBytesPerPixel;
			if (xorStep * nHeight > xorMask.length)
				return null;
			if (andMask != null) {
				if (andStep * nHeight > andMask.length)
					return null;
			}
			if (xorBpp == 32) {
				bim = new BufferedImage(nWidth, nHeight, BufferedImage.TYPE_4BYTE_ABGR);
			} else if (xorBpp == 16) {
				bim = new BufferedImage(nWidth, nHeight, BufferedImage.TYPE_USHORT_555_RGB);
			} else if (xorBpp == 8) {
				bim = new BufferedImage(nWidth, nHeight, BufferedImage.TYPE_BYTE_INDEXED, colormap);
			} else {
				bim = new BufferedImage(nWidth, nHeight, BufferedImage.TYPE_3BYTE_BGR);
			}
			for (y = 0; y < nHeight; y++) {
				int xorBitsIdx = 0;
				int andBitsIdx = 0;
				byte[] xorBits = new byte[xorStep];
				byte[] andBits = new byte[andStep];
				andBit = 0x80;
				if (!vFlip) {
					if (andMask != null)
						System.arraycopy(andMask, andStep * y, andBits, 0, andBits.length);
					System.arraycopy(xorMask, xorStep * y, xorBits, 0, xorBits.length);
				} else {
					if (andMask != null)
						System.arraycopy(andMask, andStep * (nHeight - y - 1), andBits, 0, andBits.length);
					System.arraycopy(xorMask, xorStep * (nHeight - y - 1), xorBits, 0, xorBits.length);
				}
				for (x = 0; x < nWidth; x++) {
					if (xorBpp == 32) {
						// TODO
						xorPixel = bim.getRGB(x, y);
					} else if (xorBpp == 16) {
						// TODO
						xorPixel = bim.getRGB(x, y);
					} else if (xorBpp == 8) {
						xorPixel = colormap.getRGB(xorBits[xorBitsIdx]);
					} else {
						int b1 = (xorBits[xorBitsIdx] << 16) & 0x00ffffff;
						int b2 = (xorBits[xorBitsIdx + 1] << 8) & 0x00ffffff;
						int b3 = xorBits[xorBitsIdx + 2] & 0x000000ff;
						int val = b1 | b2 | b3;
						bim.setRGB(x, y, val);
						xorPixel = bim.getRGB(x, y) & 0x00ffffff;
					}
					xorBitsIdx += xorBytesPerPixel;
					andPixel = 0;
					if (andMask != null) {
						andPixel = (andBits[andBitsIdx] & andBit) != 0 ? 1 : 0;
						if ((andBit >>= 1) == 0) {
							andBitsIdx++;
							andBit = 0x80;
						}
					}
					if (andPixel != 0) {
						if (xorPixel == 0xFF000000) /* black -> transparent */
							xorPixel = 0x00000000;
						else if (xorPixel == 0xFFFFFFFF) /* white -> inverted */
							xorPixel = getInvertedColor(x, y);
					}
					bim.setRGB(x, y, xorPixel);
				}
			}
			break;
		default:
			logger.error(String.format("Unknown cursor bpp %d", xorBpp));
			return null;
		}
		return createCustomCursor(bim, new Point(nXDst, nYDst), "", cache_idx);
	}

	/**
	 * Display a compressed bitmap direct to the backstore NOTE: Currently not
	 * functioning correctly, see Bitmap.decompressImgDirect Does not call
	 * repaint. Image is drawn to canvas on next update
	 * 
	 * @param x x coordinate within backstore for drawing of bitmap
	 * @param y y coordinate within backstore for drawing of bitmap
	 * @param width Width of bitmap
	 * @param height Height of bitmap
	 * @param size Size (bytes) of compressed bitmap data
	 * @param data Packet containing compressed bitmap data at current read
	 *            position
	 * @param Bpp Bytes-per-pixel for bitmap
	 * @param cm Colour model currently in use, if any
	 * @throws RdesktopException
	 */
	public void displayCompressed(int x, int y, int width, int height, int size, RdpPacket data, int Bpp, IndexColorModel cm)
			throws RdesktopException {
		backstore = Bitmap.decompressImgDirect(state, width, height, size, data, Bpp, cm, x, y, backstore);
	}

	/**
	 * Draw an image object to the backstore, does not call repaint. Image is
	 * drawn to canvas on next update.
	 * 
	 * @param img Image to draw to backstore
	 * @param x x coordinate for drawing location
	 * @param y y coordinate for drawing location
	 * @throws RdesktopException
	 */
	public void displayImage(Image img, int x, int y) throws RdesktopException {
		Graphics g = backstore.getDisplayGraphics();
		g.drawImage(img, x, y, null);
		/*
		 * ********* Useful test for identifying image boundaries ************
		 */
		// g.setColor(Color.RED);
		// g.drawRect(x,y,data.getWidth(null),data.getHeight(null));
		g.dispose();
	}

	/**
	 * Draw an image (from an integer array of colour data) to the backstore,
	 * does not call repaint. Image is drawn to canvas on next update.
	 * 
	 * @param data Integer array of pixel colour information
	 * @param w Width of image
	 * @param h Height of image
	 * @param x x coordinate for drawing location
	 * @param y y coordinate for drawing location
	 * @param cx Width of drawn image (clips, does not scale)
	 * @param cy Height of drawn image (clips, does not scale)
	 * @throws RdesktopException
	 */
	public void displayImage(int[] data, int w, int h, int x, int y, int cx, int cy) throws RdesktopException {
		backstore.setRGB(x, y, cx, cy, data, 0, w);
		/*
		 * ********* Useful test for identifying image boundaries ************
		 */
		// Graphics g = backstore.getGraphics();
		// g.drawImage(data,x,y,null);
		// g.setColor(Color.RED);
		// g.drawRect(x,y,cx,cy);
		// g.dispose();
	}

	/**
	 * Perform a dest blt
	 * 
	 * @param destblt DestBltOrder describing the blit to be performed
	 */
	public void drawDestBltOrder(DestBltOrder destblt) {
		int x = destblt.getX();
		int y = destblt.getY();
		if (x > this.right || y > this.bottom)
			return; // off screen
		int cx = destblt.getCX();
		int cy = destblt.getCY();
		int clipright = x + cx - 1;
		if (clipright > this.right)
			clipright = this.right;
		if (x < this.left)
			x = this.left;
		cx = clipright - x + 1;
		int clipbottom = y + cy - 1;
		if (clipbottom > this.bottom)
			clipbottom = this.bottom;
		if (y < this.top)
			y = this.top;
		cy = clipbottom - y + 1;
		rop.do_array(destblt.getOpcode(), backstore, this.width, x, y, cx, cy, null, 0, 0, 0);
		backstore.repaint(x, y, cx, cy);
	}

	/**
	 * Draw a single glyph to the screen
	 * 
	 * @param mixmode 0 for transparent background, specified colour for
	 *            background otherwide
	 * @param x x coordinate on screen at which to draw glyph
	 * @param y y coordinate on screen at which to draw glyph
	 * @param cx Width of clipping area for glyph
	 * @param cy Height of clipping area for glyph
	 * @param data Set of values defining glyph's pattern
	 * @param bgcolor Background colour for glyph pattern
	 * @param fgcolor Foreground colour for glyph pattern
	 */
	public void drawGlyph(int mixmode, int x, int y, int cx, int cy, byte[] data, int bgcolor, int fgcolor) {
		int pdata = 0;
		int index = 0x80;
		int bytes_per_row = (cx - 1) / 8 + 1;
		int newx, newy, newcx, newcy;
		int Bpp = state.getBytesPerPixel();
		// convert to 24-bit colour
		fgcolor = Bitmap.convertTo24(state, fgcolor);
		bgcolor = Bitmap.convertTo24(state, bgcolor);
		// correction for 24-bit colour
		if (Bpp == 3) {
			fgcolor = ((fgcolor & 0xFF) << 16) | (fgcolor & 0xFF00) | ((fgcolor & 0xFF0000) >> 16);
			bgcolor = ((bgcolor & 0xFF) << 16) | (bgcolor & 0xFF00) | ((bgcolor & 0xFF0000) >> 16);
		}
		// clip here instead
		if (x > this.right || y > this.bottom)
			return; // off screen
		int clipright = x + cx - 1;
		if (clipright > this.right)
			clipright = this.right;
		if (x < this.left)
			newx = this.left;
		else
			newx = x;
		newcx = clipright - x + 1; // not clipright - newx - 1
		int clipbottom = y + cy - 1;
		if (clipbottom > this.bottom)
			clipbottom = this.bottom;
		if (y < this.top)
			newy = this.top;
		else
			newy = y;
		newcy = clipbottom - newy + 1;
		int pbackstore = (newy * this.width) + x;
		pdata = bytes_per_row * (newy - y); // offset y, but not x
		if (mixmode == MIX_TRANSPARENT) { // FillStippled
			for (int i = 0; i < newcy; i++) {
				for (int j = 0; j < newcx; j++) {
					if (index == 0) { // next row
						pdata++;
						index = 0x80;
					}
					if ((data[pdata] & index) != 0) {
						if ((x + j >= newx) && (newx + j > 0) && (newy + i > 0))
							// since haven't offset x
							backstore.setRGB(newx + j, newy + i, fgcolor);
					}
					index >>= 1;
				}
				pdata++;
				index = 0x80;
				pbackstore += this.width;
				if (pdata == data.length) {
					pdata = 0;
				}
			}
		} else { // FillOpaqueStippled
			for (int i = 0; i < newcy; i++) {
				for (int j = 0; j < newcx; j++) {
					if (index == 0) { // next row
						pdata++;
						index = 0x80;
					}
					if (x + j >= newx) {
						if ((x + j > 0) && (y + i > 0)) {
							if ((data[pdata] & index) != 0)
								backstore.setRGB(x + j, y + i, fgcolor);
							else
								backstore.setRGB(x + j, y + i, bgcolor);
						}
					}
					index >>= 1;
				}
				pdata++;
				index = 0x80;
				pbackstore += this.width;
				if (pdata == data.length) {
					pdata = 0;
				}
			}
		}
		// if(logger.isInfoEnabled()) logger.info("glyph
		// \t(\t"+x+",\t"+y+"),(\t"+(x+cx-1)+",\t"+(y+cy-1)+")");
		backstore.repaint(newx, newy, newcx, newcy);
	}

	/**
	 * Draw a line to the screen
	 * 
	 * @param x1 x coordinate of start point of line
	 * @param y1 y coordinate of start point of line
	 * @param x2 x coordinate of end point of line
	 * @param y2 y coordinate of end point of line
	 * @param color colour of line
	 * @param opcode Operation code defining operation to perform on pixels
	 *            within the line
	 */
	public void drawLine(int x1, int y1, int x2, int y2, int color, int opcode) {
		// convert to 24-bit colour
		color = Bitmap.convertTo24(state, color);
		if (x1 == x2 || y1 == y2) {
			drawLineVerticalHorizontal(x1, y1, x2, y2, color, opcode);
			return;
		}
		int deltax = Math.abs(x2 - x1); // The difference between the x's
		int deltay = Math.abs(y2 - y1); // The difference between the y's
		int x = x1; // Start x off at the first pixel
		int y = y1; // Start y off at the first pixel
		int xinc1, xinc2, yinc1, yinc2;
		int num, den, numadd, numpixels;
		if (x2 >= x1) { // The x-values are increasing
			xinc1 = 1;
			xinc2 = 1;
		} else { // The x-values are decreasing
			xinc1 = -1;
			xinc2 = -1;
		}
		if (y2 >= y1) { // The y-values are increasing
			yinc1 = 1;
			yinc2 = 1;
		} else { // The y-values are decreasing
			yinc1 = -1;
			yinc2 = -1;
		}
		if (deltax >= deltay) { // There is at least one x-value for every
			// y-value
			xinc1 = 0; // Don't change the x when numerator >= denominator
			yinc2 = 0; // Don't change the y for every iteration
			den = deltax;
			num = deltax / 2;
			numadd = deltay;
			numpixels = deltax; // There are more x-values than y-values
		} else { // There is at least one y-value for every x-value
			xinc2 = 0; // Don't change the x for every iteration
			yinc1 = 0; // Don't change the y when numerator >= denominator
			den = deltay;
			num = deltay / 2;
			numadd = deltax;
			numpixels = deltay; // There are more y-values than x-values
		}
		for (int curpixel = 0; curpixel <= numpixels; curpixel++) {
			setPixel(opcode, x, y, color); // Draw the current pixel
			num += numadd; // Increase the numerator by the top of the fraction
			if (num >= den) { // Check if numerator >= denominator
				num -= den; // Calculate the new numerator value
				x += xinc1; // Change the x as appropriate
				y += yinc1; // Change the y as appropriate
			}
			x += xinc2; // Change the x as appropriate
			y += yinc2; // Change the y as appropriate
		}
		int x_min = x1 < x2 ? x1 : x2;
		int x_max = x1 > x2 ? x1 : x2;
		int y_min = y1 < y2 ? y1 : y2;
		int y_max = y1 > y2 ? y1 : y2;
		backstore.repaint(x_min, y_min, x_max - x_min + 1, y_max - y_min + 1);
	}

	/**
	 * Draw a line to the screen
	 * 
	 * @param line LineOrder describing line to be drawn
	 */
	public void drawLineOrder(LineOrder line) {
		int x1 = line.getStartX();
		int y1 = line.getStartY();
		int x2 = line.getEndX();
		int y2 = line.getEndY();
		int fgcolor = line.getPen().getColor();
		int opcode = line.getOpcode() - 1;
		drawLine(x1, y1, x2, y2, fgcolor, opcode);
	}

	/**
	 * Helper function for drawLine, draws a horizontal or vertical line using a
	 * much faster method than used for diagonal lines
	 * 
	 * @param x1 x coordinate of start point of line
	 * @param y1 y coordinate of start point of line
	 * @param x2 x coordinate of end point of line
	 * @param y2 y coordinate of end point of line
	 * @param color colour of line
	 * @param opcode Operation code defining operation to perform on pixels
	 *            within the line
	 */
	public void drawLineVerticalHorizontal(int x1, int y1, int x2, int y2, int color, int opcode) {
		int pbackstore;
		int i;
		// only vertical or horizontal lines
		if (y1 == y2) { // HORIZONTAL
			if (y1 >= this.top && y1 <= this.bottom) { // visible
				if (x2 > x1) { // x inc, y1=y2
					if (x1 < this.left)
						x1 = this.left;
					if (x2 > this.right)
						x2 = this.right;
					pbackstore = y1 * this.width + x1;
					for (i = 0; i < x2 - x1; i++) {
						rop.do_pixel(opcode, backstore, x1 + i, y1, color);
						pbackstore++;
					}
					backstore.repaint(x1, y1, x2 - x1 + 1, 1);
				} else { // x dec, y1=y2
					if (x2 < this.left)
						x2 = this.left;
					if (x1 > this.right)
						x1 = this.right;
					pbackstore = y1 * this.width + x1;
					for (i = 0; i < x1 - x2; i++) {
						rop.do_pixel(opcode, backstore, x2 + i, y1, color);
						pbackstore--;
					}
					backstore.repaint(x2, y1, x1 - x2 + 1, 1);
				}
			}
		} else { // x1==x2 VERTICAL
			if (x1 >= this.left && x1 <= this.right) { // visible
				if (y2 > y1) { // x1=x2, y inc
					if (y1 < this.top)
						y1 = this.top;
					if (y2 > this.bottom)
						y2 = this.bottom;
					pbackstore = y1 * this.width + x1;
					for (i = 0; i < y2 - y1; i++) {
						rop.do_pixel(opcode, backstore, x1, y1 + i, color);
						pbackstore += this.width;
					}
					backstore.repaint(x1, y1, 1, y2 - y1 + 1);
				} else { // x1=x2, y dec
					if (y2 < this.top)
						y2 = this.top;
					if (y1 > this.bottom)
						y1 = this.bottom;
					pbackstore = y1 * this.width + x1;
					for (i = 0; i < y1 - y2; i++) {
						rop.do_pixel(opcode, backstore, x1, y2 + i, color);
						pbackstore -= this.width;
					}
					backstore.repaint(x1, y2, 1, y1 - y2 + 1);
				}
			}
		}
		// if(logger.isInfoEnabled()) logger.info("line
		// \t(\t"+x1+",\t"+y1+"),(\t"+x2+",\t"+y2+")");
	}

	/**
	 * Perform a memory blit
	 * 
	 * @param memblt MemBltOrder describing the blit to be performed
	 */
	public void drawMemBltOrder(MemBltOrder memblt) {
		int x = memblt.getX();
		int y = memblt.getY();
		if (x > this.right || y > this.bottom)
			return; // off screen
		int cx = memblt.getCX();
		int cy = memblt.getCY();
		int srcx = memblt.getSrcX();
		int srcy = memblt.getSrcY();
		// Perform standard clipping checks, x-axis
		int clipright = x + cx - 1;
		if (clipright > this.right)
			clipright = this.right;
		if (x < this.left)
			x = this.left;
		cx = clipright - x + 1;
		// Perform standard clipping checks, y-axis
		int clipbottom = y + cy - 1;
		if (clipbottom > this.bottom)
			clipbottom = this.bottom;
		if (y < this.top)
			y = this.top;
		cy = clipbottom - y + 1;
		srcx += x - memblt.getX();
		srcy += y - memblt.getY();
		if (logger.isDebugEnabled())
			logger.debug("MEMBLT x=" + x + " y=" + y + " cx=" + cx + " cy=" + cy + " srcx=" + srcx + " srcy=" + srcy + " opcode="
					+ memblt.getOpcode());
		try {
			Bitmap bitmap = cache.getBitmap(memblt.getCacheID(), memblt.getCacheIDX());
			// IndexColorModel cm = cache.get_colourmap(memblt.getColorTable());
			// should use the colormap, but requires high color backstore...
			if (x + cx > backstore.getDisplayWidth() || y + cy > backstore.getDisplayHeight()) {
				backingStoreResize(x + cx, y + cy, false);
			}
			rop.do_array(memblt.getOpcode(), backstore, this.width, x, y, cx, cy, bitmap.getBitmapData(), bitmap.getWidth(), srcx,
					srcy);
			backstore.repaint(x, y, cx, cy);
		} catch (RdesktopException e) {
		}
	}

	/**
	 * Perform a pattern blit on the screen
	 * 
	 * @param patblt PatBltOrder describing the blit to be performed
	 */
	public void drawPatBltOrder(PatBltOrder patblt) {
		Brush brush = patblt.getBrush();
		int x = patblt.getX();
		int y = patblt.getY();
		if (x > this.right || y > this.bottom)
			return; // off screen
		int cx = patblt.getCX();
		int cy = patblt.getCY();
		int fgcolor = patblt.getForegroundColor();
		int bgcolor = patblt.getBackgroundColor();
		int opcode = patblt.getOpcode();
		patBltOrder(opcode, x, y, cx, cy, fgcolor, bgcolor, brush);
	}

	/**
	 * Draw a multi-point set of lines to the screen
	 * 
	 * @param polyline PolyLineOrder describing the set of lines to draw
	 */
	public void drawPolyLineOrder(PolyLineOrder polyline) {
		int x = polyline.getX();
		int y = polyline.getY();
		int fgcolor = polyline.getForegroundColor();
		int datasize = polyline.getDataSize();
		byte[] databytes = polyline.getData();
		int lines = polyline.getLines();
		// convert to 24-bit colour
		fgcolor = Bitmap.convertTo24(state, fgcolor);
		// hack - data as single element byte array so can pass by ref to
		// parse_delta
		// see http://www.rgagnon.com/javadetails/java-0035.html
		int[] data = new int[1];
		data[0] = ((lines - 1) / 4) + 1;
		int flags = 0;
		int index = 0;
		int opcode = polyline.getOpcode() - 1;
		for (int line = 0; (line < lines) && (data[0] < datasize); line++) {
			int xfrom = x;
			int yfrom = y;
			if (line % 4 == 0)
				flags = databytes[index++];
			if ((flags & 0xc0) == 0)
				flags |= 0xc0; /* none = both */
			if ((flags & 0x40) != 0)
				x += parse_delta(databytes, data);
			if ((flags & 0x80) != 0)
				y += parse_delta(databytes, data);
			// logger.info("polyline
			// "+line+","+xfrom+","+yfrom+","+x+","+y+","+fgcolor+","+opcode);
			drawLine(xfrom, yfrom, x, y, fgcolor, opcode);
			flags <<= 2;
		}
	}

	/**
	 * Draw a rectangle to the screen
	 * 
	 * @param rect RectangleOrder defining the rectangle to be drawn
	 */
	public void drawRectangleOrder(RectangleOrder rect) {
		// if(logger.isInfoEnabled()) logger.info("RectangleOrder!");
		fillRectangle(rect.getX(), rect.getY(), rect.getCX(), rect.getCY(), rect.getColor());
	}

	/**
	 * Perform a screen blit
	 * 
	 * @param screenblt ScreenBltOrder describing the blit to be performed
	 */
	public void drawScreenBltOrder(ScreenBltOrder screenblt) {
		int x = screenblt.getX();
		int y = screenblt.getY();
		if (x > this.right || y > this.bottom)
			return; // off screen
		int cx = screenblt.getCX();
		int cy = screenblt.getCY();
		int srcx = screenblt.getSrcX();
		int srcy = screenblt.getSrcY();
		int clipright = x + cx - 1;
		if (clipright > this.right)
			clipright = this.right;
		if (x < this.left)
			x = this.left;
		cx = clipright - x + 1;
		int clipbottom = y + cy - 1;
		if (clipbottom > this.bottom)
			clipbottom = this.bottom;
		if (y < this.top)
			y = this.top;
		cy = clipbottom - y + 1;
		srcx += x - screenblt.getX();
		srcy += y - screenblt.getY();
		rop.do_array(screenblt.getOpcode(), backstore, this.width, x, y, cx, cy, null, this.width, srcx, srcy);
		backstore.repaint(x, y, cx, cy);
	}

	/**
	 * Perform a tri blit on the screen
	 * 
	 * @param triblt TriBltOrder describing the blit
	 */
	public void drawTriBltOrder(TriBltOrder triblt) {
		int x = triblt.getX();
		int y = triblt.getY();
		if (x > this.right || y > this.bottom)
			return; // off screen
		int cx = triblt.getCX();
		int cy = triblt.getCY();
		int srcx = triblt.getSrcX();
		int srcy = triblt.getSrcY();
		int fgcolor = triblt.getForegroundColor();
		int bgcolor = triblt.getBackgroundColor();
		Brush brush = triblt.getBrush();
		// convert to 24-bit colour
		fgcolor = Bitmap.convertTo24(state, fgcolor);
		bgcolor = Bitmap.convertTo24(state, bgcolor);
		// Perform standard clipping checks, x-axis
		int clipright = x + cx - 1;
		if (clipright > this.right)
			clipright = this.right;
		if (x < this.left)
			x = this.left;
		cx = clipright - x + 1;
		// Perform standard clipping checks, y-axis
		int clipbottom = y + cy - 1;
		if (clipbottom > this.bottom)
			clipbottom = this.bottom;
		if (y < this.top)
			y = this.top;
		cy = clipbottom - y + 1;
		try {
			Bitmap bitmap = cache.getBitmap(triblt.getCacheID(), triblt.getCacheIDX());
			switch (triblt.getOpcode()) {
			case 0x69: // PDSxxn
				rop.do_array(ROP2_XOR, backstore, this.width, x, y, cx, cy, bitmap.getBitmapData(), bitmap.getWidth(), srcx, srcy);
				patBltOrder(ROP2_NXOR, x, y, cx, cy, fgcolor, bgcolor, brush);
				break;
			case 0xb8: // PSDPxax
				patBltOrder(ROP2_XOR, x, y, cx, cy, fgcolor, bgcolor, brush);
				rop.do_array(ROP2_AND, backstore, this.width, x, y, cx, cy, bitmap.getBitmapData(), bitmap.getWidth(), srcx, srcy);
				patBltOrder(ROP2_XOR, x, y, cx, cy, fgcolor, bgcolor, brush);
				break;
			case 0xc0: // PSa
				rop.do_array(ROP2_COPY, backstore, this.width, x, y, cx, cy, bitmap.getBitmapData(), bitmap.getWidth(), srcx, srcy);
				patBltOrder(ROP2_AND, x, y, cx, cy, fgcolor, bgcolor, brush);
				break;
			default:
				logger.warn("Unimplemented Triblt opcode:" + triblt.getOpcode());
				rop.do_array(ROP2_COPY, backstore, this.width, x, y, cx, cy, bitmap.getBitmapData(), bitmap.getWidth(), srcx, srcy);
			}
		} catch (RdesktopException e) {
		}
	}

	/**
	 * Draw a filled rectangle to the screen
	 * 
	 * @param x x coordinate (left) of rectangle
	 * @param y y coordinate (top) of rectangle
	 * @param cx Width of rectangle
	 * @param cy Height of rectangle
	 * @param color Colour of rectangle
	 */
	public void fillRectangle(int x, int y, int cx, int cy, int color) {
		// clip here instead
		if (x > this.right || y > this.bottom)
			return; // off screen
		int Bpp = state.getBytesPerPixel();
		// convert to 24-bit colour
		color = Bitmap.convertTo24(state, color);
		// correction for 24-bit colour
		if (Bpp == 3)
			color = ((color & 0xFF) << 16) | (color & 0xFF00) | ((color & 0xFF0000) >> 16);
		// Perform standard clipping checks, x-axis
		int clipright = x + cx - 1;
		if (clipright > this.right)
			clipright = this.right;
		if (x < this.left)
			x = this.left;
		cx = clipright - x + 1;
		// Perform standard clipping checks, y-axis
		int clipbottom = y + cy - 1;
		if (clipbottom > this.bottom)
			clipbottom = this.bottom;
		if (y < this.top)
			y = this.top;
		cy = clipbottom - y + 1;
		// construct rectangle as integer array, filled with color
		int[] rect = new int[cx * cy];
		for (int i = 0; i < rect.length; i++)
			rect[i] = color;
		// draw rectangle to backstore
		if (logger.isDebugEnabled())
			logger.debug("rect \t(\t" + x + ",\t" + y + "),(\t" + (x + cx - 1) + ",\t" + (y + cy - 1) + ")/"
					+ backstore.getDisplayWidth() + "x" + backstore.getDisplayHeight());
		backstore.setRGB(x, y, cx, cy, rect, 0, cx);
		backstore.repaint(x, y, cx, cy); // seems to be faster than
											// Graphics.fillRect
		// according to JProbe
	}

	/**
	 * Handle the window gaining focus, notify input classes
	 */
	public void gainedFocus() {
		if (input != null)
			input.gainedFocus();
	}

	public Display getDisplay() {
		return backstore;
	}

	/**
	 * Retrieve an image from the backstore, as integer pixel information
	 * 
	 * @param x x coordinate of image to retrieve
	 * @param y y coordinage of image to retrieve
	 * @param cx width of image to retrieve
	 * @param cy height of image to retrieve
	 * @return Requested area of backstore, as an array of integer pixel colours
	 */
	public int[] getImage(int x, int y, int cx, int cy) {
		int[] data = new int[cx * cy];
		data = backstore.getRGB(x, y, cx, cy, null, // no existing image data to
				// add to
				0, // retrieving as complete image, no offset needed
				cx);
		return data;
	}

	/**
	 * return Input instance associated with this canvas.
	 */
	public Input getInput() {
		return (input);
	}

	/**
	 * Handle the window losing focus, notify input classes
	 */
	public void lostFocus() {
		if (input != null)
			input.lostFocus();
	}

	public void movePointer(int x, int y) {
		Point p = backstore.getLocationOnScreen();
		x = x + p.x;
		y = y + p.y;
		if (robot != null)
			robot.mouseMove(x, y);
	}

	/**
	 * Draw a pattern to the screen (pattern blit)
	 * 
	 * @param opcode Code defining operation to be performed
	 * @param x x coordinate for left of blit area
	 * @param y y coordinate for top of blit area
	 * @param cx Width of blit area
	 * @param cy Height of blit area
	 * @param fgcolor Foreground colour for pattern
	 * @param bgcolor Background colour for pattern
	 * @param brush Brush object defining pattern to be drawn
	 */
	public void patBltOrder(int opcode, int x, int y, int cx, int cy, int fgcolor, int bgcolor, Brush brush) {
		// convert to 24-bit colour
		fgcolor = Bitmap.convertTo24(state, fgcolor);
		bgcolor = Bitmap.convertTo24(state, bgcolor);
		// Perform standard clipping checks, x-axis
		int clipright = x + cx - 1;
		if (clipright > this.right)
			clipright = this.right;
		if (x < this.left)
			x = this.left;
		cx = clipright - x + 1;
		// Perform standard clipping checks, y-axis
		int clipbottom = y + cy - 1;
		if (clipbottom > this.bottom)
			clipbottom = this.bottom;
		if (y < this.top)
			y = this.top;
		cy = clipbottom - y + 1;
		int i;
		int[] src = null;
		switch (brush.getStyle()) {
		case 0: // solid
			// make efficient version of rop later with int fgcolor and boolean
			// usearray set to false for single colour
			src = new int[cx * cy];
			for (i = 0; i < src.length; i++)
				src[i] = fgcolor;
			if (logger.isDebugEnabled())
				logger.debug("x=" + x + " y=" + y + " cx=" + cx + " cy=" + cy + " width=" + this.width + " height=" + this.height
						+ " imgwidth=" + backstore.getDisplayWidth() + " imgheight=" + backstore.getDisplayHeight());
			rop.do_array(opcode, backstore, this.width, x, y, cx, cy, src, cx, 0, 0);
			backstore.repaint(x, y, cx, cy);
			break;
		case 2: // hatch
			System.out.println("hatch");
			break;
		case 3: // pattern
			int brushx = brush.getXOrigin();
			int brushy = brush.getYOrigin();
			byte[] pattern = brush.getPattern();
			byte[] ipattern = pattern;
			/*
			 * // not sure if this inversion is needed byte[] ipattern = new
			 * byte[8]; for(i=0;i<ipattern.length;i++) {
			 * ipattern[ipattern.length-1-i] = pattern[i]; }
			 */
			src = new int[cx * cy];
			int psrc = 0;
			for (i = 0; i < cy; i++) {
				for (int j = 0; j < cx; j++) {
					if ((ipattern[(i + brushy) % 8] & (0x01 << ((j + brushx) % 8))) == 0)
						src[psrc] = fgcolor;
					else
						src[psrc] = bgcolor;
					psrc++;
				}
			}
			rop.do_array(opcode, backstore, this.width, x, y, cx, cy, src, cx, 0, 0);
			backstore.repaint(x, y, cx, cy);
			break;
		default:
			logger.warn("Unsupported brush style " + brush.getStyle());
		}
	}

	/**
	 * Draw an image (from an integer array of colour data) to the backstore,
	 * also calls repaint to draw image to canvas
	 * 
	 * @param x x coordinate at which to draw image
	 * @param y y coordinate at which to draw image
	 * @param cx Width of drawn image (clips, does not scale)
	 * @param cy Height of drawn image (clips, does not scale)
	 * @param data Image to draw, represented as an array of integer pixel
	 *            colours
	 */
	public void putImage(int x, int y, int cx, int cy, int[] data) {
		backstore.setRGBNoConversion(x, y, cx, cy, data, 0, // drawing entire
				// image, no
				// offset needed
				cx);
		backstore.repaint(x, y, cx, cy);
	}

	/**
	 * Set cache for this session
	 * 
	 * @param cache Cache to be used in this session
	 */
	public void registerCache(Cache cache) {
		this.cache = cache;
	}

	/**
	 * Register the Rdp layer to act as the communications interface to this
	 * canvas
	 * 
	 * @param rdp Rdp object controlling Rdp layer communication
	 */
	public void registerCommLayer(Rdp rdp) {
		this.rdp = rdp;
		if (fbKeys != null)
			input = new Input(context, state, this, rdp, fbKeys);
	}

	/**
	 * Register keymap
	 * 
	 * @param keys Keymapping object for use in handling keyboard events
	 */
	public void registerKeyboard(KeyCode_FileBased keys) {
		this.fbKeys = keys;
		if (rdp != null) {
			// rdp and keys have been registered...
			if (input != null)
				input.removeInputListeners();
			input = new Input(context, state, this, rdp, keys);
		}
	}

	// @Override
	// public void addNotify() {
	// super.addNotify();
	// /*
	// * if (robot == null) { try { robot = new Robot(); } catch(AWTException
	// * e) { logger.warn("Pointer movement not allowed"); } }
	// */
	// }
	/**
	 * Register a colour palette with this canvas
	 * 
	 * @param cm Colour model to be used with this canvas
	 */
	public void registerPalette(IndexColorModel cm) {
		this.colormap = cm;
		backstore.setIndexColorModel(cm);
	}

	/**
	 * Reset clipping boundaries for canvas
	 */
	public void resetClip() {
		Graphics g = backstore.getDisplayGraphics();
		Rectangle bounds = backstore.getBounds();
		g.setClip(bounds.x, bounds.y, bounds.width, bounds.height);
		this.top = 0;
		this.left = 0;
		this.right = this.width - 1; // changed
		this.bottom = this.height - 1; // changed
	}

	/**
	 * Set clipping boundaries for canvas, based on a bounds order
	 * 
	 * @param bounds Order defining new boundaries
	 */
	public void setClip(BoundsOrder bounds) {
		Graphics g = backstore.getDisplayGraphics();
		g.setClip(bounds.getLeft(), bounds.getTop(), bounds.getRight() - bounds.getLeft(), bounds.getBottom() - bounds.getTop());
		this.top = bounds.getTop();
		this.left = bounds.getLeft();
		this.right = bounds.getRight();
		this.bottom = bounds.getBottom();
		if (this.right >= backstore.getDisplayWidth() || this.bottom >= backstore.getDisplayHeight()) {
			backingStoreResize(this.right + 1, this.bottom + 1, false);
		}
	}

	/**
	 * Perform an operation on a pixel in the backstore
	 * 
	 * @param opcode ID of operation to perform
	 * @param x x coordinate of pixel
	 * @param y y coordinate of pixel
	 * @param color Colour value to be used in operation
	 */
	public void setPixel(int opcode, int x, int y, int color) {
		int Bpp = state.getBytesPerPixel();
		// correction for 24-bit colour
		if (Bpp == 3)
			color = ((color & 0xFF) << 16) | (color & 0xFF00) | ((color & 0xFF0000) >> 16);
		if ((x < this.left) || (x > this.right) || (y < this.top) || (y > this.bottom)) { // Clip
		} else {
			rop.do_pixel(opcode, backstore, x, y, color);
		}
	}

	/**
	 * Notify the input classes that the connection is ready for sending
	 * messages
	 */
	public void triggerReadyToSend() {
		input.triggerReadyToSend();
	}

	/**
	 * Create an AWT Cursor from an image
	 * 
	 * @param wincursor
	 * @param p
	 * @param s
	 * @param cache_idx
	 * @return Generated Cursor object
	 */
	protected RdpCursor createCustomCursor(Image wincursor, Point p, String s, int cache_idx) {
		if (logger.isDebugEnabled())
			logger.debug(String.format("Creating custom cursor at %s (cached %d)", p, cache_idx));
		return backstore.createCursor("", p, wincursor);
	}

	private int getInvertedColor(int x, int y) {
		return ((x + y) & 1) != 0 ? Color.white.getRGB() : 0;
	}

	/**
	 * Parse a delta co-ordinate in polyline order form
	 * 
	 * @param buffer
	 * @param offset
	 * @return
	 */
	static int parse_delta(byte[] buffer, int[] offset) {
		int value = buffer[offset[0]++] & 0xff;
		int two_byte = value & 0x80;
		if ((value & 0x40) != 0) /* sign bit */
			value |= ~0x3f;
		else
			value &= 0x3f;
		if (two_byte != 0)
			value = (value << 8) | (buffer[offset[0]++] & 0xff);
		return value;
	}
}
