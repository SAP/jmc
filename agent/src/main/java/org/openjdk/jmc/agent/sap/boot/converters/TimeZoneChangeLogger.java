/*
 * Copyright (c) 2025 SAP SE. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
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

		// If this same, don't log it.
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
