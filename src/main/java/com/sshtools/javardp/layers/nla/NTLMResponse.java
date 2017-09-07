package com.sshtools.javardp.layers.nla;

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.Packet;

public class NTLMResponse implements PacketPayload {
	static Logger logger = LoggerFactory.getLogger(NTLMResponse.class);
	private NTLMState state;

	public NTLMResponse(NTLMState state) {
		this.state = state;
	}

	@Override
	public Packet write() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void read(Packet packet) throws IOException {
		NTLMPacket ntlmp = (NTLMPacket) packet;
		byte[] sig = new byte[NTLM.SIG.length];
		ntlmp.copyToByteArray(sig, 0, 0, sig.length);
		ntlmp.incrementPosition(NTLM.SIG.length);
		ntlmp.getLittleEndian32(); // message type
		if (!Arrays.equals(sig, NTLM.SIG))
			throw new IOException("Does not appear to be an NTLM response.");
		byte[] data = ntlmp.getOffsetArray();
		state.setFlags(ntlmp.getLittleEndian32());
		if ((state.getFlags() & NTLM.NTLMSSP_REQUEST_TARGET) == 0)
			state.setTarget(null);
		else
			state.setTarget(state.getDecodedString(data));
		byte[] challenge = new byte[8];
		ntlmp.copyToByteArray(challenge, 0, ntlmp.getPosition(), 8);
		state.setChallenge(challenge);
		ntlmp.incrementPosition(8);
		ntlmp.fill(8); // reserved
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_TARGET_INFO) != 0) {
			state.setAvPairs(data.length == 0 ? null : new NTLMAVPairs(ntlmp.getOffsetArray()));
			logger.debug(String.format("NTLM Target Info : %s", state.getAvPairs()));
		}
		if ((state.getFlags() & NTLM.NTLMSSP_NEGOTIATE_VERSION) != 0) {
			Packet p = new Packet(8);
			ntlmp.copyToPacket(p, ntlmp.getPosition(), 0, p.capacity());
			ntlmp.incrementPosition(8);
			state.getClientVersion().read(p);
			logger.debug(String.format("NTLM Version : %s", state.getClientVersion()));
		}
	}
}