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
import com.sshtools.javardp.Options;
import com.sshtools.javardp.RdpPacket;

/**
 * @author Tom Elliott
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestChannel extends VChannel {
	public TestChannel(IContext context, Options options, String name, int flags) {
		super(context, options);
		this.name = name;
		this.flags = flags;
	}

	private String name;
	private int flags;

	public String name() {
		return name;
	}

	public int flags() {
		return flags;
	}

	public void process(RdpPacket data) {
	}
}
