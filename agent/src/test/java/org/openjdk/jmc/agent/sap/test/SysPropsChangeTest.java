/*
 * Copyright (c) 2024 SAP SE. All rights reserved.
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

import java.io.File;
import java.util.Properties;

// You can run it via (if the cwd is the agent directory):
// java -javaagent:target/agent-1.0.1-SNAPSHOT.jar=traceSysPropsChange -cp target/test-classes org.openjdk.jmc.agent.sap.test.SystemPropertiesChanger
public class SysPropsChangeTest extends TestBase {

	public static void main(String[] args) {
		new SysPropsChangeTest().dispatch(args);
	}

	@Override
	protected void runAllTests() throws Exception {
		JavaAgentRunner runner = getRunner("traceSysPropsChange,logDest=stdout");
		runner.start("changeSystemProps");
		runner.waitForEnd();
		assertLinesContainsRegExp(runner.getStdoutLines(),
				"System property 'TEST_KEY' changed from 'null' to 'TEST_VAL'");
		assertLinesContainsRegExp(runner.getStdoutLines(),
				"System properties 'TEST_KEY' with value 'TEST_VAL' removed");
		assertLinesContainsRegExp(runner.getStdoutLines(), SysPropsChangeTest.class.getName());
		assertLinesNotContainsRegExp(runner.getStdoutLines(), "TEST_KEY_NO_SYS");
	}

	public static void changeSystemProps() {
		new File("testfile");
		System.setProperty("TEST_KEY", "TEST_VAL");
		System.getProperties().remove("TEST_KEY");
		Properties props = new Properties();
		props.put("TEST_KEY_NO_SYS", "TEST_ADD_VALUE");
		props.setProperty("TEST_KEY_NO_SYS", "TEST_CHANGE_VALUE");
		props.remove("TEST_KEY_NO_SYS");
	}
}
