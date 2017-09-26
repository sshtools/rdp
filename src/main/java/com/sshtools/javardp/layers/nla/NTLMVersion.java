package com.sshtools.javardp.layers.nla;

import java.io.IOException;

import com.sshtools.javardp.Packet;

public class NTLMVersion implements PacketPayload {
	public static final int NTLMSSP_REVISION_W2K3 = 0x0F;
	public static final int WINDOWS_MAJOR_VERSION_5 = 0x05;
	public static final int WINDOWS_MAJOR_VERSION_6 = 0x06;
	public static final int WINDOWS_MAJOR_VERSION_10 = 0x0A;
	public static final int WINDOWS_MINOR_VERSION_0 = 0x00;
	public static final int WINDOWS_MINOR_VERSION_1 = 0x01;
	public static final int WINDOWS_MINOR_VERSION_2 = 0x02;
	public static final int WINDOWS_MINOR_VERSION_3 = 0x03;
	
	private int major = 6;
	private int minor = 1;
	private int build = 7601;
	private int revision = 15;

	public NTLMVersion() {
	}
	
	public NTLMVersion(int major, int minor, int build, int revision) {
		super();
		this.major = major;
		this.minor = minor;
		this.build = build;
		this.revision = revision;
	}

	@Override
	public Packet write() throws IOException {
		Packet p = new Packet(8);
		p.set8(major);
		p.set8(minor);
		p.setLittleEndian16(build);
		p.fill(3);
		p.set8(revision);
		return p;
	}

	@Override
	public void read(Packet packet) throws IOException {
		major = packet.get8();
		minor = packet.get8();
		build = packet.getLittleEndian16();
		packet.incrementPosition(3);
		revision = packet.get8();
	}

	public int getMajor() {
		return major;
	}

	public void setMajor(int major) {
		this.major = major;
	}

	public int getMinor() {
		return minor;
	}

	public void setMinor(int minor) {
		this.minor = minor;
	}

	public int getBuild() {
		return build;
	}

	public void setBuild(int build) {
		this.build = build;
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}

	@Override
	public String toString() {
		return "NTLMVersion [major=" + major + ", minor=" + minor + ", build=" + build + ", revision=" + revision + "]";
	}
}