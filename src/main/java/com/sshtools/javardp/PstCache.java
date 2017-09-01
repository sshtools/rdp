/* PstCache.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Handle persistent caching
 */
package com.sshtools.javardp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.graphics.Bitmap;

public class PstCache {
	public static final int MAX_CELL_SIZE = 0x1000; /* pixels */
	static int g_pstcache_Bpp;

	static boolean g_pstcache_enumerated = false;

	static File[] g_pstcache_fd = new File[8];
	static int g_stamp;
	static Logger logger = LoggerFactory.getLogger(PstCache.class);
	protected static boolean IS_PERSISTENT(int id) {
		return (id < 8 && g_pstcache_fd[id] != null);
	}

	/* Update usage info for a bitmap */
	protected static void touchBitmap(int cache_id, int cache_idx, int stamp) {
		logger.info("PstCache.touchBitmap");
		FileOutputStream fd;
		if (!IS_PERSISTENT(cache_id) || cache_idx >= Rdp.BMPCACHE2_NUM_PSTCELLS)
			return;
		try {
			fd = new FileOutputStream(g_pstcache_fd[cache_id]);
			fd.write(toBigEndian32(stamp), 12 + cache_idx * (g_pstcache_Bpp * MAX_CELL_SIZE + CELLHEADER.size()), 4);
			// rd_lseek_file(fd, 12 + cache_idx * (g_pstcache_Bpp *
			// MAX_CELL_SIZE + sizeof(CELLHEADER))); // this seems to do nothing
			// (return 0) in rdesktop
			// rd_write_file(fd, &stamp, sizeof(stamp)); // same with this
			// one???
		} catch (IOException e) {
			return;
		}
	}

	/* list the bitmaps from the persistent cache file */
	static int pstcache_enumerate(State state, int cache_id, int[] idlist) throws IOException, RdesktopException {
		logger.info("PstCache.pstcache_enumerate");
		FileInputStream fd;
		int n, c = 0;
		CELLHEADER cellhdr = null;
		if (!(state.getOptions().isBitmapCaching() && state.getOptions().isPersistentBitmapCaching() && IS_PERSISTENT(cache_id)))
			return 0;
		/*
		 * The server disconnects if the bitmap cache content is sent more than
		 * once
		 */
		if (g_pstcache_enumerated)
			return 0;
		logger.debug("pstcache enumeration... ");
		for (n = 0; n < Rdp.BMPCACHE2_NUM_PSTCELLS; n++) {
			fd = new FileInputStream(g_pstcache_fd[cache_id]);
			byte[] cellhead_data = new byte[CELLHEADER.size()];
			if (fd.read(cellhead_data, n * (g_pstcache_Bpp * MAX_CELL_SIZE + CELLHEADER.size()), CELLHEADER.size()) <= 0)
				break;
			cellhdr = new CELLHEADER(cellhead_data);
			int result = 0;
			for (int i = 0; i < cellhdr.bitmap_id.length; i++) {
				result += cellhdr.bitmap_id[i];
			}
			if (result != 0) {
				for (int i = 0; i < 8; i++) {
					idlist[(n * 8) + i] = cellhdr.bitmap_id[i];
				}
				if (cellhdr.stamp != 0) {
					/*
					 * Pre-caching is not possible with 8bpp because a colourmap
					 * is needed to load them
					 */
					if (state.getOptions().isPrecacheBitmaps() && (state.getServerBpp() > 8)) {
						if (pstcache_load_bitmap(state, cache_id, n))
							c++;
					}
					g_stamp = Math.max(g_stamp, cellhdr.stamp);
				}
			} else {
				break;
			}
		}
		logger.info(n + " bitmaps in persistent cache, " + c + " bitmaps loaded in memory\n");
		g_pstcache_enumerated = true;
		return n;
	}

	/* initialise the persistent bitmap cache */
	static boolean pstcache_init(State state, int cache_id) {
		// int fd;
		String filename;
		if (g_pstcache_enumerated)
			return true;
		g_pstcache_fd[cache_id] = null;
		if (!(state.getOptions().isBitmapCaching() && state.getOptions().isPersistentBitmapCaching()))
			return false;
		g_pstcache_Bpp = state.getBytesPerPixel();
		filename = "./cache/pstcache_" + cache_id + "_" + g_pstcache_Bpp;
		logger.debug("persistent bitmap cache file: " + filename);
		File cacheDir = new File("./cache/");
		if (!cacheDir.exists() && !cacheDir.mkdir()) {
			logger.warn("failed to get/make cache directory");
			return false;
		}
		File f = new File(filename);
		try {
			if (!f.exists() && !f.createNewFile()) {
				logger.warn("Could not create cache file");
				return false;
			}
		} catch (IOException e) {
			return false;
		}
		/*
		 * if (!rd_lock_file(fd, 0, 0)) {logger.warn(
		 * "Persistent bitmap caching is disabled. (The file is already in use)\n"
		 * ); rd_close_file(fd); return false; }
		 */
		g_pstcache_fd[cache_id] = f;
		return true;
	}

	/* Load a bitmap from the persistent cache */
	static boolean pstcache_load_bitmap(State state, int cache_id, int cache_idx) throws IOException, RdesktopException {
		logger.info("PstCache.pstcache_load_bitmap");
		byte[] celldata = null;
		FileInputStream fd;
		// CELLHEADER cellhdr;
		Bitmap bitmap;
		byte[] cellHead = null;
		if (!state.getOptions().isPersistentBitmapCaching())
			return false;
		if (!IS_PERSISTENT(cache_id) || cache_idx >= Rdp.BMPCACHE2_NUM_PSTCELLS)
			return false;
		fd = new FileInputStream(g_pstcache_fd[cache_id]);
		int offset = cache_idx * (g_pstcache_Bpp * MAX_CELL_SIZE + CELLHEADER.size());
		fd.read(cellHead, offset, CELLHEADER.size());
		CELLHEADER c = new CELLHEADER(cellHead);
		// rd_lseek_file(fd, cache_idx * (g_pstcache_Bpp * MAX_CELL_SIZE +
		// sizeof(CELLHEADER)));
		// rd_read_file(fd, &cellhdr, sizeof(CELLHEADER));
		// celldata = (uint8 *) xmalloc(cellhdr.length);
		// rd_read_file(fd, celldata, cellhdr.length);
		celldata = new byte[c.length];
		fd.read(celldata);
		logger.debug("Loading bitmap from disk (" + cache_id + ":" + cache_idx + ")\n");
		bitmap = new Bitmap(state, celldata, c.width, c.height, 0, 0, state.getBytesPerPixel());
		// bitmap = ui_create_bitmap(cellhdr.width, cellhdr.height, celldata);
		Orders.cache.putBitmap(cache_id, cache_idx, bitmap, c.stamp);
		// xfree(celldata);
		return true;
	}

	/* Store a bitmap in the persistent cache */
	static boolean pstcache_put_bitmap(State state, int cache_id, int cache_idx, byte[] bitmap_id, int width, int height,
			int length, byte[] data) throws IOException {
		logger.info("PstCache.pstcache_put_bitmap");
		FileOutputStream fd;
		CELLHEADER cellhdr = new CELLHEADER();
		if (!IS_PERSISTENT(cache_id) || cache_idx >= Rdp.BMPCACHE2_NUM_PSTCELLS)
			return false;
		cellhdr.bitmap_id = bitmap_id;
		// memcpy(cellhdr.bitmap_id, bitmap_id, 8/* sizeof(BITMAP_ID) */);
		cellhdr.width = width;
		cellhdr.height = height;
		cellhdr.length = length;
		cellhdr.stamp = 0;
		fd = new FileOutputStream(g_pstcache_fd[cache_id]);
		int offset = cache_idx * (state.getBytesPerPixel() * MAX_CELL_SIZE + CELLHEADER.size());
		fd.write(cellhdr.toBytes(), offset, CELLHEADER.size());
		fd.write(data);
		// rd_lseek_file(fd, cache_idx * (g_pstcache_Bpp * MAX_CELL_SIZE +
		// sizeof(CELLHEADER)));
		// rd_write_file(fd, &cellhdr, sizeof(CELLHEADER));
		// rd_write_file(fd, data, length);
		return true;
	}

	private static byte[] toBigEndian32(int value) {
		byte[] out = new byte[4];
		out[0] = (byte) (value & 0xFF);
		out[1] = (byte) (value & 0xFF00);
		out[2] = (byte) (value & 0xFF0000);
		out[3] = (byte) (value & 0xFF000000);
		return out;
	}
}

/* Header for an entry in the persistent bitmap cache file */
class CELLHEADER {
	byte[] bitmap_id = new byte[8]; // int8 *
	int length; // int16
	int stamp; // int32
	int width, height; // int8

	public CELLHEADER() {
	}

	public CELLHEADER(byte[] data) {
		for (int i = 0; i < bitmap_id.length; i++)
			bitmap_id[i] = data[i];
		width = data[bitmap_id.length];
		height = data[bitmap_id.length + 1];
		length = (data[bitmap_id.length + 2] >> 8) + data[bitmap_id.length + 3];
		stamp = (data[bitmap_id.length + 6] >> 24) + (data[bitmap_id.length + 6] >> 16) + (data[bitmap_id.length + 6] >> 8)
				+ data[bitmap_id.length + 7];
	}

	public byte[] toBytes() {
		return null;
	}

	static int size() {
		return 8 * 8 + 8 * 2 + 16 + 32;
	}
}
