package com.sshtools.javardp.layers.nla;

import java.io.IOException;

interface DataPayload {
	byte[] write() throws IOException;
}