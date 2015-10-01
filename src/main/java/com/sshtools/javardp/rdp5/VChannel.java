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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sshtools.javardp.Constants;
import com.sshtools.javardp.IContext;
import com.sshtools.javardp.Input;
import com.sshtools.javardp.Options;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.RdpPacket;
import com.sshtools.javardp.RdpPacket;
import com.sshtools.javardp.Secure;
import com.sshtools.javardp.crypto.CryptoException;

public abstract class VChannel {
	protected static Log logger = LogFactory.getLog(Input.class);
	private int mcs_id = 0;
	protected Options options;
	private IContext context;

	/**
	 * Provide the name of this channel
	 * 
	 * @return Channel name as string
	 */
	public abstract String name();

	/**
	 * Provide the set of flags specifying working options for this channel
	 * 
	 * @return Option flags
	 */
	public abstract int flags();

	/**
	 * Process a packet sent on this channel
	 * 
	 * @param data Packet sent to this channel
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 */
	public abstract void process(RdpPacket data) throws RdesktopException, IOException, CryptoException;

	public int mcs_id() {
		return mcs_id;
	}

	/**
	 * Set the MCS ID for this channel
	 * 
	 * @param mcs_id New MCS ID
	 */
	public void set_mcs_id(int mcs_id) {
		this.mcs_id = mcs_id;
	}

	public VChannel(IContext context, Options options) {
		this.options = options;
		this.context = context;
	}

	/**
	 * Initialise a packet for transmission over this virtual channel
	 * 
	 * @param length Desired length of packet
	 * @return Packet prepared for this channel
	 * @throws RdesktopException
	 */
	public RdpPacket init(int length) throws RdesktopException {
		RdpPacket s;
		s = context.getSecure().init(options.encryption ? Secure.SEC_ENCRYPT : 0, length + 8);
		s.setHeader(RdpPacket.CHANNEL_HEADER);
		s.incrementPosition(8);
		return s;
	}

	/**
	 * Send a packet over this virtual channel
	 * 
	 * @param data Packet to be sent
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 */
	public void send_packet(RdpPacket data) throws RdesktopException, IOException, CryptoException {
		if (context.getSecure() == null)
			return;
		int length = data.size();
		int data_offset = 0;
		int packets_sent = 0;
		int num_packets = (length / VChannels.CHANNEL_CHUNK_LENGTH);
		num_packets += length - (VChannels.CHANNEL_CHUNK_LENGTH) * num_packets;
		while (data_offset < length) {
			int thisLength = Math.min(VChannels.CHANNEL_CHUNK_LENGTH, length - data_offset);
			RdpPacket s = context.getSecure().init(Constants.encryption ? Secure.SEC_ENCRYPT : 0, 8 + thisLength);
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
			if (context.getSecure() != null)
				context.getSecure().send_to_channel(s, Constants.encryption ? Secure.SEC_ENCRYPT : 0, this.mcs_id());
			packets_sent++;
		}
	}
}
