/* TestChannel.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Dummy RDP5 channel for testing purposes
 */
package com.sshtools.javardp.rdp5;

import com.sshtools.javardp.IContext;
import com.sshtools.javardp.RdpPacket;
import com.sshtools.javardp.State;

/**
 * @author Tom Elliott
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestChannel extends VChannel {
	private int flags;

	private String name;
	public TestChannel(IContext context, State state, String name, int flags) {
		super(context, state);
		this.name = name;
		this.flags = flags;
	}

	@Override
	public int flags() {
		return flags;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public void process(RdpPacket data) {
	}
}
