package com.sshtools.javardp.layers.nla;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.MessageProp;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.javardp.CredentialProvider.CredentialType;
import com.sshtools.javardp.DefaultGSSCallback;
import com.sshtools.javardp.HexDump;
import com.sshtools.javardp.State;
import com.sshtools.javardp.layers.Transport;

public class NLA3 {
	static Logger logger = LoggerFactory.getLogger(Transport.class);
	private State state;
	private Transport transport;
	private HexDump dump;
	private Configuration configuration;

	public NLA3(State state, Transport transport) {
		this.state = state;
		this.transport = transport;
		dump = new HexDump();
	}

	public void start() throws IOException {
		if (state.getOptions().getJaasConfiguration() == null) {
			// configuration = new Configuration() {
			// @Override
			// public AppConfigurationEntry[] getAppConfigurationEntry(String
			// name) {
			// final Map<String, String> options = new HashMap<String,
			// String>();
			// return new AppConfigurationEntry[] { new
			// AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
			// LoginModuleControlFlag.REQUIRED, options) };
			// }
			//
			// @Override
			// public void refresh() {
			// }
			// };
		} else {
			configuration = state.getOptions().getJaasConfiguration();
		}
		try {
			sendNegotiate();
		} catch (GSSException ge) {
			throw new IOException("GSS error.", ge);
		}
	}

	private void sendNegotiate() throws IOException, GSSException {
		logger.info("Sending SPNEGO");
		if (configuration == null) {
			doSPNEGO("realm/" + state.getCredential("nla", 0, CredentialType.DOMAIN));
		} else {
			CallbackHandler gsscall = new DefaultGSSCallback(state.getCredential("nla", 0, CredentialType.USERNAME));
			LoginContext lc = null;
			try {
				lc = new LoginContext("com.sun.security.jgss.initiate", null, gsscall, configuration);
				lc.login();
				Subject subject = lc.getSubject();
				Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
					@Override
					public Void run() throws Exception {
						doSPNEGO("host/" + state.getWorkstationName());
						return null;
					}
				});
			} catch (PrivilegedActionException pae) {
				if (pae.getException() instanceof GSSException) {
					throw (GSSException) pae.getException();
				} else if (pae.getException() instanceof IOException) {
					throw (IOException) pae.getException();
				} else
					throw new IOException("Privileged action exception.", pae.getException());
			} catch (LoginException e1) {
				throw new IOException("Login error.", e1);
			}
		}
		// try {
		// doSPNEGO("Administrator@SOUTHPARK");
		// } catch (GSSException e) {
		// throw new IOException("GSS error.", e);
		// }
	}

	private void doSPNEGO(String serverPrinc) throws GSSException, IOException, UnsupportedEncodingException {
		DataOutputStream outStream = transport.getOut();
		DataInputStream inStream = transport.getIn();
		GSSManager manager = GSSManager.getInstance();
		Oid[] oids = manager.getMechs();
		for (int i = 0; i < oids.length; i++) {
			logger.info(String.format("GSSAPI supports %s. %s", oids[i], Arrays.asList(manager.getNamesForMech(oids[i]))));
		}
		/*
		 * This Oid is used to represent the SPNEGO GSS-API mechanism. It is
		 * defined in RFC 2478. We will use this Oid whenever we need to
		 * indicate to the GSS-API that it must use SPNEGO for some purpose.
		 */
		Oid spnegoOid = new Oid("1.3.6.1.5.5.2");
		/*
		 * Create a GSSName out of the server's name.
		 */
		GSSName serverName = manager.createName(serverPrinc, GSSName.NT_HOSTBASED_SERVICE, spnegoOid);
		GSSCredential creds = null;
		/*
		 * Create a GSSContext for mutual authentication with the server. -
		 * serverName is the GSSName that represents the server. - krb5Oid is
		 * the Oid that represents the mechanism to use. The client chooses the
		 * mechanism to use. - null is passed in for client credentials -
		 * DEFAULT_LIFETIME lets the mechanism decide how long the context can
		 * remain valid. Note: Passing in null for the credentials asks GSS-API
		 * to use the default credentials. This means that the mechanism will
		 * look among the credentials stored in the current Subject to find the
		 * right kind of credentials that it needs.
		 */
		GSSContext context = manager.createContext(serverName, spnegoOid, creds, GSSContext.DEFAULT_LIFETIME);
		// Set the desired optional features on the context. The client
		// chooses these options.
		context.requestMutualAuth(true); // Mutual authentication
		context.requestConf(true); // Will use confidentiality later
		context.requestInteg(true); // Will use integrity later
		// Do the context eastablishment loop
		byte[] token = new byte[0];
		while (!context.isEstablished()) {
			// token is ignored on the first call
			token = context.initSecContext(token, 0, token.length);
			// Send a token to the server if one was generated by
			// initSecContext
			if (token != null) {
				logger.info(String.format("Will send token of size %d from initSecContext", token.length));
				HexDump.encode(token, "Token:");
				outStream.writeInt(token.length);
				outStream.write(token);
				outStream.flush();
			}
			// If the client is done with context establishment
			// then there will be no more tokens to read in this loop
			if (!context.isEstablished()) {
				token = new byte[inStream.readInt()];
				HexDump.encode(token, "Reading Token:");
				logger.info(String.format("Will read input token of size %d for processing by initSecContext", token.length));
				inStream.readFully(token);
			}
		}
		System.out.println("Context Established! ");
		System.out.println("Client principal is " + context.getSrcName());
		System.out.println("Server principal is " + context.getTargName());
		/*
		 * If mutual authentication did not take place, then only the client was
		 * authenticated to the server. Otherwise, both client and server were
		 * authenticated to each other.
		 */
		if (context.getMutualAuthState())
			System.out.println("Mutual authentication took place!");
		byte[] messageBytes = "Hello There!".getBytes("UTF-8");
		/*
		 * The first MessageProp argument is 0 to request the default
		 * Quality-of-Protection. The second argument is true to request privacy
		 * (encryption of the message).
		 */
		MessageProp prop = new MessageProp(0, true);
		/*
		 * Encrypt the data and send it across. Integrity protection is always
		 * applied, irrespective of confidentiality (i.e., encryption). You can
		 * use the same token (byte array) as that used when establishing the
		 * context.
		 */
		System.out.println("Sending message: " + new String(messageBytes, "UTF-8"));
		token = context.wrap(messageBytes, 0, messageBytes.length, prop);
		outStream.writeInt(token.length);
		outStream.write(token);
		outStream.flush();
		/*
		 * Now we will allow the server to decrypt the message, append a
		 * time/date on it, and send then it back.
		 */
		token = new byte[inStream.readInt()];
		System.out.println("Will read token of size " + token.length);
		inStream.readFully(token);
		byte[] replyBytes = context.unwrap(token, 0, token.length, prop);
		System.out.println("Received message: " + new String(replyBytes, "UTF-8"));
		System.out.println("Done.");
		context.dispose();
	}
}
