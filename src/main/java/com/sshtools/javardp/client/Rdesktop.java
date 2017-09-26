/* Rdesktop.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 * Copyright (c) 20017 SSHTools Limited
 *
 * Purpose: Main class, launches session
 */
package com.sshtools.javardp.client;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.ConnectionException;
import com.sshtools.javardp.DefaultCredentialsProvider;
import com.sshtools.javardp.Options;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.SecurityType;
import com.sshtools.javardp.State;
import com.sshtools.javardp.io.DefaultIO;
import com.sshtools.javardp.keymapping.KeyCode_FileBased;
import com.sshtools.javardp.layers.Rdp;
import com.sshtools.javardp.rdp5.VChannels;
import com.sshtools.javardp.rdp5.cliprdr.ClipChannel;

public class Rdesktop {
	static Logger logger;
	private static final String keyMapPath = "keymaps/";
	private static String mapFile = "en-gb";
	private static boolean showTools;
	private static SendEvent toolFrame = null;
	private static Rdp rdpLayer;

	public static void main(String[] args) throws Exception, RdesktopException {
		// Ensure that static variables are properly initialised
		Options options = new Options();
		int port = 3389;
		showTools = false;
		mapFile = "en-gb";
		toolFrame = null;
		boolean fKdeHack = false;
		DefaultCredentialsProvider dcp = new DefaultCredentialsProvider();
		String arg = null;
		String server = null;
		String progname = Rdesktop.class.getName();
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
		cliOptions.addOption("l", "log", true, "Log level");
		cliOptions.addOption("K", "skencmethod", true, "Session key encryption method (1=40 bit|2=128 bit|8=56 bit|16=FIPS)");
		PosixParser parser = new PosixParser();
		try {
			CommandLine cli = parser.parse(cliOptions, args);
			// Debug level
			String level = "warn";
			if (cli.hasOption("log")) {
				level = cli.getOptionValue("log");
			}
			System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", level);
			logger = LoggerFactory.getLogger(Rdesktop.class);
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
			if (cli.hasOption("console")) {
				options.setConsoleSession(true);
			}
			if (cli.hasOption("loadLicense")) {
				options.setLoadLicence(true);
			}
			if (cli.hasOption("saveLicense")) {
				options.setSaveLicence(true);
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
						throw new UnrecognizedOptionException(String.format("Invalid fullscreen option %s", arg));
					}
				}
			}
			if (cli.hasOption('g')) {
				arg = cli.getOptionValue('g');
				int cut = arg.indexOf("x", 0);
				if (cut == -1) {
					throw new UnrecognizedOptionException(String.format("Invalid geometry %s", arg));
				}
				options.setWidth(Integer.parseInt(arg.substring(0, cut)));
				options.setHeight(Integer.parseInt(arg.substring(cut + 1)));
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
					throw new UnrecognizedOptionException("Invalid port number");
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
			if(cli.hasOption('K')) {
				options.setSessionKeyEncryptionMethod(Integer.parseInt(cli.getOptionValue('K')));
			}
			@SuppressWarnings("unchecked")
			List<String> remainingArgs = cli.getArgList();
			if (remainingArgs.size() == 1) {
				arg = (String) remainingArgs.get(0);
				int colonat = arg.indexOf(":", 0);
				if (colonat == -1) {
					server = arg;
				} else {
					server = arg.substring(0, colonat);
					port = Integer.parseInt(arg.substring(colonat + 1));
				}
				if (server.equalsIgnoreCase("localhost"))
					server = "127.0.0.1";
			} else {
				throw new UnrecognizedOptionException("A server name is required.");
			}
		} catch (UnrecognizedOptionException uoe) {
			System.err.println(String.format("%s: %s", progname, uoe.getMessage()));
			usage(cliOptions);
			System.exit(2);
		}
		// Now do the startup...
		if (args.length == 0) {
			usage(cliOptions);
			System.exit(2);
		}
		// Configure a keyboard layout
		KeyCode_FileBased keyMap = null;
		try {
			URL resource = Rdesktop.class.getResource("/" + keyMapPath + mapFile);
			InputStream istr = resource.openStream();
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
			options.setKeymap(keyMap);
		} catch (Exception kmEx) {
			System.err.println(String.format("%s: %s", progname, kmEx.getMessage()));
			System.exit(1);
		}
		rdpLayer = null;
		State state = new State(options);
		VChannels channels = new VChannels(state);
		ClipChannel clipChannel = new ClipChannel();
		// Initialise all RDP5 channels
		if (state.isRDP5()) {
			// TODO: implement all relevant channels
			if (options.isMapClipboard())
				channels.register(clipChannel);
		}
		final RdesktopFrame window = new RdesktopFrame(state);
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// Remove to get rid of tool window
				if ((showTools) && (toolFrame != null))
					toolFrame.dispose();
				// End
				if (rdpLayer != null && rdpLayer != null && rdpLayer.isConnected()) {
					logger.info("Disconnecting ...");
					rdpLayer.disconnect();
					logger.info("Disconnected");
				}
				System.exit(0);
			}
		});
		window.setClip(clipChannel);
		rdpLayer = new Rdp(window, state, channels);
		// Attempt to connect to server on port Options.port
		try {
			rdpLayer.connect(new DefaultIO(InetAddress.getByName(server), port), dcp, options.getCommand(), options.getDirectory());
			if (showTools) {
				toolFrame = new SendEvent(rdpLayer);
				toolFrame.setVisible(true);
			}
			logger.info("Connection successful");
			rdpLayer.mainLoop();
			// End
		} catch (ConnectionException e) {
			System.err.println(String.format("%s: %s", progname, e.getMessage()));
			System.exit(1);
		} catch (UnknownHostException e) {
			System.err.println(String.format("%s: %s", progname, e.getMessage()));
			System.exit(1);
		} catch (SocketException s) {
			if (rdpLayer.isConnected()) {
				System.err.println(String.format("%s: %s", progname, s.getMessage()));
			}
			System.exit(1);
		} catch (RdesktopException e) {
			System.err.println(String.format("%s: %s", progname, e.getMessage()));
			System.exit(1);
		} finally {
			window.dispose();
		}
	}

	public static void usage(org.apache.commons.cli.Options options) {
		System.err.println("SSHTools RDP");
		HelpFormatter fmt = new HelpFormatter();
		fmt.printHelp(Rdesktop.class.getName(), options, true);
	}
}
