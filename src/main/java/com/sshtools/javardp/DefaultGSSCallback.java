package com.sshtools.javardp;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

public class DefaultGSSCallback implements CallbackHandler {
	private String username = null;
	private String password = null;

	public DefaultGSSCallback(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public void setUser(String uname) {
		username = uname;
	}

	public void setPassword(String pword) {
		password = pword;
	}

	@Override
	public void handle(Callback[] callbacks) {
		Callback cl = callbacks[0];
		if (cl instanceof NameCallback) {
			((NameCallback) cl).setName(username);
		}
		if (cl instanceof PasswordCallback && password != null) {
			((PasswordCallback) cl).setPassword(password.toCharArray());
		}
	}
}
