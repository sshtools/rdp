/* RdpMenu.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:43 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Menu bar for main frame
 */
package com.sshtools.javardp.client;

import java.awt.Event;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.sshtools.javardp.graphics.RdesktopCanvas;

public class RdpMenu extends MenuBar {
	RdesktopFrame parent;

	/**
	 * Initialise the properJavaRDP menu bar and attach to an RdesktopFrame
	 * 
	 * @param parent Menu is attached to this frame
	 */
	public RdpMenu(final RdesktopFrame parent) {
		MenuItem item;
		this.parent = parent;
		Menu m = new Menu("File");
		item = new MenuItem("Send CTRL-ALT-DEL");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendCtrlAltDel();
			}
		});
		m.add(item);
		m.addSeparator();
		item = new MenuItem("Exit");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				parent.dispose();
			}
		});
		m.add(item);
		this.add(m);
		/*
		 * m = new Menu("Input"); m.add(new MenuItem("Insert Symbol"));
		 * m.addSeparator(); m.add(new MenuItem("Turn Caps-Lock On")); m.add(new
		 * MenuItem("Turn Num-Lock On")); m.add(new
		 * MenuItem("Turn Scroll-Lock On")); this.add(m);
		 * 
		 * m = new Menu("Display"); MenuItem mi = null;
		 * 
		 * if(!Options.fullscreen){ mi = new MenuItem("Fullscreen Mode");
		 * mi.disable(); }else mi = new MenuItem("Windowed Mode");
		 * 
		 * m.add(mi); this.add(m);
		 */
	}

	/**
	 * @param event event
	 * @param arg  argument
	 * @return handled
	 * @deprecated Replaced by action listeners.
	 */
	@Deprecated
	public boolean action(Event event, Object arg) {
		/*
		 * if(arg == "Turn Caps-Lock On") ((MenuItem)
		 * event.target).setLabel("Turn Caps-Lock Off"); if(arg ==
		 * "Turn Caps-Lock Off") ((MenuItem)
		 * event.target).setLabel("Turn Caps-Lock On");
		 * 
		 * if(arg == "Turn Num-Lock On") ((MenuItem)
		 * event.target).setLabel("Turn Num-Lock Off"); if(arg ==
		 * "Turn Num-Lock Off") ((MenuItem)
		 * event.target).setLabel("Turn Num-Lock On");
		 * 
		 * if(arg == "Turn Scroll-Lock On") ((MenuItem)
		 * event.target).setLabel("Turn Scroll-Lock Off"); if(arg ==
		 * "Turn Scroll-Lock Off") ((MenuItem)
		 * event.target).setLabel("Turn Scroll-Lock On");
		 * 
		 * if(arg == "Exit") Common.exit();
		 * 
		 * if(arg == "Fullscreen Mode"){ parent.goFullScreen(); ((MenuItem)
		 * event.target).setLabel("Windowed Mode"); }
		 * 
		 * if(arg == "Windowed Mode"){ parent.leaveFullScreen(); ((MenuItem)
		 * event.target).setLabel("Fullscreen Mode"); }
		 */
		return false;
	}

	private void sendCtrlAltDel() {
		RdesktopCanvas canvas = parent.getCanvas();
		canvas.getInput().sendCtrlAltDel();
	}
}
