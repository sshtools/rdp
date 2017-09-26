/*
 * Options.java Component: ProperJavaRDP Revision: $Revision: 1.1 $ Author:
 * $Author: brett $ Date: $Date: 2011/11/28 14:13:42 $ Copyright (c) 2005
 * Propero Limited Purpose: Global storage of user-definable options
 */
package com.sshtools.javardp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import javax.net.ssl.X509TrustManager;
import javax.security.auth.login.Configuration;

import org.apache.commons.lang3.SystemUtils;

import com.sshtools.javardp.keymapping.KeyCode_FileBased;
import com.sshtools.javardp.layers.Rdp;
import com.sshtools.javardp.layers.Secure;

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
	private String workstationName = ""; // -n hostname
	private String command = ""; // -s command
	private int connectionType = Rdp.CONNECTION_TYPE_WAN;
	private boolean consoleSession = false;
	private boolean debugHexdump = false;
	private String directory = ""; // -d directory
	private boolean fullscreen = false;
	private int height = 0; // -g widthxheight
	private boolean hideDecorations = false;
	// packets
	private boolean loadLicence = false;
	private boolean lowLatency = true; // disables bandwidth saving tcp
	private boolean mapClipboard = true;
	private boolean orders = true;
	private boolean owncolmap = false;
	private boolean packetEncryption = true;
	private boolean polygonEllipseOrders = false;
	private boolean precacheBitmaps = false;
	private boolean rdp5 = true;
	private int rdp5PerformanceFlags = Rdp.PERF_DISABLE_CURSOR_SHADOW | Rdp.PERF_DISABLE_CURSORSETTINGS
			| Rdp.PERF_DISABLE_FULLWINDOW_DRAG | Rdp.PERF_DISABLE_MENU_ANIMATIONS | Rdp.PERF_DISABLE_THEMING
			| Rdp.PERF_DISABLE_WALLPAPER;
	private boolean remapHash = true;
	private boolean saveLicence = false;
	private boolean sendmotion = true;
	private int width = 0; // -g widthxheight
	// number of bytes are used for a pixel
	private int winButtonSize = 0; /* If zero, disable single app mode */
	private String windowTitle = "SSHTools RDP"; // -T windowTitle
	private int osMinor = -1;
	private int osMajor = -1;
	private boolean desktopSave;
	private String clientDomain;
	private String clientIp = "";
	private int clientAddressFamily = -1;
	private String clientDir;
	private TimeZone timeZone = TimeZone.getDefault();
	private Configuration jaasConfiguration;
	private String cookie;
	private List<SecurityType> securityTypes = new ArrayList<>(Arrays.asList(SecurityType.supported()));
	private int lmCompatibility = 3;
	private CacheBackend persistentCacheBackend;
	private KeyCode_FileBased keymap;
	private X509TrustManager trustManager;
	private int sessionKeyEncryptionMethod = Secure.SEC_40BIT_ENCRYPTION | Secure.SEC_128BIT_ENCRYPTION | Secure.SEC_56BIT_ENCRYPTION;

	public Options() {
		if (SystemUtils.IS_OS_WINDOWS_NT && !SystemUtils.IS_OS_WINDOWS_NT && !SystemUtils.IS_OS_WINDOWS_95
				&& !SystemUtils.IS_OS_WINDOWS_98 && !SystemUtils.IS_OS_WINDOWS_ME) {
			setBuiltInLicence(true);
		}
		if (SystemUtils.IS_OS_MAC) {
			setCapsSendsUpAndDown(false);
		}
	}

	public X509TrustManager getTrustManager() {
		return trustManager;
	}

	public void setTrustManager(X509TrustManager trustManager) {
		this.trustManager = trustManager;
	}

	public KeyCode_FileBased getKeymap() {
		return keymap;
	}

	public void setKeymap(KeyCode_FileBased keymap) {
		this.keymap = keymap;
	}

	public CacheBackend getPersistentCacheBackend() {
		return persistentCacheBackend;
	}

	public void setPersistentCacheBackend(CacheBackend persistentCacheBackend) {
		this.persistentCacheBackend = persistentCacheBackend;
	}

	public String getClientDomain() {
		return clientDomain;
	}

	public void setClientDomain(String clientDomain) {
		this.clientDomain = clientDomain;
	}

	public String getCookie() {
		return cookie;
	}

	public List<SecurityType> getSecurityTypes() {
		return securityTypes;
	}

	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	public Configuration getJaasConfiguration() {
		return jaasConfiguration;
	}

	public void setJaasConfiguration(Configuration jaasConfiguration) {
		this.jaasConfiguration = jaasConfiguration;
	}

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
		switch (clientAddressFamily) {
		case -1:
		case Rdp.AF_INET:
		case Rdp.AF_INET6:
			this.clientAddressFamily = clientAddressFamily;
		default:
			throw new IllegalArgumentException(
					String.format("Must be one of AF_INET(0x%x), AF_INET6(0x%x) or AUTO(-1)", Rdp.AF_INET, Rdp.AF_INET6));
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

	public String getWorkstationName() {
		return workstationName;
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

	public int getHeight() {
		return height;
	}

	public int getRdp5PerformanceFlags() {
		return rdp5PerformanceFlags;
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

	public void setWorkstationName(String clientName) {
		this.workstationName = clientName;
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

	public void setFullscreen(boolean fullscreen) {
		this.fullscreen = fullscreen;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setHideDecorations(boolean hideDecorations) {
		this.hideDecorations = hideDecorations;
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

	public int getSecurityTypesMask() {
		int i = 0;
		for (SecurityType t : getSecurityTypes())
			i |= t.getMask();
		return i;
	}

	public int getLMCompatibility() {
		return lmCompatibility;
	}

	public void setLMCompatibility(int lmCompatibility) {
		this.lmCompatibility = lmCompatibility;
	}

	public void setSessionKeyEncryptionMethod(int sessionKeyEncryptionMethod) {
		this.sessionKeyEncryptionMethod = sessionKeyEncryptionMethod;
	}

	public int getSessionKeyEncryptionMethod() {
		return sessionKeyEncryptionMethod;
	}
	
	public int getBestSessionKeyEncryptionMethod() {
		if((sessionKeyEncryptionMethod & Secure.SEC_FIPS_ENCRYPTION) != 0)
			return Secure.SEC_FIPS_ENCRYPTION;
		if((sessionKeyEncryptionMethod & Secure.SEC_128BIT_ENCRYPTION) != 0)
			return Secure.SEC_128BIT_ENCRYPTION;
		if((sessionKeyEncryptionMethod & Secure.SEC_56BIT_ENCRYPTION) != 0)
			return Secure.SEC_56BIT_ENCRYPTION;
		if((sessionKeyEncryptionMethod & Secure.SEC_40BIT_ENCRYPTION) != 0)
			return Secure.SEC_40BIT_ENCRYPTION;
		return 0;
	}
}
