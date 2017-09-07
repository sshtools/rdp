package com.sshtools.javardp.layers.nla;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.sshtools.javardp.CredentialProvider.CredentialType;
import com.sshtools.javardp.Packet;

public class NTLMNegotiate implements PacketPayload {
	private NTLMState state;

	public NTLMNegotiate(NTLMState state) {
		this.state = state;
	}

	@Override
	public Packet write() throws IOException {
		String domain = state.getState().getCredential("ntlm", 0, CredentialType.DOMAIN);
		String client = state.getState().getWorkstationName();
		byte[] domainBytes = domain == null ? NTLM.NULL_BYTES : domain.getBytes(NTLMState.STANDARD_ENCODING);
		byte[] clientBytes = client == null ? NTLM.NULL_BYTES : client.getBytes(NTLMState.STANDARD_ENCODING);
		int flags = state.getFlags();
		if (StringUtils.isNotBlank(domain)) {
			flags |= NTLM.NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED;
		} else
			flags &= (NTLM.NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED ^ 0xffffffff);
		if (StringUtils.isNotBlank(client)) {
			flags |= NTLM.NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED;
		} else
			flags &= (NTLM.NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED ^ 0xffffffff);
		int pktlen = 16 + domainBytes.length + clientBytes.length + 16;
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_VERSION) != 0)
			pktlen += 8;
		NTLMPacket packet = new NTLMPacket(pktlen);
		packet.copyFromByteArray(NTLM.SIG, 0, 0, NTLM.SIG.length);
		packet.incrementPosition(NTLM.SIG.length);
		packet.setLittleEndian32(1);
		packet.setLittleEndian32(flags);
		int off = 32;
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_VERSION) != 0)
			off += 8;
		off += packet.setOffsetArray(off, domainBytes);
		packet.setOffsetArray(off, clientBytes);
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_VERSION) != 0)
			packet.setPacket(state.getClientVersion().write());
		return packet;
	}

	@Override
	public void read(Packet packet) throws IOException {
		throw new UnsupportedOperationException();
	}
}