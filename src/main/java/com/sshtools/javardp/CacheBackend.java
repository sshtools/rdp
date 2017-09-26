package com.sshtools.javardp;

import java.io.IOException;

public interface CacheBackend {
	void start(State state);
	
	boolean init(int cache_id);

	boolean pstcache_put_bitmap(State state, int cache_id, int cache_idx, byte[] bitmap_id, int width, int height, int length,
			byte[] data) throws IOException;

	boolean IS_PERSISTENT(int id);

	void touchBitmap(int cache_id, int cache_idx, int stamp);

	int nextSeq();
}
