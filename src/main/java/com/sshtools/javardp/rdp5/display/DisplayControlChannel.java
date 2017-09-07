package com.sshtools.javardp.rdp5.display;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.Collection;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import com.sshtools.javardp.IContext;
import com.sshtools.javardp.Packet;
import com.sshtools.javardp.RdesktopException;
import com.sshtools.javardp.SecurityType;
import com.sshtools.javardp.State;
import com.sshtools.javardp.layers.Secure;
import com.sshtools.javardp.rdp5.VChannel;
import com.sshtools.javardp.rdp5.VChannels;

public class DisplayControlChannel extends VChannel {
	final static int DISPLAYCONTROL_PDU_TYPE_CAPS = 0x00000005;
	final static int DISPLAYCONTROL_PDU_TYPE_CAPS_LEN = 12;
	final static int DISPLAYCONTROL_PDU_TYPE_MONITOR_LAYOUT = 0x00000002;
	final static int DISPLAYCONTROL_PDU_TYPE_MONITOR_LAYOUT_LEN = 12;
	private int maxMonitorAreaFactorA;
	private int maxMonitorAreaFactorB;
	private int maxNumMonitors;

	public DisplayControlChannel(IContext context, State state) {
		super(context, state);
	}

	@Override
	public int flags() {
		// TODO no idea
		return VChannels.CHANNEL_OPTION_INITIALIZED | VChannels.CHANNEL_OPTION_ENCRYPT_RDP | VChannels.CHANNEL_OPTION_COMPRESS_RDP
				| VChannels.CHANNEL_OPTION_SHOW_PROTOCOL;
	}

	public long getMaxArea() {
		return maxNumMonitors * maxMonitorAreaFactorA * maxMonitorAreaFactorB;
	}

	public int getMaxMonitorAreaFactorA() {
		return maxMonitorAreaFactorA;
	}

	public int getMaxMonitorAreaFactorB() {
		return maxMonitorAreaFactorB;
	}

	public int getMaxNumMonitors() {
		return maxNumMonitors;
	}

	public boolean isInitialised() {
		return maxNumMonitors > 0;
	}

	@Override
	public String name() {
		return "Microsoft::Windows::RDS::DisplayControl";
	}

	@Override
	public void process(Packet data) throws RdesktopException, IOException {
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

	public void sendDisplayControlCaps(Collection<MonitorLayout> layout)
			throws RdesktopException, IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
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
		Packet p = super.init(totLen);
		p.setLittleEndian16(DISPLAYCONTROL_PDU_TYPE_MONITOR_LAYOUT_LEN);
		p.setLittleEndian32(0);
		p.setLittleEndian32(DISPLAYCONTROL_PDU_TYPE_MONITOR_LAYOUT);
		p.setLittleEndian32(40);
		p.setLittleEndian32(layout.size());
		for (MonitorLayout l : layout) {
			l.writer(p);
		}
		p.markEnd();
		context.getSecure().send_to_channel(p, state.getSecurityType() == SecurityType.STANDARD ? Secure.SEC_ENCRYPT : 0,
				this.mcs_id());
	}
}
