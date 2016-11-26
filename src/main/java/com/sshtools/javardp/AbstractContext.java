package com.sshtools.javardp;

public abstract class AbstractContext implements IContext {
	private Secure secure;
	private MCS mcs;
	private Rdp rdp;
	private boolean underApplet;
	private boolean readyToSend;

	public boolean isReadyToSend() {
		return readyToSend;
	}

	public void setReadyToSend() {
		readyToSend = true;
	}

	public void setSecure(Secure secure) {
		this.secure = secure;
	}

	public Secure getSecure() {
		return secure;
	}

	public void setMcs(MCS mcs) {
		this.mcs = mcs;
	}

	public MCS getMcs() {
		return mcs;
	}

	public void setRdp(Rdp rdp) {
		this.rdp = rdp;
	}

	public Rdp getRdp() {
		return rdp;
	}

	public void setUnderApplet(boolean underApplet) {
		this.underApplet = underApplet;
	}

	public boolean isUnderApplet() {
		return underApplet;
	}

	public abstract void exit();

}
