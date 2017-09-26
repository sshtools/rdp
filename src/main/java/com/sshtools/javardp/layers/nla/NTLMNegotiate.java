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
		String domain = "";
		String workstation = "";
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_VERSION) == 0) {
			/*
			 * If the NTLMSSP_NEGOTIATE_VERSION flag is set by the client
			 * application, the Version field MUST be set to the current version
			 * (section 2.2.2.10), the DomainName field MUST be set to a
			 * zero-length string, and the Workstation field MUST be set to a
			 * zero-length string.
			 */
			domain = state.getState().getCredential("ntlm", 0, CredentialType.DOMAIN);
			workstation = state.getState().getWorkstationName();
		}
		byte[] domainBytes = NTLM.NULL_BYTES;
		byte[] clientBytes = NTLM.NULL_BYTES;
		int flags = state.getFlags();
		if (StringUtils.isNotBlank(domain)) {
			flags |= NTLM.NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED;
		} else
			flags &= (NTLM.NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED ^ 0xffffffff);
		if (StringUtils.isNotBlank(workstation)) {
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