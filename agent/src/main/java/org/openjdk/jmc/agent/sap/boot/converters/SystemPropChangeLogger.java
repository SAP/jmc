/*
 * Copyright (c) 2025 SAP SE. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.agent.sap.boot.converters;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

import org.openjdk.jmc.agent.sap.boot.util.ArgumentsHolder;
import org.openjdk.jmc.agent.sap.boot.util.Arguments;
import org.openjdk.jmc.agent.sap.boot.util.LoggingUtils;
import org.openjdk.jmc.agent.sap.boot.util.OutputCommand;

public class SystemPropChangeLogger {
	private static final Properties systemProps = AccessController
			.doPrivileged((PrivilegedAction<Properties>) () -> System.getProperties());

	public static final OutputCommand command = new OutputCommand("traceSysPropsChange",
			"Traces changes to the system properties.");

	private static final ArgumentsHolder holder = command.getArguments();

	private static final ThreadLocal<String> usedKey = new ThreadLocal<String>();
	private static final ThreadLocal<String> usedValue = new ThreadLocal<String>();

	// This is just used to get the old value in the JFR event too.
	public static String logOldValue(Properties props) {
		String key = usedKey.get();

		return props.getProperty(key);
	}

	public static boolean logProperties(Properties props) {
		Arguments args = holder.get();
		String key = usedKey.get();
		assert key == null;
		String val = usedValue.get();

		usedKey.remove();
		usedValue.remove();

		if (props == systemProps) {
			String oldVal = props.getProperty(key);

			if (val == null) {
				LoggingUtils.logWithStack(args, "System properties '" + key + "' with value '" + oldVal + "' removed",
						2);
			} else {
				LoggingUtils.logWithStack(args,
						"System property '" + key + "' changed from '" + oldVal + "' to '" + val + "'", 2);
			}

			return true;
		}

		return false;
	}

	public static String logKey(Object key) {
		assert usedKey.get() == null;

		if (key instanceof String) {
			usedKey.set((String) key);

			return (String) key;
		}

		return "<Object>";
	}

	public static String logValue(Object value) {
		assert usedValue.get() == null;

		if ((value instanceof String) || (value == null)) {
			usedValue.set((String) value);

			return (String) value;
		}

		return "<Object>";
	}
}
