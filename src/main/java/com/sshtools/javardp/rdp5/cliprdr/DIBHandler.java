/* DIBHandler.java
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

import java.awt.Frame;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.ImageObserver;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sshtools.javardp.IContext;
import com.sshtools.javardp.Input;
import com.sshtools.javardp.RdpPacket;
import com.sshtools.javardp.Utilities;

public class DIBHandler extends TypeHandler implements ImageObserver {
	protected static Log logger = LogFactory.getLog(Input.class);
	private IContext context;

	public DIBHandler(IContext context) {
		this.context = context;
	}

	public boolean formatValid(int format) {
		return (format == CF_DIB);
	}

	public boolean mimeTypeValid(String mimeType) {
		return mimeType.equals("image");
	}

	public int preferredFormat() {
		return CF_DIB;
	}

	public String name() {
		return "CF_DIB";
	}

	public void handleData(RdpPacket data, int length, ClipInterface c) {
		// System.out.println("DIBHandler.handleData");
		BMPToImageThread t = new BMPToImageThread(data, length, c);
		t.start();
	}

	public void send_data(Transferable in, ClipInterface c) {
		byte[] out = null;
		try {
			if (in != null && in.isDataFlavorSupported(Utilities.imageFlavor)) {
				Image img = (Image) in.getTransferData(Utilities.imageFlavor);
				ClipBMP b = new ClipBMP();
				MediaTracker mediaTracker = new MediaTracker(new Frame());
				mediaTracker.addImage(img, 0);
				try {
					mediaTracker.waitForID(0);
				} catch (InterruptedException ie) {
					System.err.println(ie);
					if (!context.isUnderApplet())
						System.exit(1);
				}
				if (img == null)
					return;
				int width = img.getWidth(this);
				int height = img.getHeight(this);
				out = b.getBitmapAsBytes(img, width, height);
				c.send_data(out, out.length);
			}
		} catch (UnsupportedFlavorException e) {
			System.err.println("Failed to send DIB: UnsupportedFlavorException");
		} catch (IOException e) {
			System.err.println("Failed to send DIB: IOException");
		}
	}

	public boolean imageUpdate(Image arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
		return false;
	}
}
