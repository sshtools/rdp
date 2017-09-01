package com.sshtools.javardp.rdp5.display;

import com.sshtools.javardp.RdpPacket;

public class MonitorLayout {
	public final static int ORIENTATION_LANDSCAPE = 0;
	public final static int ORIENTATION_LANDSCAPE_FLIPPED = 180;
	public final static int ORIENTATION_PORTRAIT = 90;
	public final static int ORIENTATION_PORTRAIT_FLIPPED = 270;
	private int desktopScale = 100;
	private int deviceScale = 100;
	private int height;
	private int left;
	private int orientation = ORIENTATION_LANDSCAPE;
	private int physicalHeight;
	private int physicalWidth;
	private boolean primary;
	private int top;
	private int width;

	public MonitorLayout(boolean primary, int left, int top, int width, int height) {
		super();
		this.primary = primary;
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
	}

	public long getArea() {
		return width * height;
	}

	public int getDesktopScale() {
		return desktopScale;
	}

	public int getDeviceScale() {
		return deviceScale;
	}

	public int getHeight() {
		return height;
	}

	public int getLeft() {
		return left;
	}

	public int getOrientation() {
		return orientation;
	}

	public int getPhysicalHeight() {
		return physicalHeight;
	}

	public int getPhysicalWidth() {
		return physicalWidth;
	}

	public int getTop() {
		return top;
	}

	public int getWidth() {
		return width;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setDesktopScale(int desktopScale) {
		this.desktopScale = desktopScale;
	}

	public void setDeviceScale(int deviceScale) {
		this.deviceScale = deviceScale;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setLeft(int left) {
		this.left = left;
	}

	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}

	public void setPhysicalHeight(int physicalHeight) {
		this.physicalHeight = physicalHeight;
	}

	public void setPhysicalWidth(int physicalWidth) {
		this.physicalWidth = physicalWidth;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public void setTop(int top) {
		this.top = top;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void writer(RdpPacket p) {
		p.setLittleEndian32(left);
		p.setLittleEndian32(top);
		p.setLittleEndian32(width);
		p.setLittleEndian32(height);
		p.setLittleEndian32(physicalWidth);
		p.setLittleEndian32(physicalHeight);
		p.setLittleEndian32(orientation);
		p.setLittleEndian32(desktopScale);
		p.setLittleEndian32(deviceScale);
	}
}
