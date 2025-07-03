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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.openjdk.jmc.agent.test.util.OutputReader;

public abstract class TestBase {

	private static boolean smokeTestsOnly;
	public static String DONE = "DONE";
	public static long MAX_TEST_CASE_DURATION = 5 * 60;

	public void dispatch(String[] args) {
		try {
			if (args.length == 0) {
				runAllTests();
			} else {
				Thread killer = new Thread(() -> {
					while (true) {
						try {
							Thread.sleep(MAX_TEST_CASE_DURATION * 1000);
							System.err.println("Test run in timeout.");
							System.exit(1);
						} catch (InterruptedException e) {
							// Ignore
						}
					}
				}, "Timeout Thread");
				killer.setDaemon(true);
				killer.start();

				try {
					Method m = this.getClass().getDeclaredMethod(args[0]);
					m.invoke(this);
				} catch (NoSuchMethodException e) {
					throw new RuntimeException("Undefined test '" + args[0] + "'");
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Test failed", e);
		}
	}

	public JavaAgentRunner getRunner(String options, String ... vmArgs) {
		return new JavaAgentRunner(getClass(), options, vmArgs);
	}

	private static File getJfrFile() {
		File outputDir = new File("target", "output");

		if (!outputDir.exists()) {
			outputDir.mkdir();
		}

		return new File(outputDir, "test.jfr").getAbsoluteFile();
	}

	public JavaAgentRunner getRunnerWithJFR(String options, String ... vmArgs) {
		File jfrFile = getJfrFile();
		jfrFile.delete();

		String[] newVmArgs = new String[vmArgs.length + 1];
		System.arraycopy(vmArgs, 0, newVmArgs, 0, vmArgs.length);
		newVmArgs[vmArgs.length] = "-XX:StartFlightRecording=filename=" + jfrFile.getPath();

		return new JavaAgentRunner(getClass(), options, newVmArgs);
	}

	public String[] getJfrOutput(String idFilter) throws IOException, InterruptedException {
		File jfrFile = getJfrFile();

		if (!jfrFile.exists()) {
			throw new FileNotFoundException(jfrFile.getPath());
		}

		ProcessBuilder pb = new ProcessBuilder("jfr", "print", "--events", idFilter, jfrFile.getPath());
		Process process = pb.start();
		StringBuilder output = new StringBuilder();
		OutputReader reader = new OutputReader(process.getInputStream(), output);
		Thread worker = new Thread(reader);
		worker.setDaemon(true);
		worker.start();
		process.waitFor();
		worker.join();

		return reader.getLines();
	}

	protected abstract void runAllTests() throws Exception;

	private static void failLines(String[] lines, String msg) {
		failLines(lines, msg, -1);
	}

	private static void failLines(String[] lines, String msg, int markedLine) {
		System.err.println(msg + ":");
		System.err.println("---- START");

		for (int i = 0; i < lines.length; ++i) {
			if (i == markedLine) {
				System.err.println("=> " + lines[i]);
			} else {
				System.err.println("   " + lines[i]);
			}
		}

		System.err.println("---- END");
		throw new AssertionError(msg);
	}

	public static void assertNrOfLines(String[] lines, int expectedNrOfLines) {
		if (lines.length != expectedNrOfLines) {
			failLines(lines, "Expected " + expectedNrOfLines + " lines but got " + lines.length, -1);
		}
	}

	public static void assertLinesContains(String[] lines, String ... substrings) {
		outer: for (String substring : substrings) {
			for (String line : lines) {
				if (line.indexOf(substring) >= 0) {
					continue outer;
				}
			}

			failLines(lines, "Could not find '" + substring + "' in the lines");
		}
	}

	public static void assertLinesContainsInOrder(String[] lines, String ... substrings) {
		int index = 0;
		for (String line : lines) {
			String substring = substrings[index];

			if (line.indexOf(substring) >= 0) {
				++index;

				if (index == substrings.length) {
					return;
				}
			}
		}

		failLines(lines, "Could not find '" + substrings[index] + "' in the lines");
	}

	public static void assertLinesContainsRegExp(String[] lines, String ... regexps) {
		outer: for (String regexp : regexps) {
			Pattern pattern = Pattern.compile(regexp);

			for (String line : lines) {
				if (pattern.matcher(line).find()) {
					continue outer;
				}
			}

			failLines(lines, "Could not find regexp '" + regexp + "' in the lines");
		}
	}

	public static void assertLinesNotContains(String[] lines, String ... substrings) {
		for (String substring : substrings) {
			for (String line : lines) {
				if (line.indexOf(substring) >= 0) {
					failLines(lines, "Unexpectedly found '" + substring + "' in the lines");
				}
			}
		}
	}

	public static void assertLinesNotContainsRegExp(String[] lines, String ... regexps) {
		for (String regexp : regexps) {
			Pattern pattern = Pattern.compile(regexp);

			for (int i = 0; i < lines.length; ++i) {
				if (pattern.matcher(lines[i]).find()) {
					failLines(lines, "Unexpectedly found regexp '" + regexp + "' in the lines", i);
					return;
				}
			}
		}
	}

	public static void assertNotNull(Object obj) {
		if (obj == null) {
			throw new AssertionError("Object is null");
		}
	}

	public static void setSmokeTestOnly() {
		smokeTestsOnly = true;
	}

	public static boolean smokeTestsOnly() {
		return smokeTestsOnly;
	}

	public void assertRunnerFinished(JavaAgentRunner runner) {
		int result = runner.waitForEnd();

		if (result != 0) {
			throw new AssertionError("Exit code " + result + " for " + runner.getCommandLine());
		}
	}

	public static void sleep(long seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {

		}
	}

	protected static void done(int index, long waitTime) {
		System.out.println(DONE + index);

		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected static void done() {
		System.out.println(DONE + "*");

		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
