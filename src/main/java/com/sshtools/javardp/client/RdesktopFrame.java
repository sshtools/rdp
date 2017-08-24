/* RdesktopFrame.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Window for RDP session
 */
package com.sshtools.javardp.client;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.Constants;
import com.sshtools.javardp.IContext;
import com.sshtools.javardp.MCS;
import com.sshtools.javardp.Options;
import com.sshtools.javardp.RdesktopCanvas;
import com.sshtools.javardp.Rdp;
import com.sshtools.javardp.Secure;
import com.sshtools.javardp.keymapping.KeyCode_FileBased;
import com.sshtools.javardp.rdp5.cliprdr.ClipChannel;

//import javax.swing.Box;
public class RdesktopFrame extends Frame implements IContext {
	static Logger logger = LoggerFactory.getLogger(RdesktopFrame.class);
	public RdesktopCanvas canvas = null;
	public Rdp rdp = null;
	public RdpMenu menu = null;
	private boolean readyToSend;
	private boolean loggedOn;

	/**
	 * Register the clipboard channel
	 * 
	 * @param c ClipChannel object for controlling clipboard mapping
	 */
	public void setClip(ClipChannel c) {
		((JComponent) canvas.getDisplay()).addFocusListener(c);
	}

	public void setLoggedOn() {
		loggedOn = true;
	}

	public boolean isReadyToSend() {
		return readyToSend;
	}

	public void setReadyToSend() {
		readyToSend = true;
	}

	/**
	 * Displays details of the Exception e in an error dialog via the
	 * RdesktopFrame window and reports this through the logger, then prints a
	 * stack trace.
	 * <p>
	 * The application then exits iff sysexit == true
	 * 
	 * @param e
	 * @param sysexit
	 * @param RdpLayer
	 * @param window
	 */
	public void error(Exception e, boolean sysexit) {
		try {
			String msg1 = e.getClass().getName();
			String msg2 = e.getMessage();
			logger.error(msg1 + ": " + msg2);
			String[] msg = { msg1, msg2 };
			Rdesktop.showErrorDialog(msg);
			// e.printStackTrace(System.err);
		} catch (Exception ex) {
			logger.warn("Exception in Rdesktop.error: " + ex.getClass().getName() + ": " + ex.getMessage());
		}
		Rdesktop.exit(0, this, sysexit);
	}

	/**
	 * @deprecated ActionListener should be used instead.
	 */
	/*
	 * public boolean action(Event event, Object arg) { if (menu != null) return
	 * menu.action(event, arg); return false; }
	 */
	protected boolean inFullscreen = false;

	/**
	 * Switch in/out of fullscreen mode
	 */
	public void toggleFullScreen() {
		if (inFullscreen)
			leaveFullScreen();
		else
			goFullScreen();
	}

	private boolean menuVisible = false;
	protected Options options;
	private Secure secure;
	private boolean underApplet;
	private MCS mcs;

	/**
	 * Display the menu bar
	 */
	public void showMenu() {
		if (menu == null)
			menu = new RdpMenu(this);
		if (!menuVisible)
			this.setMenuBar(menu);
		canvas.getDisplay().repaint();
		menuVisible = true;
	}

	protected void fullscreen() {
		setUndecorated(true);
		setExtendedState(Frame.MAXIMIZED_BOTH);
		inFullscreen = false;
	}

	public void goFullScreen() {
		if (!options.fullscreen)
			return;
		inFullscreen = true;
		if (this.isDisplayable())
			this.dispose();
		this.setVisible(false);
		this.setLocation(0, 0);
		this.setUndecorated(true);
		this.setVisible(true);
		// setExtendedState (Frame.MAXIMIZED_BOTH);
		// GraphicsEnvironment env =
		// GraphicsEnvironment.getLocalGraphicsEnvironment();
		// GraphicsDevice myDevice = env.getDefaultScreenDevice();
		// if (myDevice.isFullScreenSupported())
		// myDevice.setFullScreenWindow(this);
		this.pack();
	}

	public void leaveFullScreen() {
		if (!options.fullscreen)
			return;
		inFullscreen = false;
		if (this.isDisplayable())
			this.dispose();
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice myDevice = env.getDefaultScreenDevice();
		if (myDevice.isFullScreenSupported())
			myDevice.setFullScreenWindow(null);
		this.setLocation(10, 10);
		this.setUndecorated(false);
		this.setVisible(true);
		// setExtendedState (Frame.NORMAL);
		this.pack();
	}

	/**
	 * Hide the menu bar
	 */
	public void hideMenu() {
		if (menuVisible)
			this.setMenuBar(null);
		// canvas.setSize(this.WIDTH, this.HEIGHT);
		canvas.getDisplay().repaint();
		menuVisible = false;
	}

	/**
	 * Toggle the menu on/off (show if hidden, hide if visible)
	 * 
	 */
	public void toggleMenu() {
		if (!menuVisible)
			showMenu();
		else
			hideMenu();
	}

	/**
	 * Create a new RdesktopFrame. Size defined by Options.width and
	 * Options.height Creates RdesktopCanvas occupying entire frame
	 */
	public RdesktopFrame(Options options) {
		super();
		this.options = options;
		setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		menu = new RdpMenu(this);
		setMenuBar(menu);
		new RdesktopCanvas(this, options, options.width, options.height);
		add((JComponent) this.canvas.getDisplay());
		setTitle(options.windowTitle);
		if (Constants.OS == Constants.WINDOWS)
			setResizable(false);
		// Windows has to setResizable(false) before pack,
		// else draws on the frame
		if (options.fullscreen) {
			goFullScreen();
			pack();
			setLocation(0, 0);
		} else {// centre
			pack();
			centreWindow();
		}
		logger.info("canvas:" + ((JComponent) this.canvas.getDisplay()).getSize());
		logger.info("frame: " + getSize());
		logger.info("insets:" + getInsets());
		if (Constants.OS != Constants.WINDOWS)
			setResizable(false);
		// Linux Java 1.3 needs pack() before setResizeable
		addWindowListener(new RdesktopWindowAdapter());
		((JComponent) this.canvas.getDisplay()).addFocusListener(new RdesktopFocusListener());
		if (Constants.OS == Constants.WINDOWS) {
			// redraws screen on window move
			addComponentListener(new RdesktopComponentAdapter());
		}
		((JComponent) this.canvas.getDisplay()).requestFocus();
	}

	public void init(RdesktopCanvas canvas) {
		this.canvas = canvas;
	}

	/**
	 * Retrieve the canvas contained within this frame
	 * 
	 * @return RdesktopCanvas object associated with this frame
	 */
	public RdesktopCanvas getCanvas() {
		return this.canvas;
	}

	/**
	 * Register the RDP communications layer with this frame
	 * 
	 * @param rdp Rdp object encapsulating the RDP comms layer
	 */
	public void registerCommLayer(Rdp rdp) {
		this.rdp = rdp;
		canvas.registerCommLayer(rdp);
	}

	/**
	 * Register keymap
	 * 
	 * @param keys Keymapping object for use in handling keyboard events
	 */
	public void registerKeyboard(KeyCode_FileBased keys) {
		canvas.registerKeyboard(keys);
	}

	class RdesktopFocusListener implements FocusListener {
		public void focusGained(FocusEvent arg0) {
			if (Constants.OS == Constants.WINDOWS) {
				// canvas.repaint();
				canvas.getDisplay().repaint(0, 0, options.width, options.height);
			}
			// gained focus..need to check state of locking keys
			canvas.gainedFocus();
		}

		public void focusLost(FocusEvent arg0) {
			// lost focus - need clear keys that are down
			canvas.lostFocus();
		}
	}

	class RdesktopWindowAdapter extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			hide();
			Rdesktop.exit(0, RdesktopFrame.this, true);
		}

		public void windowLostFocus(WindowEvent e) {
			logger.info("windowLostFocus");
			// lost focus - need clear keys that are down
			canvas.lostFocus();
		}

		public void windowDeiconified(WindowEvent e) {
			if (Constants.OS == Constants.WINDOWS) {
				// canvas.repaint();
				canvas.getDisplay().repaint(0, 0, options.width, options.height);
			}
			canvas.gainedFocus();
		}

		public void windowActivated(WindowEvent e) {
			if (Constants.OS == Constants.WINDOWS) {
				// canvas.repaint();
				canvas.getDisplay().repaint(0, 0, options.width, options.height);
			}
			// gained focus..need to check state of locking keys
			canvas.gainedFocus();
		}

		public void windowGainedFocus(WindowEvent e) {
			if (Constants.OS == Constants.WINDOWS) {
				// canvas.repaint();
				canvas.getDisplay().repaint(0, 0, options.width, options.height);
			}
			// gained focus..need to check state of locking keys
			canvas.gainedFocus();
		}
	}

	class RdesktopComponentAdapter extends ComponentAdapter {
		public void componentMoved(ComponentEvent e) {
			canvas.getDisplay().repaint(0, 0, options.width, options.height);
		}
	}

	/**
	 * Notify the canvas that the connection is ready for sending messages
	 */
	public void triggerReadyToSend() {
		this.show();
		canvas.triggerReadyToSend();
	}

	/**
	 * Centre a window to the screen
	 * 
	 * @param f Window to be centred
	 */
	public static void centreWindow(Window f) {
		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension window_size = f.getSize();
		int x = (screen_size.width - window_size.width) / 2;
		if (x < 0)
			x = 0; // window can be bigger than screen
		int y = (screen_size.height - window_size.height) / 2;
		if (y < 0)
			y = 0; // window can be bigger than screen
		f.setLocation(x, y);
	}

	/**
	 * Centre this window
	 */
	public void centreWindow() {
		centreWindow(this);
	}

	public void exit() {
		// TODO Auto-generated method stub
	}

	public Rdp getRdp() {
		return rdp;
	}

	public Secure getSecure() {
		return secure;
	}

	public boolean isUnderApplet() {
		return underApplet;
	}

	public MCS getMcs() {
		return mcs;
	}

	public void setMcs(MCS mcs) {
		this.mcs = mcs;
	}

	public void setSecure(Secure secure) {
		this.secure = secure;
	}

	public void registerDrawingSurface() {
		rdp.registerDrawingSurface(canvas);
	}

	public void setRdp(Rdp rdp) {
		this.rdp = rdp;
	}

	public void screenResized(int width, int height, boolean clientInitiated) {
		setSize(width, height);
	}
}
