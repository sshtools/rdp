/*
 * Options.java Component: ProperJavaRDP Revision: $Revision: 1.1 $ Author:
 * $Author: brett $ Date: $Date: 2011/11/28 14:13:42 $ Copyright (c) 2005
 * Propero Limited Purpose: Global storage of user-definable options
 */
package com.sshtools.javardp;

import java.util.TimeZone;

import org.apache.commons.lang3.SystemUtils;

public class Options {
	public final static int BUFFEREDIMAGE_BITMAP_DECOMPRESSION = 1;
	public final static int DIRECT_BITMAP_DECOMPRESSION = 0;
	public final static int INTEGER_BITMAP_DECOMPRESSION = 2;
	// public boolean paste_hack = true;
	private boolean altkeyQuiet = false;
	private boolean bitmapCaching = false;
	private int bitmapDecompressionStore = INTEGER_BITMAP_DECOMPRESSION;
	private int bpp = 16;
	private boolean builtInLicence = false;
	private boolean capsSendsUpAndDown = true;
	private String clientName = ""; // -n hostname
	private String command = ""; // -s command
	private int connectionType = Rdp.CONNECTION_TYPE_WAN;
	private boolean consoleSession = false;
	private boolean debugHexdump = false;
	private String directory = ""; // -d directory
	private String domain = ""; // -d domain
	private boolean fullscreen = false;
	private int height = 0; // -g widthxheight
	private boolean hideDecorations = false;
	// packets
	private int keylayout = 0x809; // UK by default
	private boolean loadLicence = false;
	private boolean lowLatency = true; // disables bandwidth saving tcp
	private boolean mapClipboard = true;
	private boolean orders = true;
	private boolean owncolmap = false;
	private boolean packetEncryption = true;
	private char[] password = new char[0]; // -p password
	private boolean persistentBitmapCaching = false;
	private boolean polygonEllipseOrders = false;
	private boolean precacheBitmaps = false;
	private boolean rdp5 = true;
	private int rdp5PerformanceFlags = Rdp.RDP5_NO_CURSOR_SHADOW | Rdp.RDP5_NO_CURSORSETTINGS | Rdp.RDP5_NO_FULLWINDOWDRAG
			| Rdp.RDP5_NO_MENUANIMATIONS | Rdp.RDP5_NO_THEMING | Rdp.RDP5_NO_WALLPAPER;
	private boolean remapHash = true;
	private boolean saveLicence = false;
	private boolean sendmotion = true;
	private boolean ssl = false;
	private String username = "Administrator"; // -u username
	private int width = 0; // -g widthxheight
	// number of bytes are used for a pixel
	private int winButtonSize = 0; /* If zero, disable single app mode */
	private String windowTitle = "SSHTools RDP"; // -T windowTitle
	private int osMinor = -1;
	private int osMajor = -1;
	private boolean desktopSave;
	private String clientIp = "";
	private int clientAddressFamily = -1;
	private String clientDir;
	private TimeZone timeZone = TimeZone.getDefault();

	public TimeZone getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	public String getClientDir() {
		return clientDir;
	}

	public void setClientDir(String clientDir) {
		this.clientDir = clientDir;
	}

	public String getClientIp() {
		return clientIp;
	}

	public int getClientAddressFamily() {
		return clientAddressFamily;
	}

	public void setClientAddressFamily(int clientAddressFamily) {
		switch(clientAddressFamily) {
		case -1:
		case Rdp.AF_INET:
		case Rdp.AF_INET6:
			this.clientAddressFamily = clientAddressFamily;
		default:
			throw new IllegalArgumentException(String.format("Must be one of AF_INET(0x%x), AF_INET6(0x%x) or AUTO(-1)", Rdp.AF_INET, Rdp.AF_INET6));
		}
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public int getBitmapDecompressionStore() {
		return bitmapDecompressionStore;
	}

	public int getBpp() {
		return bpp;
	}

	public String getClientName() {
		return clientName;
	}

	public String getCommand() {
		return command;
	}

	public int getConnectionType() {
		return connectionType;
	}

	public String getDirectory() {
		return directory;
	}

	public String getDomain() {
		return domain;
	}

	public int getHeight() {
		return height;
	}

	public int getKeylayout() {
		return keylayout;
	}

	public char[] getPassword() {
		return password;
	}

	public int getRdp5PerformanceFlags() {
		return rdp5PerformanceFlags;
	}

	public String getUsername() {
		return username;
	}

	public int getWidth() {
		return width;
	}

	public int getWinButtonSize() {
		return winButtonSize;
	}

	public String getWindowTitle() {
		return windowTitle;
	}

	public boolean isAltkeyQuiet() {
		return altkeyQuiet;
	}

	public boolean isBitmapCaching() {
		return bitmapCaching;
	}

	public boolean isBuiltInLicence() {
		return builtInLicence;
	}

	public boolean isCapsSendsUpAndDown() {
		return capsSendsUpAndDown;
	}

	public boolean isConsoleSession() {
		return consoleSession;
	}

	public boolean isDebugHexdump() {
		return debugHexdump;
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

	public boolean isHideDecorations() {
		return hideDecorations;
	}

	public boolean isLoadLicence() {
		return loadLicence;
	}

	public boolean isLowLatency() {
		return lowLatency;
	}

	public boolean isMapClipboard() {
		return mapClipboard;
	}

	public boolean isOrders() {
		return orders;
	}

	public boolean isOwncolmap() {
		return owncolmap;
	}

	public boolean isPacketEncryption() {
		return packetEncryption;
	}

	public boolean isPersistentBitmapCaching() {
		return persistentBitmapCaching;
	}

	public boolean isPolygonEllipseOrders() {
		return polygonEllipseOrders;
	}

	public boolean isPrecacheBitmaps() {
		return precacheBitmaps;
	}

	public boolean isRdp5() {
		return rdp5;
	}

	public boolean isRemapHash() {
		return remapHash;
	}

	public boolean isSaveLicence() {
		return saveLicence;
	}

	public boolean isSendmotion() {
		return sendmotion;
	}

	public boolean isSsl() {
		return ssl;
	}

	public void setAltkeyQuiet(boolean altkeyQuiet) {
		this.altkeyQuiet = altkeyQuiet;
	}

	public void setBitmapCaching(boolean bitmapCaching) {
		this.bitmapCaching = bitmapCaching;
	}

	public void setBitmapDecompressionStore(int bitmapDecompressionStore) {
		this.bitmapDecompressionStore = bitmapDecompressionStore;
	}

	public void setBpp(int bpp) {
		this.bpp = bpp;
	}

	public void setBuiltInLicence(boolean builtInLicence) {
		this.builtInLicence = builtInLicence;
	}

	public void setCapsSendsUpAndDown(boolean capsSendsUpAndDown) {
		this.capsSendsUpAndDown = capsSendsUpAndDown;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public void setConnectionType(int connectionType) {
		this.connectionType = connectionType;
	}

	public void setConsoleSession(boolean consoleSession) {
		this.consoleSession = consoleSession;
	}

	public void setDebugHexdump(boolean debugHexdump) {
		this.debugHexdump = debugHexdump;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setFullscreen(boolean fullscreen) {
		this.fullscreen = fullscreen;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setHideDecorations(boolean hideDecorations) {
		this.hideDecorations = hideDecorations;
	}

	public void setKeylayout(int keylayout) {
		this.keylayout = keylayout;
	}

	public void setLoadLicence(boolean loadLicence) {
		this.loadLicence = loadLicence;
	}

	public void setLowLatency(boolean lowLatency) {
		this.lowLatency = lowLatency;
	}

	public void setMapClipboard(boolean mapClipboard) {
		this.mapClipboard = mapClipboard;
	}

	public void setOrders(boolean orders) {
		this.orders = orders;
	}

	public void setOwncolmap(boolean owncolmap) {
		this.owncolmap = owncolmap;
	}

	public void setPacketEncryption(boolean packetEncryption) {
		this.packetEncryption = packetEncryption;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public void setPersistentBitmapCaching(boolean persistentBitmapCaching) {
		this.persistentBitmapCaching = persistentBitmapCaching;
	}

	public void setPolygonEllipseOrders(boolean polygonEllipseOrders) {
		this.polygonEllipseOrders = polygonEllipseOrders;
	}

	public void setPrecacheBitmaps(boolean precacheBitmaps) {
		this.precacheBitmaps = precacheBitmaps;
	}

	public void setRdp5(boolean rdp5) {
		this.rdp5 = rdp5;
		if (!rdp5)
			bpp = 8;
	}

	public void setRdp5PerformanceFlags(int rdp5PerformanceFlags) {
		this.rdp5PerformanceFlags = rdp5PerformanceFlags;
	}

	public void setRemapHash(boolean remapHash) {
		this.remapHash = remapHash;
	}

	public void setSaveLicence(boolean saveLicence) {
		this.saveLicence = saveLicence;
	}

	public void setSendmotion(boolean sendmotion) {
		this.sendmotion = sendmotion;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setWidth(int width) {
		this.width = width & ~3;
	}

	public void setWinButtonSize(int winButtonSize) {
		this.winButtonSize = winButtonSize;
	}

	public void setWindowTitle(String windowTitle) {
		this.windowTitle = windowTitle;
	}

	public int getOsMinor() {
		if (osMinor == -1) {
			if (SystemUtils.IS_OS_WINDOWS_95)
				return 0x0002;
			else if (SystemUtils.IS_OS_WINDOWS_NT)
				return 0x0003;
			else if (System.getenv("DISPLAY") != null)
				return 0x0007;
			return 0;
		}
		return osMinor;
	}

	public int getOsMajor() {
		if (osMajor == -1) {
			if (SystemUtils.IS_OS_WINDOWS)
				return 0x0001;
			else if (SystemUtils.IS_OS_OS2)
				return 0x0002;
			else if (SystemUtils.IS_OS_MAC_OSX)
				return 0x0006;
			else if (SystemUtils.IS_OS_MAC)
				return 0x0003;
			else if (SystemUtils.IS_OS_UNIX)
				return 0x0008;
			return 0;
		}
		return osMajor;
	}

	public void setOsMinor(int osMinor) {
		this.osMinor = osMinor;
	}

	public void setOsMajor(int osMajor) {
		this.osMajor = osMajor;
	}

	public boolean isDesktopSave() {
		return desktopSave;
	}

	public void setDesktopSave(boolean desktopSave) {
		this.desktopSave = desktopSave;
	}
}
