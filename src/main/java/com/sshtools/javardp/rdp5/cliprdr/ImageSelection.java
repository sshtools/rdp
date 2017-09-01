/* ImageSelection.java
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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import com.sshtools.javardp.Utilities;

public class ImageSelection implements Transferable {
	// the Image object which will be housed by the ImageSelection
	private Image image;

	public ImageSelection(Image image) {
		this.image = image;
	}

	// Returns Image object housed by Transferable object
	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (!Utilities.imageFlavor.equals(flavor)) {
			throw new UnsupportedFlavorException(flavor);
		}
		// else return the payload
		return image;
	}

	// Returns the supported flavors of our implementation
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { Utilities.imageFlavor };
	}

	// Returns true if flavor is supported
	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return Utilities.imageFlavor.equals(flavor);
	}
}
