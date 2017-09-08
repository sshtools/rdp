/* VChannel.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Abstract class for RDP5 channels
 */
package com.sshtools.javardp.rdp5;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.IContext;
import com.sshtools.javardp.Packet;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.SecurityType;
import com.sshtools.javardp.State;
import com.sshtools.javardp.layers.Secure;

public abstract class VChannel {
	static Logger logger = LoggerFactory.getLogger(VChannel.class);
	protected IContext context;
	protected State state;
	protected Secure secure;
	private int mcs_id = 0;

	public final void start(IContext context, State state, Secure secure) {
		this.state = state;
		this.secure = secure;
		this.context = context;
		onStart();
	}

	protected void onStart() {
	}

	/**
	 * Provide the set of flags specifying working options for this channel
	 * 
	 * @return Option flags
	 */
	public abstract int flags();

	/**
	 * Initialise a packet for transmission over this virtual channel
	 * 
	 * @param length Desired length of packet
	 * @return Packet prepared for this channel
	 * @throws RdesktopException
	 */
	public Packet init(int length) throws RdesktopException {
		Packet s;
		s = secure.init(state.getSecurityType() == SecurityType.STANDARD ? Secure.SEC_ENCRYPT : 0, length + 8);
		s.setHeader(Packet.CHANNEL_HEADER);
		s.incrementPosition(8);
		return s;
	}

	public int mcs_id() {
		return mcs_id;
	}

	/**
	 * Provide the name of this channel
	 * 
	 * @return Channel name as string
	 */
	public abstract String name();

	/**
	 * Process a packet sent on this channel
	 * 
	 * @param data Packet sent to this channel
	 * @throws RdesktopException
	 * @throws IOException
	 */
	public abstract void process(Packet data) throws RdesktopException, IOException;

	/**
	 * Send a packet over this virtual channel
	 * 
	 * @param data Packet to be sent
	 * @throws RdesktopException
	 * @throws IOException
	 */
	public void send_packet(Packet data) throws RdesktopException, IOException {
		if (secure == null)
			return;
		int length = data.size();
		int data_offset = 0;
		int num_packets = (length / VChannels.CHANNEL_CHUNK_LENGTH);
		num_packets += length - (VChannels.CHANNEL_CHUNK_LENGTH) * num_packets;
		while (data_offset < length) {
			int thisLength = Math.min(VChannels.CHANNEL_CHUNK_LENGTH, length - data_offset);
			Packet s = secure.init(state.getSecurityType() == SecurityType.STANDARD ? Secure.SEC_ENCRYPT : 0, 8 + thisLength);
			s.setLittleEndian32(length);
			int flags = ((data_offset == 0) ? VChannels.CHANNEL_FLAG_FIRST : 0);
			if (data_offset + thisLength >= length)
				flags |= VChannels.CHANNEL_FLAG_LAST;
			if ((this.flags() & VChannels.CHANNEL_OPTION_SHOW_PROTOCOL) != 0)
				flags |= VChannels.CHANNEL_FLAG_SHOW_PROTOCOL;
			s.setLittleEndian32(flags);
			s.copyFromPacket(data, data_offset, s.getPosition(), thisLength);
			s.incrementPosition(thisLength);
			s.markEnd();
			data_offset += thisLength;
			secure.send_to_channel(s, state.getSecurityType() == SecurityType.STANDARD ? Secure.SEC_ENCRYPT : 0, this.mcs_id());
		}
	}

	/**
	 * Set the MCS ID for this channel
	 * 
	 * @param mcs_id New MCS ID
	 */
	public void set_mcs_id(int mcs_id) {
		this.mcs_id = mcs_id;
	}
}
