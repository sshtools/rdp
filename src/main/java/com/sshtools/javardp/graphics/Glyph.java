/* Glyph.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Represents data for individual glyphs, used
 *          for drawing text
 */
package com.sshtools.javardp.graphics;

//import java.awt.*;
//import java.awt.image.*;

public class Glyph {

	private int baseline = 0;
	private int character = 0;
	private int font = 0;
	private byte[] fontdata = null;
	private int height = 0;
	private int offset = 0;
	private int width = 0;

	/**
	 * Construct a Glyph object
	 * 
	 * @param font Font ID for Glyph
	 * @param character Character ID for Glyph
	 * @param offset x-offset of Glyph data for drawing
	 * @param baseline y-offset of Glyph data for drawing
	 * @param width Width of Glyph, in pixels
	 * @param height Height of Glyph, in pixels
	 * @param fontdata Data detailing Glyph's graphical representation
	 */
	public Glyph(int font, int character, int offset, int baseline, int width, int height, byte[] fontdata) {
		this.font = font;
		this.character = character;
		this.offset = offset;
		this.baseline = baseline;
		this.width = width;
		this.height = height;
		this.fontdata = fontdata;
	}

	/**
	 * Retrive y-offset of Glyph data
	 * 
	 * @return y-offset
	 */
	public int getBaseLine() {
		return this.baseline;
	}

	/**
	 * Return character ID of this Glyph
	 * 
	 * @return ID of character represented by this Glyph
	 */
	public int getCharacter() {
		return this.character;
	}

	/**
	 * Retrieve the font ID for this Glyph
	 * 
	 * @return Font ID
	 */
	public int getFont() {
		return this.font;
	}

	/**
	 * Graphical data for this Glyph
	 * 
	 * @return Data defining graphical representation of this Glyph
	 */
	public byte[] getFontData() {
		return this.fontdata;
	}

	/**
	 * Return height of Glyph
	 * 
	 * @return Glyph height, in pixels
	 */
	public int getHeight() {
		return this.height;
	}

	/**
	 * Retrive x-offset of Glyph data
	 * 
	 * @return x-offset
	 */
	public int getOffset() {
		return this.offset;
	}

	/**
	 * Return width of Glyph
	 * 
	 * @return Glyph width, in pixels
	 */
	public int getWidth() {
		return this.width;
	}
}
