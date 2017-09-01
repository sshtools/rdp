/* BMPToImageThread.java
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

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import com.sshtools.javardp.RdpPacket;

public class BMPToImageThread extends Thread {

	ClipInterface c;
	RdpPacket data;
	int length;

	public BMPToImageThread(RdpPacket data, int length, ClipInterface c) {
		super();
		this.data = data;
		this.length = length;
		this.c = c;
	}

	@Override
	public void run() {
		String thingy = "";
		OutputStream out = null;

		int origin = data.getPosition();

		int head_len = data.getLittleEndian32();

		data.setPosition(origin);

		byte[] content = new byte[length];

		for (int i = 0; i < length; i++) {
			content[i] = (byte) (data.get8() & 0xFF);
		}

		Image img = ClipBMP.loadbitmap(new ByteArrayInputStream(content));
		ImageSelection imageSelection = new ImageSelection(img);
		c.copyToClipboard(imageSelection);
	}

}
