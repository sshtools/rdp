/* LicenceStore.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Handle saving and loading of licences
 */
package com.sshtools.javardp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class LicenceStore {
	static Log logger = LogFactory.getLog(Licence.class);
	protected Options options;

	public LicenceStore(Options options) {
		this.options = options;
	}

	/**
	 * Load a licence from a file
	 * 
	 * @return Licence data stored in file
	 */
	public byte[] load_licence() {
		String path = options.licence_path + "/licence." + options.hostname;
		byte[] data = null;
		try {
			FileInputStream fd = new FileInputStream(path);
			data = new byte[fd.available()];
			fd.read(data);
		} catch (FileNotFoundException e) {
			logger.warn("Licence file not found!");
		} catch (IOException e) {
			logger.warn("IOException in load_licence");
		}
		return data;
	}

	/**
	 * Save a licence to file
	 * 
	 * @param databytes Licence data to store
	 */
	public void save_licence(byte[] databytes) {
		/* set and create the directory -- if it doesn't exist. */
		// String home = "/root";
		String dirpath = options.licence_path;// home+"/.rdesktop";
		String filepath = dirpath + "/licence." + options.hostname;
		File file = new File(dirpath);
		file.mkdir();
		try {
			FileOutputStream fd = new FileOutputStream(filepath);
			/* write to the licence file */
			fd.write(databytes);
			fd.close();
			logger.info("Stored licence at " + filepath);
		} catch (FileNotFoundException e) {
			logger.warn("save_licence: file path not valid!");
		} catch (IOException e) {
			logger.warn("IOException in save_licence");
		}
	}
}
