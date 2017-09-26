/* VChannels.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Static store for all registered channels 
 */
package com.sshtools.javardp.rdp5;

import java.io.IOException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.Packet;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.State;
import com.sshtools.javardp.Utilities;
import com.sshtools.javardp.layers.MCS;

public class VChannels implements Iterable<VChannel> {
	public static final int CHANNEL_CHUNK_LENGTH = 1600;
	public static final int CHANNEL_FLAG_FIRST = 0x01;
	public static final int CHANNEL_FLAG_LAST = 0x02;
	public static final int CHANNEL_FLAG_SHOW_PROTOCOL = 0x10;
	public static final int CHANNEL_OPTION_COMPRESS_RDP = 0x00800000;
	public static final int CHANNEL_OPTION_ENCRYPT_RDP = 0x40000000;
	/* Virtual channel options */
	public static final int CHANNEL_OPTION_INITIALIZED = 0x80000000;
	public static final int CHANNEL_OPTION_SHOW_PROTOCOL = 0x00200000;
	public static final int MAX_CHANNELS = 4;
	public static final int STATUS_ACCESS_DENIED = 0xc0000022;
	public static final int STATUS_INVALID_DEVICE_REQUEST = 0xc0000010;
	public static final int STATUS_INVALID_PARAMETER = 0xc000000d;
	/* NT status codes for RDPDR */
	public static final int STATUS_SUCCESS = 0x00000000;
	public static final int WAVE_FORMAT_ADPCM = 2;
	public static final int WAVE_FORMAT_ALAW = 6;
	public static final int WAVE_FORMAT_MULAW = 7;
	/* Sound format constants */
	public static final int WAVE_FORMAT_PCM = 1;
	static Logger logger = LoggerFactory.getLogger(VChannels.class);
	private VChannel channels[] = new VChannel[MAX_CHANNELS];
	private byte[] fragment_buffer = null;
	private int num_channels;
	private State state;

	/**
	 * Initialise the maximum number of Virtual Channels
	 */
	public VChannels(State state) {
		this.state = state;
		channels = new VChannel[MAX_CHANNELS];
	}

	/**
	 * Retrieve the VChannel object for the numbered channel
	 * 
	 * @param c Channel number
	 * @return The requested Virtual Channel
	 */
	public VChannel channel(int c) {
		if (c < num_channels)
			return channels[c];
		else
			return null;
	}

	/**
	 * Process a packet sent on a numbered channel
	 * 
	 * @param data Packet sent to channel
	 * @param mcsChannel Number specified for channel
	 * @throws RdesktopException
	 * @throws IOException
	 */
	public void channel_process(Packet data, int mcsChannel) throws RdesktopException, IOException {
		int flags = 0;
		VChannel channel = null;
		int i;
		for (i = 0; i < num_channels; i++) {
			if (mcs_id(i) == mcsChannel) {
				channel = channels[i];
				break;
			}
		}
		if (i >= num_channels) {
			logger.warn(String.format("Data from unknown channel %d", mcsChannel));
			return;
		}
		data.getLittleEndian32(); // length
		flags = data.getLittleEndian32();
		if (((flags & CHANNEL_FLAG_FIRST) != 0) && ((flags & CHANNEL_FLAG_LAST) != 0)) {
			// single fragment - pass straight up
			logger.debug(String.format("%s will process packet", channel));
			channel.process(data);
		} else {
			// append to the defragmentation buffer
			byte[] content = new byte[data.getEnd() - data.getPosition()];
			data.copyToByteArray(content, 0, data.getPosition(), content.length);
			fragment_buffer = Utilities.concatenateBytes(fragment_buffer, content);
			if ((flags & CHANNEL_FLAG_LAST) != 0) {
				Packet fullpacket = new Packet(fragment_buffer.length);
				fullpacket.copyFromByteArray(fragment_buffer, 0, 0, fragment_buffer.length);
				// process the entire reconstructed packet
				logger.debug(String.format("%s will process fully constructed packet", channel));
				channel.process(fullpacket);
				fragment_buffer = null;
			}
		}
	}

	/**
	 * Remove all registered virtual channels
	 */
	public void clear() {
		channels = new VChannel[MAX_CHANNELS];
		num_channels = 0;
	}

	/**
	 * Retrieve the VChannel object for the specified MCS channel ID
	 * 
	 * @param channelno MCS ID for the required channel
	 * @return Virtual Channel associated with the supplied MCS ID
	 */
	public VChannel find_channel_by_channelno(int channelno) {
		if (channelno > MCS.MCS_GLOBAL_CHANNEL + num_channels) {
			logger.warn(
					"Channel " + channelno + " not defined. Highest channel defined is " + MCS.MCS_GLOBAL_CHANNEL + num_channels);
			return null;
		} else
			return channels[channelno - MCS.MCS_GLOBAL_CHANNEL - 1];
	}

	/**
	 * Obtain the MCS ID for a specific numbered channel
	 * 
	 * @param c Channel number for which to obtain MCS ID
	 * @return MCS ID associated with the supplied channel number
	 */
	public int mcs_id(int c) {
		return MCS.MCS_GLOBAL_CHANNEL + 1 + c;
	}

	public int num_channels() {
		return num_channels;
	}

	/**
	 * Register a new virtual channel
	 * 
	 * @param v Virtual channel to be registered
	 * @return True if successful
	 * @throws RdesktopException
	 */
	public boolean register(VChannel v) throws RdesktopException {
		if (!state.isRDP5()) {
			return false;
		}
		if (num_channels >= MAX_CHANNELS)
			throw new RdesktopException("Channel table full. Could not register channel.");
		channels[num_channels] = v;
		v.set_mcs_id(MCS.MCS_GLOBAL_CHANNEL + 1 + num_channels);
		num_channels++;
		return true;
	}

	@Override
	public Iterator<VChannel> iterator() {
		return new Iterator<VChannel>() {
			int idx = -1;

			@Override
			public boolean hasNext() {
				return idx + 1 < num_channels;
			}

			@Override
			public VChannel next() {
				idx++;
				return channels[idx];
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
