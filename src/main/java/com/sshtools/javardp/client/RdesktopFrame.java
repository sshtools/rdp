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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.JComponent;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.IContext;
import com.sshtools.javardp.State;
import com.sshtools.javardp.graphics.RdesktopCanvas;
import com.sshtools.javardp.rdp5.cliprdr.ClipChannel;

//import javax.swing.Box;
public class RdesktopFrame extends Frame implements IContext {
	static Logger logger = LoggerFactory.getLogger(RdesktopFrame.class);
	public RdesktopCanvas canvas = null;
	public RdpMenu menu = null;
	/**
	 * @deprecated ActionListener should be used instead.
	 */
	/*
	 * public boolean action(Event event, Object arg) { if (menu != null) return
	 * menu.action(event, arg); return false; }
	 */
	@Deprecated
	protected boolean inFullscreen = false;
	private boolean loggedOn;
	private boolean menuVisible = false;
	private State state;

	/**
	 * Create a new RdesktopFrame. Size defined by Options.width and
	 * Options.height Creates RdesktopCanvas occupying entire frame
	 */
	public RdesktopFrame(State state) {
		super();
		setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		menu = new RdpMenu(this);
		setMenuBar(menu);
		canvas = new RdesktopCanvas(this, state);
		canvas.getDisplay().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getY() != 0) {
					hideMenu();
				}
			}
		});
		add((JComponent) this.canvas.getDisplay());
		setTitle(state.getOptions().getWindowTitle());
		if (SystemUtils.IS_OS_WINDOWS)
			setResizable(false);
		// Windows has to setResizable(false) before pack,
		// else draws on the frame
		if (state.getOptions().isFullscreen()) {
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
		if (SystemUtils.IS_OS_WINDOWS)
			setResizable(false);
		// Linux Java 1.3 needs pack() before setResizeable
		addWindowListener(new RdesktopWindowAdapter());
		((JComponent) this.canvas.getDisplay()).addFocusListener(new RdesktopFocusListener());
		if (SystemUtils.IS_OS_WINDOWS) {
			// redraws screen on window move
			addComponentListener(new RdesktopComponentAdapter());
		}
		((JComponent) this.canvas.getDisplay()).requestFocus();
	}

	/**
	 * Centre this window
	 */
	public void centreWindow() {
		centreWindow(this);
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
	@Override
	public void error(Exception e, boolean sysexit) {
		logger.error("Error occured.", e);
		if (sysexit)
			dispose();
	}

	/**
	 * Retrieve the canvas contained within this frame
	 * 
	 * @return RdesktopCanvas object associated with this frame
	 */
	public RdesktopCanvas getCanvas() {
		return this.canvas;
	}

	public void goFullScreen() {
		if (!state.getOptions().isFullscreen())
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

	public void leaveFullScreen() {
		if (!state.getOptions().isFullscreen())
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

	@Override
	public byte[] loadLicense() throws IOException {
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		return prefs.getByteArray("licence." + state.getWorkstationName(), null);
	}

	@Override
	public void saveLicense(byte[] license) throws IOException {
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		prefs.putByteArray("licence." + state.getWorkstationName(), license);
	}

	@Override
	public void screenResized(int width, int height, boolean clientInitiated) {
		setSize(width, height);
	}

	/**
	 * Register the clipboard channel
	 * 
	 * @param c ClipChannel object for controlling clipboard mapping
	 */
	public void setClip(ClipChannel c) {
		((JComponent) canvas.getDisplay()).addFocusListener(c);
	}

	@Override
	public void setLoggedOn() {
		loggedOn = true;
	}

	@Override
	public void readyToSend() {
		this.setVisible(true);
		canvas.triggerReadyToSend();
	}

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

	/**
	 * Switch in/out of fullscreen mode
	 */
	@Override
	public void toggleFullScreen() {
		if (inFullscreen)
			leaveFullScreen();
		else
			goFullScreen();
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

	protected void fullscreen() {
		setUndecorated(true);
		setExtendedState(Frame.MAXIMIZED_BOTH);
		inFullscreen = false;
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

	class RdesktopComponentAdapter extends ComponentAdapter {
		@Override
		public void componentMoved(ComponentEvent e) {
			canvas.getDisplay().repaint(0, 0, state.getWidth(), state.getHeight());
		}
	}

	class RdesktopFocusListener implements FocusListener {
		@Override
		public void focusGained(FocusEvent arg0) {
			if (SystemUtils.IS_OS_WINDOWS) {
				// canvas.repaint();
				canvas.getDisplay().repaint(0, 0, state.getWidth(), state.getHeight());
			}
			// gained focus..need to check state of locking keys
			canvas.gainedFocus();
		}

		@Override
		public void focusLost(FocusEvent arg0) {
			// lost focus - need clear keys that are down
			canvas.lostFocus();
		}
	}

	class RdesktopWindowAdapter extends WindowAdapter {
		@Override
		public void windowActivated(WindowEvent e) {
			if (SystemUtils.IS_OS_WINDOWS) {
				// canvas.repaint();
				canvas.getDisplay().repaint(0, 0, state.getWidth(), state.getHeight());
			}
			// gained focus..need to check state of locking keys
			canvas.gainedFocus();
		}

		@Override
		public void windowDeiconified(WindowEvent e) {
			if (SystemUtils.IS_OS_WINDOWS) {
				// canvas.repaint();
				canvas.getDisplay().repaint(0, 0, state.getWidth(), state.getHeight());
			}
			canvas.gainedFocus();
		}

		@Override
		public void windowGainedFocus(WindowEvent e) {
			if (SystemUtils.IS_OS_WINDOWS) {
				// canvas.repaint();
				canvas.getDisplay().repaint(0, 0, state.getWidth(), state.getHeight());
			}
			// gained focus..need to check state of locking keys
			canvas.gainedFocus();
		}

		@Override
		public void windowLostFocus(WindowEvent e) {
			logger.info("windowLostFocus");
			// lost focus - need clear keys that are down
			canvas.lostFocus();
		}
	}
}
