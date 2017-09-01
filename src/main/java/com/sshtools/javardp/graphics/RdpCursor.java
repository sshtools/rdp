package com.sshtools.javardp.graphics;

import java.awt.Image;
import java.awt.Point;

public class RdpCursor {

	private Image data;
	private Point hotspot;
	private String name;

	public RdpCursor(Point hotspot, String name, Image data) {
		super();
		this.hotspot = hotspot;
		this.name = name;
		this.data = data;
	}

	public Image getData() {
		return data;
	}

	public Point getHotspot() {
		return hotspot;
	}

	public String getName() {
		return name;
	}

	public void setData(Image data) {
		this.data = data;
	}

	public void setHotspot(Point hotspot) {
		this.hotspot = hotspot;
	}

	public void setName(String name) {
		this.name = name;
	}

}
