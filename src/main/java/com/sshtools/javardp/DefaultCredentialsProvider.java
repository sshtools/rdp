package com.sshtools.javardp;

import java.util.ArrayList;
import java.util.List;

public class DefaultCredentialsProvider implements CredentialProvider {
	private String domain;
	private String username;
	private char[] password;

	public DefaultCredentialsProvider() {
	}

	public DefaultCredentialsProvider(String domain, String username) {
		super();
		this.domain = domain;
		this.username = username;
	}

	public DefaultCredentialsProvider(String domain, String username, char[] password) {
		super();
		this.domain = domain;
		this.username = username;
		this.password = password;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public char[] getPassword() {
		return password;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	@Override
	public List<String> getCredentials(String scope, int attempts, CredentialType... types) {
		List<String> c = new ArrayList<>();
		for (CredentialType t : types) {
			switch (t) {
			case DOMAIN:
				c.add(domain);
				break;
			case USERNAME:
				c.add(username);
				break;
			case PASSWORD:
				c.add(password == null ? null : new String(password));
				break;
			}
		}
		return c;
	}
}
