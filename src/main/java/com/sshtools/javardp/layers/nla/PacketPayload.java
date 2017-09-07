package com.sshtools.javardp.layers.nla;

import java.io.IOException;

import com.sshtools.javardp.Packet;

interface PacketPayload {
	Packet write() throws IOException;
	
	void read(Packet packet) throws IOException;
}