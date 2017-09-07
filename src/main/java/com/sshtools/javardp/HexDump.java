/* HexDump.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Manages debug information for all data
 *          sent and received, outputting in hex format
 */
package com.sshtools.javardp;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HexDump {
	static Logger logger = LoggerFactory.getLogger(HexDump.class);

	/**
	 * Encode data as hex and output as a string along with supplied custom
	 * message
	 * 
	 * @param data Array of byte data to be encoded
	 * @param msg Message to include with outputted hex debug messages
	 * @return string
	 */
	public static String dump(byte[] data, String msg/* PrintStream out */) {
		int count = 0;
		StringBuilder index = new StringBuilder();
		String number;
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		pw.println(msg);
		StringBuilder txt = new StringBuilder();
		int v;
		while (count < data.length) {
			index.append(Integer.toHexString(count));
			switch (index.length()) {
			case (1):
				index.insert(0, "0000");
				break;
			case (2):
				index.insert(0, "000");
				break;
			case (3):
				index.insert(0, "00");
				break;
			case (4):
				index.insert(0, "0");
				break;
			case (5):
				break;
			default:
				return sw.toString();
			}
			index.append(": ");
			for (int i = 0; i < 16; i++) {
				if (count >= data.length) {
					for (int j = i; j < 16; j++) {
						index.append("   ");
						txt.append(' ');
					}
					break;
				}
				v = data[count] & 0x000000ff;
				number = Integer.toHexString(v);
				switch (number.length()) {
				case (1):
					number = "0".concat(number);
					break;
				case (2):
					break;
				default:
					index.append(" |");
					index.append(txt);
					index.append("|");
					pw.println(index);
					return sw.toString();
				}
				index.append(number + " ");
				txt.append(Character.isISOControl((char) v) ? '.' : (char) v);
				count++;
			}
			index.append(" |");
			index.append(txt);
			index.append("|");
			pw.println(index);
			index.setLength(0);
			txt.setLength(0);
		}
		return sw.toString();
	}

	/**
	 * Encode data as hex and output as debug messages along with supplied
	 * custom message
	 * 
	 * @param data Array of byte data to be encoded
	 * @param msg Message to include with outputted hex debug messages
	 */
	public static void encode(byte[] data, String msg) {
		logger.debug(dump(data, msg));
	}
}
