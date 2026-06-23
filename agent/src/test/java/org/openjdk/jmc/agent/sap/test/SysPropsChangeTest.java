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

import java.io.File;
import java.util.Properties;

// You can run it via (if the cwd is the agent directory):
// java -cp target/test-classes org.openjdk.jmc.agent.sap.test.SysPropsChangeTest
public class SysPropsChangeTest extends TestBase {

	public static void main(String[] args) {
		new SysPropsChangeTest().dispatch(args);
	}

	@Override
	protected void runAllTests() throws Exception {
		JavaAgentRunner runner = getRunnerWithJFR("traceSysPropsChange,logDest=stdout");
		runner.start("changeSystemProps");
		runner.waitForEnd();
		assertLinesContains(runner.getStdoutLines(), "System property 'TEST_KEY' changed from 'null' to 'TEST_VAL'",
				"System properties 'TEST_KEY' with value 'TEST_VAL' removed", SysPropsChangeTest.class.getName());
		assertLinesNotContains(runner.getStdoutLines(), "TEST_KEY_NO_SYS");
		assertLinesContainsInOrder(getJfrOutput("jdk.log.*"), "fieldValue = \"TEST_VAL\"", "OldValue = N/A",
				"IsSystemProperty = true", "RemovedValue = \"TEST_VAL\"", "IsSystemProperty = true", "OldValue = N/A",
				"IsSystemProperty = false", "OldValue = \"TEST_ADD_VALUE\"", "RemovedValue = \"TEST_CHANGE_VALUE\"",
				"RemovedValue = N/A");

		// Check if we can omit the stack.
		runner = getRunner("traceSysPropsChange,logDest=stderr,logWithStack=false");
		runner.start("changeSystemProps");
		runner.waitForEnd();
		assertLinesContains(runner.getStderrLines(), "System property 'TEST_KEY' changed from 'null' to 'TEST_VAL'",
				"System properties 'TEST_KEY' with value 'TEST_VAL' removed");
		assertLinesNotContains(runner.getStderrLines(), SysPropsChangeTest.class.getName());

		// Check if we get help
		runner = getRunner("traceSysPropsChange,help");
		runner.start("changeSystemProps");
		runner.waitForEnd();
		assertLinesContains(runner.getStderrLines(), "Help for command 'traceSysPropsChange'",
				"Traces changes to the system properties", "logWithStack");
	}

	public static void changeSystemProps() {
		new File("testfile");
		System.setProperty("TEST_KEY", "TEST_VAL");
		System.getProperties().remove("TEST_KEY");
		Properties props = new Properties();
		props.put("TEST_KEY_NO_SYS", "TEST_ADD_VALUE");
		props.setProperty("TEST_KEY_NO_SYS", "TEST_CHANGE_VALUE");
		props.remove("TEST_KEY_NO_SYS");
		props.remove("TEST_KEY_NO_SYS");
	}
}
