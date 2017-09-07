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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.ConnectionException;
import com.sshtools.javardp.Constants;
import com.sshtools.javardp.DefaultCredentialsProvider;
import com.sshtools.javardp.IContext;
import com.sshtools.javardp.Options;
import com.sshtools.javardp.OrderException;
import com.sshtools.javardp.RdesktopDisconnectException;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.Rdp;
import com.sshtools.javardp.SecurityType;
import com.sshtools.javardp.State;
import com.sshtools.javardp.io.DefaultIO;
import com.sshtools.javardp.keymapping.KeyCode_FileBased;
import com.sshtools.javardp.rdp5.VChannels;
import com.sshtools.javardp.rdp5.cliprdr.ClipChannel;

public class Rdesktop {
	static Logger logger = LoggerFactory.getLogger(Rdesktop.class);
	private static boolean enable_menu;
	private static boolean keep_running;
	private static String keyMapLocation = "";
	private static final String keyMapPath = "keymaps/";
	private static String mapFile = "en-gb";
	private static boolean showTools;
	private static SendEvent toolFrame = null;

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
		logger.error(emsg);
		String[] msg = { emsg };
		showErrorDialog(msg);
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
	 * 
	 * @param args
	 * @throws OrderException
	 * @throws RdesktopException
	 */
	public static void main(String[] args) throws Exception, RdesktopException {
		// Ensure that static variables are properly initialised
		Options options = new Options();
		int port = 3389;
		keep_running = true;
		showTools = false;
		mapFile = "en-gb";
		keyMapLocation = "";
		toolFrame = null;
		// Failed to run native client, drop back to Java client instead.
		// parse arguments
		boolean fKdeHack = false;
		int c;
		DefaultCredentialsProvider dcp = new DefaultCredentialsProvider();
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
		cliOptions.addOption("S", "no-ssl", false, "Do not uue SSL");
		cliOptions.addOption("N", "no-nla", false, "Do not uue NLA");
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
				options.setDebugHexdump(true);
			}
			if (cli.hasOption("packetTools")) {
				showTools = true;
			}
			if (cli.hasOption("noRemapHash")) {
				options.setRemapHash(false);
			}
			if (cli.hasOption("quietAlt")) {
				options.setAltkeyQuiet(true);
			}
			if (cli.hasOption("noEncryption")) {
				options.setPacketEncryption(false);
			}
			if (cli.hasOption("4")) {
				options.setRdp5(false);
			}
			if (cli.hasOption("no-ssl")) {
				for (SecurityType t : SecurityType.supported()) {
					if (t.isSSL()) {
						options.getSecurityTypes().remove(t);
					}
				}
			}
			if (cli.hasOption("no-nla")) {
				for (SecurityType t : SecurityType.supported()) {
					if (t.isNLA()) {
						options.getSecurityTypes().remove(t);
					}
				}
			}
			if (cli.hasOption("enableMenu")) {
				enable_menu = true;
			}
			if (cli.hasOption("console")) {
				options.setConsoleSession(true);
			}
			if (cli.hasOption("loadLicense")) {
				options.setLoadLicence(true);
			}
			if (cli.hasOption("saveLicense")) {
				options.setSaveLicence(true);
			}
			if (cli.hasOption("persistantCache")) {
				options.setPersistentBitmapCaching(true);
			}
			if (cli.hasOption('o')) {
				options.setBpp(Integer.parseInt(cli.getOptionValue('o')));
			}
			if (cli.hasOption('b')) {
				options.setLowLatency(false);
			}
			if (cli.hasOption('m')) {
				mapFile = cli.getOptionValue('m');
			}
			if (cli.hasOption('c')) {
				options.setDirectory(cli.getOptionValue('c'));
			}
			if (cli.hasOption('d')) {
				dcp.setDomain(cli.getOptionValue('d'));
			}
			if (cli.hasOption('f')) {
				Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
				// ensure width a multiple of 4
				options.setWidth(screen_size.width);
				options.setHeight(screen_size.height);
				options.setFullscreen(true);
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
				options.setWidth(Integer.parseInt(arg.substring(0, cut)));
				options.setHeight(Integer.parseInt(arg.substring(cut + 1)));
			}
			if (cli.hasOption('k')) {
				arg = cli.getOptionValue('k');
				// Options.keylayout = KeyLayout.strToCode(arg);
				if (options.getKeylayout() == -1) {
					System.err.println(progname + ": Invalid key layout: " + arg);
					usage(cliOptions);
				}
			}
			if (cli.hasOption('n')) {
				options.setWorkstationName(cli.getOptionValue('n'));
			}
			if (cli.hasOption('p')) {
				dcp.setPassword(cli.getOptionValue('p').toCharArray());
			}
			if (cli.hasOption('s')) {
				options.setCommand(cli.getOptionValue('s'));
			}
			if (cli.hasOption('u')) {
				dcp.setUsername(cli.getOptionValue('u'));
			}
			if (cli.hasOption('t')) {
				arg = cli.getOptionValue('t');
				try {
					port = Integer.parseInt(arg);
				} catch (NumberFormatException nex) {
					System.err.println(progname + ": Invalid port number: " + arg);
					usage(cliOptions);
				}
			}
			if (cli.hasOption('T')) {
				arg = cli.getOptionValue('T').replace('_', ' ');
				options.setWindowTitle(arg);
			}
			if (cli.hasOption('?')) {
				HelpFormatter fmt = new HelpFormatter();
				fmt.printHelp(Rdesktop.class.getName(), cliOptions);
			}
			if (fKdeHack) {
				options.setHeight(options.getHeight() - 46);
			}
			List remainingArgs = cli.getArgList();
			if (remainingArgs.size() == 1) {
				arg = (String) remainingArgs.get(0);
				int colonat = arg.indexOf(":", 0);
				if (colonat == -1) {
					server = arg;
				} else {
					server = arg.substring(0, colonat);
					port = Integer.parseInt(arg.substring(colonat + 1));
				}
			} else {
				System.err.println(progname + ": A server name is required!");
				usage(cliOptions);
			}
		} catch (UnrecognizedOptionException uoe) {
			System.err.println(uoe.getMessage());
			usage(cliOptions);
		}
		// Now do the startup...
		System.err.println("SSHTools RDP");
		if (args.length == 0)
			usage(cliOptions);
		String java = System.getProperty("java.specification.version");
		logger.info("Java version is " + java);
		String os = System.getProperty("os.name");
		String osvers = System.getProperty("os.version");
		if (os.equals("Windows 2000") || os.equals("Windows XP"))
			options.setBuiltInLicence(true);
		logger.info("Operating System is " + os + " version " + osvers);
		if (os.startsWith("Linux"))
			Constants.OS = Constants.LINUX;
		else if (os.startsWith("Windows"))
			Constants.OS = Constants.WINDOWS;
		else if (os.startsWith("Mac"))
			Constants.OS = Constants.MAC;
		if (Constants.OS == Constants.MAC)
			options.setCapsSendsUpAndDown(false);
		// Configure a keyboard layout
		KeyCode_FileBased keyMap = null;
		try {
			// logger.info("looking for: " + "/" + keyMapPath + mapFile);
			URL resource = Rdesktop.class.getResource("/" + keyMapPath + mapFile);
			InputStream istr = resource.openStream();
			// logger.info("istr = " + istr);
			if (istr == null) {
				if (logger.isDebugEnabled())
					logger.debug("Loading keymap from filename");
				keyMap = new KeyCode_FileBased(options, keyMapPath + mapFile);
			} else {
				if (logger.isDebugEnabled())
					logger.debug("Loading keymap from InputStream");
				keyMap = new KeyCode_FileBased(options, resource, istr);
			}
			if (istr != null)
				istr.close();
			options.setKeylayout(keyMap.getMapCode());
		} catch (Exception kmEx) {
			String[] msg = { (kmEx.getClass() + ": " + kmEx.getMessage()) };
			showErrorDialog(msg);
			kmEx.printStackTrace();
			Rdesktop.exit(0, null, true);
		}
		Rdp rdpLayer = null;
		State state = new State(options);
		VChannels channels = new VChannels(state);
		RdesktopFrame window = new RdesktopFrame(state);
		ClipChannel clipChannel = new ClipChannel(window, state);
		// Initialise all RDP5 channels
		if (state.isRDP5()) {
			// TODO: implement all relevant channels
			if (options.isMapClipboard())
				channels.register(clipChannel);
		}
		window.setClip(clipChannel);
		if (logger.isDebugEnabled())
			logger.debug("Registering keyboard...");
		if (keyMap != null)
			window.registerKeyboard(keyMap);
		if (logger.isDebugEnabled())
			logger.debug("keep_running = " + keep_running);
		while (keep_running) {
			if (logger.isDebugEnabled())
				logger.debug("Initialising RDP layer...");
			rdpLayer = new Rdp(window, state, channels);
			window.setRdp(rdpLayer);
			if (logger.isDebugEnabled())
				logger.debug("Registering drawing surface...");
			window.registerDrawingSurface();
			if (logger.isDebugEnabled())
				logger.debug("Registering comms layer...");
			window.registerCommLayer(rdpLayer);
			if (logger.isDebugEnabled())
				logger.info("Connecting to " + server + ":" + port + " ...");
			if (server.equalsIgnoreCase("localhost"))
				server = "127.0.0.1";
			if (rdpLayer != null) {
				// Attempt to connect to server on port Options.port
				try {
					rdpLayer.connect(new DefaultIO(InetAddress.getByName(server), port), dcp, options.getCommand(),
							options.getDirectory());
					// Remove to get rid of sendEvent tool
					if (showTools) {
						toolFrame = new SendEvent(rdpLayer);
						toolFrame.show();
					}
					// End
					if (keep_running) {
						logger.info("Connection successful");
						// now show window after licence negotiation
						try {
							rdpLayer.mainLoop();
						} catch (RdesktopDisconnectException rde) {
							String msg[] = { "Connection terminated", rde.getMessage() };
							showErrorDialog(msg);
							logger.warn("Connection terminated: " + rde.getMessage(), rde);
							Rdesktop.exit(0, window, true);
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
						logger.error(s.getClass().getName() + " " + s.getMessage());
						// s.printStackTrace();
						window.error(s, true);
						Rdesktop.exit(0, window, true);
					}
				} catch (RdesktopException e) {
					String msg1 = e.getClass().getName();
					String msg2 = e.getMessage();
					logger.error(msg1 + ": " + msg2);
					e.printStackTrace(System.err);
					if (!window.isReadyToSend()) {
						// maybe the licence server was having a comms
						// problem, retry?
						String msg[] = { "The terminal server reset connection before licence negotiation completed.",
								"Possible cause: terminal server could not connect to licence server.", "Retry?" };
						// if (!retry) {
						logger.info("Selected not to retry.");
						Rdesktop.exit(0, window, true);
						// } else {
						// if (rdpLayer != null && rdpLayer.isConnected()) {
						// logger.info("Disconnecting ...");
						// rdpLayer.disconnect();
						// logger.info("Disconnected");
						// }
						// logger.info("Retrying connection...");
						// keep_running = true; // retry
						// continue;
						// }
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
				logger.error("The communications layer could not be initiated!");
			}
		}
		Rdesktop.exit(0, window, true);
	}

	/**
	 * Display an error dialog with the title "properJavaRDP error"
	 * 
	 * @param msg Array of message lines to display in dialog box
	 */
	public static void showErrorDialog(String[] msg) {
		StringBuilder bui = new StringBuilder();
		for (String m : msg) {
			if (bui.length() > 0) {
				bui.append("\n\n");
			}
			bui.append(m);
		}
		JOptionPane.showMessageDialog(null, bui.toString());
	}

	/**
	 * Outputs version and usage information via System.err
	 * 
	 */
	public static void usage(org.apache.commons.cli.Options options) {
		System.err.println("SSHTools RDP");
		HelpFormatter fmt = new HelpFormatter();
		fmt.printHelp(Rdesktop.class.getName(), options, true);
		Rdesktop.exit(0, null, true);
	}
}
