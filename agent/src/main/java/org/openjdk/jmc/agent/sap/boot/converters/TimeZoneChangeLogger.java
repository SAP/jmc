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

import java.util.TimeZone;

import org.openjdk.jmc.agent.sap.boot.util.ArgumentsHolder;
import org.openjdk.jmc.agent.sap.boot.util.Command;
import org.openjdk.jmc.agent.sap.boot.util.LoggingUtils;
import org.openjdk.jmc.agent.sap.boot.util.OutputCommand;

public class TimeZoneChangeLogger {

	public static Command command = new OutputCommand("traceTimeZoneChange",
			"Traces when the default time zone is changed.") {
		public void preTraceInit() {
			TimeZone.getDefault(); // Trigger loading before we trace to avoid circularity errors.
		}
	};

	private static final ArgumentsHolder holder = command.getArguments();

	public static String logDefaultTimeZoneChange(TimeZone newZone) {
		String result = newZone.getDisplayName();

		// If this doesn't changes the default time zone, don't log it.
		if (changesDefaultTimeZone(newZone)) {
			LoggingUtils.logWithStack(holder.get(),
					"Changed default time zone to " + result + " (" + newZone.toZoneId().toString() + ").", 2);
		}

		return result;
	}

	public static String logDefaultTimeZoneIdChange(TimeZone newZone) {
		return newZone.toZoneId().getId();
	}

	public static String logDefaultTimeZone(TimeZone newZone) {
		return TimeZone.getDefault().getDisplayName();
	}

	public static String logDefaultTimeZoneId(TimeZone newZone) {
		return TimeZone.getDefault().toZoneId().getId();
	}

	public static boolean changesDefaultTimeZone(TimeZone newZone) {
		return !newZone.toZoneId().equals(TimeZone.getDefault().toZoneId());
	}
}
