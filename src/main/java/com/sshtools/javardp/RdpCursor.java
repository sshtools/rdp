package com.sshtools.javardp;

import java.awt.Image;
import java.awt.Point;

public class RdpCursor {

	private Point hotspot;
	private String name;
	private Image data;

	public RdpCursor(Point hotspot, String name, Image data) {
		super();
		this.hotspot = hotspot;
		this.name = name;
		this.data = data;
	}

	public Point getHotspot() {
		return hotspot;
	}

	public void setHotspot(Point hotspot) {
		this.hotspot = hotspot;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Image getData() {
		return data;
	}

	public void setData(Image data) {
		this.data = data;
	}

}
