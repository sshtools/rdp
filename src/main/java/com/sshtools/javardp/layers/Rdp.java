/* Rdp.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Rdp layer of communication
 */
package com.sshtools.javardp.layers;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.IndexColorModel;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.ConnectionException;
import com.sshtools.javardp.CredentialProvider;
import com.sshtools.javardp.IContext;
import com.sshtools.javardp.Options;
import com.sshtools.javardp.OrderException;
import com.sshtools.javardp.Orders;
import com.sshtools.javardp.Packet;
import com.sshtools.javardp.RdesktopDisconnectException;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.SecurityType;
import com.sshtools.javardp.State;
import com.sshtools.javardp.CredentialProvider.CredentialType;
import com.sshtools.javardp.graphics.Bitmap;
import com.sshtools.javardp.graphics.RdesktopCanvas;
import com.sshtools.javardp.graphics.RdpCursor;
import com.sshtools.javardp.io.IO;
import com.sshtools.javardp.rdp5.VChannel;
import com.sshtools.javardp.rdp5.VChannels;

public class Rdp implements Layer<Layer<?>> {
	static Logger logger = LoggerFactory.getLogger(Rdp.class);
	/* RDP bitmap cache (version 2) constants */
	public static final int BMPCACHE2_C0_CELLS = 0x78;
	public static final int BMPCACHE2_C1_CELLS = 0x78;
	public static final int BMPCACHE2_C2_CELLS = 0x150;
	public static final int BMPCACHE2_NUM_PSTCELLS = 0x9f6;
	public final static byte CONNECTION_TYPE_AUTODETECT = 0x08;
	public final static byte CONNECTION_TYPE_BROADBAND_HIGH = 0x0;
	public final static byte CONNECTION_TYPE_BROADBAND_LOW = 0x02;
	public final static byte CONNECTION_TYPE_LAN = 0x06;
	/* Connection types */
	public final static byte CONNECTION_TYPE_MODEM = 0x01;
	public final static byte CONNECTION_TYPE_SATELLITE = 0x03;
	public final static byte CONNECTION_TYPE_WAN = 0x05;
	public final static int KEYBOARD_TYPE_IBM_102_OR_102 = 0x00000004;
	public final static int KEYBOARD_TYPE_IBM_PC_AT = 0x00000003;
	/* Keyboard types */
	public final static int KEYBOARD_TYPE_IBM_PC_XT = 0x00000001;
	public final static int KEYBOARD_TYPE_JAPANESE = 0x00000007;
	public final static int KEYBOARD_TYPE_NOKIA_1050 = 0x00000005;
	public final static int KEYBOARD_TYPE_NOKIA_9140 = 0x00000006;
	public final static int KEYBOARD_TYPE_OLIVETTI_ICO = 0x00000002;
	/* Client info (login) flags */
	public static final int INFO_MOUSE = 0x01;
	public static final int INFO_DISABLECTRLALTDEL = 0x02;
	public static final int INFO_LOGON_AUTO = 0x8;
	public static final int INFO_UNICODE = 0x10;
	public static final int INFO_MAXIMIZE_SHELL = 0x20;
	public static final int INFO_ENABLEWINDOWS_KEY = 0x100;
	public static final int INFO_MOUSE_HAS_WHEEL = 0x200;
	/* constants for RDP Layer */
	public static final int SECURITY_TYPE_HYBRID = 0x02;
	public static final int SECURITY_TYPE_HYBRID_EX = 0x04;
	public static final int SECURITY_TYPE_RDSTLS = 0x03;
	public static final int SECURITY_TYPE_SSL = 0x01;
	/* Security protocol types */
	public static final int SECURITY_TYPE_STANDARD = 0x00;
	private static final int BMPCACHE2_FLAG_PERSIST = (1 << 31);
	/* RDP capapabilitie packet lengths */
	private static final int RDP_CAPLEN_ACTIVATE = 12;
	private static final int RDP_CAPLEN_BITMAP = 28;
	private static final int RDP_CAPLEN_BMPCACHE = 40;
	private static final int RDP_CAPLEN_BRUSH = 8;
	private static final int RDP_CAPLEN_CONTROL = 12;
	private static final int RDP_CAPLEN_FONTS = 8;
	private static final int RDP_CAPLEN_GENERAL = 24;
	private static final int RDP_CAPLEN_GLYPHCACHE = 52;
	private static final int RDP_CAPLEN_INPUT = 88;
	private static final int RDP_CAPLEN_OFFSCREEN_BITMAP_CACHE = 12;
	private static final int RDP_CAPLEN_ORDER = 88;
	private static final int RDP_CAPLEN_POINTER = 10;
	private static final int RDP_CAPLEN_SHARE = 8;
	private static final int RDP_CAPLEN_SOUND = 6;
	private static final int RDP_CAPLEN_VIRTUAL_CHANNELS = 12;
	private static final int RDP_CAPSET_ACTIVATE = 0x07; // 7
	private static final int RDP_CAPSET_BITMAP = 0x02; // 2
	private static final int RDP_CAPSET_BITMAP_CACHE_HOST = 0x12; // 18
	private static final int RDP_CAPSET_BITMAP_CODECS = 0x1D; // 29
	private static final int RDP_CAPSET_BMPCACHE = 0x04; // 4
	private static final int RDP_CAPSET_BMPCACHE2 = 0x13; // 19
	private static final int RDP_CAPSET_BRUSH = 0x0F; // 15
	private static final int RDP_CAPSET_COLOR_CACHE = 0x0A; // 10
	private static final int RDP_CAPSET_COMPDESK = 0x19; // 25
	private static final int RDP_CAPSET_CONTROL = 0x05; // 5
	private static final int RDP_CAPSET_DRAWGDIPLUS = 0x16; // 22
	private static final int RDP_CAPSET_FONTS = 0x0E; // 14
	private static final int RDP_CAPLEN_COLCACHE = 0x08;
	private static final int RDP_CAPLEN_UNKNOWN = 60;
	/* RDP capabilities */
	private static final int RDP_CAPSET_GENERAL = 0x01; // 1
	private static final int RDP_CAPSET_COLCACHE = 10;
	private static final int RDP_CAPSET_GLYPHCACHE = 0x10; // 16
	private static final int RDP_CAPSET_INPUT = 0x0D; // 13
	private static final int RDP_CAPSET_LARGE_POINTER = 0x1B; // 27
	private static final int RDP_CAPSET_MULTIFRAGMENT_UPDATE = 0x1A; // 26
	private static final int RDP_CAPSET_OFFSCREEN_BITMAP_CACHE = 0x11; // 17
	private static final int RDP_CAPSET_ORDER = 0x03; // 3
	private static final int RDP_CAPSET_POINTER = 0x08; // 8
	private static final int RDP_CAPSET_RAIL = 0x17; // 23
	private static final int RDP_CAPSET_SHARE = 0x09; // 9
	private static final int RDP_CAPSET_SOUND = 0x0C; // 12
	private static final int RDP_CAPSET_SURFACE_COMMANDS = 0x1C; // 28
	private static final int RDP_CAPSET_VIRTUAL_CHANNELS = 0x14; // 20
	private static final int RDP_CAPSET_WINDOW_LIST = 0x18; // 24
	// Control PDU types
	private static final int RDP_CTL_COOPERATE = 4;
	private static final int RDP_CTL_DETACH = 3;
	private static final int RDP_CTL_GRANT_CONTROL = 2;
	private static final int RDP_CTL_REQUEST_CONTROL = 1;
	// Data PDU Types
	private static final int RDP_DATA_PDU_BELL = 34;
	private static final int RDP_DATA_PDU_CONTROL = 20;
	private static final int RDP_DATA_PDU_SET_ERROR = 47;
	private static final int RDP_DATA_PDU_FONT2 = 39;
	private static final int RDP_DATA_PDU_INPUT = 28;
	private static final int RDP_DATA_PDU_LOGON = 38;
	private static final int RDP_DATA_PDU_POINTER = 27;
	private static final int RDP_DATA_PDU_SYNCHRONISE = 31;
	private static final int RDP_DATA_PDU_UPDATE = 2;
	// System Pointer Types
	private static final int RDP_NULL_POINTER = 0;
	private static final int RDP_PDU_CONFIRM_ACTIVE = 3;
	private static final int RDP_PDU_DATA = 7;
	private static final int RDP_PDU_DEACTIVATE_ALL = 6;
	// PDU Types
	private static final int RDP_PDU_DEMAND_ACTIVE = 1;
	private static final int RDP_POINTER_CACHED = 7;
	private static final int RDP_POINTER_COLOR = 6;
	private static final int RDP_POINTER_MOVE = 3;
	private static final int RDP_POINTER_POINTER = 8;
	// Pointer PDU Types
	private static final int RDP_POINTER_SYSTEM = 1;
	private static final byte[] RDP_SOURCE = { (byte) 0x4D, (byte) 0x53, (byte) 0x54, (byte) 0x53, (byte) 0x43, (byte) 0x00 }; // string
	private static final int RDP_UPDATE_BITMAP = 1;
	// Update PDU Types
	private static final int RDP_UPDATE_ORDERS = 0;
	private static final int RDP_UPDATE_PALETTE = 2;
	private static final int RDP_UPDATE_SYNCHRONIZE = 3;
	// Input flags
	private static final int INPUT_FLAG_SCANCODES = 0x0001;
	private static final int INPUT_FLAG_MOUSEX = 0x0004;
	private static final int INPUT_FLAG_FASTPATH_INPUT = 0x0008;
	private static final int INPUT_FLAG_UNICODE = 0x0010;
	private static final int INPUT_FLAG_FASTPATH_INPUT2 = 0x0020;
	private static final int INPUT_FLAG_UNUSED1 = 0x0040;
	private static final int INPUT_FLAG_UNUSED2 = 0x0080;
	private static final int INPUT_FLAG_MOUSE_HWHEEL = 0x0100;
	private static final int INPUT_FLAG_QOE_TIMESTAMPS = 0x0200;
	// Address Family
	public static final int AF_INET = 0x0002;
	public static final int AF_INET6 = 0x0017;
	// Performance flags
	public static final int PERF_DISABLE_NOTHING = 0x00;
	public static final int PERF_DISABLE_CURSOR_SHADOW = 0x20;
	public static final int PERF_DISABLE_CURSORSETTINGS = 0x40; /*
																 * disables
																 * cursor
																 * blinking
																 */
	public static final int PERF_DISABLE_FULLWINDOW_DRAG = 0x02;
	public static final int PERF_DISABLE_MENU_ANIMATIONS = 0x04;
	public static final int PERF_DISABLE_THEMING = 0x08;
	public static final int PERF_DISABLE_WALLPAPER = 0x01;
	public static final int PERF_ENABLE_FONT_SMOOTHING = 0x80;
	public static final int PERF_ENABLE_DESKTOP_COMPOSITION = 0x100;
	//
	private static final int RDP5_FLAG = 0x0030;
	protected Orders orders = null;
	// MSTSC
	// encoded
	// as 7 byte
	// US-Ascii
	protected Secure secureLayer = null;
	boolean deactivated;
	private boolean connected = false;
	private IContext context;
	private int next_packet = 0;
	private State state;
	private Packet stream = null;
	private VChannels channels;

	/**
	 * Initialise RDP comms layer, and register virtual channels
	 * 
	 * @param channels Virtual channels to be used in connection
	 */
	public Rdp(IContext context, State state, VChannels channels) {
		state.setRdp(this);
		this.channels = channels;
		this.context = context;
		this.state = state;
		secureLayer = new Secure(context, state, channels, this);
		this.orders = new Orders(state);
	}

	public VChannels getChannels() {
		return channels;
	}

	/**
	 * Connect to a server
	 * 
	 * @param username Username for log on
	 * @param server Server to connect to
	 * @param flags Flags defining logon type
	 * @param domain Domain for log on
	 * @param password Password for log on
	 * @param command Alternative shell for session
	 * @param directory Initial working directory for connection
	 * @throws ConnectionException
	 * @throws OrderException
	 * @throws CryptoException
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws SocketException
	 * @throws UnknownHostException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 */
	public void connect(IO io, CredentialProvider credentialProvider, String command, String directory)
			throws ConnectionException, UnknownHostException, SocketException, IOException, RdesktopException, OrderException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		state.setCredentialProvider(credentialProvider);
		secureLayer.connect(io);
		this.connected = true;
		this.sendLogonInfo(command, directory);
		// TODO should this be here any more as it should all now be negotiated
		// properly
		if (state.getSecurityType() == SecurityType.STANDARD && !state.getOptions().isPacketEncryption()) {
			logger.info("Disabling encryption because packet encryption turned off.");
			state.setSecurityType(SecurityType.NONE);
		}
	}

	/**
	 * Disconnect from an RDP session
	 */
	public void disconnect() {
		this.connected = false;
		secureLayer.disconnect();
	}

	/**
	 * Retrieve status of connection
	 * 
	 * @return True if connection to RDP session
	 */
	public boolean isConnected() {
		return this.connected;
	}

	/**
	 * RDP receive loop
	 * 
	 * @throws IOException
	 * @throws RdesktopException
	 * @throws OrderException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws CryptoException
	 * @throws ShortBufferException
	 */
	public void mainLoop() throws IOException, RdesktopException, OrderException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, ShortBufferException {
		int[] type = new int[1];
		Packet data = null;
		while (true) {
			try {
				data = this.receive(type);
				if (data == null)
					return;
			} catch (EOFException e) {
				return;
			} catch (IOException ioe) {
				if (state.getLastReason() > 0)
					throw new RdesktopDisconnectException(state.getLastReason());
			}
			processPacket(type, data);
		}
	}

	private void processPacket(int[] type, Packet data) throws RdesktopException, IOException, OrderException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, ShortBufferException {
		switch (type[0]) {
		case (Rdp.RDP_PDU_DEMAND_ACTIVE):
			if (logger.isDebugEnabled())
				logger.debug("Rdp.RDP_PDU_DEMAND_ACTIVE");
			// get this after licence negotiation, just before the 1st
			// order...
			this.processDemandActive(data);
			// can use this to trigger things that have to be done
			// before
			// 1st order
			logger.info("Past license negotiation");
			context.readyToSend();
			state.setActive(true);
			break;
		case (Rdp.RDP_PDU_DEACTIVATE_ALL):
			logger.info("Got deactivate all.");
			if (state.getLastReason() > 0)
				throw new RdesktopDisconnectException(state.getLastReason());
			else {
				logger.info("Re-negotiate capabilities");
			}
			this.stream = null; // ty this fix
			break;
		case (Rdp.RDP_PDU_DATA):
			if (logger.isDebugEnabled())
				logger.debug("Rdp.RDP_PDU_DATA");
			// all the others should be this
			this.processData(data);
			break;
		case 0:
			break; // 32K keep alive fix, see receive() - rdesktop
					// 1.2.0.
		default:
			throw new RdesktopException("Unimplemented type in main loop :" + type[0]);
		}
	}

	/**
	 * Process an RDP5 packet
	 * 
	 * @param s Packet to be processed
	 * @param e True if packet is encrypted
	 * @throws RdesktopException
	 * @throws OrderException
	 */
	public void rdp5_process(Packet s, boolean e) throws RdesktopException, OrderException {
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
	 */
	public void rdp5_process(Packet s, boolean encryption, boolean shortform) throws RdesktopException, OrderException {
		if (logger.isDebugEnabled())
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
			if (logger.isDebugEnabled())
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
	void rdp5_process_channel(Packet s, int channelno) {
		VChannel channel = channels.find_channel_by_channelno(channelno);
		if (channel != null) {
			try {
				channel.process(s);
			} catch (Exception e) {
			}
		}
	}

	public void sendInput(int time, int message_type, int device_flags, int param1, int param2) {
		Packet data = null;
		try {
			data = this.initData(16);
		} catch (RdesktopException e) {
			context.error(e, false);
		}
		data.setLittleEndian16(1); /* number of events */
		data.setLittleEndian16(0); /* pad */
		data.setLittleEndian32(time);
		data.setLittleEndian16(message_type);
		data.setLittleEndian16(device_flags);
		data.setLittleEndian16(param1);
		data.setLittleEndian16(param2);
		data.markEnd();
		if (logger.isDebugEnabled())
			logger.debug("input");
		try {
			this.sendData(data, RDP_DATA_PDU_INPUT);
		} catch (RdesktopException e) {
			logger.error("Failed to send input.", e);
			context.error(e, true);
		} catch (IOException e) {
			logger.error("Failed to send input.", e);
			context.error(e, true);
		}
	}

	@Override
	public Layer<?> getParent() {
		return null;
	}

	protected void process_cached_pointer_pdu(Packet data) throws RdesktopException {
		if (logger.isDebugEnabled())
			logger.debug("Rdp.RDP_POINTER_CACHED");
		int cache_idx = data.getLittleEndian16();
		if (logger.isDebugEnabled())
			logger.debug(String.format("Setting cache cursor %d", cache_idx));
		state.getCanvas().getDisplay().setCursor(state.getCache().getCursor(cache_idx));
	}

	protected void process_colour_pointer_pdu(Packet data) throws RdesktopException {
		if (logger.isDebugEnabled())
			logger.debug("Rdp.RDP_POINTER_COLOR");
		int x = 0, y = 0, width = 0, height = 0, cache_idx = 0, masklen = 0, datalen = 0;
		byte[] mask = null, pixel = null;
		RdpCursor cursor = null;
		cache_idx = data.getLittleEndian16();
		x = data.getLittleEndian16();
		y = data.getLittleEndian16();
		width = data.getLittleEndian16();
		height = data.getLittleEndian16();
		masklen = data.getLittleEndian16();
		datalen = data.getLittleEndian16();
		mask = new byte[masklen];
		pixel = new byte[datalen];
		data.copyToByteArray(pixel, 0, data.getPosition(), datalen);
		data.incrementPosition(datalen);
		data.copyToByteArray(mask, 0, data.getPosition(), masklen);
		data.incrementPosition(masklen);
		cursor = state.getCanvas().createCursor(x, y, width, height, mask, pixel, cache_idx, 24, true);
		// logger.info("Creating and setting cursor " + cache_idx);
		state.getCanvas().getDisplay().setCursor(cursor);
		state.getCache().putCursor(cache_idx, cursor);
	}

	protected void process_colour_pointer_pdu_new(Packet data) throws RdesktopException {
		if (logger.isDebugEnabled())
			logger.debug("Rdp.RDP_POINTER_POINTER");
		int x = 0, y = 0, width = 0, height = 0, cache_idx = 0, masklen = 0, datalen = 0;
		byte[] mask = null, pixel = null;
		RdpCursor cursor = null;
		int xorBpp = data.getLittleEndian16();
		cache_idx = data.getLittleEndian16();
		x = data.getLittleEndian16();
		y = data.getLittleEndian16();
		width = data.getLittleEndian16();
		height = data.getLittleEndian16();
		masklen = data.getLittleEndian16();
		datalen = data.getLittleEndian16();
		mask = new byte[masklen];
		pixel = new byte[datalen];
		data.copyToByteArray(pixel, 0, data.getPosition(), datalen);
		data.incrementPosition(datalen);
		data.copyToByteArray(mask, 0, data.getPosition(), masklen);
		data.incrementPosition(masklen);
		cursor = state.getCanvas().createCursor(x, y, width, height, mask, pixel, cache_idx, xorBpp, false);
		state.getCanvas().getDisplay().setCursor(cursor);
		state.getCache().putCursor(cache_idx, cursor);
	}

	/* Process a null system pointer PDU */
	protected void process_null_system_pointer_pdu(Packet s) throws RdesktopException {
		// FIXME: We should probably set another cursor here,
		// like the X window system base cursor or something.
		state.getCanvas().getDisplay().setCursor(state.getCache().getCursor(0));
	}

	protected void processBitmapUpdates(Packet data) throws RdesktopException {
		// logger.info("processBitmapUpdates");
		int n_updates = 0;
		int left = 0, top = 0, right = 0, bottom = 0, width = 0, height = 0;
		int cx = 0, cy = 0, bitsperpixel = 0, compression = 0, buffersize = 0, size = 0;
		byte[] pixel = null;
		int minX, minY, maxX, maxY;
		maxX = maxY = 0;
		minX = state.getCanvas().getDisplay().getDisplayWidth();
		minY = state.getCanvas().getDisplay().getDisplayHeight();
		n_updates = data.getLittleEndian16();
		for (int i = 0; i < n_updates; i++) {
			left = data.getLittleEndian16();
			top = data.getLittleEndian16();
			right = data.getLittleEndian16();
			bottom = data.getLittleEndian16();
			width = data.getLittleEndian16();
			height = data.getLittleEndian16();
			bitsperpixel = data.getLittleEndian16();
			compression = data.getLittleEndian16();
			buffersize = data.getLittleEndian16();
			cx = right - left + 1;
			cy = bottom - top + 1;
			if (minX > left)
				minX = left;
			if (minY > top)
				minY = top;
			if (maxX < right)
				maxX = right;
			if (maxY < bottom)
				maxY = bottom;
			/* Server may limit bpp - this is how we find out */
			if (state.getServerBpp() != bitsperpixel) {
				logger.warn("Server limited colour depth to " + bitsperpixel + " bits");
				state.setServerBpp(bitsperpixel);
			}
			if (compression == 0) {
				// logger.info("compression == 0");
				pixel = new byte[width * height * state.getBytesPerPixel()];
				for (int y = 0; y < height; y++) {
					data.copyToByteArray(pixel, (height - y - 1) * (width * state.getBytesPerPixel()), data.getPosition(),
							width * state.getBytesPerPixel());
					data.incrementPosition(width * state.getBytesPerPixel());
				}
				state.getCanvas().displayImage(Bitmap.convertImage(state, pixel, state.getBytesPerPixel()), width, height, left,
						top, cx, cy);
				continue;
			}
			if ((compression & 0x400) != 0) {
				// logger.info("compression & 0x400 != 0");
				size = buffersize;
			} else {
				// logger.info("compression & 0x400 == 0");
				data.incrementPosition(2); // pad
				size = data.getLittleEndian16();
				data.incrementPosition(4); // line size, final size
			}
			if (state.getBytesPerPixel() == 1) {
				pixel = Bitmap.decompress(width, height, size, data, state.getBytesPerPixel());
				if (pixel != null)
					state.getCanvas().displayImage(Bitmap.convertImage(state, pixel, state.getBytesPerPixel()), width, height, left,
							top, cx, cy);
				else
					logger.warn("Could not decompress bitmap");
			} else {
				if (state.getOptions().getBitmapDecompressionStore() == Options.INTEGER_BITMAP_DECOMPRESSION) {
					int[] pixeli = Bitmap.decompressInt(state, width, height, size, data, state.getBytesPerPixel());
					if (pixeli != null)
						state.getCanvas().displayImage(pixeli, width, height, left, top, cx, cy);
					else
						logger.warn("Could not decompress bitmap");
				} else if (state.getOptions().getBitmapDecompressionStore() == Options.BUFFEREDIMAGE_BITMAP_DECOMPRESSION) {
					Image pix = Bitmap.decompressImg(state, width, height, size, data, state.getBytesPerPixel(), null);
					if (pix != null)
						state.getCanvas().displayImage(pix, left, top);
					else
						logger.warn("Could not decompress bitmap");
				} else {
					state.getCanvas().displayCompressed(left, top, width, height, size, data, state.getBytesPerPixel(), null);
				}
			}
		}
		state.getCanvas().getDisplay().repaint(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}

	/**
	 * Process a set error PDU
	 * 
	 * @param data Packet containing set error PDU at current read position
	 * @return Code specifying the reason
	 */
	protected int processSetErrorPdu(Packet data) {
		int v = data.getLittleEndian32();
		int vv = v & 0xff;
		if (logger.isDebugEnabled())
			logger.debug(String.format("Received set error PDU (%d - %d)", v, vv));
		return v;
	}

	protected void processPalette(Packet data) {
		int n_colors = 0;
		IndexColorModel cm = null;
		byte[] palette = null;
		byte[] red = null;
		byte[] green = null;
		byte[] blue = null;
		int j = 0;
		data.incrementPosition(2); // pad
		n_colors = data.getLittleEndian16(); // Number of Colors in Palette
		data.incrementPosition(2); // pad
		palette = new byte[n_colors * 3];
		red = new byte[n_colors];
		green = new byte[n_colors];
		blue = new byte[n_colors];
		data.copyToByteArray(palette, 0, data.getPosition(), palette.length);
		data.incrementPosition(palette.length);
		for (int i = 0; i < n_colors; i++) {
			red[i] = palette[j];
			green[i] = palette[j + 1];
			blue[i] = palette[j + 2];
			j += 3;
		}
		cm = new IndexColorModel(8, n_colors, red, green, blue);
		state.getCanvas().registerPalette(cm);
	}

	/**
	 * Process server capabilities
	 * 
	 * @param data Packet containing capability set data at current read
	 *            position
	 */
	void processServerCaps(Packet data, int length) {
		int n;
		int next, start;
		int ncapsets, capset_type, capset_length;
		start = data.getPosition();
		ncapsets = data.getLittleEndian16(); // in_uint16_le(s, ncapsets);
		data.incrementPosition(2); // in_uint8s(s, 2); /* pad */
		for (n = 0; n < ncapsets; n++) {
			if (data.getPosition() > start + length)
				return;
			capset_type = data.getLittleEndian16(); // in_uint16_le(s,
			// capset_type);
			capset_length = data.getLittleEndian16(); // in_uint16_le(s,
			// capset_length);
			next = data.getPosition() + capset_length - 4;
			switch (capset_type) {
			case RDP_CAPSET_GENERAL:
				processGeneralCaps(state, data);
				break;
			case RDP_CAPSET_BITMAP:
				if (processBitmapCaps(state, data)) {
					state.getCanvas().backingStoreResize(state.getWidth(), state.getHeight(), false);
				}
				break;
			case RDP_CAPSET_POINTER:
				processPointerCaps(state, data);
				break;
			case RDP_CAPSET_SHARE:
				processShareCaps(state, data);
				break;
			case RDP_CAPSET_VIRTUAL_CHANNELS:
				logger.info("Unhandled CAPSET virtual channels");
				break;
			case RDP_CAPSET_DRAWGDIPLUS:
				logger.info("Unhandled CAPSET draw GDP plus");
				break;
			case RDP_CAPSET_FONTS:
				logger.info("Unhandled CAPSET fonts");
				break;
			case RDP_CAPSET_BITMAP_CODECS:
				logger.info("Unhandled CAPSET bitmap codecs");
				break;
			case RDP_CAPSET_ORDER:
				logger.info("Unhandled CAPSET order");
				break;
			case RDP_CAPSET_COLOR_CACHE:
				logger.info("Unhandled CAPSET color cache");
				break;
			case RDP_CAPSET_BITMAP_CACHE_HOST:
				logger.info("Unhandled CAPSET bitmap cache host");
				break;
			case RDP_CAPSET_LARGE_POINTER:
				logger.info("Unhandled CAPSET large pointer");
				break;
			case RDP_CAPSET_INPUT:
				logger.info("Unhandled CAPSET input");
				break;
			case RDP_CAPSET_RAIL:
				logger.info("Unhandled CAPSET rail");
				break;
			case RDP_CAPSET_WINDOW_LIST:
				logger.info("Unhandled CAPSET window list");
				break;
			case RDP_CAPSET_COMPDESK:
				logger.info("Unhandled CAPSET compdesk");
				break;
			case RDP_CAPSET_MULTIFRAGMENT_UPDATE:
				logger.info("Unhandled CAPSET multifragment");
				break;
			case RDP_CAPSET_SURFACE_COMMANDS:
				logger.info("Unhandled CAPSET surface commands");
				break;
			default:
				logger.warn("Unhandled CAPSET " + capset_type);
				break;
			}
			data.setPosition(next);
		}
	}

	/**
	 * Initialise a packet for sending data on the RDP layer
	 * 
	 * @param size Size of RDP data
	 * @return Packet initialised for RDP
	 * @throws RdesktopException
	 */
	private Packet initData(int size) throws RdesktopException {
		Packet buffer = null;
		buffer = secureLayer.init(state.getSecurityType() == SecurityType.STANDARD ? Secure.SEC_ENCRYPT : 0, size + 18);
		buffer.pushLayer(Packet.RDP_HEADER, 18);
		// buffer.setHeader(RdpPacket.RDP_HEADER);
		// buffer.incrementPosition(18);
		// buffer.setStart(buffer.getPosition());
		return buffer;
	}

	private void process_system_pointer_pdu(Packet data) {
		int system_pointer_type = 0;
		data.getLittleEndian16(system_pointer_type); // in_uint16(s,
		// system_pointer_type);
		switch (system_pointer_type) {
		case RDP_NULL_POINTER:
			if (logger.isDebugEnabled())
				logger.debug("RDP_NULL_POINTER");
			state.getCanvas().getDisplay().setCursor(null);
			break;
		default:
			logger.warn("Unimplemented system pointer message 0x" + Integer.toHexString(system_pointer_type));
			// unimpl("System pointer message 0x%x\n", system_pointer_type);
		}
	}

	/**
	 * Process a data PDU received from the server
	 * 
	 * @param data Packet containing data PDU at current read position
	 * @return True if disconnect PDU was received
	 * @throws RdesktopException
	 * @throws OrderException
	 */
	private void processData(Packet data) throws RdesktopException, OrderException {
		int data_type;
		data_type = 0;
		data.incrementPosition(6); // skip shareid, pad, streamid
		int len = data.getLittleEndian16();
		data_type = data.get8();
		int ctype = data.get8(); // compression type
		int clen = data.getLittleEndian16(); // compression length
		clen -= 18;
		switch (data_type) {
		case (Rdp.RDP_DATA_PDU_UPDATE):
			if (logger.isDebugEnabled())
				logger.debug("Rdp.RDP_DATA_PDU_UPDATE");
			this.processUpdate(data);
			break;
		case RDP_DATA_PDU_CONTROL:
			if (logger.isDebugEnabled())
				logger.debug(("Received Control PDU\n"));
			break;
		case RDP_DATA_PDU_SYNCHRONISE:
			if (logger.isDebugEnabled())
				logger.debug(("Received Sync PDU\n"));
			break;
		case (Rdp.RDP_DATA_PDU_POINTER):
			if (logger.isDebugEnabled())
				logger.debug("Received pointer PDU");
			this.processPointer(data);
			break;
		case (Rdp.RDP_DATA_PDU_BELL):
			if (logger.isDebugEnabled())
				logger.debug("Received bell PDU");
			Toolkit tx = Toolkit.getDefaultToolkit();
			tx.beep();
			break;
		case (Rdp.RDP_DATA_PDU_LOGON):
			if (logger.isDebugEnabled())
				logger.debug("User logged on");
			context.setLoggedOn();
			break;
		case RDP_DATA_PDU_SET_ERROR:
			state.setLastReason(processSetErrorPdu(data));
			break;
		default:
			logger.warn("Unimplemented Data PDU type " + data_type);
			break;
		}
	}

	/**
	 * Process an activation demand from the server (received between licence
	 * negotiation and 1st order)
	 * 
	 * @param data Packet containing demand at current read position
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws OrderException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws ShortBufferException
	 */
	private void processDemandActive(Packet data) throws RdesktopException, IOException, OrderException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, ShortBufferException {
		int type[] = new int[1];
		state.setShareId(data.getLittleEndian32());
		logger.info(String.format("Share ID is %d (%04x)", state.getShareId(), state.getShareId()));
		int len_src_descriptor = data.getLittleEndian16();
		int len_combined_caps = data.getLittleEndian16();
		data.incrementPosition(len_src_descriptor); // in_uint8s(s,
													// len_src_descriptor);
		processServerCaps(data, len_combined_caps);
		this.sendConfirmActive();
		this.sendSynchronize();
		this.sendControl(RDP_CTL_COOPERATE);
		this.sendControl(RDP_CTL_REQUEST_CONTROL);
		this.sendFonts(1);
		this.sendFonts(2);
		this.receive(type); // Receive RDP_PDU_SYNCHRONIZE
		this.receive(type); // Receive RDP_CTL_COOPERATE
		this.receive(type); // Receive RDP_CTL_GRANT_CONTROL
		this.receive(type); // Receive TS_FONT_MAP_PDU (0x28)
		logger.info("reset order state");
		this.orders.resetOrderState();
	}

	private void processPointer(Packet data) throws RdesktopException {
		int message_type = 0;
		int x = 0, y = 0;
		message_type = data.getLittleEndian16();
		data.incrementPosition(2);
		switch (message_type) {
		case (Rdp.RDP_POINTER_MOVE):
			if (logger.isDebugEnabled())
				logger.debug("Rdp.RDP_POINTER_MOVE");
			x = data.getLittleEndian16();
			y = data.getLittleEndian16();
			if (data.getPosition() <= data.getEnd()) {
				state.getCanvas().movePointer(x, y);
			}
			break;
		case (Rdp.RDP_POINTER_COLOR):
			process_colour_pointer_pdu(data);
			break;
		case (Rdp.RDP_POINTER_CACHED):
			process_cached_pointer_pdu(data);
			break;
		case RDP_POINTER_SYSTEM:
			process_system_pointer_pdu(data);
			break;
		case RDP_POINTER_POINTER:
			process_colour_pointer_pdu_new(data);
			break;
		default:
			break;
		}
	}

	private void processUpdate(Packet data) throws OrderException, RdesktopException {
		int update_type = 0;
		update_type = data.getLittleEndian16();
		switch (update_type) {
		case (Rdp.RDP_UPDATE_ORDERS):
			data.incrementPosition(2); // pad
			int n_orders = data.getLittleEndian16();
			data.incrementPosition(2); // pad
			this.orders.processOrders(data, next_packet, n_orders);
			break;
		case (Rdp.RDP_UPDATE_BITMAP):
			this.processBitmapUpdates(data);
			break;
		case (Rdp.RDP_UPDATE_PALETTE):
			this.processPalette(data);
			break;
		case (Rdp.RDP_UPDATE_SYNCHRONIZE):
			break;
		default:
			logger.warn("Unimplemented Update type " + update_type);
		}
	}

	/**
	 * Receive a packet from the RDP layer
	 * 
	 * @param type Type of PDU received, stored in type[0]
	 * @return Packet received from RDP layer
	 * @throws IOException
	 * @throws RdesktopException
	 * @throws CryptoException
	 * @throws OrderException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws ShortBufferException
	 */
	private Packet receive(int[] type) throws IOException, RdesktopException, OrderException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, ShortBufferException {
		int length = 0;
		if ((this.stream == null) || (this.next_packet >= this.stream.getEnd())) {
			this.stream = secureLayer.receive();
			if (stream == null)
				return null;
			this.next_packet = this.stream.getPosition();
		} else {
			this.stream.setPosition(this.next_packet);
		}
		length = this.stream.getLittleEndian16();
		/* 32k packets are really 8, keepalive fix - rdesktop 1.2.0 */
		if (length == 0x8000) {
			logger.warn("32k packet keepalive fix");
			next_packet += 8;
			type[0] = 0;
			return stream;
		}
		type[0] = this.stream.getLittleEndian16() & 0xf;
		if (logger.isDebugEnabled())
			logger.debug("receive " + type[0]);
		if (stream.getPosition() != stream.getEnd()) {
			stream.incrementPosition(2);
		}
		this.next_packet += length;
		return stream;
	}

	private void sendFontCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_FONTS);
		data.setLittleEndian16(RDP_CAPLEN_FONTS);
		data.setLittleEndian16(1); /* Support fontlist */
		data.setLittleEndian16(0); /* pad */
	}

	private void sendActivateCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_ACTIVATE);
		data.setLittleEndian16(RDP_CAPLEN_ACTIVATE);
		data.setLittleEndian16(0); /* Help key */
		data.setLittleEndian16(0); /* Help index key */
		data.setLittleEndian16(0); /* Extended help key */
		data.setLittleEndian16(0); /* Window activate */
	}

	/* Output bitmap cache v2 capability set */
	private void sendBitmapcache2Caps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_BMPCACHE2);
		data.setLittleEndian16(RDP_CAPLEN_BMPCACHE);
		data.setLittleEndian16(state.getOptions().getPersistentCacheBackend() != null ? 2 : 0); /* version */
		data.setBigEndian16(3); /* number of caches in this set */
		/* max cell size for cache 0 is 16x16, 1 = 32x32, 2 = 64x64, etc */
		data.setLittleEndian32(BMPCACHE2_C0_CELLS);
		data.setLittleEndian32(BMPCACHE2_C1_CELLS);
		if (state.getOptions().getPersistentCacheBackend().init(2)) {
			logger.info("Persistent cache initialized");
			data.setLittleEndian32(BMPCACHE2_NUM_PSTCELLS | BMPCACHE2_FLAG_PERSIST);
		} else {
			logger.info("Persistent cache not initialized");
			data.setLittleEndian32(BMPCACHE2_C2_CELLS);
		}
		data.incrementPosition(20); // out_uint8s(s, 20); /* other bitmap caches
		// not used */
	}

	private void sendBitmapcacheCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_BMPCACHE);
		data.setLittleEndian16(RDP_CAPLEN_BMPCACHE);
		data.incrementPosition(24); /* unused */
		data.setLittleEndian16(0x258); /* entries */
		data.setLittleEndian16(0x100); /* max cell size */
		data.setLittleEndian16(0x12c); /* entries */
		data.setLittleEndian16(0x400); /* max cell size */
		data.setLittleEndian16(0x106); /* entries */
		data.setLittleEndian16(0x1000); /* max cell size */
	}

	private void sendBitmapCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_BITMAP);
		data.setLittleEndian16(RDP_CAPLEN_BITMAP);
		data.setLittleEndian16(state.getServerBpp()); /* Preferred BPP */
		data.setLittleEndian16(1); /* Receive 1 BPP */
		data.setLittleEndian16(1); /* Receive 4 BPP */
		data.setLittleEndian16(1); /* Receive 8 BPP */
		data.setLittleEndian16(state.getWidth()); /* Desktop width */
		data.setLittleEndian16(state.getHeight()); /* Desktop height */
		data.setLittleEndian16(0); /* Pad */
		data.setLittleEndian16(1); /* Allow resize */
		data.setLittleEndian16(1); // MUST be supported apparently
		// data.setLittleEndian16(
		// options.getOptions().isBbitmap_compression ? 1 : 0); /*
		// * Support compression
		// */
		data.setLittleEndian16(0); /* Unknown */
		data.setLittleEndian16(1); /* Unknown */
		data.setLittleEndian16(0); /* Pad */
	}

	private void sendColorcacheCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_COLCACHE);
		data.setLittleEndian16(RDP_CAPLEN_COLCACHE);
		data.setLittleEndian16(6); /* cache size */
		data.setLittleEndian16(0); /* pad */
	}

	private void sendConfirmActive()
			throws RdesktopException, IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		int caplen = RDP_CAPLEN_GENERAL + // 1
				RDP_CAPLEN_BITMAP + // 2
				RDP_CAPLEN_ORDER + // 3
				RDP_CAPLEN_BMPCACHE + // 4
				RDP_CAPLEN_COLCACHE + // 5
				RDP_CAPLEN_ACTIVATE + // 6
				RDP_CAPLEN_CONTROL + // 7
				RDP_CAPLEN_POINTER + // 8
				RDP_CAPLEN_SHARE + // 9
				RDP_CAPLEN_GLYPHCACHE + // 10
				RDP_CAPLEN_INPUT + // 11
				RDP_CAPLEN_SOUND + // 12
				RDP_CAPLEN_FONTS + // 13
				RDP_CAPLEN_OFFSCREEN_BITMAP_CACHE + // 14
				RDP_CAPLEN_VIRTUAL_CHANNELS + // 15
				RDP_CAPLEN_BRUSH + // 15
				4;
		// for W2k.
		// Purpose
		// unknown
		int sec_flags = state.getSecurityType() == SecurityType.STANDARD ? RDP5_FLAG | Secure.SEC_ENCRYPT : RDP5_FLAG;
		Packet data = secureLayer.init(sec_flags, 6 + 14 + caplen + RDP_SOURCE.length);
		// RdpPacket data = this.init(14 + caplen +
		// RDP_SOURCE.length);
		data.setLittleEndian16(2 + 14 + caplen + RDP_SOURCE.length);
		data.setLittleEndian16((RDP_PDU_CONFIRM_ACTIVE | 0x10));
		data.setLittleEndian16(state.getMcsUserId() /* McsUserID() */ + 1001);
		data.setLittleEndian32(state.getShareId());
		data.setLittleEndian16(0x3ea); // user id
		data.setLittleEndian16(RDP_SOURCE.length);
		data.setLittleEndian16(caplen);
		data.copyFromByteArray(RDP_SOURCE, 0, data.getPosition(), RDP_SOURCE.length);
		data.incrementPosition(RDP_SOURCE.length);
		data.setLittleEndian16(0xf); // num_caps
		data.incrementPosition(2); // pad
		sendGeneralCaps(data); // 1
		sendBitmapCaps(data); // 2
		sendOrderCaps(data); // 3
		if (state.isRDP5() && state.getOptions().getPersistentCacheBackend() != null) {
			logger.info("Persistent caching enabled");
			sendBitmapcache2Caps(data); // 4
		} else
			sendBitmapcacheCaps(data); // 4
		sendColorcacheCaps(data); // 5
		sendActivateCaps(data); // 6
		sendControlCaps(data); // 7
		sendPointerCaps(data); // 8
		sendShareCaps(data); // 9
		sendGlyphCacheCaps(data); // 10
		sendInputCaps(data); // 11
		sendSoundCaps(data); // 12
		sendFontCaps(data); // 13
		sendOffscreenBitmapCacheCaps(data); // 14
		sendVirtualChannelCaps(data); // 15
		sendBrushCaps(data); // 16
		data.markEnd();
		if (logger.isDebugEnabled())
			logger.debug("confirm active");
		// this.send(data, RDP_PDU_CONFIRM_ACTIVE);
		secureLayer.send(data, sec_flags);
	}

	private void sendControl(int action) throws RdesktopException, IOException {
		Packet data = this.initData(8);
		data.setLittleEndian16(action);
		data.setLittleEndian16(0); // userid
		data.setLittleEndian32(0); // control id
		data.markEnd();
		if (logger.isDebugEnabled())
			logger.debug("control");
		this.sendData(data, RDP_DATA_PDU_CONTROL);
	}

	private void sendControlCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_CONTROL);
		data.setLittleEndian16(RDP_CAPLEN_CONTROL);
		data.setLittleEndian16(0); /* Control capabilities */
		data.setLittleEndian16(0); /* Remote detach */
		data.setLittleEndian16(2); /* Control interest */
		data.setLittleEndian16(2); /* Detach interest */
	}

	/**
	 * Send a packet on the RDP layer
	 * 
	 * @param data Packet to send
	 * @param data_pdu_type Type of data
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void sendData(Packet data, int data_pdu_type) throws RdesktopException, IOException {
		try {
			state.getCommLock().acquire();
			try {
				int length;
				data.setPosition(data.getHeader(Packet.RDP_HEADER));
				length = data.getEnd() - data.getPosition();
				data.setLittleEndian16(length);
				data.setLittleEndian16(RDP_PDU_DATA | 0x10);
				data.setLittleEndian16(state.getMcsUserId() + 1001);
				data.setLittleEndian32(state.getShareId());
				data.set8(0); // pad
				data.set8(1); // stream id
				data.setLittleEndian16(length - 14);
				data.set8(data_pdu_type);
				data.set8(0); // compression type
				data.setLittleEndian16(0); // compression length
				secureLayer.send(data, state.getSecurityType() == SecurityType.STANDARD ? Secure.SEC_ENCRYPT : 0);
			} finally {
				state.getCommLock().release();
			}
		} catch (InterruptedException ie) {
			throw new RdesktopException("Interrupted waiting to send data.", ie);
		}
	}

	private void sendFonts(int seq) throws RdesktopException, IOException {
		Packet data = this.initData(8);
		data.setLittleEndian16(0); /* number of fonts */
		data.setLittleEndian16(0x3e); /* unknown */
		data.setLittleEndian16(seq); /* unknown */
		data.setLittleEndian16(0x32); /* entry size */
		data.markEnd();
		if (logger.isDebugEnabled())
			logger.debug("fonts");
		this.sendData(data, RDP_DATA_PDU_FONT2);
	}

	private void sendGeneralCaps(Packet data) {
		logger.info("Sending general caps");
		data.setLittleEndian16(RDP_CAPSET_GENERAL);
		data.setLittleEndian16(RDP_CAPLEN_GENERAL);
		data.setLittleEndian16(
				state.getOptions().getOsMajor()); /* OS major type */
		logger.info(String.format("     OS Major: %d", state.getOptions().getOsMajor()));
		data.setLittleEndian16(
				state.getOptions().getOsMinor()); /* OS minor type */
		logger.info(String.format("     OS Minor: %d", state.getOptions().getOsMinor()));
		data.setLittleEndian16(0x200); /* Protocol version */
		data.setLittleEndian16(state.isRDP5() ? 0x40d : 0);
		// data.setLittleEndian16(Options.use_rdp5 ? 0x1d04 : 0); // this seems
		/*
		 * Pad, according to T.128. 0x40d seems to trigger the server to start
		 * sending RDP5 packets. However, the value is 0x1d04 with W2KTSK and
		 * NT4MS. Hmm.. Anyway, thankyou, Microsoft, for sending such
		 * information in a padding field..
		 */
		data.setLittleEndian16(0); /* Compression types */
		data.setLittleEndian16(state.isRDP5() ? 0x0004
				: 0); /* Extra Flags - long credentials supported */
		data.setLittleEndian16(0); /* Update capability */
		data.setLittleEndian16(0); /* Remote unshare capability */
		data.setLittleEndian16(0); /* Compression level */
		data.set8(0); /* Refresh Rect support */
		data.set8(0); /* Suppress output support */
	}

	private void sendInputCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_INPUT);
		data.setLittleEndian16(RDP_CAPLEN_INPUT);
		data.setLittleEndian16(INPUT_FLAG_SCANCODES); // flags (SCANCODES
														// required, important,
														// crashes if zero)
		data.setLittleEndian16(0); // pad
		data.setLittleEndian32(state.getOptions().getKeymap().getMapCode());
		// TODO make these Options
		data.setLittleEndian32(0x00000004); // IBM 102/103
		data.setLittleEndian32(0); // keyboard subtype
		data.setLittleEndian32(12); // function keys
		for (int i = 0; i < 16; i++) {
			data.setLittleEndian32(0);
		}
	}

	private void sendBrushCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_BRUSH);
		data.setLittleEndian16(RDP_CAPLEN_BRUSH);
		data.setLittleEndian32(0x03);
	}

	private void sendVirtualChannelCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_VIRTUAL_CHANNELS);
		data.setLittleEndian16(RDP_CAPLEN_VIRTUAL_CHANNELS);
		data.setLittleEndian32(0); /* no support for vchannel compressionn */
		data.setLittleEndian32(0); /* chunk size */
	}

	private void sendSoundCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_SOUND);
		data.setLittleEndian16(RDP_CAPLEN_SOUND);
		data.setLittleEndian16(1); /* supports beep */
	}

	private void sendOffscreenBitmapCacheCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_OFFSCREEN_BITMAP_CACHE);
		data.setLittleEndian16(RDP_CAPLEN_OFFSCREEN_BITMAP_CACHE);
		data.setLittleEndian32(0); /* no support */
		data.setLittleEndian16(0); /* cache size in k */
		data.setLittleEndian16(0); /* cache entries in k */
	}

	private void sendGlyphCacheCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_GLYPHCACHE);
		data.setLittleEndian16(RDP_CAPLEN_GLYPHCACHE);
		for (int i = 0; i < 10; i++) {
			data.setLittleEndian16(0xfe); /* no. of entries */
			data.setLittleEndian16(0xfe); /* cell size */
		}
		data.setLittleEndian16(0x100); /* frag cache entries */
		data.setLittleEndian16(0x100); /* frag cache size */
		data.setLittleEndian16(2); /* glyph support level */
		data.setLittleEndian16(0); /* pad */
	}

	/**
	 * Send user logon details to the server
	 *
	 * @param flags Set of flags defining logon type
	 * @param domain Domain for logon
	 * @param username Username for logon
	 * @param password Password for logon
	 * @param command Alternative shell for session
	 * @param directory Starting working directory for session
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 * @throws CryptoException
	 */
	private void sendLogonInfo(String command, String directory)
			throws RdesktopException, IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		int flags = INFO_MOUSE | INFO_DISABLECTRLALTDEL | INFO_UNICODE | INFO_MAXIMIZE_SHELL | INFO_ENABLEWINDOWS_KEY
				| INFO_MOUSE_HAS_WHEEL;
		String domain;
		String username;
		char[] password = new char[0];
		List<String> creds = state.getCredentialProvider().getCredentials("login", 0, CredentialType.DOMAIN,
				CredentialType.USERNAME, CredentialType.PASSWORD);
		if (creds == null) {
			domain = username = "";
		} else {
			domain = StringUtils.defaultIfBlank(creds.get(0), "");
			username = StringUtils.defaultIfBlank(creds.get(1), "");
			password = StringUtils.defaultIfBlank(creds.get(2), "").toCharArray();
		}
		directory = StringUtils.defaultIfBlank(directory, "");
		if (password != null && password.length > 0)
			flags |= INFO_LOGON_AUTO;
		int len_ip = 2 * state.getClientIp().length();
		int len_dll = 2 * state.getClientDir().length();
		int sec_flags = state.getSecurityType() == SecurityType.STANDARD ? Secure.SEC_LOGON_INFO | Secure.SEC_ENCRYPT
				: Secure.SEC_LOGON_INFO;
		int domainlen = 2 * domain.length();
		int userlen = 2 * username.length();
		int passlen = 2 * (password == null ? 0 : password.length);
		int commandlen = 2 * command.length();
		int dirlen = 2 * directory.length();
		int packetlen = 18 + domainlen + userlen + passlen + commandlen + dirlen + 10;
		if (state.isRDP5()) {
			packetlen += 2 + 4 + len_ip + 4 + len_dll + 172 + 4 + 4 + 2 + 28 + 2 + 2;
		}
		if (logger.isDebugEnabled())
			logger.debug("Sending RDP Logon packet");
		Packet data = secureLayer.init(sec_flags, packetlen);
		data = secureLayer.init(sec_flags, packetlen);
		data.setLittleEndian32(0); // code page
		data.setLittleEndian32(flags);
		data.setLittleEndian16(domainlen);
		data.setLittleEndian16(userlen);
		data.setLittleEndian16(passlen);
		data.setLittleEndian16(commandlen);
		data.setLittleEndian16(dirlen);
		data.outUnicodeString(domain, domainlen);
		data.outUnicodeString(username, userlen);
		if (0 < passlen)
			data.outUnicodeString(new String(password), passlen);
		else
			data.setLittleEndian16(0);
		data.outUnicodeString(command, commandlen);
		data.outUnicodeString(directory, dirlen);
		if (state.isRDP5()) {
			if (logger.isDebugEnabled())
				logger.debug("Sending RDP5+ extra login info");
			/* Extra Info */
			data.setLittleEndian16(state.getClientAddressFamily());
			data.setLittleEndian16(len_ip + 2);
			data.outUnicodeString(state.getClientIp(), len_ip);
			data.setLittleEndian16(len_dll + 2);
			data.outUnicodeString(state.getClientDir(), len_dll);
			sendTimeZone(data);
			data.setLittleEndian32(0); // client session ID
			data.setLittleEndian32(state.getOptions().getRdp5PerformanceFlags());
			data.setLittleEndian16(0); // auto reconnect cookie length
			data.fill(28); // TODO auto reconnect cookie
			data.setLittleEndian16(16); // reserved 1
			data.setLittleEndian16(16); // reserved 2
		}
		data.markEnd();
		byte[] buffer = new byte[data.getEnd()];
		data.copyToByteArray(buffer, 0, 0, data.getEnd());
		secureLayer.send(data, sec_flags);
	}

	private void sendTimeZone(Packet data) {
		// 172 bytes
		TimeZone tz = state.getOptions().getTimeZone();
		data.setLittleEndian32(tz.getRawOffset() / 1000 / 60);
		writeTimeZone(data, tz, tz.getDisplayName(false, TimeZone.LONG));
		writeTimeZone(data, tz, tz.getDisplayName(true, TimeZone.LONG));
	}

	private void writeTimeZone(Packet data, TimeZone tz, String name) {
		// 84 bytes
		data.outUnicodeString(name, name.length() * 2);
		data.incrementPosition(62 - (2 * name.length()));
		/*
		 * TODO actually write proper transition dates. For this we will need
		 * Joda library as JDK has no built in facility and it's a complex
		 * problem
		 */
		data.setLittleEndian32(0); // Date 1
		data.setLittleEndian32(0); // Date 2
		data.setLittleEndian32(0); // Date 3
		data.setLittleEndian32(0); // Date 4
		data.setLittleEndian32(0); // Bias
	}

	private void sendOrderCaps(Packet data) {
		byte[] order_caps = new byte[32];
		order_caps[0] = 1; /* dest blt */
		order_caps[1] = 1; /* pat blt */// nb no rectangle orders if this is 0
		order_caps[2] = 1; /* screen blt */
		order_caps[3] = (byte) (state.getOptions().isBitmapCaching() ? 1 : 0); /* memblt */
		order_caps[4] = 0; /* triblt */
		order_caps[8] = 1; /* line */
		order_caps[9] = 1; /* line */
		order_caps[10] = 1; /* rect */
		order_caps[11] = state.getOptions().isDesktopSave() ? (byte) 1 : 0; /* desksave */
		order_caps[13] = 1; /* memblt */
		order_caps[14] = 1; /* triblt */
		order_caps[20] = (byte) (state.getOptions().isPolygonEllipseOrders() ? 1 : 0); /* polygon */
		order_caps[21] = (byte) (state.getOptions().isPolygonEllipseOrders() ? 1 : 0); /* polygon2 */
		order_caps[22] = 1; /* polyline */
		order_caps[25] = (byte) (state.getOptions().isPolygonEllipseOrders() ? 1 : 0); /* ellipse */
		order_caps[26] = (byte) (state.getOptions().isPolygonEllipseOrders() ? 1 : 0); /* ellipse2 */
		order_caps[27] = 1; /* text2 */
		data.setLittleEndian16(RDP_CAPSET_ORDER);
		data.setLittleEndian16(RDP_CAPLEN_ORDER);
		data.setLittleEndian32(0);/* terminal desc 1 */
		data.setLittleEndian32(0);/* terminal desc 2 */
		data.setLittleEndian32(0);/* terminal desc 3 */
		data.setLittleEndian32(0); /* terminal desc 4 */
		data.setLittleEndian32(0); /* pad */
		data.setLittleEndian16(1); /* Cache X granularity */
		data.setLittleEndian16(20); /* Cache Y granularity */
		data.setLittleEndian16(0); /* Pad */
		data.setLittleEndian16(1); /* Max order level */
		data.setLittleEndian16(0); /* Number of fonts */
		data.setLittleEndian16(0x2a); /* Capability flags */
		data.copyFromByteArray(order_caps, 0, data.getPosition(),
				32); /* Orders supported */
		data.incrementPosition(32);
		data.setLittleEndian16(0x6a1); /* Text capability flags */
		data.setLittleEndian16(0); /* Order support Ex Flags */
		data.setLittleEndian32(0); /* Pad */
		data.setLittleEndian32(state.getOptions().isDesktopSave() ? 0x38400
				: 0); /* Desktop cache size */
		data.setLittleEndian32(0); /* Pad */
		data.setLittleEndian16(0x04); /* ANSI code page */
		data.setLittleEndian16(0); /* Pad */
	}

	private void sendPointerCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_POINTER);
		data.setLittleEndian16(RDP_CAPLEN_POINTER);
		data.setLittleEndian16(
				state.isColorPointer() ? 1 : 0); /* Colour pointer */
		data.setLittleEndian16(state
				.getColorPointerCacheSize()); /* Color Pointer Cache size */
		data.setLittleEndian16(
				state.getPointerCacheSize()); /* Pointer (new) Cache size */
	}

	private void sendShareCaps(Packet data) {
		data.setLittleEndian16(RDP_CAPSET_SHARE);
		data.setLittleEndian16(RDP_CAPLEN_SHARE);
		data.setLittleEndian16(0); /* userid */
		data.setLittleEndian16(0); /* pad */
	}

	private void sendSynchronize()
			throws RdesktopException, IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Packet data = this.initData(4);
		data.setLittleEndian16(1); // type
		data.setLittleEndian16(state.getServerChannelId());
		data.markEnd();
		if (logger.isDebugEnabled())
			logger.debug("sync");
		this.sendData(data, RDP_DATA_PDU_SYNCHRONISE);
	}

	/**
	 * Process a bitmap capability set
	 * 
	 * @param data Packet containing capability set data at current read
	 *            position
	 * @return resized
	 */
	static boolean processBitmapCaps(State options, Packet data) {
		int width, height, bpp;
		bpp = data.getLittleEndian16(); // in_uint16_le(s, bpp);
		data.incrementPosition(6); // in_uint8s(s, 6);
		width = data.getLittleEndian16(); // in_uint16_le(s, width);
		height = data.getLittleEndian16(); // in_uint16_le(s, height);
		if (logger.isDebugEnabled())
			logger.debug("setting desktop size and bpp to: " + width + "x" + height + "x" + bpp);
		/*
		 * The server may limit bpp and change the size of the desktop (for
		 * example when shadowing another session).
		 */
		if (options.getServerBpp() != bpp) {
			logger.warn("colour depth changed from " + options.getServerBpp() + " to " + bpp);
			options.setServerBpp(bpp);
		}
		if (options.getWidth() != width || options.getHeight() != height) {
			logger.warn(
					"screen size changed from " + options.getWidth() + "x" + options.getHeight() + " to " + width + "x" + height);
			options.setWidth(width);
			options.setHeight(height);
			return true;
			// ui_resize_window(); TODO: implement resize thingy
		}
		return false;
	}

	/**
	 * Process a general capability set
	 *
	 * @param data Packet containing capability set data at current read
	 *            position
	 */
	static void processGeneralCaps(State state, Packet data) {
		int pad2octetsB; /* rdp5 flags? */
		data.incrementPosition(10); // in_uint8s(s, 10);
		pad2octetsB = data.getLittleEndian16(); // in_uint16_le(s, pad2octetsB);
		if (pad2octetsB != 0)
			state.setRDP5(false);
	}

	static void processShareCaps(State state, Packet data) {
		state.setServerChannelId(data.getLittleEndian16());
		logger.info(String.format("Server channel is %d (%04x)", state.getServerChannelId(), state.getServerChannelId()));
	}

	static void processPointerCaps(State state, Packet data) {
		state.setColorPointer(data.getLittleEndian16() == 1);
		state.setColorPointerCacheSize(data.getLittleEndian16());
		if (data.size() >= 10)
			state.setPointerCacheSize(data.getLittleEndian16());
		else
			state.setPointerCacheSize(0);
	}
}
