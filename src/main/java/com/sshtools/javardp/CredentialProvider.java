package com.sshtools.javardp;

import java.util.List;

public interface CredentialProvider {
	public enum CredentialType {
		DOMAIN, USERNAME, PASSWORD
	}

	List<String> getCredentials(String scope, int attempts, CredentialType... types);
}
