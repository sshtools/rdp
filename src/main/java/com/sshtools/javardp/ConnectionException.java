/* ConnectionException.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1 $
 * Author: $Author: brett $
 * Date: $Date: 2011/11/28 14:13:42 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: 
 */

package com.sshtools.javardp;

import java.io.IOException;

public class ConnectionException extends IOException {

	public ConnectionException(String message) {
		super(message);
	}

}
