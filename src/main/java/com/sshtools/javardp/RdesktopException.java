/* 
 * General RDP related exception.
 *
 * Copyright (c) 2005 Propero Limited
 * Copyright (c) 2017 SSHTools Limited
 */
package com.sshtools.javardp;

public class RdesktopException extends Exception {

	private static final long serialVersionUID = 6735403404532243398L;

	public RdesktopException() {
		super();
	}

	public RdesktopException(String message) {
		super(message);
	}

	public RdesktopException(String message, Throwable cause) {
		super(message, cause);
	}

	public RdesktopException(Throwable cause) {
		super(cause);
	}

}
