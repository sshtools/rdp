/* Cache.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Handle caching of bitmaps, cursors, colour maps,
 *          text and fonts
 */
package com.sshtools.javardp;

import java.awt.image.IndexColorModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.graphics.Bitmap;
import com.sshtools.javardp.graphics.Glyph;
import com.sshtools.javardp.graphics.RdpCursor;

public class Cache {
	static Logger logger = LoggerFactory.getLogger(Cache.class);
	private static final int RDPCACHE_COLOURMAPSIZE = 0x06; // unified patch
	private Bitmap[][] bitmapcache = new Bitmap[3][600];
	private IndexColorModel[] colourcache = new IndexColorModel[RDPCACHE_COLOURMAPSIZE];
	private RdpCursor[] cursorcache = new RdpCursor[32];
	private Glyph[][] fontcache = new Glyph[12][256];
	private int[] highdeskcache = new int[921600];
	private int num_bitmaps_in_memory[] = new int[3];
	private DataBlob[] textcache = new DataBlob[256];
	private State state;

	public Cache(State state) {
		this.state = state;
		if(state.getOptions().getPersistentCacheBackend() != null)
			state.getOptions().getPersistentCacheBackend().start(state);
	}

	/**
	 * Retrieve the indexed colour model from the specified cache
	 * 
	 * @param cache_id ID of cache from which to retrieve colour model
	 * @return Indexed colour model for specified cache
	 * @throws RdesktopException
	 */
	public IndexColorModel get_colourmap(int cache_id) throws RdesktopException {
		IndexColorModel map = null;
		if (cache_id < colourcache.length) {
			map = colourcache[cache_id];
			if (map != null)
				return map;
		}
		throw new RdesktopException("Could not get colourmap with cache_id=" + cache_id);
	}

	/**
	 * Retrieve a bitmap from the cache
	 * 
	 * @param cache_id ID of cache from which to retrieve bitmap
	 * @param cache_idx ID of bitmap to return
	 * @return Bitmap stored in specified location within the cache
	 * @throws RdesktopException
	 */
	public Bitmap getBitmap(int cache_id, int cache_idx) throws RdesktopException {
		Bitmap bitmap = null;
		if ((cache_id < bitmapcache.length) && (cache_idx < bitmapcache[0].length)) {
			bitmap = bitmapcache[cache_id][cache_idx];
			/*
			 * try { if (bitmap != null ||
			 * PstCache.pstcache_load_bitmap(cache_id, cache_idx)){ if
			 * (PstCache.IS_PERSISTENT(cache_id)) TOUCH(cache_id, cache_idx);
			 * return bitmap; } } catch (IOException e) { e.printStackTrace(); }
			 * catch (RdesktopException e) { e.printStackTrace(); }
			 */
			if (bitmap != null)
				return bitmap;
		}
		throw new RdesktopException("Could not get Bitmap!");
	}

	/**
	 * Retrieve a Cursor object from the cache
	 * 
	 * @param cache_idx ID of cache in which the Cursor is stored
	 * @return Cursor stored in specified cache
	 * @throws RdesktopException
	 */
	public RdpCursor getCursor(int cache_idx) throws RdesktopException {
		RdpCursor cursor = null;
		if (cache_idx < cursorcache.length) {
			cursor = cursorcache[cache_idx];
			if (cursor != null) {
				return cursor;
			}
		}
		throw new RdesktopException("Cursor not found");
	}

	/**
	 * Retrieve an image from the desktop cache
	 * 
	 * @param offset Offset of image data within desktop cache
	 * @param cx Width of image
	 * @param cy Height of image
	 * @return Integer pixel data for image requested
	 * @throws RdesktopException
	 */
	public int[] getDesktopInt(int offset, int cx, int cy) throws RdesktopException {
		int length = cx * cy;
		int pdata = 0;
		int[] data = new int[length];
		if (offset > highdeskcache.length)
			offset = 0;
		if (offset + length <= highdeskcache.length) {
			for (int i = 0; i < cy; i++) {
				System.arraycopy(highdeskcache, offset, data, pdata, cx);
				offset += cx;
				pdata += cx;
			}
			return data;
		}
		throw new RdesktopException("Could not get Bitmap");
	}

	/**
	 * Retrieve a Glyph for a specified character in a specified font
	 * 
	 * @param font ID of desired font for Glyph
	 * @param character ID of desired character
	 * @return Glyph representing character in font
	 * @throws RdesktopException
	 */
	public Glyph getFont(int font, int character) throws RdesktopException {
		if ((font < fontcache.length) && (character < fontcache[0].length)) {
			Glyph glyph = fontcache[font][character];
			if (glyph != null) {
				return glyph;
			}
		}
		throw new RdesktopException("Could not get Font:" + font + ", " + character);
	}

	/**
	 * Retrieve text stored in the cache
	 * 
	 * @param cache_id ID of cache containing text
	 * @return Text stored in specified cache, represented as a DataBlob
	 * @throws RdesktopException
	 */
	public DataBlob getText(int cache_id) throws RdesktopException {
		DataBlob entry = null;
		if (cache_id < textcache.length) {
			entry = textcache[cache_id];
			if (entry != null) {
				if (entry.getData() != null) {
					return entry;
				}
			}
		}
		throw new RdesktopException("Could not get Text:" + cache_id);
	}

	/**
	 * Assign a colour model to a specified cache
	 * 
	 * @param cache_id ID of cache to which the colour map should be added
	 * @param map Indexed colour model to assign to the cache
	 * @throws RdesktopException
	 */
	public void put_colourmap(int cache_id, IndexColorModel map) throws RdesktopException {
		if (cache_id < colourcache.length)
			colourcache[cache_id] = map;
		else
			throw new RdesktopException("Could not put colourmap with cache_id=" + cache_id);
	}

	/**
	 * Add a bitmap to the cache
	 * 
	 * @param cache_id ID of cache to which the Bitmap should be added
	 * @param cache_idx ID of location in specified cache in which to store the
	 *            Bitmap
	 * @param bitmap Bitmap object to store in cache
	 * @param stamp Timestamp for storage of bitmap
	 * @throws RdesktopException
	 */
	public void putBitmap(int cache_id, int cache_idx, Bitmap bitmap, int stamp) throws RdesktopException {
		// Bitmap old;
		if ((cache_id < bitmapcache.length) && (cache_idx < bitmapcache[0].length)) {
			bitmapcache[cache_id][cache_idx] = bitmap;
			/*
			 * if (Options.use_rdp5) { if (++num_bitmaps_in_memory[cache_id] >
			 * Rdp.BMPCACHE2_C2_CELLS) removeLRUBitmap(cache_id); }
			 * 
			 * bitmapcache[cache_id][cache_idx] = bitmap;
			 * bitmapcache[cache_id][cache_idx].usage = stamp;
			 */
		} else {
			throw new RdesktopException("Could not put Bitmap!");
		}
	}

	/**
	 * Assign a Cursor object to a specific cache
	 * 
	 * @param cache_idx ID of cache to store Cursor in
	 * @param cursor Cursor object to assign to cache
	 * @throws RdesktopException
	 */
	public void putCursor(int cache_idx, RdpCursor cursor) throws RdesktopException {
		if (cache_idx < cursorcache.length) {
			cursorcache[cache_idx] = cursor;
		} else {
			throw new RdesktopException("Could not put Cursor!");
		}
	}

	/**
	 * Store an image in the desktop cache
	 * 
	 * @param offset Location in desktop cache to begin storage of supplied data
	 * @param cx Width of image to store
	 * @param cy Height of image to store
	 * @param data Array of integer pixel values representing image to be stored
	 * @throws RdesktopException
	 */
	public void putDesktop(int offset, int cx, int cy, int[] data) throws RdesktopException {
		int length = cx * cy;
		int pdata = 0;
		if (offset > highdeskcache.length)
			offset = 0;
		if (offset + length <= highdeskcache.length) {
			for (int i = 0; i < cy; i++) {
				System.arraycopy(data, pdata, highdeskcache, offset, cx);
				offset += cx;
				pdata += cx;
			}
		} else {
			throw new RdesktopException("Could not put Desktop");
		}
	}

	/**
	 * Add a font to the cache
	 * 
	 * @param glyph Glyph containing references to relevant font
	 * @throws RdesktopException
	 */
	public void putFont(Glyph glyph) throws RdesktopException {
		if ((glyph.getFont() < fontcache.length) && (glyph.getCharacter() < fontcache[0].length)) {
			fontcache[glyph.getFont()][glyph.getCharacter()] = glyph;
		} else {
			throw new RdesktopException("Could not put font");
		}
	}

	/**
	 * Store text in the cache
	 * 
	 * @param cache_id ID of cache in which to store the text
	 * @param entry DataBlob representing the text to be stored
	 * @throws RdesktopException
	 */
	public void putText(int cache_id, DataBlob entry) throws RdesktopException {
		if (cache_id < textcache.length) {
			textcache[cache_id] = entry;
		} else {
			throw new RdesktopException("Could not put Text");
		}
	}

	/**
	 * Update the persistent bitmap cache MRU information on exit
	 */
	public void saveState() {
		int id, idx;
		for (id = 0; id < bitmapcache.length; id++)
			if (state.getOptions().getPersistentCacheBackend() != null
					&& state.getOptions().getPersistentCacheBackend().IS_PERSISTENT(id))
				for (idx = 0; idx < bitmapcache[id].length; idx++)
					state.getOptions().getPersistentCacheBackend().touchBitmap(id, idx, bitmapcache[id][idx].usage);
	}

	/**
	 * Remove the least-recently-used bitmap from the specified cache
	 * 
	 * @param cache_id Number of cache from which to remove bitmap
	 */
	void removeLRUBitmap(int cache_id) {
		int i;
		int cache_idx = 0;
		int m = 0xffffffff;
		for (i = 0; i < bitmapcache[cache_id].length; i++) {
			if ((bitmapcache[cache_id][i] != null) && (bitmapcache[cache_id][i].getBitmapData() != null)
					&& (bitmapcache[cache_id][i].usage < m)) {
				cache_idx = i;
				m = bitmapcache[cache_id][i].usage;
			}
		}
		bitmapcache[cache_id][cache_idx] = null;
		--num_bitmaps_in_memory[cache_id];
	}

	void TOUCH(int id, int idx) {
		bitmapcache[id][idx].usage = state.getOptions().getPersistentCacheBackend().nextSeq();
	}
}
