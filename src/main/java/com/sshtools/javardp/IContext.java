package com.sshtools.javardp;

import java.io.IOException;

public interface IContext {
	void dispose();

	void error(Exception e, boolean dispose);

	byte[] loadLicense() throws IOException;

	void saveLicense(byte[] license) throws IOException;

	void screenResized(int width, int height, boolean clientInitiated);

	void setLoggedOn();

	void toggleFullScreen();

	void readyToSend();
}