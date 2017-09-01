package com.sshtools.javardp;

import java.io.IOException;

import com.sshtools.javardp.graphics.RdesktopCanvas;
import com.sshtools.javardp.layers.MCS;
import com.sshtools.javardp.layers.Secure;

public interface IContext {
	void dispose();

	void error(Exception e, boolean sysexit);

	void exit();

	boolean getLockingKeyState(int vk);

	MCS getMcs();

	Rdp getRdp();

	Secure getSecure();

	void hideMenu();

	void init(RdesktopCanvas canvas);

	boolean isReadyToSend();

	boolean isUnderApplet();

	byte[] loadLicense() throws IOException;

	void registerDrawingSurface();

	void saveLicense(byte[] license) throws IOException;

	void screenResized(int width, int height, boolean clientInitiated);

	void setLoggedOn();

	void setMcs(MCS mcsLayer);

	void setRdp(Rdp rdp);

	void setReadyToSend();

	void setSecure(Secure secureLayer);

	void toggleFullScreen();

	void triggerReadyToSend();
}