package com.sshtools.javardp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IO {
	InputStream getInputStream() throws IOException;

	OutputStream getOutputStream() throws IOException;

	void closeIO() throws IOException;
}
