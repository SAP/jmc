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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class TestRunner {
	private static Class<?>[] testClasses = new Class[] {UnsafeAllocationTest.class, SysPropsChangeTest.class,
			TimeZoneChangeTest.class, LocaleChangeTest.class, OpenFileStatisticTest.class, GenericLoggingTest.class};

	public static void main(String[] args) throws Exception {
		ArrayList<String> leftArgs = new ArrayList<>(Arrays.asList(args));

		while (leftArgs.size() > 0) {
			String first = leftArgs.get(0);

			if (first.equals("-dump")) {
				JavaAgentRunner.setDumpOnExit(true);
				leftArgs.remove(0);
			} else if (first.equals("-debug")) {
				if (leftArgs.size() < 2) {
					throw new Exception("Missing port to '-debug'");
				}

				JavaAgentRunner.setDebugPort(Integer.parseInt(leftArgs.get(1)));
				leftArgs.remove(0);
				leftArgs.remove(0);
			} else if (first.equals("-smoke")) {
				TestBase.setSmokeTestOnly();
				leftArgs.remove(0);
			} else {
				break;
			}
		}

		HashSet<String> toRun = new HashSet<>(leftArgs);

		for (Class<?> testClass : testClasses) {
			boolean run = false;

			if (leftArgs.size() == 0) {
				run = true;
			} else {
				for (String arg : leftArgs) {
					if (testClass.getName().endsWith("." + arg)) {
						run = true;
						toRun.remove(arg);
					}
				}
			}

			if (run) {
				Method mainMethod = testClass.getDeclaredMethod("main", String[].class);
				mainMethod.invoke(null, new Object[] {new String[0]});
			}
		}

		if (toRun.size() > 0) {
			throw new RuntimeException("Could not find test " + toRun.iterator().next());
		}
	}
}
