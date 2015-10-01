package com.sshtools.javardp;

import com.sshtools.javardp.rdp5.Rdp5;

public abstract class AbstractContext implements IContext {
	private Secure secure;
	private MCS mcs;
	private Rdp5 rdp;
	private boolean underApplet;

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

	public void setRdp(Rdp5 rdp) {
		this.rdp = rdp;
	}

	public Rdp5 getRdp() {
		return rdp;
	}

	public void setUnderApplet(boolean underApplet) {
		this.underApplet = underApplet;
	}

	public boolean isUnderApplet() {
		return underApplet;
	}

	public abstract void exit();

	public abstract void triggerReadyToSend();
}
