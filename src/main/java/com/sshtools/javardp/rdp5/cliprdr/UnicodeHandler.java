/* UnicodeHandler.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: 
 */
package com.sshtools.javardp.rdp5.cliprdr;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import com.sshtools.javardp.RdpPacket;
import com.sshtools.javardp.Utilities;

public class UnicodeHandler extends TypeHandler {

	public boolean formatValid(int format) {
		return (format == CF_UNICODETEXT);
	}

	public boolean mimeTypeValid(String mimeType) {
		return mimeType.equals("text");
	}

	public int preferredFormat() {
		return CF_UNICODETEXT;
	}

	public void handleData(RdpPacket data, int length, ClipInterface c) {
		String thingy = "";
		for (int i = 0; i < length; i += 2) {
			int aByte = data.getLittleEndian16();
			if (aByte != 0)
				thingy += (char) (aByte);
		}
		c.copyToClipboard(new StringSelection(thingy));
		// return(new StringSelection(thingy));
	}

	public String name() {
		return "CF_UNICODETEXT";
	}

	public byte[] fromTransferable(Transferable in) {
		String s;
		if (in != null) {
			try {
				s = (String) (in.getTransferData(DataFlavor.stringFlavor));
			} catch (Exception e) {
				s = e.toString();
			}

			// TODO: think of a better way of fixing this
			s = s.replace('\n', (char) 0x0a);
			// s = s.replaceAll("" + (char) 0x0a, "" + (char) 0x0d + (char)
			// 0x0a);
			s = Utilities.strReplaceAll(s, "" + (char) 0x0a, "" + (char) 0x0d + (char) 0x0a);
			byte[] sBytes = s.getBytes();
			int length = sBytes.length;
			int lengthBy2 = length * 2;
			RdpPacket p = new RdpPacket(lengthBy2);
			for (int i = 0; i < sBytes.length; i++) {
				p.setLittleEndian16(sBytes[i]);
			}
			sBytes = new byte[length * 2];
			p.copyToByteArray(sBytes, 0, 0, lengthBy2);
			return sBytes;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.elusiva.rdp.rdp5.cliprdr.TypeHandler#send_data(java.awt.datatransfer
	 * .Transferable)
	 */
	public void send_data(Transferable in, ClipInterface c) {
		byte[] data = fromTransferable(in);
		c.send_data(data, data.length);
	}

}
