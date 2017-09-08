/* Input.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Handles input events and sends relevant input data
 *          to server
 */
package com.sshtools.javardp;

import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Collections;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.graphics.RdesktopCanvas;
import com.sshtools.javardp.keymapping.KeyCode;
import com.sshtools.javardp.keymapping.KeyCode_FileBased;

public class Input {
	protected static final int KBD_FLAG_DOWN = 0x4000;
	protected static final int KBD_FLAG_EXT = 0x0100;
	// QUIET flag is actually as below (not 0x1000 as in rdesktop)
	protected static final int KBD_FLAG_QUIET = 0x200;
	// Using this flag value (0x0001) seems to do nothing, and after running
	// through other possible values, the RIGHT flag does not appear to be
	// implemented
	protected static final int KBD_FLAG_RIGHT = 0x0001;
	protected static final int KBD_FLAG_UP = 0x8000;
	protected static final int MOUSE_FLAG_BUTTON1 = 0x1000;
	protected static final int MOUSE_FLAG_BUTTON2 = 0x2000;
	protected static final int MOUSE_FLAG_BUTTON3 = 0x4000;
	protected static final int MOUSE_FLAG_BUTTON4 = 0x0280; // wheel up -
	// rdesktop 1.2.0
	protected static final int MOUSE_FLAG_BUTTON5 = 0x0380; // wheel down -
	// rdesktop 1.2.0
	protected static final int MOUSE_FLAG_DOWN = 0x8000;
	protected static final int MOUSE_FLAG_MOVE = 0x0800;
	protected static final int RDP_INPUT_CODEPOINT = 1;
	protected static final int RDP_INPUT_MOUSE = 0x8001;
	protected static final int RDP_INPUT_SCANCODE = 4;
	protected static final int RDP_INPUT_SYNCHRONIZE = 0;
	protected static final int RDP_INPUT_VIRTKEY = 2;
	protected static final int RDP_KEYPRESS = 0;
	protected static final int RDP_KEYRELEASE = KBD_FLAG_DOWN | KBD_FLAG_UP;
	protected static int time = 0;
	static Logger logger = LoggerFactory.getLogger(Input.class);
	public boolean keyDownWindows = false;
	public KeyEvent lastKeyEvent = null;
	public boolean modifiersValid = false;
	protected boolean altDown = false;
	protected RdesktopCanvas canvas = null;
	protected boolean capsLockOn = false;
	protected IContext context;
	protected boolean ctrlDown = false;
	protected long last_mousemove = 0;
	protected boolean numLockOn = false;
	protected Vector pressedKeys;
	protected boolean scrollLockOn = false;
	protected boolean serverAltDown = false;
	KeyCode keys = null;
	private RdesktopKeyAdapter keyListener;
	private RdesktopMouseAdapter mouseListener;
	private RdesktopMouseMotionAdapter mouseMotionListener;
	private State state;

	/**
	 * Create a new Input object with a given keymap object
	 * 
	 * @param c Canvas on which to listen for input events
	 * @param r Rdp layer on which to send input messages
	 * @param k Key map to use in handling keyboard events
	 */
	public Input(IContext context, State state, RdesktopCanvas canvas) {
		this.context = context;
		this.state = state;
		this.canvas = canvas;
		addInputListeners();
		pressedKeys = new Vector();
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.setDefaultFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.setDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
	}

	/**
	 * Add all relevant input listeners to the canvas
	 */
	public void addInputListeners() {
		if (mouseListener == null) {
			canvas.getDisplay().addMouseListener(mouseListener = new RdesktopMouseAdapter());
			canvas.getDisplay().addMouseMotionListener(mouseMotionListener = new RdesktopMouseMotionAdapter());
			canvas.getDisplay().addKeyListener(keyListener = new RdesktopKeyAdapter());
		}
		canvas.getDisplay().addMouseWheelListener(new RdesktopMouseWheelAdapter());
	}

	/**
	 * Release any modifier keys that may be depressed.
	 */
	public void clearKeys() {
		if (!modifiersValid)
			return;
		altDown = false;
		ctrlDown = false;
		if (lastKeyEvent == null)
			return;
		if (lastKeyEvent.isShiftDown())
			sendScancode(getTime(), RDP_KEYRELEASE, 0x2a); // shift
		if (lastKeyEvent.isAltDown() || serverAltDown) {
			sendScancode(getTime(), RDP_KEYRELEASE, 0x38); // ALT
			sendScancode(getTime(), RDP_KEYPRESS | KBD_FLAG_QUIET, 0x38); // ALT
			sendScancode(getTime(), RDP_KEYRELEASE | KBD_FLAG_QUIET, 0x38); // l.alt
		}
		if (lastKeyEvent.isControlDown()) {
			sendScancode(getTime(), RDP_KEYRELEASE, 0x1d); // l.ctrl
			// sendScancode(getTime(), RDP_KEYPRESS | KBD_FLAG_QUIET, 0x1d); //
			// Ctrl
			// sendScancode(getTime(), RDP_KEYRELEASE | KBD_FLAG_QUIET, 0x1d);
			// // ctrl
		}
		if (lastKeyEvent != null && lastKeyEvent.isAltGraphDown())
			sendScancode(getTime(), RDP_KEYRELEASE, 0x38 | KeyCode.SCANCODE_EXTENDED); // r.alt
	}

	/**
	 * Handle the main canvas gaining focus. Check locking key states.
	 */
	public void gainedFocus() {
		doLockKeys(); // ensure lock key states are correct
	}

	/**
	 * Act on any keyboard shortcuts that a specified KeyEvent may describe
	 * 
	 * @param time Time stamp for event to send to server
	 * @param e Keyboard event to be checked for shortcut keys
	 * @param pressed True if key was pressed, false if released
	 * @return True if a shortcut key combination was detected and acted upon,
	 *         false otherwise
	 */
	public boolean handleShortcutKeys(long time, KeyEvent e, boolean pressed) {
		if (!e.isAltDown())
			return false;
		if (!altDown)
			return false; // all of the below have ALT on
		switch (e.getKeyCode()) {
		/*
		 * case KeyEvent.VK_M: if(pressed) ((RdesktopFrame_Localised)
		 * canvas.getParent()).toggleMenu(); break;
		 */
		case KeyEvent.VK_ENTER:
			sendScancode(time, RDP_KEYRELEASE, 0x38);
			altDown = false;
			context.toggleFullScreen();
			break;
		/*
		 * The below case block handles "real" ALT+TAB events. Once the TAB in
		 * an ALT+TAB combination has been pressed, the TAB is sent to the
		 * server with the quiet flag on, as is the subsequent ALT-up.
		 * 
		 * This ensures that the initial ALT press is "undone" by the server.
		 * 
		 * --- Tom Elliott, 7/04/05
		 */
		case KeyEvent.VK_TAB: // Alt+Tab received, quiet combination
			sendScancode(time, (pressed ? RDP_KEYPRESS : RDP_KEYRELEASE) | KBD_FLAG_QUIET, 0x0f);
			if (!pressed) {
				sendScancode(time, RDP_KEYRELEASE | KBD_FLAG_QUIET, 0x38); // Release
				// Alt
			}
			if (pressed)
				if (logger.isDebugEnabled())
					logger.debug("Alt + Tab pressed, ignoring, releasing tab");
			break;
		case KeyEvent.VK_PAGE_UP: // Alt + PgUp = Alt-Tab
			sendScancode(time, pressed ? RDP_KEYPRESS : RDP_KEYRELEASE, 0x0f); // TAB
			if (pressed)
				if (logger.isDebugEnabled())
					logger.debug("shortcut pressed: sent ALT+TAB");
			break;
		case KeyEvent.VK_PAGE_DOWN: // Alt + PgDown = Alt-Shift-Tab
			if (pressed) {
				sendScancode(time, RDP_KEYPRESS, 0x2a); // Shift
				sendScancode(time, RDP_KEYPRESS, 0x0f); // TAB
				if (logger.isDebugEnabled())
					logger.debug("shortcut pressed: sent ALT+SHIFT+TAB");
			} else {
				sendScancode(time, RDP_KEYRELEASE, 0x0f); // TAB
				sendScancode(time, RDP_KEYRELEASE, 0x2a); // Shift
			}
			break;
		case KeyEvent.VK_INSERT: // Alt + Insert = Alt + Esc
			sendScancode(time, pressed ? RDP_KEYPRESS : RDP_KEYRELEASE, 0x01); // ESC
			if (pressed)
				if (logger.isDebugEnabled())
					logger.debug("shortcut pressed: sent ALT+ESC");
			break;
		case KeyEvent.VK_HOME: // Alt + Home = Ctrl + Esc (Start)
			if (pressed) {
				sendScancode(time, RDP_KEYRELEASE, 0x38); // ALT
				sendScancode(time, RDP_KEYPRESS, 0x1d); // left Ctrl
				sendScancode(time, RDP_KEYPRESS, 0x01); // Esc
				if (logger.isDebugEnabled())
					logger.debug("shortcut pressed: sent CTRL+ESC (Start)");
			} else {
				sendScancode(time, RDP_KEYRELEASE, 0x01); // escape
				sendScancode(time, RDP_KEYRELEASE, 0x1d); // left ctrl
				// sendScancode(time,RDP_KEYPRESS,0x38); // ALT
			}
			break;
		case KeyEvent.VK_END: // Ctrl+Alt+End = Ctrl+Alt+Del
			if (ctrlDown) {
				sendScancode(time, pressed ? RDP_KEYPRESS : RDP_KEYRELEASE, 0x53 | KeyCode.SCANCODE_EXTENDED); // DEL
				if (pressed)
					if (logger.isDebugEnabled())
						logger.debug("shortcut pressed: sent CTRL+ALT+DEL");
			}
			break;
		case KeyEvent.VK_DELETE: // Alt + Delete = Menu
			if (pressed) {
				sendScancode(time, RDP_KEYRELEASE, 0x38); // ALT
				// need to do another press and release to shift focus from
				// to/from menu bar
				sendScancode(time, RDP_KEYPRESS, 0x38); // ALT
				sendScancode(time, RDP_KEYRELEASE, 0x38); // ALT
				sendScancode(time, RDP_KEYPRESS, 0x5d | KeyCode.SCANCODE_EXTENDED); // Menu
				if (logger.isDebugEnabled())
					logger.debug("shortcut pressed: sent MENU");
			} else {
				sendScancode(time, RDP_KEYRELEASE, 0x5d | KeyCode.SCANCODE_EXTENDED); // Menu
				// sendScancode(time,RDP_KEYPRESS,0x38); // ALT
			}
			break;
		case KeyEvent.VK_SUBTRACT: // Ctrl + Alt + Minus (on NUM KEYPAD) =
			// Alt+PrtSc
			if (ctrlDown) {
				if (pressed) {
					sendScancode(time, RDP_KEYRELEASE, 0x1d); // Ctrl
					sendScancode(time, RDP_KEYPRESS, 0x37 | KeyCode.SCANCODE_EXTENDED); // PrtSc
					if (logger.isDebugEnabled())
						logger.debug("shortcut pressed: sent ALT+PRTSC");
				} else {
					sendScancode(time, RDP_KEYRELEASE, 0x37 | KeyCode.SCANCODE_EXTENDED); // PrtSc
					sendScancode(time, RDP_KEYPRESS, 0x1d); // Ctrl
				}
			}
			break;
		case KeyEvent.VK_ADD: // Ctrl + ALt + Plus (on NUM KEYPAD) = PrtSc
		case KeyEvent.VK_EQUALS: // for laptops that can't do Ctrl-Alt+Plus
			if (ctrlDown) {
				if (pressed) {
					sendScancode(time, RDP_KEYRELEASE, 0x38); // Alt
					sendScancode(time, RDP_KEYRELEASE, 0x1d); // Ctrl
					sendScancode(time, RDP_KEYPRESS, 0x37 | KeyCode.SCANCODE_EXTENDED); // PrtSc
					if (logger.isDebugEnabled())
						logger.debug("shortcut pressed: sent PRTSC");
				} else {
					sendScancode(time, RDP_KEYRELEASE, 0x37 | KeyCode.SCANCODE_EXTENDED); // PrtSc
					sendScancode(time, RDP_KEYPRESS, 0x1d); // Ctrl
					sendScancode(time, RDP_KEYPRESS, 0x38); // Alt
				}
			}
			break;
		default:
			return false;
		}
		if (!altDown)
			return false; // all of the below have ALT on
		switch (e.getKeyCode()) {
		case KeyEvent.VK_MINUS: // for laptops that can't do Ctrl+Alt+Minus
			if (ctrlDown) {
				if (pressed) {
					sendScancode(time, RDP_KEYRELEASE, 0x1d); // Ctrl
					sendScancode(time, RDP_KEYPRESS, 0x37 | KeyCode.SCANCODE_EXTENDED); // PrtSc
					if (logger.isDebugEnabled())
						logger.debug("shortcut pressed: sent ALT+PRTSC");
				} else {
					sendScancode(time, RDP_KEYRELEASE, 0x37 | KeyCode.SCANCODE_EXTENDED); // PrtSc
					sendScancode(time, RDP_KEYPRESS, 0x1d); // Ctrl
				}
			}
			break;
		default:
			return false;
		}
		return true;
	}

	/**
	 * Deal with modifier keys as control, alt or caps lock
	 * 
	 * @param time Time stamp for key event
	 * @param e Key event to check for special keys
	 * @param pressed True if key was pressed, false if released
	 * @return
	 */
	public boolean handleSpecialKeys(long time, KeyEvent e, boolean pressed) {
		if (handleShortcutKeys(time, e, pressed))
			return true;
		switch (e.getKeyCode()) {
		case KeyEvent.VK_CONTROL:
			ctrlDown = pressed;
			return false;
		case KeyEvent.VK_ALT:
			altDown = pressed;
			return false;
		case KeyEvent.VK_CAPS_LOCK:
			if (pressed && state.getOptions().isCapsSendsUpAndDown())
				capsLockOn = !capsLockOn;
			if (!state.getOptions().isCapsSendsUpAndDown()) {
				if (pressed)
					capsLockOn = true;
				else
					capsLockOn = false;
			}
			return false;
		case KeyEvent.VK_NUM_LOCK:
			if (pressed)
				numLockOn = !numLockOn;
			return false;
		case KeyEvent.VK_SCROLL_LOCK:
			if (pressed)
				scrollLockOn = !scrollLockOn;
			return false;
		case KeyEvent.VK_PAUSE: // untested
			if (pressed) { // E1 1D 45 E1 9D C5
				state.getRdp().sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0xe1, 0);
				state.getRdp().sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0x1d, 0);
				state.getRdp().sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0x45, 0);
				state.getRdp().sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0xe1, 0);
				state.getRdp().sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0x9d, 0);
				state.getRdp().sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0xc5, 0);
			} else { // release left ctrl
				state.getRdp().sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYRELEASE, 0x1d, 0);
			}
			break;
		// Removed, as java on MacOS send the option key as VK_META
		/*
		 * case KeyEvent.VK_META: // Windows key logger.debug("Windows key
		 * received"); if(pressed){ sendScancode(time, RDP_KEYPRESS, 0x1d); //
		 * left ctrl sendScancode(time, RDP_KEYPRESS, 0x01); // escape } else{
		 * sendScancode(time, RDP_KEYRELEASE, 0x01); // escape
		 * sendScancode(time, RDP_KEYRELEASE, 0x1d); // left ctrl } break;
		 */
		// haven't found a way to detect BREAK key in java - VK_BREAK doesn't
		// exist
		/*
		 * case KeyEvent.VK_BREAK: if(pressed){
		 * sendScancode(time,RDP_KEYPRESS,(KeyCode.SCANCODE_EXTENDED | 0x46));
		 * sendScancode(time,RDP_KEYPRESS,(KeyCode.SCANCODE_EXTENDED | 0xc6)); }
		 * // do nothing on release break;
		 */
		default:
			return false; // not handled - use sendScancode instead
		}
		return true; // handled - no need to use sendScancode
	}

	/**
	 * Handle loss of focus to the main canvas. Clears all depressed keys
	 * (sending release messages to the server.
	 */
	public void lostFocus() {
		clearKeys();
		modifiersValid = false;
	}

	/**
	 * Remove all relevant input listeners to the canvas
	 */
	public void removeInputListeners() {
		if (mouseListener != null) {
			canvas.getDisplay().removeMouseListener(mouseListener);
			canvas.getDisplay().removeMouseMotionListener(mouseMotionListener);
			canvas.getDisplay().removeKeyListener(keyListener);
			mouseListener = null;
			mouseMotionListener = null;
			keyListener = null;
		}
	}

	/**
	 * Send CTRL-ALT-DEL combination.
	 */
	public void sendCtrlAltDel() {
		sendScancode(getTime(), RDP_KEYPRESS, 0x1d); // CTRL
		sendScancode(getTime(), RDP_KEYPRESS, 0x38); // ALT
		sendScancode(getTime(), RDP_KEYPRESS, 0x53 | KeyCode.SCANCODE_EXTENDED); // DEL
		sendScancode(getTime(), RDP_KEYRELEASE, 0x53 | KeyCode.SCANCODE_EXTENDED); // DEL
		sendScancode(getTime(), RDP_KEYRELEASE, 0x38); // ALT
		sendScancode(getTime(), RDP_KEYRELEASE, 0x1d); // CTRL
	}

	/**
	 * Send a sequence of key actions to the server
	 * 
	 * @param pressSequence String representing a sequence of key actions.
	 *            Actions are represented as a pair of consecutive characters,
	 *            the first character's value (cast to integer) being the
	 *            scancode to send, the second (cast to integer) of the pair
	 *            representing the action (0 == UP, 1 == DOWN, 2 == QUIET UP, 3
	 *            == QUIET DOWN).
	 */
	public void sendKeyPresses(String pressSequence) {
		try {
			String debugString = "Sending keypresses: ";
			for (int i = 0; i < pressSequence.length(); i += 2) {
				int scancode = pressSequence.charAt(i);
				int action = pressSequence.charAt(i + 1);
				int flags = 0;
				if (action == KeyCode_FileBased.UP)
					flags = RDP_KEYRELEASE;
				else if (action == KeyCode_FileBased.DOWN)
					flags = RDP_KEYPRESS;
				else if (action == KeyCode_FileBased.QUIETUP)
					flags = RDP_KEYRELEASE | KBD_FLAG_QUIET;
				else if (action == KeyCode_FileBased.QUIETDOWN)
					flags = RDP_KEYPRESS | KBD_FLAG_QUIET;
				long t = getTime();
				debugString += "(0x" + Integer.toHexString(scancode) + ", "
						+ ((action == KeyCode_FileBased.UP || action == KeyCode_FileBased.QUIETUP) ? "up" : "down")
						+ ((flags & KBD_FLAG_QUIET) != 0 ? " quiet" : "") + " at " + t + ")";
				sendScancode(t, flags, scancode);
			}
			if (pressSequence.length() > 0)
				if (logger.isDebugEnabled())
					logger.debug(debugString);
		} catch (Exception ex) {
			return;
		}
	}

	/**
	 * Send a keyboard event to the server
	 * 
	 * @param time Time stamp to identify this event
	 * @param flags Flags defining the nature of the event (eg:
	 *            press/release/quiet/extended)
	 * @param scancode Scancode value identifying the key in question
	 */
	public void sendScancode(long time, int flags, int scancode) {
		if (scancode == 0x38) { // be careful with alt
			if ((flags & RDP_KEYRELEASE) != 0) {
				// logger.info("Alt release, serverAltDown = " + serverAltDown);
				serverAltDown = false;
			}
			if ((flags == RDP_KEYPRESS)) {
				// logger.info("Alt press, serverAltDown = " + serverAltDown);
				serverAltDown = true;
			}
		}
		if ((scancode & KeyCode.SCANCODE_EXTENDED) != 0) {
			state.getRdp().sendInput((int) time, RDP_INPUT_SCANCODE, flags | KBD_FLAG_EXT, scancode & ~KeyCode.SCANCODE_EXTENDED,
					0);
		} else
			state.getRdp().sendInput((int) time, RDP_INPUT_SCANCODE, flags, scancode, 0);
	}

	/**
	 * Send keypress events for any modifier keys that are currently down
	 */
	public void setKeys() {
		if (!modifiersValid)
			return;
		if (lastKeyEvent == null)
			return;
		if (lastKeyEvent.isShiftDown())
			sendScancode(getTime(), RDP_KEYPRESS, 0x2a); // shift
		if (lastKeyEvent.isAltDown())
			sendScancode(getTime(), RDP_KEYPRESS, 0x38); // l.alt
		if (lastKeyEvent.isControlDown())
			sendScancode(getTime(), RDP_KEYPRESS, 0x1d); // l.ctrl
		if (lastKeyEvent != null && lastKeyEvent.isAltGraphDown())
			sendScancode(getTime(), RDP_KEYPRESS, 0x38 | KeyCode.SCANCODE_EXTENDED); // r.alt
	}

	/**
	 * Turn off any locking key, check states if available
	 */
	public void triggerReadyToSend() {
		// rdp.sendInput(0, RDP_INPUT_SYNCHRONIZE, 0, 0, 0);
		capsLockOn = false;
		numLockOn = false;
		scrollLockOn = false;
		doLockKeys(); // ensure lock key states are correct
	}

	protected void doLockKeys() {
		if (logger.isDebugEnabled())
			logger.debug("doLockKeys");
		if (canvas.getDisplay().getLockingKeyState(KeyEvent.VK_CAPS_LOCK) != capsLockOn) {
			capsLockOn = !capsLockOn;
			if (logger.isDebugEnabled())
				logger.debug("CAPS LOCK toggle");
			sendScancode(getTime(), RDP_KEYPRESS, 0x3a);
			sendScancode(getTime(), RDP_KEYRELEASE, 0x3a);
		}
		if (canvas.getDisplay().getLockingKeyState(KeyEvent.VK_NUM_LOCK) != numLockOn) {
			numLockOn = !numLockOn;
			if (logger.isDebugEnabled())
				logger.debug("NUM LOCK toggle");
			sendScancode(getTime(), RDP_KEYPRESS, 0x45);
			sendScancode(getTime(), RDP_KEYRELEASE, 0x45);
		}
		if (canvas.getDisplay().getLockingKeyState(KeyEvent.VK_SCROLL_LOCK) != scrollLockOn) {
			scrollLockOn = !scrollLockOn;
			if (logger.isDebugEnabled())
				logger.debug("SCROLL LOCK toggle");
			sendScancode(getTime(), RDP_KEYPRESS, 0x46);
			sendScancode(getTime(), RDP_KEYRELEASE, 0x46);
		}
	}

	/**
	 * Handle pressing of the middle mouse button, sending relevent event data
	 * to the server
	 * 
	 * @param e MouseEvent detailing circumstances under which middle button was
	 *            pressed
	 */
	protected void middleButtonPressed(MouseEvent e) {
		/*
		 * if (Options.paste_hack && ctrlDown){ try{ canvas.setBusyCursor();
		 * }catch (RdesktopException ex){ logger.warn(ex.getMessage()); } if
		 * (capsLockOn){ logger.debug("Turning caps lock off for paste"); //
		 * turn caps lock off sendScancode(getTime(), RDP_KEYPRESS, 0x3a); //
		 * caps lock sendScancode(getTime(), RDP_KEYRELEASE, 0x3a); // caps lock
		 * } paste(); if (capsLockOn){ // turn caps lock back on
		 * logger.debug("Turning caps lock back on after paste");
		 * sendScancode(getTime(), RDP_KEYPRESS, 0x3a); // caps lock
		 * sendScancode(getTime(), RDP_KEYRELEASE, 0x3a); // caps lock }
		 * canvas.unsetBusyCursor(); } else
		 */
		state.getRdp().sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON3 | MOUSE_FLAG_DOWN, e.getX(), e.getY());
	}

	/**
	 * Handle release of the middle mouse button, sending relevent event data to
	 * the server
	 * 
	 * @param e MouseEvent detailing circumstances under which middle button was
	 *            released
	 */
	protected void middleButtonReleased(MouseEvent e) {
		/* if (!Options.paste_hack || !ctrlDown) */
		state.getRdp().sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON3, e.getX(), e.getY());
	}

	/**
	 * Retrieve the next "timestamp", by incrementing previous stamp (up to the
	 * maximum value of an integer, at which the timestamp is reverted to 1)
	 * 
	 * @return New timestamp value
	 */
	public static int getTime() {
		time++;
		if (time == Integer.MAX_VALUE)
			time = 1;
		return time;
	}

	class RdesktopKeyAdapter extends KeyAdapter {
		/**
		 * Construct an RdesktopKeyAdapter based on the parent KeyAdapter class
		 */
		public RdesktopKeyAdapter() {
			super();
		}

		/**
		 * Handle a keyPressed event, sending any relevant keypresses to the
		 * server
		 */
		@Override
		public void keyPressed(KeyEvent e) {
			lastKeyEvent = e;
			modifiersValid = true;
			long time = getTime();
			// Some java versions have keys that don't generate keyPresses -
			// here we add the key so we can later check if it happened
			pressedKeys.addElement(new Integer(e.getKeyCode()));
			if (logger.isDebugEnabled())
				logger.debug("PRESSED keychar='" + e.getKeyChar() + "' keycode=0x" + Integer.toHexString(e.getKeyCode()) + " char='"
						+ ((char) e.getKeyCode()) + "'");
			if (state.getRdp() != null) {
				if (!handleSpecialKeys(time, e, true)) {
					sendKeyPresses(state.getOptions().getKeymap().getKeyStrokes(e));
				}
				// sendScancode(time, RDP_KEYPRESS, keys.getScancode(e));
			}
		}

		/**
		 * Handle a keyReleased event, sending any relevent key events to the
		 * server
		 */
		@Override
		public void keyReleased(KeyEvent e) {
			// Some java versions have keys that don't generate keyPresses -
			// we added the key to the vector in keyPressed so here we check for
			// it
			Integer keycode = new Integer(e.getKeyCode());
			if (!pressedKeys.contains(keycode)) {
				this.keyPressed(e);
			}
			pressedKeys.removeElement(keycode);
			lastKeyEvent = e;
			modifiersValid = true;
			long time = getTime();
			if (logger.isDebugEnabled())
				logger.debug("RELEASED keychar='" + e.getKeyChar() + "' keycode=0x" + Integer.toHexString(e.getKeyCode())
						+ " char='" + ((char) e.getKeyCode()) + "'");
			if (state.getRdp() != null) {
				if (!handleSpecialKeys(time, e, false))
					sendKeyPresses(state.getOptions().getKeymap().getKeyStrokes(e));
				// sendScancode(time, RDP_KEYRELEASE, keys.getScancode(e));
			}
		}

		/**
		 * Handle a keyTyped event, sending any relevant keypresses to the
		 * server
		 */
		@Override
		public void keyTyped(KeyEvent e) {
			lastKeyEvent = e;
			modifiersValid = true;
			long time = getTime();
			// Some java versions have keys that don't generate keyPresses -
			// here we add the key so we can later check if it happened
			pressedKeys.addElement(new Integer(e.getKeyCode()));
			if (logger.isDebugEnabled())
				logger.debug("TYPED keychar='" + e.getKeyChar() + "' keycode=0x" + Integer.toHexString(e.getKeyCode()) + " char='"
						+ ((char) e.getKeyCode()) + "'");
			if (state.getRdp() != null) {
				if (!handleSpecialKeys(time, e, true))
					sendKeyPresses(state.getOptions().getKeymap().getKeyStrokes(e));
				// sendScancode(time, RDP_KEYPRESS, keys.getScancode(e));
			}
		}
	}

	class RdesktopMouseAdapter extends MouseAdapter {
		public RdesktopMouseAdapter() {
			super();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			int time = getTime();
			if (state.getRdp() != null) {
				if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
					if (logger.isDebugEnabled())
						logger.debug("Mouse Button 1 Pressed.");
					state.getRdp().sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON1 | MOUSE_FLAG_DOWN, e.getX(), e.getY());
				} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
					if (logger.isDebugEnabled())
						logger.debug("Mouse Button 3 Pressed.");
					state.getRdp().sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON2 | MOUSE_FLAG_DOWN, e.getX(), e.getY());
				} else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) {
					if (logger.isDebugEnabled())
						logger.debug("Middle Mouse Button Pressed.");
					middleButtonPressed(e);
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			int time = getTime();
			if (state.getRdp() != null) {
				if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
					state.getRdp().sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON1, e.getX(), e.getY());
				} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
					state.getRdp().sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON2, e.getX(), e.getY());
				} else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) {
					middleButtonReleased(e);
				}
			}
		}
	}

	class RdesktopMouseMotionAdapter extends MouseMotionAdapter {
		public RdesktopMouseMotionAdapter() {
			super();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			int time = getTime();
			// if(logger.isInfoEnabled()) logger.info("mouseMoved to
			// "+e.getX()+", "+e.getY()+" at "+time);
			if (state.getRdp() != null) {
				state.getRdp().sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_MOVE, e.getX(), e.getY());
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			int time = getTime();
			// Code to limit mouse events to 4 per second. Doesn't seem to
			// affect performance
			// long mTime = System.currentTimeMillis();
			// if((mTime - Input.last_mousemove) < 250) return;
			// Input.last_mousemove = mTime;
			// if(logger.isInfoEnabled()) logger.info("mouseMoved to
			// "+e.getX()+", "+e.getY()+" at "+time);
			// TODO: complete menu show/hide section
			/*
			 * if(e.getY() == 0) ((RdesktopFrame_Localised)
			 * canvas.getParent()).showMenu(); else ((RdesktopFrame_Localised)
			 * canvas.getParent()).hideMenu();
			 */
			if (state.getRdp() != null) {
				state.getRdp().sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_MOVE, e.getX(), e.getY());
			}
		}
	}

	private class RdesktopMouseWheelAdapter implements MouseWheelListener {
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int time = getTime();
			// if(logger.isInfoEnabled()) logger.info("mousePressed at "+time);
			if (state.getRdp() != null) {
				if (e.getWheelRotation() < 0) { // up
					state.getRdp().sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON4 | MOUSE_FLAG_DOWN, e.getX(), e.getY());
				} else { // down
					state.getRdp().sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON5 | MOUSE_FLAG_DOWN, e.getX(), e.getY());
				}
			}
		}
	}
}
