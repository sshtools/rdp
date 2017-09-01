package com.sshtools.javardp;

public class RdesktopLicenseException extends RdesktopException {
	private static final long serialVersionUID = 1L;
	public final static int LICENSE_ERROR_CLIENT_INVALID_SERVER_CERTIFICATE = 0x00000001;
	public final static int LICENSE_ERROR_CLIENT_NO_LICESE = 0x00000002;
	public final static int LICENSE_ERROR_SERVER_INVALID_SCOPE = 0x00000004;
	public final static int LICENSE_ERROR_SERVER_NO_LICENSE_SERVER = 0x00000006;
	public final static int LICENSE_STATUS_SERVER_VALID_CLIENT = 0x00000007;
	public final static int LICENSE_ERROR_SERVER_INVALID_CLIENT = 0x00000008;
	public final static int LICENSE_ERROR_SERVER_INVALID_PRODUJCTID = 0x0000000B;
	public final static int LICENSE_ERROR_SERVER_MESSAGE_LEN = 0x0000000C;
	public final static int LICENSE_ERROR_INVALID_MAC = 0x00000003;
	public final static int LICENSE_STATE_TOTAL_ABORT = 0x00000001;
	public final static int LICENSE_STATE_NO_TRANSITION = 0x00000002;
	public final static int LICENSE_STATE_RESET_PHASE_TO_START = 0x00000003;
	public final static int LICENSE_STATE_RESEND_LAST_MESSAGE = 0x00000004;
	private int reason;
	private int state;

	public RdesktopLicenseException(int reason, int state) {
		super(String.format("%s (%s)", licenseErrorReason(reason), getStateName(state)));
		this.reason = reason;
		this.state = state;
	}

	public RdesktopLicenseException(int reason, int state, Throwable cause) {
		super(String.format("%s (%s)", licenseErrorReason(reason), getStateName(state)), cause);
		this.reason = reason;
		this.state = state;
	}

	public int getState() {
		return state;
	}

	public int getReason() {
		return reason;
	}

	public static String getStateName(int state) {
		switch (state) {
		case LICENSE_STATE_NO_TRANSITION:
			return "No transition";
		case LICENSE_STATE_RESEND_LAST_MESSAGE:
			return "Resend last message";
		case LICENSE_STATE_RESET_PHASE_TO_START:
			return "Resend reset phase to start";
		case LICENSE_STATE_TOTAL_ABORT:
			return "Total abort";
		}
		return "Unknown state";
	}

	public static String licenseErrorReason(int reason) {
		switch (reason) {
		case LICENSE_ERROR_CLIENT_INVALID_SERVER_CERTIFICATE:
			return "Invalid server certificate.";
		case LICENSE_ERROR_CLIENT_NO_LICESE:
			return "No license.";
		case LICENSE_ERROR_SERVER_INVALID_SCOPE:
			return "Invalid scope.";
		case LICENSE_ERROR_SERVER_NO_LICENSE_SERVER:
			return "No license server.";
		case LICENSE_STATUS_SERVER_VALID_CLIENT:
			return "Valid client.";
		case LICENSE_ERROR_SERVER_INVALID_CLIENT:
			return "Invalid client.";
		case LICENSE_ERROR_SERVER_INVALID_PRODUJCTID:
			return "Invalid product ID.";
		case LICENSE_ERROR_SERVER_MESSAGE_LEN:
			return "Invalid message length.";
		case LICENSE_ERROR_INVALID_MAC:
			return "Invalid MAC.";
		default:
			return String.format("Unknown license error 0x%x", reason);
		}
	}
}
