package com.sshtools.javardp.layers.nla;

import java.io.IOException;

public class HResultException extends IOException {
	public enum Severity {
		SUCCESS, INFORMATIONAL, WARNING, ERROR
	}

	private static final long serialVersionUID = -894055754620935454L;
	private Severity severity;
	private boolean customer;
	private boolean ntStatus;
	private int facility;
	private int code;

	public HResultException(int code) {
		this(code, null);
	}

	public HResultException(int code, Throwable cause) {
		super(cause);
		customer = ((code >> 29) & 0x01) != 0;
		ntStatus = ((code >> 28) & 0x01) != 0;
		if (ntStatus) {
			severity = Severity.values()[((code >> 30) & 0x03)];
			facility = ((code >> 16) & 0xFFF);
			this.code = code & 0xffff;
		} else {
			severity = ((code >> 31) & 0x01) != 0 ? Severity.ERROR : Severity.SUCCESS;
			facility = ((code >> 16) & 0x7FF);
			this.code = code & 0xffff;
		}
	}

	public Severity getSeverity() {
		return severity;
	}

	public boolean isCustomer() {
		return customer;
	}

	public boolean isNtStatus() {
		return ntStatus;
	}

	public int getFacility() {
		return facility;
	}

	public int getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		StringBuilder i = new StringBuilder();
		i.append(String.format("[%s] ", severity));
		if (customer)
			i.append("(Customer Defined) ");
		if (ntStatus)
			i.append("{NTSTATUS} ");
		i.append("Facility: ");
		switch (facility) {
		case 0:
			i.append("NULL");
			break;
		case 1:
			i.append("RPC");
			break;
		case 2:
			i.append("Dispatch");
			break;
		case 3:
			i.append("Storage");
			break;
		case 4:
			i.append("ITF");
			break;
		case 7:
			i.append("Win32");
			break;
		case 8:
			i.append("Windows");
			break;
		case 9:
			i.append("Security");
			break;
		case 10:
			i.append("Control");
			break;
		case 11:
			i.append("Cert");
			break;
		case 12:
			i.append("Internet");
			break;
		case 13:
			i.append("MediaServer");
			break;
		case 14:
			i.append("MSMQ");
			break;
		case 15:
			i.append("SetupAPI");
			break;
		case 16:
			i.append("Scard");
			break;
		case 17:
			i.append("ComPlus");
			break;
		case 18:
			i.append("AAF");
			break;
		case 19:
			i.append("URT");
			break;
		case 20:
			i.append("ACS");
			break;
		case 21:
			i.append("DPlay");
			break;
		case 22:
			i.append("UMI");
			break;
		case 23:
			i.append("SXS");
			break;
		case 24:
			i.append("WindowsCE");
			break;
		case 25:
			i.append("HTTP");
			break;
		case 26:
			i.append("UserModeCommonLog");
			break;
		case 31:
			i.append("UserModeFilterManager");
			break;
		case 32:
			i.append("BackgroundCopy");
			break;
		case 33:
			i.append("Configuration");
			break;
		case 34:
			i.append("StateManagement");
			break;
		case 35:
			i.append("MetaDirectory");
			break;
		case 36:
			i.append("WindowsUpdate");
			break;
		case 37:
			i.append("DirectoryService");
			break;
		case 38:
			i.append("Graphics");
			break;
		case 39:
			i.append("Shell");
			break;
		case 40:
			i.append("TPMServices");
			break;
		case 41:
			i.append("TPMSoftward");
			break;
		case 48:
			i.append("PLA");
			break;
		case 49:
			i.append("FVE");
			break;
		case 50:
			i.append("FWP");
			break;
		case 51:
			i.append("WinRM");
			break;
		case 52:
			i.append("NDIS");
			break;
		case 53:
			i.append("UserModeHypervisor");
			break;
		case 54:
			i.append("CMI");
			break;
		case 55:
			i.append("UserModeVirtualisation");
			break;
		case 56:
			i.append("UserModeVolMgr");
			break;
		case 57:
			i.append("BCD");
			break;
		case 58:
			i.append("UserModeVHD");
			break;
		case 60:
			i.append("SDiag");
			break;
		case 61:
			i.append("WebServices");
			break;
		case 80:
			i.append("WindowsDefender");
			break;
		case 81:
			i.append("OPC");
			break;
		default:
			i.append("Unknown");
			break;
		}
		i.append(String.format(" (0x%x:%d)", facility, facility));
		i.append(String.format(" Code: [0x%x:%d]", code, code));
		return i.toString();
	}
}
