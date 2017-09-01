/* DataBlob.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Encapsulates Blobs of byte data, of
 *          arbitrary size (although data cannot
 *          be changed once set)
 */
package com.sshtools.javardp;

public class DataBlob {

	private byte[] data = null;
	private int size = 0;

	/**
	 * Construct a DataBlob with a givne size and content. Once constructed, the
	 * DataBlob cannot be modified
	 * 
	 * @param size Size of data
	 * @param data Array of byte data to store in blob
	 */
	public DataBlob(int size, byte[] data) {
		this.size = size;
		this.data = data;
	}

	/**
	 * Retrieve data stored in this DataBlob
	 * 
	 * @return Stored data
	 */
	public byte[] getData() {
		return this.data;
	}

	/**
	 * Retrieve size of data stored in this DataBlob
	 * 
	 * @return Size of stored data
	 */
	public int getSize() {
		return this.size;
	}
}
