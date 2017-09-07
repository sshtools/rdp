/* Utilities.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Provide replacements for useful methods that were unavailable prior to
 *          Java 1.4 (Java 1.1 compliant).
 */

package com.sshtools.javardp;

import java.awt.datatransfer.DataFlavor;

public class Utilities {

	public static DataFlavor imageFlavor = DataFlavor.imageFlavor;

	/**
	 * Split a string into segments separated by a specified substring
	 * 
	 * @param in Input string
	 * @param splitWith String with which to split input string
	 * @return Array of separated string segments
	 */
	public static String[] split(String in, String splitWith) {
		return in.split(splitWith);
	}

	/**
	 * Replaces each substring of this string that matches the given regular
	 * expression with the given replacement.
	 * 
	 * @param in Input string
	 * @param regex Regular expression describing patterns to match within input
	 *            string
	 * @param replace Patterns matching regular expression within input are
	 *            replaced with this string
	 * @return
	 */
	public static String strReplaceAll(String in, String find, String replace) {
		return in.replaceAll(find, replace);
	}
	
	/**
	 * Return a byte array trimmed to a maximum size. If the array is smaller than
	 * the requested length, it will NOT be padded
	 * 
	 * @param arr array of bytes
	 * @param len maximum length.
	 */
	public static byte[] trimBytes(byte[] arr, int len) {
		if(arr.length <= len)
			return arr;
		byte[] a = new byte[len];
		System.arraycopy(arr, 0, a, 0, len);
		return a;
	}
	
	/**
	 * Return a byte array trimmed to a maximum size. If the array is smaller than
	 * the requested length, it will be padded with zeroes
	 * 
	 * @param arr array of bytes
	 * @param len maximum length.
	 */
	public static byte[] padBytes(byte[] arr, int len) {
		byte[] a = new byte[len];
		System.arraycopy(arr, 0, a, 0, Math.min(len, arr.length));
		return a;
	}
	
	/**
	 * Concatenate byte arrays into a larger array.
	 * 
	 * @param arrs arrays
	 * @return concatenated array
	 */
	public static byte[] concatenateBytes(byte[]... arrs) {
		int l = 0;
		for(byte[] b : arrs)
			l += b.length;
		byte[] a = new byte[l];
		l = 0;
		for(byte[] b : arrs) {
			System.arraycopy(b, 0, a, l, b.length);
			l += b.length;
		}
		return a;
	}

	/**
	 * Create a 4-byte byte array representing the 32 bit value in little endian format.
	 * 
	 * @param val value
	 * @return bytes
	 */
	public static byte[] intToBytes(int val) {
		return new byte[] { (byte) (val & 0xFF), (byte) (val >> 8 & 0xFF), (byte) (val >> 16 & 0xFF), (byte) (val >> 24 & 0xFF) };
	}
}
