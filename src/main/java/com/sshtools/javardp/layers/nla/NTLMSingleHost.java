package com.sshtools.javardp.layers.nla;

import java.io.IOException;
import java.util.Arrays;

import com.sshtools.javardp.Packet;

public class NTLMSingleHost implements PacketPayload {
	private byte[] customData;
	private byte[] machineId;

	@Override
	public Packet write() throws IOException {
		Packet p = new Packet(44);
		p.setLittleEndian32(0); // z4
		if (customData == null)
			p.incrementPosition(8);
		else
			p.copyFromByteArray(customData, 0, p.getPosition(), 8);
		if (machineId == null)
			p.incrementPosition(32);
		else
			p.copyFromByteArray(machineId, 0, p.getPosition(), 32);
		return p;
	}

	@Override
	public void read(Packet packet) throws IOException {
		packet.getLittleEndian32(); // z4
		packet.copyToByteArray(customData, 0, packet.getPosition(), 8);
		packet.incrementPosition(8);
		packet.copyToByteArray(machineId, 0, packet.getPosition(), 32);
		packet.incrementPosition(32);
	}

	public byte[] getCustomData() {
		return customData;
	}

	public void setCustomData(byte[] customData) {
		this.customData = customData;
	}

	public byte[] getMachineId() {
		return machineId;
	}

	public void setMachineId(byte[] machineId) {
		this.machineId = machineId;
	}

	@Override
	public String toString() {
		return "NTLMSingleHost [customData=" + Arrays.toString(customData) + ", machineId=" + Arrays.toString(machineId) + "]";
	}
}
