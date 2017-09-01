package com.sshtools.javardp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IO {
	void closeIO() throws IOException;

	InputStream getInputStream() throws IOException;

	OutputStream getOutputStream() throws IOException;
}
