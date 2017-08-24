package com.sshtools.javardp.rdp5.display;

import java.io.IOException;
import java.util.Collection;

import com.sshtools.javardp.Constants;
import com.sshtools.javardp.IContext;
import com.sshtools.javardp.Options;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.RdpPacket;
import com.sshtools.javardp.Secure;
import com.sshtools.javardp.crypto.CryptoException;
import com.sshtools.javardp.rdp5.VChannel;
import com.sshtools.javardp.rdp5.VChannels;

public class DisplayControlChannel extends VChannel {
	final static int DISPLAYCONTROL_PDU_TYPE_CAPS = 0x00000005;
	final static int DISPLAYCONTROL_PDU_TYPE_CAPS_LEN = 12;
	final static int DISPLAYCONTROL_PDU_TYPE_MONITOR_LAYOUT = 0x00000002;
	final static int DISPLAYCONTROL_PDU_TYPE_MONITOR_LAYOUT_LEN = 12;
	private int maxNumMonitors;
	private int maxMonitorAreaFactorA;
	private int maxMonitorAreaFactorB;

	public DisplayControlChannel(IContext context, Options options) {
		super(context, options);
	}

	@Override
	public String name() {
		return "Microsoft::Windows::RDS::DisplayControl";
	}

	public int getMaxNumMonitors() {
		return maxNumMonitors;
	}

	public int getMaxMonitorAreaFactorA() {
		return maxMonitorAreaFactorA;
	}

	public int getMaxMonitorAreaFactorB() {
		return maxMonitorAreaFactorB;
	}

	public int flags() {
		// TODO no idea
		return VChannels.CHANNEL_OPTION_INITIALIZED | VChannels.CHANNEL_OPTION_ENCRYPT_RDP | VChannels.CHANNEL_OPTION_COMPRESS_RDP
				| VChannels.CHANNEL_OPTION_SHOW_PROTOCOL;
	}

	@Override
	public void process(RdpPacket data) throws RdesktopException, IOException, CryptoException {
		data.getLittleEndian32();
		int type = data.getLittleEndian32();
		switch (type) {
		case DISPLAYCONTROL_PDU_TYPE_CAPS:
			maxNumMonitors = data.getLittleEndian32();
			maxMonitorAreaFactorA = data.getLittleEndian32();
			maxMonitorAreaFactorB = data.getLittleEndian32();
			break;
		default:
			throw new RdesktopException(
					"Expected DISPLAYCONTROL_PDU_TYPE_CAPS (" + DISPLAYCONTROL_PDU_TYPE_CAPS + ") but got " + type);
		}
	}

	public long getMaxArea() {
		return maxNumMonitors * maxMonitorAreaFactorA * maxMonitorAreaFactorB;
	}
	
	public boolean isInitialised() {
		return maxNumMonitors > 0;
	}

	public void sendDisplayControlCaps(Collection<MonitorLayout> layout) throws RdesktopException, IOException, CryptoException {
		if (maxNumMonitors == 0)
			throw new RdesktopException("Never recieved monitor layout from server.");
		long area = 0;
		for (MonitorLayout l : layout) {
			area += l.getArea();
		}
		if (area > getMaxArea()) {
			throw new RdesktopException(String.format("%s exceeds maximum area supported by server (%d)", area, getMaxArea()));
		}
		int totLen = DISPLAYCONTROL_PDU_TYPE_MONITOR_LAYOUT_LEN + (40 * layout.size());
		RdpPacket p = super.init(totLen);
		p.setLittleEndian16(DISPLAYCONTROL_PDU_TYPE_MONITOR_LAYOUT_LEN);
		p.setLittleEndian32(0);
		p.setLittleEndian32(DISPLAYCONTROL_PDU_TYPE_MONITOR_LAYOUT);
		p.setLittleEndian32(40);
		p.setLittleEndian32(layout.size());
		for (MonitorLayout l : layout) {
			l.writer(p);
		}
		p.markEnd();
		context.getSecure().send_to_channel(p, Constants.encryption ? Secure.SEC_ENCRYPT : 0, this.mcs_id());
	}
}