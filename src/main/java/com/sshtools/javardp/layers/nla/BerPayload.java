package com.sshtools.javardp.layers.nla;

import java.io.IOException;

import org.openmuc.jasn1.ber.types.BerType;

interface BerPayload {
	BerType write() throws IOException;
}