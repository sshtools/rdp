/* Rdp5.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Handle RDP5 orders
 */
package com.sshtools.javardp.rdp5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.IContext;
import com.sshtools.javardp.Options;
import com.sshtools.javardp.OrderException;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.Rdp;
import com.sshtools.javardp.RdpPacket;
import com.sshtools.javardp.crypto.CryptoException;
import com.sshtools.javardp.rdp5.cliprdr.DIBHandler;

public class Rdp5 extends Rdp {
	
	static Logger logger = LoggerFactory.getLogger(Rdp5.class);
	
	private VChannels channels;

	/**
	 * Initialise the RDP5 communications layer, with specified virtual channels
	 * 
	 * @param channels Virtual channels for RDP layer
	 */
	public Rdp5(IContext context, Options options, VChannels channels) {
		super(context, options, channels);
		this.channels = channels;
	}

	/**
	 * Process an RDP5 packet
	 * 
	 * @param s Packet to be processed
	 * @param e True if packet is encrypted
	 * @throws RdesktopException
	 * @throws OrderException
	 * @throws CryptoException
	 */
	public void rdp5_process(RdpPacket s, boolean e) throws RdesktopException, OrderException, CryptoException {
		rdp5_process(s, e, false);
	}

	/**
	 * Process an RDP5 packet
	 * 
	 * @param s Packet to be processed
	 * @param encryption True if packet is encrypted
	 * @param shortform True if packet is of the "short" form
	 * @throws RdesktopException
	 * @throws OrderException
	 * @throws CryptoException
	 */
	public void rdp5_process(RdpPacket s, boolean encryption, boolean shortform) throws RdesktopException,
			OrderException, CryptoException {
		logger.debug("Processing RDP 5 order");
		int length, count;
		int type;
		int next;
		if (encryption) {
			s.incrementPosition(shortform ? 6 : 7 /* XXX HACK */); /* signature */
			byte[] data = new byte[s.size() - s.getPosition()];
			s.copyToByteArray(data, 0, s.getPosition(), data.length);
			byte[] packet = secureLayer.decrypt(data);
		}
		// printf("RDP5 data:\n");
		// hexdump(s->p, s->end - s->p);
		while (s.getPosition() < s.getEnd()) {
			type = s.get8();
			length = s.getLittleEndian16();
			/* next_packet = */next = s.getPosition() + length;
			logger.debug("RDP5: type = " + type);
			switch (type) {
			case 0: /* orders */
				count = s.getLittleEndian16();
				orders.processOrders(s, next, count);
				break;
			case 1: /* bitmap update (???) */
				s.incrementPosition(2); /* part length */
				processBitmapUpdates(s);
				break;
			case 2: /* palette */
				s.incrementPosition(2);
				processPalette(s);
				break;
			case 3: /* probably an palette with offset 3. Weird */
				break;
			case 5:
				process_null_system_pointer_pdu(s);
				break;
			case 6: // default pointer
				break;
			case 9:
				process_colour_pointer_pdu(s);
				break;
			case 10:
				process_cached_pointer_pdu(s);
				break;
			default:
				logger.warn("Unimplemented RDP5 opcode " + type);
			}
			s.setPosition(next);
		}
	}

	/**
	 * Process an RDP5 packet from a virtual channel
	 * 
	 * @param s Packet to be processed
	 * @param channelno Channel on which packet was received
	 */
	void rdp5_process_channel(RdpPacket s, int channelno) {
		VChannel channel = channels.find_channel_by_channelno(channelno);
		if (channel != null) {
			try {
				channel.process(s);
			} catch (Exception e) {
			}
		}
	}
}
