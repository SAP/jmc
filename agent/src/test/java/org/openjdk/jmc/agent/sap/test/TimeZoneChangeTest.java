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
package org.openjdk.jmc.agent.sap.test;

import java.util.TimeZone;

public class TimeZoneChangeTest extends TestBase {
	private static final TimeZone zone0 = TimeZone.getDefault();
	private static final TimeZone zone1a = TimeZone.getTimeZone("Europe/Berlin");
	private static final TimeZone zone1b = TimeZone.getTimeZone("America/Los_Angeles");
	private static final boolean zone0is1a = zone0.toZoneId().equals(zone1a.toZoneId());
	private static final TimeZone zone1 = zone0is1a ? zone1b : zone1a;

	public static void main(String[] args) {
		new TimeZoneChangeTest().dispatch(args);
	}

	@Override
	protected void runAllTests() throws Exception {
		JavaAgentRunner runner = getRunner("traceTimeZoneChange,logDest=stdout");
		runner.start("changeTimeZones");
		runner.waitForEnd();
		assertLinesContainsRegExp(runner.getStdoutLines(),
				"Changed default time zone to Central European.* Time [(]CET[)]",
				"Changed default time zone to Greenwich Mean Time [(]Etc/GMT+0[)]",
				"Changed default time zone to Central European Standard Time [(]Europe/Berlin[)].");
		runner = getRunnerWithJFR("traceTimeZoneChange,logDest=stdout");
		runner.start("changeForJFR");
		runner.waitForEnd();
		String[] lines = getJfrOutput("jdk.log.*");
		assertLinesContainsInOrder(lines, "fieldNewTimeZone = \"" + zone0.getDisplayName(),
				"fieldNewTimeZoneId = \"" + zone0.toZoneId().getId(), "fieldOldTimeZone = \"" + zone0.getDisplayName(),
				"fieldOldTimeZoneId = \"" + zone0.toZoneId().getId(), "fieldChangesDefault = false",
				"fieldNewTimeZone = \"" + zone1.getDisplayName(), "fieldNewTimeZoneId = \"" + zone1.toZoneId().getId(),
				"fieldOldTimeZone = \"" + zone0.getDisplayName(), "fieldOldTimeZoneId = \"" + zone0.toZoneId().getId(),
				"fieldChangesDefault = true", "fieldNewTimeZone = \"" + zone0.getDisplayName(),
				"fieldNewTimeZoneId = \"" + zone0.toZoneId().getId(), "fieldOldTimeZone = \"" + zone1.getDisplayName(),
				"fieldOldTimeZoneId = \"" + zone1.toZoneId().getId(), "fieldChangesDefault = true");

	}

	public void changeTimeZones() {
		TimeZone.setDefault(TimeZone.getDefault());

		for (int i = 0; i < 2; ++i) {
			for (String id : TimeZone.getAvailableIDs()) {
				TimeZone.setDefault(TimeZone.getTimeZone(id));
			}
		}
	}

	public void changeForJFR() {
		TimeZone.setDefault(zone0);
		TimeZone.setDefault(zone1);
		TimeZone.setDefault(zone0);
	}
}
