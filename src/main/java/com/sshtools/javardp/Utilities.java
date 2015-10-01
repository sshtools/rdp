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
	 * Split a string into segments separated by a specified substring
	 * 
	 * @param in Input string
	 * @param splitWith String with which to split input string
	 * @return Array of separated string segments
	 */
	public static String[] split(String in, String splitWith) {
		return in.split(splitWith);
	}

}
