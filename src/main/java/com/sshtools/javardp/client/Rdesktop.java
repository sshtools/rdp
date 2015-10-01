/* Rdesktop.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Main class, launches session
 */
package com.sshtools.javardp.client;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sshtools.javardp.ConnectionException;
import com.sshtools.javardp.Constants;
import com.sshtools.javardp.IContext;
import com.sshtools.javardp.Options;
import com.sshtools.javardp.OrderException;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.Rdp;
import com.sshtools.javardp.Version;
import com.sshtools.javardp.keymapping.KeyCode_FileBased;
import com.sshtools.javardp.rdp5.Rdp5;
import com.sshtools.javardp.rdp5.VChannels;
import com.sshtools.javardp.rdp5.cliprdr.ClipChannel;
import com.sshtools.javardp.tools.SendEvent;

public class Rdesktop {
	/**
	 * Translate a disconnect code into a textual description of the reason for
	 * the disconnect
	 * 
	 * @param reason Integer disconnect code received from server
	 * @return Text description of the reason for disconnection
	 */
	public static String textDisconnectReason(int reason) {
		String text;
		switch (reason) {
		case exDiscReasonNoInfo:
			text = "No information available";
			break;
		case exDiscReasonAPIInitiatedDisconnect:
			text = "Server initiated disconnect";
			break;
		case exDiscReasonAPIInitiatedLogoff:
			text = "Server initiated logoff";
			break;
		case exDiscReasonServerIdleTimeout:
			text = "Server idle timeout reached";
			break;
		case exDiscReasonServerLogonTimeout:
			text = "Server logon timeout reached";
			break;
		case exDiscReasonReplacedByOtherConnection:
			text = "Another user connected to the session";
			break;
		case exDiscReasonOutOfMemory:
			text = "The server is out of memory";
			break;
		case exDiscReasonServerDeniedConnection:
			text = "The server denied the connection";
			break;
		case exDiscReasonServerDeniedConnectionFips:
			text = "The server denied the connection for security reason";
			break;
		case exDiscReasonLicenseInternal:
			text = "Internal licensing error";
			break;
		case exDiscReasonLicenseNoLicenseServer:
			text = "No license server available";
			break;
		case exDiscReasonLicenseNoLicense:
			text = "No valid license available";
			break;
		case exDiscReasonLicenseErrClientMsg:
			text = "Invalid licensing message";
			break;
		case exDiscReasonLicenseHwidDoesntMatchLicense:
			text = "Hardware id doesn't match software license";
			break;
		case exDiscReasonLicenseErrClientLicense:
			text = "Client license error";
			break;
		case exDiscReasonLicenseCantFinishProtocol:
			text = "Network error during licensing protocol";
			break;
		case exDiscReasonLicenseClientEndedProtocol:
			text = "Licensing protocol was not completed";
			break;
		case exDiscReasonLicenseErrClientEncryption:
			text = "Incorrect client license enryption";
			break;
		case exDiscReasonLicenseCantUpgradeLicense:
			text = "Can't upgrade license";
			break;
		case exDiscReasonLicenseNoRemoteConnections:
			text = "The server is not licensed to accept remote connections";
			break;
		default:
			if (reason > 0x1000 && reason < 0x7fff) {
				text = "Internal protocol error";
			} else {
				text = "Unknown reason";
			}
		}
		return text;
	}

	/* RDP5 disconnect PDU */
	public static final int exDiscReasonNoInfo = 0x0000;
	public static final int exDiscReasonAPIInitiatedDisconnect = 0x0001;
	public static final int exDiscReasonAPIInitiatedLogoff = 0x0002;
	public static final int exDiscReasonServerIdleTimeout = 0x0003;
	public static final int exDiscReasonServerLogonTimeout = 0x0004;
	public static final int exDiscReasonReplacedByOtherConnection = 0x0005;
	public static final int exDiscReasonOutOfMemory = 0x0006;
	public static final int exDiscReasonServerDeniedConnection = 0x0007;
	public static final int exDiscReasonServerDeniedConnectionFips = 0x0008;
	public static final int exDiscReasonLicenseInternal = 0x0100;
	public static final int exDiscReasonLicenseNoLicenseServer = 0x0101;
	public static final int exDiscReasonLicenseNoLicense = 0x0102;
	public static final int exDiscReasonLicenseErrClientMsg = 0x0103;
	public static final int exDiscReasonLicenseHwidDoesntMatchLicense = 0x0104;
	public static final int exDiscReasonLicenseErrClientLicense = 0x0105;
	public static final int exDiscReasonLicenseCantFinishProtocol = 0x0106;
	public static final int exDiscReasonLicenseClientEndedProtocol = 0x0107;
	public static final int exDiscReasonLicenseErrClientEncryption = 0x0108;
	public static final int exDiscReasonLicenseCantUpgradeLicense = 0x0109;
	public static final int exDiscReasonLicenseNoRemoteConnections = 0x010a;
	static Log logger = LogFactory.getLog("com.elusiva.rdp");
	private static boolean keep_running;
	private static boolean showTools;
	private static final String keyMapPath = "keymaps/";
	private static String mapFile = "en-gb";
	private static String keyMapLocation = "";
	private static SendEvent toolFrame = null;
	private static boolean enable_menu;

	/**
	 * Outputs version and usage information via System.err
	 * 
	 */
	public static void usage(org.apache.commons.cli.Options options) {
		System.err.println("Elusiva Everywhere version " + Version.version);
		HelpFormatter fmt = new HelpFormatter();
		fmt.printHelp(Rdesktop.class.getName(), options, true);
		Rdesktop.exit(0, null, true);
	}

	/**
	 * 
	 * @param args
	 * @throws OrderException
	 * @throws RdesktopException
	 */
	public static void main(String[] args) throws Exception, RdesktopException {
		// Ensure that static variables are properly initialised
		Options options = new Options();
		keep_running = true;
		showTools = false;
		mapFile = "en-gb";
		keyMapLocation = "";
		toolFrame = null;
		// Failed to run native client, drop back to Java client instead.
		// parse arguments
		int logonflags = Rdp.RDP_LOGON_NORMAL;
		boolean fKdeHack = false;
		int c;
		String arg = null;
		String server = null;
		StringBuffer sb = new StringBuffer();
		String progname = "Elusiva Everywhere";
		org.apache.commons.cli.Options cliOptions = new org.apache.commons.cli.Options();
		cliOptions.addOption("debugHex", "debug-hex", false, "Enable HEX debugging");
		cliOptions.addOption("noPasteHack", "no-paste-hack", false, "Do not enable paste hack");
		cliOptions.addOption("packetTools", "packet-tools", false, "Packet tools");
		cliOptions.addOption("quietAlt", "quiet-alt", false, "Quiet alt");
		cliOptions.addOption("noRemapHash", "no-remap-hash", false, "Do not remap hash");
		cliOptions.addOption("noEncryption", "no-encryption", false, "No encryption");
		cliOptions.addOption("4", "use-rdp4", false, "Use RDP4");
		cliOptions.addOption("ssl", "use-ssl", false, "Use SSL");
		cliOptions.addOption("enableMenu", "enable-menu", false, "Enable menu");
		cliOptions.addOption("console", "console", false, "Console");
		cliOptions.addOption("loadLicense", "load-license", false, "Load license");
		cliOptions.addOption("saveLicense", "save-license", false, "Save license");
		cliOptions.addOption("persistantCache", "persistent-cache", false, "Persistent caching");
		cliOptions.addOption("o", "bpp", true, "Bits per pixel");
		cliOptions.addOption("b", "disable-low-latency", false, "Disable low latency mode");
		cliOptions.addOption("m", "keymap", true, "Keyboard map file");
		cliOptions.addOption("c", "directory", true, "Directory");
		cliOptions.addOption("d", "domain", true, "Domain");
		cliOptions.addOption("f", "full-screen", true, "Full screen mode");
		cliOptions.addOption("g", "geometry", true, "Screen size");
		cliOptions.addOption("k", "key-layout", true, "Keyboard layout");
		cliOptions.addOption("n", "hostname", true, "Hostname");
		cliOptions.addOption("c", "command", true, "Command");
		cliOptions.addOption("p", "password", true, "Password");
		cliOptions.addOption("u", "username", true, "Username");
		cliOptions.addOption("t", "port", true, "Port");
		PosixParser parser = new PosixParser();
		try {
			CommandLine cli = parser.parse(cliOptions, args);
			if (cli.hasOption("debugHex")) {
				options.debug_hexdump = true;
			}
			if (cli.hasOption("packetTools")) {
				showTools = true;
			}
			if (cli.hasOption("noRemapHash")) {
				options.remap_hash = false;
			}
			if (cli.hasOption("quietAlt")) {
				options.altkey_quiet = true;
			}
			if (cli.hasOption("noEncryption")) {
				options.packet_encryption = false;
			}
			if (cli.hasOption("4")) {
				options.use_rdp5 = false;
				// Options.server_bpp = 8;
				options.set_bpp(8);
			}
			if (cli.hasOption("ssl")) {
				options.use_ssl = true;
			}
			if (cli.hasOption("enableMenu")) {
				enable_menu = true;
			}
			if (cli.hasOption("console")) {
				options.console_session = true;
			}
			if (cli.hasOption("loadLicense")) {
				options.load_licence = true;
			}
			if (cli.hasOption("saveLicense")) {
				options.save_licence = true;
			}
			if (cli.hasOption("persistantCache")) {
				options.persistent_bitmap_caching = true;
			}
			if (cli.hasOption('o')) {
				options.set_bpp(Integer.parseInt(cli.getOptionValue('o')));
			}
			if (cli.hasOption('b')) {
				options.low_latency = false;
			}
			if (cli.hasOption('m')) {
				mapFile = cli.getOptionValue('m');
			}
			if (cli.hasOption('c')) {
				options.directory = cli.getOptionValue('c');
			}
			if (cli.hasOption('d')) {
				options.domain = cli.getOptionValue('d');
			}
			if (cli.hasOption('f')) {
				Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
				// ensure width a multiple of 4
				options.width = screen_size.width & ~3;
				options.height = screen_size.height;
				options.fullscreen = true;
				String optVal = cli.getOptionValue('f');
				if (optVal != null) {
					if (optVal.equals("l"))
						fKdeHack = true;
					else {
						System.err.println(progname + ": Invalid fullscreen option '" + arg + "'");
						usage(cliOptions);
					}
				}
			}
			if (cli.hasOption('g')) {
				arg = cli.getOptionValue('g');
				int cut = arg.indexOf("x", 0);
				if (cut == -1) {
					System.err.println(progname + ": Invalid geometry: " + arg);
					usage(cliOptions);
				}
				options.width = Integer.parseInt(arg.substring(0, cut)) & ~3;
				options.height = Integer.parseInt(arg.substring(cut + 1));
			}
			if (cli.hasOption('k')) {
				arg = cli.getOptionValue('k');
				// Options.keylayout = KeyLayout.strToCode(arg);
				if (options.keylayout == -1) {
					System.err.println(progname + ": Invalid key layout: " + arg);
					usage(cliOptions);
				}
			}
			if (cli.hasOption('n')) {
				options.hostname = cli.getOptionValue('n');
			}
			if (cli.hasOption('p')) {
				options.password = cli.getOptionValue('p');
				logonflags |= Rdp.RDP_LOGON_AUTO;
			}
			if (cli.hasOption('s')) {
				options.command = cli.getOptionValue('s');
			}
			if (cli.hasOption('u')) {
				options.username = cli.getOptionValue('u');
			}
			if (cli.hasOption('t')) {
				arg = cli.getOptionValue('t');
				try {
					options.port = Integer.parseInt(arg);
				} catch (NumberFormatException nex) {
					System.err.println(progname + ": Invalid port number: " + arg);
					usage(cliOptions);
				}
			}
			if (cli.hasOption('T')) {
				arg = cli.getOptionValue('T').replace('_', ' ');
				options.windowTitle = arg;
			}
			if (cli.hasOption('r')) {
				arg = cli.getOptionValue('r');
				options.licence_path = arg;
			}
			if (cli.hasOption('?')) {
				HelpFormatter fmt = new HelpFormatter();
				fmt.printHelp(Rdesktop.class.getName(), cliOptions);
			}
			if (fKdeHack) {
				options.height -= 46;
			}
			List remainingArgs = cli.getArgList();
			if (remainingArgs.size() == 1) {
				arg = (String) remainingArgs.get(0);
				int colonat = arg.indexOf(":", 0);
				if (colonat == -1) {
					server = arg;
				} else {
					server = arg.substring(0, colonat);
					options.port = Integer.parseInt(arg.substring(colonat + 1));
				}
			} else {
				System.err.println(progname + ": A server name is required!");
				usage(cliOptions);
			}
		} catch (UnrecognizedOptionException uoe) {
			System.err.println(uoe.getMessage());
			usage(cliOptions);
		}
		VChannels channels = new VChannels(options);
		RdesktopFrame window = new RdesktopFrame(options);
		ClipChannel clipChannel = new ClipChannel(window, options);
		// Initialise all RDP5 channels
		if (options.use_rdp5) {
			// TODO: implement all relevant channels
			if (options.map_clipboard)
				channels.register(clipChannel);
		}
		// Now do the startup...
		logger.info("Elusiva Everywhere version " + Version.version);
		if (args.length == 0)
			usage(cliOptions);
		String java = System.getProperty("java.specification.version");
		logger.info("Java version is " + java);
		String os = System.getProperty("os.name");
		String osvers = System.getProperty("os.version");
		if (os.equals("Windows 2000") || os.equals("Windows XP"))
			options.built_in_licence = true;
		logger.info("Operating System is " + os + " version " + osvers);
		if (os.startsWith("Linux"))
			Constants.OS = Constants.LINUX;
		else if (os.startsWith("Windows"))
			Constants.OS = Constants.WINDOWS;
		else if (os.startsWith("Mac"))
			Constants.OS = Constants.MAC;
		if (Constants.OS == Constants.MAC)
			options.caps_sends_up_and_down = false;
		Rdp5 rdpLayer = null;
		window.setClip(clipChannel);
		// Configure a keyboard layout
		KeyCode_FileBased keyMap = null;
		try {
			// logger.info("looking for: " + "/" + keyMapPath + mapFile);
			URL resource = Rdesktop.class.getResource("/" + keyMapPath + mapFile);
			InputStream istr = resource.openStream();
			// logger.info("istr = " + istr);
			if (istr == null) {
				logger.debug("Loading keymap from filename");
				keyMap = new KeyCode_FileBased(options, keyMapPath + mapFile);
			} else {
				logger.debug("Loading keymap from InputStream");
				keyMap = new KeyCode_FileBased(options, resource, istr);
			}
			if (istr != null)
				istr.close();
			options.keylayout = keyMap.getMapCode();
		} catch (Exception kmEx) {
			String[] msg = { (kmEx.getClass() + ": " + kmEx.getMessage()) };
			showErrorDialog(msg);
			kmEx.printStackTrace();
			Rdesktop.exit(0, null, true);
		}
		logger.debug("Registering keyboard...");
		if (keyMap != null)
			window.registerKeyboard(keyMap);
		boolean[] deactivated = new boolean[1];
		int[] ext_disc_reason = new int[1];
		logger.debug("keep_running = " + keep_running);
		while (keep_running) {
			logger.debug("Initialising RDP layer...");
			rdpLayer = new Rdp5(window, options, channels);
			window.setRdp(rdpLayer);
			logger.debug("Registering drawing surface...");
			window.registerDrawingSurface();
			logger.debug("Registering comms layer...");
			window.registerCommLayer(rdpLayer);
			logger.info("Connecting to " + server + ":" + options.port + " ...");
			if (server.equalsIgnoreCase("localhost"))
				server = "127.0.0.1";
			if (rdpLayer != null) {
				// Attempt to connect to server on port Options.port
				try {
					rdpLayer.connect(options.username, InetAddress.getByName(server), logonflags, options.domain, options.password,
						options.command, options.directory);
					// Remove to get rid of sendEvent tool
					if (showTools) {
						toolFrame = new SendEvent(rdpLayer);
						toolFrame.show();
					}
					// End
					if (keep_running) {
						/*
						 * By setting encryption to False here, we have an
						 * encrypted login packet but unencrypted transfer of
						 * other packets
						 */
						if (!options.packet_encryption)
							options.encryption = false;
						logger.info("Connection successful");
						// now show window after licence negotiation
						rdpLayer.mainLoop(deactivated, ext_disc_reason);
						if (deactivated[0]) {
							/* clean disconnect */
							Rdesktop.exit(0, window, true);
							// return 0;
						} else {
							if (ext_disc_reason[0] == exDiscReasonAPIInitiatedDisconnect
								|| ext_disc_reason[0] == exDiscReasonAPIInitiatedLogoff) {
								/*
								 * not so clean disconnect, but nothing to worry
								 * about
								 */
								Rdesktop.exit(0, window, true);
								// return 0;
							}
							if (ext_disc_reason[0] >= 2) {
								String reason = textDisconnectReason(ext_disc_reason[0]);
								String msg[] = { "Connection terminated", reason };
								showErrorDialog(msg);
								logger.warn("Connection terminated: " + reason);
								Rdesktop.exit(0, window, true);
							}
						}
						keep_running = false; // exited main loop
						if (!window.isReadyToSend()) {
							// maybe the licence server was having a comms
							// problem, retry?
							String msg1 = "The terminal server disconnected before licence negotiation completed.";
							String msg2 = "Possible cause: terminal server could not issue a licence.";
							String[] msg = { msg1, msg2 };
							logger.warn(msg1);
							logger.warn(msg2);
							showErrorDialog(msg);
						}
					} // closing bracket to if(running)
						// Remove to get rid of tool window
					if (showTools)
						toolFrame.dispose();
					// End
				} catch (ConnectionException e) {
					String msg[] = { "Connection Exception", e.getMessage() };
					showErrorDialog(msg);
					Rdesktop.exit(0, window, true);
				} catch (UnknownHostException e) {
					window.error(e, true);
				} catch (SocketException s) {
					if (rdpLayer.isConnected()) {
						logger.fatal(s.getClass().getName() + " " + s.getMessage());
						// s.printStackTrace();
						window.error(s, true);
						Rdesktop.exit(0, window, true);
					}
				} catch (RdesktopException e) {
					String msg1 = e.getClass().getName();
					String msg2 = e.getMessage();
					logger.fatal(msg1 + ": " + msg2);
					e.printStackTrace(System.err);
					if (!window.isReadyToSend()) {
						// maybe the licence server was having a comms
						// problem, retry?
						String msg[] = { "The terminal server reset connection before licence negotiation completed.",
							"Possible cause: terminal server could not connect to licence server.", "Retry?" };
//						if (!retry) {
							logger.info("Selected not to retry.");
							Rdesktop.exit(0, window, true);
//						} else {
//							if (rdpLayer != null && rdpLayer.isConnected()) {
//								logger.info("Disconnecting ...");
//								rdpLayer.disconnect();
//								logger.info("Disconnected");
//							}
//							logger.info("Retrying connection...");
//							keep_running = true; // retry
//							continue;
//						}
					} else {
						String msg[] = { e.getMessage() };
						showErrorDialog(msg);
						Rdesktop.exit(0, window, true);
					}
				} catch (Exception e) {
					logger.warn(e.getClass().getName() + " " + e.getMessage());
					e.printStackTrace();
					window.error(e, true);
				}
			} else { // closing bracket to if(!rdp==null)
				logger.fatal("The communications layer could not be initiated!");
			}
		}
		Rdesktop.exit(0, window, true);
	}

	/**
	 * Disconnects from the server connected to through rdp and destroys the
	 * RdesktopFrame window.
	 * <p>
	 * Exits the application iff sysexit == true, providing return value n to
	 * the operating system.
	 * 
	 * @param n
	 * @param rdp
	 * @param window
	 * @param sysexit
	 */
	public static void exit(int n, IContext context, boolean sysexit) {
		keep_running = false;
		// Remove to get rid of tool window
		if ((showTools) && (toolFrame != null))
			toolFrame.dispose();
		// End
		if (context != null && context.getRdp() != null && context.getRdp().isConnected()) {
			logger.info("Disconnecting ...");
			context.getRdp().disconnect();
			logger.info("Disconnected");
		}
		if (context != null) {
			context.dispose();
		}
		System.gc();
		if (sysexit && Constants.SystemExit) {
			if (!context.isUnderApplet())
				System.exit(n);
		}
	}

	/**
	 * Displays an error dialog via the RdesktopFrame window containing the
	 * customised message emsg, and reports this through the logging system.
	 * <p>
	 * The application then exits iff sysexit == true
	 * 
	 * @param emsg
	 * @param RdpLayer
	 * @param window
	 * @param sysexit
	 */
	public static void customError(String emsg, IContext window, boolean sysexit) {
		logger.fatal(emsg);
		String[] msg = { emsg };
		showErrorDialog(msg);
		Rdesktop.exit(0, window, true);
	}

	/**
	 * Display an error dialog with the title "properJavaRDP error"
	 * 
	 * @param msg Array of message lines to display in dialog box
	 */
	public static void showErrorDialog(String[] msg) {
		StringBuilder bui = new StringBuilder();
		for(String m : msg) {
			if(bui.length() > 0) {
				bui.append("\n\n");
			}
			bui.append(m);
		}
		JOptionPane.showMessageDialog(null, bui.toString());
	}
}
