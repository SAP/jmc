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

package org.openjdk.jmc.agent.sap.boot.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.regex.Pattern;

public class CommandArguments {
	private final HashMap<String, String> args;
	private final Command command;

	public CommandArguments(Command command) {
		this.command = command;
		String optionsLine = AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				return System.getProperty("com.sap.jvm.jmcagent.options." + command.getName(), "");
			}
		});
		this.args = getOptions(optionsLine);
	}

	public CommandArguments(String optionsLine) {
		this.command = null;
		this.args = getOptions(optionsLine);
	}

	public CommandArguments(String line, Command command) {
		this.command = command;
		this.args = getOptions(line);
	}

	public Command getCommand() {
		return command;
	}

	public boolean hasHelpOption() {
		return args.containsKey("help");
	}

	public String getString(String option, String defaultResult) {
		if (args.containsKey(option)) {
			return args.get(option);
		}

		return defaultResult;
	}

	public boolean getBoolean(String option, boolean defaultResult) {
		if (args.containsKey(option)) {
			return Boolean.parseBoolean(args.get(option));
		}

		return defaultResult;
	}

	public Pattern getPattern(String option, Pattern defaultResult) {
		if (args.containsKey(option)) {
			return Pattern.compile(args.get(option));
		}

		return defaultResult;
	}

	public int getInt(String option, int defaultRersult) {
		if (args.containsKey(option)) {
			try {
				return Integer.parseInt(args.get(option));
			} catch (NumberFormatException e) {
				reportOptionError(option, "Could not parse integer value");
				System.exit(1);
			}
		}

		return defaultRersult;
	}

	public String getUnknownArgument() {
		for (String key : args.keySet()) {
			if (!command.hasOption(key)) {
				return key;
			}
		}

		return null;
	}

	public long getLong(String option, long defaultRersult) {
		if (args.containsKey(option)) {
			try {
				return Long.parseLong(args.get(option));
			} catch (NumberFormatException e) {
				reportOptionError(option, "Could not parse integer value");
			}
		}

		return defaultRersult;
	}

	private long parseUnits(String option, long defaultRersult, char[] suffixes, long[] scale) {
		if (!args.containsKey(option)) {
			return defaultRersult;
		}

		String rest = args.get(option);
		long result = 0;

		while (!rest.isEmpty()) {
			long part = 0;
			boolean isNeg = false;

			if (rest.startsWith("-")) {
				isNeg = true;
				rest = rest.substring(1);
			}

			while (!rest.isEmpty()) {
				int c = rest.charAt(0);

				if ((c < '0') || (c > '9')) {
					break;
				}

				part = part * 10 + (c - '0');
				rest = rest.substring(1);
			}

			if (rest.isEmpty()) {
				result += part * (isNeg ? -1 : 1);
				break;
			}

			boolean found = false;

			for (int i = 0; i < suffixes.length; ++i) {
				if (rest.charAt(0) == suffixes[i]) {
					result += part * scale[i] * (isNeg ? -1 : 1);
					found = true;
					break;
				}
			}

			if (!found) {
				reportOptionError(option, "Unknown unit '" + rest.charAt(0) + "'.");
			}

			rest = rest.substring(1);
		}

		return result;
	}

	public long getSize(String option, long defaultRersult) {
		return parseUnits(option, defaultRersult, new char[] {'k', 'M', 'G'},
				new long[] {1024, 1024 * 1024, 1024 * 1024 * 1024});
	}

	public long getDurationInSeconds(String option, long defaultRersult) {
		return parseUnits(option, defaultRersult, new char[] {'s', 'm', 'h', 'd'}, new long[] {1, 60, 3600, 3600 * 24});
	}

	private static String dequote(String str) {
		StringBuilder result = new StringBuilder();

		for (int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);

			if (c == '\\') {
				if (i + 1 < str.length()) {
					result.append(str.charAt(i + 1));
					i += 1;
				} else {
					// Trailing backslash is treated just as a backslash.
					result.append(c);
				}
			} else {
				result.append(c);
			}
		}

		return result.toString();
	}

	private static HashMap<String, String> getOptions(String line) {
		HashMap<String, String> result = new HashMap<>();
		String[] keysAndValues = line.split("(?<!\\\\),");

		for (String keyAndValue : keysAndValues) {
			if (keyAndValue.length() > 0) {
				String[] parts = keyAndValue.split("(?<!\\\\)=", 2);

				if (parts[0].length() > 0) {
					result.put(dequote(parts[0]), parts.length == 1 ? null : dequote(parts[1]));
				}
			}
		}

		return result;
	}

	private void reportOptionError(String option, String msg) {
		System.err.println("Error in option " + option + "=" + getString(option, "<empty>") + " for command '"
				+ command.getName() + "'");
		System.err.println(msg);
		System.exit(1);
	}
}
