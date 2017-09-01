package com.sshtools.javardp;

import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.io.File;
import java.net.InetAddress;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class State {
	final static Logger LOG = LoggerFactory.getLogger(State.class);
	private String clientName;
	private ColorModel colorModel = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);
	private boolean colorPointer = true;
	private int colorPointerCacheSize = 20;
	private SecurityType securityType = SecurityType.STANDARD;
	private int height;
	private boolean negotiated;
	private Options options;
	private int pointerCacheSize = 20;
	private boolean rdp5;
	private int serverBpp = 24;
	private int serverRdpVersion;
	private int width;
	private boolean licenceIssued = false;
	private boolean readCert = false;
	private int serverChannelId;
	private int shareId;
	private boolean active;
	private int lastReason;
	private String clientIp;
	private int clientAddressFamily;
	private String clientDir;

	public State(Options options) {
		this.options = options;
		/*
		 * Work out client IP / name to use. It may be configured, or
		 * automatically detected (default)
		 */
		try {
			clientName = options.getClientName();
			if (StringUtils.isBlank(clientName))
				clientName = new StringTokenizer(InetAddress.getLocalHost().getHostName(), ".").nextToken().trim();
		} catch (Exception e) {
		}
		try {
			clientIp = options.getClientIp();
			if (StringUtils.isBlank(clientIp))
				clientIp = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
		}
		if (clientName == null || clientName.length() == 0)
			clientName = "localhost";
		if (clientIp == null || clientIp.length() == 0)
			clientIp = "127.0.0.1";
		clientName = clientName.toUpperCase();
		
		/* Client dir */
		clientDir = options.getClientDir();
		if (StringUtils.isBlank(clientDir)) {
			if (SystemUtils.IS_OS_WINDOWS)
				clientDir = System.getProperty("user.dir");
			else
				clientDir = "C:" + System.getProperty("user.dir").replace(File.separator, "\\");
		}
		/* Client address family */
		clientAddressFamily = options.getClientAddressFamily();
		if (clientAddressFamily == -1) {
			if (clientIp.matches("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")) {
				clientAddressFamily = Rdp.AF_INET;
			} else {
				clientAddressFamily = Rdp.AF_INET6;
			}
		}
		/* Initial state. */
		width = options.getWidth();
		height = options.getHeight();
		serverBpp = options.getBpp();
		if(options.isSsl()) {
			//securityType = SecurityType.HYBRID;
			securityType = SecurityType.SSL;
		}
		this.rdp5 = options.isRdp5();
	}

	public String getClientDir() {
		return clientDir;
	}

	public void setClientDir(String clientDir) {
		this.clientDir = clientDir;
	}

	public int getClientAddressFamily() {
		return clientAddressFamily;
	}

	public void setClientAddressFamily(int clientAddressFamily) {
		this.clientAddressFamily = clientAddressFamily;
	}

	public String getClientIp() {
		return clientIp;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getServerChannelId() {
		return serverChannelId;
	}

	public void setServerChannelId(int serverChannelId) {
		this.serverChannelId = serverChannelId;
	}

	public boolean isReadCert() {
		return readCert;
	}

	public void setReadCert(boolean readCert) {
		this.readCert = readCert;
	}

	public boolean isLicenceIssued() {
		return licenceIssued;
	}

	public void setLicenceIssued(boolean licenceIssued) {
		this.licenceIssued = licenceIssued;
	}

	public int getByteMask() {
		// public final int bpp_mask = 0xFFFFFF >> 8 * (3 - Bpp); // Correction
		// value to
		// // ensure only the
		// // relevant
		if (serverBpp == 8)
			return 0xFF;
		else
			return 0xFFFFFF;
	}

	public int getBytesPerPixel() {
		return (serverBpp + 7) / 8;
	}

	public String getClientName() {
		return clientName;
	}

	public ColorModel getColorModel() {
		return colorModel;
	}

	public int getColorPointerCacheSize() {
		return colorPointerCacheSize;
	}

	public int getHeight() {
		return height;
	}

	public Options getOptions() {
		return options;
	}

	public int getPointerCacheSize() {
		return pointerCacheSize;
	}

	public int getServerBpp() {
		return serverBpp;
	}

	public int getServerRdpVersion() {
		return serverRdpVersion;
	}

	public int getWidth() {
		return width;
	}

	public boolean isColorPointer() {
		return colorPointer;
	}

	public SecurityType getSecurityType() {
		return securityType;
	}

	public boolean isNegotiated() {
		return negotiated;
	}

	public boolean isRDP5() {
		return rdp5;
	}

	public void setColorPointer(boolean colorPointer) {
		this.colorPointer = colorPointer;
	}

	public void setColorPointerCacheSize(int colorPointerCacheSize) {
		this.colorPointerCacheSize = colorPointerCacheSize;
	}

	public void setSecurityType(SecurityType securityType) {
		this.securityType = securityType;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setNegotiated() {
		this.negotiated = true;
	}

	public void setPointerCacheSize(int pointerCacheSize) {
		this.pointerCacheSize = pointerCacheSize;
	}

	public void setRDP5(boolean rdp5) {
		this.rdp5 = rdp5;
	}

	public void setServerBpp(int serverBpp) {
		this.serverBpp = serverBpp;
		colorModel = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);
	}

	public void setServerRdpVersion(int serverRdpVersion) {
		this.serverRdpVersion = serverRdpVersion;
		if (serverRdpVersion == 1) {
			LOG.info("Switching to RDP4 because of server version.");
			rdp5 = false;
		}
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getShareId() {
		return shareId;
	}

	public void setShareId(int shareId) {
		this.shareId = shareId;
	}

	public int getLastReason() {
		return lastReason;
	}

	public void setLastReason(int lastReason) {
		this.lastReason = lastReason;
	}
}
