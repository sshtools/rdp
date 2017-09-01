package com.sshtools.javardp;

import com.sshtools.javardp.layers.MCS;
import com.sshtools.javardp.layers.Secure;

public abstract class AbstractContext implements IContext {
	private MCS mcs;
	private Rdp rdp;
	private boolean readyToSend;
	private Secure secure;
	private boolean underApplet;

	@Override
	public abstract void exit();

	@Override
	public boolean getLockingKeyState(int vk) {
		return false;
	}

	@Override
	public MCS getMcs() {
		return mcs;
	}

	@Override
	public Rdp getRdp() {
		return rdp;
	}

	@Override
	public Secure getSecure() {
		return secure;
	}

	@Override
	public boolean isReadyToSend() {
		return readyToSend;
	}

	@Override
	public boolean isUnderApplet() {
		return underApplet;
	}

	@Override
	public void setMcs(MCS mcs) {
		this.mcs = mcs;
	}

	@Override
	public void setRdp(Rdp rdp) {
		this.rdp = rdp;
	}

	@Override
	public void setReadyToSend() {
		readyToSend = true;
	}

	@Override
	public void setSecure(Secure secure) {
		this.secure = secure;
	}

	public void setUnderApplet(boolean underApplet) {
		this.underApplet = underApplet;
	}

}
