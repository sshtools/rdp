package com.sshtools.javardp;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public enum SecurityType {
	NONE, STANDARD, SSL, HYBRID, RDSTLS, HYBRID_EX;
	public static SecurityType[] fromMasks(int selectedProtocol) {
		List<SecurityType> types = new LinkedList<SecurityType>();
		types.add(STANDARD);
		if ((selectedProtocol & 0x00000001) != 0)
			types.add(SSL);
		if ((selectedProtocol & 0x00000002) != 0)
			types.add(HYBRID);
		if ((selectedProtocol & 0x00000004) != 0)
			types.add(RDSTLS);
		if ((selectedProtocol & 0x00000008) != 0)
			types.add(HYBRID_EX);
		return types.toArray(new SecurityType[0]);
	}

	public static SecurityType fromMask(int protocol) {
		if ((protocol & 0x00000001) != 0)
			return SSL;
		if ((protocol & 0x00000002) != 0)
			return HYBRID;
		if ((protocol & 0x00000004) != 0)
			return RDSTLS;
		if ((protocol & 0x00000008) != 0)
			return HYBRID_EX;
		return STANDARD;
	}

	public int getMask() {
		switch (this) {
		case SSL:
			return 0x00000001;
		case HYBRID:
			return 0x00000002;
		case RDSTLS:
			return 0x00000004;
		case HYBRID_EX:
			return 0x00000008;
		default:
			return 0;
		}
	}

	/**
	 * The default supported security types.
	 * 
	 * @return supported types
	 */
	public static SecurityType[] supported() {
		return new SecurityType[] { SecurityType.STANDARD, SecurityType.SSL, SecurityType.HYBRID };
	}

	public boolean isSSL() {
		return Arrays.asList(SecurityType.HYBRID, SecurityType.SSL, SecurityType.HYBRID_EX, SecurityType.RDSTLS).contains(this);
	}

	public boolean isNLA() {
		return Arrays.asList(SecurityType.HYBRID, SecurityType.HYBRID_EX, SecurityType.RDSTLS).contains(this);
	}
}
