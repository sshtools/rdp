/* ClipChannel.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: 
 */
package com.sshtools.javardp.rdp5.cliprdr;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.CommunicationMonitor;
import com.sshtools.javardp.Constants;
import com.sshtools.javardp.IContext;
import com.sshtools.javardp.Options;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.RdpPacket;
import com.sshtools.javardp.Secure;
import com.sshtools.javardp.crypto.CryptoException;
import com.sshtools.javardp.rdp5.VChannel;
import com.sshtools.javardp.rdp5.VChannels;

public class ClipChannel extends VChannel implements ClipInterface, ClipboardOwner, FocusListener {

	static Logger logger = LoggerFactory.getLogger(ClipChannel.class);
	
	String[] types = { "unused", "CF_TEXT", "CF_BITMAP", "CF_METAFILEPICT", "CF_SYLK", "CF_DIF", "CF_TIFF", "CF_OEMTEXT", "CF_DIB",
		"CF_PALETTE", "CF_PENDATA", "CF_RIFF", "CF_WAVE", "CF_UNICODETEXT", "CF_ENHMETAFILE", "CF_HDROP", "CF_LOCALE", "CF_MAX" };
	
	// Message types
	public static final int CLIPRDR_CONNECT = 1;
	public static final int CLIPRDR_FORMAT_ANNOUNCE = 2;
	public static final int CLIPRDR_FORMAT_ACK = 3;
	public static final int CLIPRDR_DATA_REQUEST = 4;
	public static final int CLIPRDR_DATA_RESPONSE = 5;
	// Message status codes
	public static final int CLIPRDR_REQUEST = 0;
	public static final int CLIPRDR_RESPONSE = 1;
	public static final int CLIPRDR_ERROR = 2;
	Clipboard clipboard;
	// TypeHandler for data currently being awaited
	TypeHandler currentHandler = null;
	// All type handlers available
	TypeHandlerList allHandlers = null;
	byte[] localClipData = null;
	private IContext context;

	public ClipChannel(IContext context, Options options) {
		super(context, options);
		this.context = context;
		this.clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		// initialise all clipboard format handlers
		allHandlers = new TypeHandlerList();
		allHandlers.add(new UnicodeHandler());
		allHandlers.add(new TextHandler());
		allHandlers.add(new DIBHandler(context));
		// allHandlers.add(new MetafilepictHandler());
	}

	/*
	 * VChannel inherited abstract methods
	 */
	public String name() {
		return "cliprdr";
	}

	public int flags() {
		return VChannels.CHANNEL_OPTION_INITIALIZED | VChannels.CHANNEL_OPTION_ENCRYPT_RDP | VChannels.CHANNEL_OPTION_COMPRESS_RDP
			| VChannels.CHANNEL_OPTION_SHOW_PROTOCOL;
	}

	/*
	 * Data processing methods
	 */
	public void process(RdpPacket data) throws RdesktopException, IOException, CryptoException {
		int type, status;
		int length, format;
		type = data.getLittleEndian16();
		status = data.getLittleEndian16();
		length = data.getLittleEndian32();
		if (status == CLIPRDR_ERROR) {
			if (type == CLIPRDR_FORMAT_ACK) {
				send_format_announce();
				return;
			}
			return;
		}
		switch (type) {
		case CLIPRDR_CONNECT:
			send_format_announce();
			break;
		case CLIPRDR_FORMAT_ANNOUNCE:
			handle_clip_format_announce(data, length);
			return;
		case CLIPRDR_FORMAT_ACK:
			break;
		case CLIPRDR_DATA_REQUEST:
			handle_data_request(data);
			break;
		case CLIPRDR_DATA_RESPONSE:
			handle_data_response(data, length);
			break;
		case 7:
			break;
		default:
			// System.out.println("Unimplemented packet type! " + type);
		}
	}

	public void send_null(int type, int status) {
		RdpPacket s;
		s = new RdpPacket(12);
		s.setLittleEndian16(type);
		s.setLittleEndian16(status);
		s.setLittleEndian32(0);
		s.setLittleEndian32(0); // pad
		s.markEnd();
		try {
			this.send_packet(s);
		} catch (RdesktopException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		} catch (CryptoException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	void send_format_announce() throws RdesktopException, IOException, CryptoException {
		Transferable clipData = clipboard.getContents(clipboard);
		DataFlavor[] dataTypes = clipData.getTransferDataFlavors();
		TypeHandlerList availableFormats = allHandlers.getHandlersForClipboard(dataTypes);
		RdpPacket s;
		int number_of_formats = availableFormats.count();
		s = new RdpPacket(number_of_formats * 36 + 12);
		s.setLittleEndian16(CLIPRDR_FORMAT_ANNOUNCE);
		s.setLittleEndian16(CLIPRDR_REQUEST);
		s.setLittleEndian32(number_of_formats * 36);
		TypeHandler handler = null;
		for (Iterator i = availableFormats.iterator(); i.hasNext();) {
			handler = (TypeHandler) i.next();
			s.setLittleEndian32(handler.preferredFormat());
			s.incrementPosition(32);
		}
		s.setLittleEndian32(0); // pad
		s.markEnd();
		send_packet(s);
	}

	private void handle_clip_format_announce(RdpPacket data, int length) throws RdesktopException, IOException, CryptoException {
		TypeHandlerList serverTypeList = new TypeHandlerList();
		// System.out.print("Available types: ");
		for (int c = length; c >= 36; c -= 36) {
			int typeCode = data.getLittleEndian32();
			// if(typeCode < types.length) System.out.print(types[typeCode] +
			// " ");
			data.incrementPosition(32);
			serverTypeList.add(allHandlers.getHandlerForFormat(typeCode));
		}
		// System.out.println();
		send_null(CLIPRDR_FORMAT_ACK, CLIPRDR_RESPONSE);
		currentHandler = serverTypeList.getFirst();
		if (currentHandler != null)
			request_clipboard_data(currentHandler.preferredFormat());
	}

	void handle_data_request(RdpPacket data) throws RdesktopException, IOException, CryptoException {
		int format = data.getLittleEndian32();
		Transferable clipData = clipboard.getContents(this);
		byte[] outData = null;
		TypeHandler outputHandler = allHandlers.getHandlerForFormat(format);
		if (outputHandler != null) {
			outputHandler.send_data(clipData, this);
			// outData = outputHandler.fromTransferable(clipData);
			// if(outData != null){
			// send_data(outData,outData.length);
			// return;
			// }
			// else System.out.println("Clipboard data to send == null!");
		}
		// this.send_null(CLIPRDR_DATA_RESPONSE,CLIPRDR_ERROR);
	}

	void handle_data_response(RdpPacket data, int length) {
		// if(currentHandler !=
		// null)clipboard.setContents(currentHandler.handleData(data,
		// length),this);
		// currentHandler = null;
		if (currentHandler != null)
			currentHandler.handleData(data, length, this);
		currentHandler = null;
	}

	void request_clipboard_data(int formatcode) throws RdesktopException, IOException, CryptoException {
		RdpPacket s = context.getSecure().init(Constants.encryption ? Secure.SEC_ENCRYPT : 0, 24);
		s.setLittleEndian32(16); // length
		int flags = VChannels.CHANNEL_FLAG_FIRST | VChannels.CHANNEL_FLAG_LAST;
		if ((this.flags() & VChannels.CHANNEL_OPTION_SHOW_PROTOCOL) != 0)
			flags |= VChannels.CHANNEL_FLAG_SHOW_PROTOCOL;
		s.setLittleEndian32(flags);
		s.setLittleEndian16(CLIPRDR_DATA_REQUEST);
		s.setLittleEndian16(CLIPRDR_REQUEST);
		s.setLittleEndian32(4); // Remaining length
		s.setLittleEndian32(formatcode);
		s.setLittleEndian32(0); // Unknown. Garbage pad?
		s.markEnd();
		context.getSecure().send_to_channel(s, Constants.encryption ? Secure.SEC_ENCRYPT : 0, this.mcs_id());
	}

	public void send_data(byte[] data, int length) {
		CommunicationMonitor.lock(this);
		RdpPacket all = new RdpPacket(12 + length);
		all.setLittleEndian16(CLIPRDR_DATA_RESPONSE);
		all.setLittleEndian16(CLIPRDR_RESPONSE);
		all.setLittleEndian32(length + 4); // don't know why, but we need to add
		// between 1 and 4 to the length,
		// otherwise the server cliprdr thread hangs
		all.copyFromByteArray(data, 0, all.getPosition(), length);
		all.incrementPosition(length);
		all.setLittleEndian32(0);
		try {
			this.send_packet(all);
		} catch (RdesktopException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			if (!context.isUnderApplet())
				context.exit();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			if (!context.isUnderApplet())
				context.exit();
		} catch (CryptoException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			if (!context.isUnderApplet())
				context.exit();
		}
		CommunicationMonitor.unlock(this);
	}

	/*
	 * FocusListener methods
	 */
	public void focusGained(FocusEvent arg0) {
		// synchronise the clipboard types here, so the server knows what's
		// available
		if (options.use_rdp5) {
			try {
				send_format_announce();
			} catch (RdesktopException e) {
			} catch (IOException e) {
			} catch (CryptoException e) {
			}
		}
	}

	public void focusLost(FocusEvent arg0) {
	}

	/*
	 * Support methods
	 */
	private void reset_bool(boolean[] x) {
		for (int i = 0; i < x.length; i++)
			x[i] = false;
	}

	/*
	 * ClipboardOwner methods
	 */
	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		logger.debug("Lost clipboard ownership");
	}

	public void copyToClipboard(Transferable t) {
		clipboard.setContents(t, this);
	}

	public Clipboard getClipboard() {
		return clipboard;
	}

	public void setClipboard(Clipboard clipboard) {
		this.clipboard = clipboard;
	}
}
