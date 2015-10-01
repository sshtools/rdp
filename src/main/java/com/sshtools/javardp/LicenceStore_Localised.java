/* LicenceStore_Localised.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Java 1.4 specific extension of LicenceStore class
 */
// Created on 05-Aug-2003

package com.sshtools.javardp;

import java.util.prefs.Preferences;

public class LicenceStore_Localised extends LicenceStore {

	public LicenceStore_Localised(Options options) {
		super(options);
	}

	public byte[] load_licence() {
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		return prefs.getByteArray("licence." + options.hostname, null);

	}

	public void save_licence(byte[] databytes) {
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		prefs.putByteArray("licence." + options.hostname, databytes);
	}

}
