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

package org.openjdk.jmc.agent.sap.test;

import java.util.TimeZone;

public class TimeZoneChangeTest extends TestBase {

	public static void main(String[] args) {
		new TimeZoneChangeTest().dispatch(args);
	}

	@Override
	protected void runAllTests() throws Exception {
		JavaAgentRunner runner = getRunner("traceTimeZoneChange,logDest=stdout");
		runner.start("changeTimeZones");
		runner.waitForEnd();
		assertLinesContains(runner.getStdoutLines(), "Changed default time zone to Central European Time (CET)",
				"Changed default time zone to Greenwich Mean Time (Etc/GMT+0)",
				"Changed default time zone to Central European Standard Time (Europe/Berlin).");
	}

	public void changeTimeZones() {
		TimeZone.setDefault(TimeZone.getDefault());

		for (String id : TimeZone.getAvailableIDs()) {
			TimeZone.setDefault(TimeZone.getTimeZone(id));
		}
	}
}
