package com.sshtools.javardp;

public interface IContext {
	Rdp getRdp();

	void setRdp(Rdp rdp);

	void dispose();

	boolean isUnderApplet();

	Secure getSecure();
	
	void init(RdesktopCanvas canvas);

	void exit();

	void setSecure(Secure secureLayer);

	void setMcs(MCS mcsLayer);

	void triggerReadyToSend();

	MCS getMcs();

	void registerDrawingSurface();

	void hideMenu();

	void setReadyToSend();

	void setLoggedOn();

	boolean isReadyToSend();
	
	void error(Exception e, boolean sysexit);

	void screenResized(int width, int height, boolean clientInitiated);

	void toggleFullScreen();
}