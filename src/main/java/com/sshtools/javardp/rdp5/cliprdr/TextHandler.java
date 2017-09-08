/* TextHandler.java
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
import java.io.IOException;

import com.sshtools.javardp.Packet;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.Utilities;

public class TextHandler extends TypeHandler {
	@Override
	public boolean formatValid(int format) {
		return (format == CF_TEXT);
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
			return s.getBytes();
		}
		return null;
	}

	public Transferable handleData(Packet data, int length) {
		String thingy = "";
		for (int i = 0; i < length; i++) {
			int aByte = data.get8();
			if (aByte != 0)
				thingy += (char) (aByte & 0xFF);
		}
		return (new StringSelection(thingy));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.elusiva.rdp.rdp5.cliprdr.TypeHandler#handleData(com.elusiva.rdp.
	 * RdpPacket , int, com.elusiva.rdp.rdp5.cliprdr.ClipInterface)
	 */
	@Override
	public void handleData(Packet data, int length, ClipInterface c) {
		String thingy = "";
		for (int i = 0; i < length; i++) {
			int aByte = data.get8();
			if (aByte != 0)
				thingy += (char) (aByte & 0xFF);
		}
		c.copyToClipboard(new StringSelection(thingy));
	}

	@Override
	public boolean mimeTypeValid(String mimeType) {
		return mimeType.equals("text");
	}

	@Override
	public String name() {
		return "CF_TEXT";
	}

	@Override
	public int preferredFormat() {
		return CF_TEXT;
	}

	@Override
	public void send_data(Transferable in, ClipInterface c) throws RdesktopException, IOException {
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
			// return s.getBytes();
			c.send_data(s.getBytes(), s.length());
		}
	}
}
