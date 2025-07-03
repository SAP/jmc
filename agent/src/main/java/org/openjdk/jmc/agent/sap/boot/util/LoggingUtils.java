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

package org.openjdk.jmc.agent.sap.boot.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Formatter;
import java.util.HashMap;
import java.util.IdentityHashMap;

public class LoggingUtils {
	public static final String LOG_DEST = "logDest";
	public static final String LOG_WITH_STACK = "logWithStack";
	private static HashMap<String, PrintStream> outputs = new HashMap<>();
	private static IdentityHashMap<PrintStream, Formatter> formatters = new IdentityHashMap<>();

	public static void addOptions(Command command) {
		command.addOption(LOG_DEST,
				"Specifies the output destination. Can be 'stdout', 'stderr', 'none' or a file name. "
						+ "Prepend the filename with a '+' to append to the file instead of overwriting it.");
		command.addOption(LOG_WITH_STACK, "If true, print a stack trace for every log output.");
	}

	public static void addOptionsWithStack(Command command) {
		addOptions(command);
		command.addOption(LOG_WITH_STACK, "Print a stack trace for every log output.");
	}

	public static boolean doesOutput(Arguments args) {
		return !"none".equals(args.getString(LOG_DEST, "stderr"));
	}

	public static Formatter getFormatter(Arguments args) {
		PrintStream stream = getStream(args);
		Formatter formatter;

		synchronized (formatters) {
			formatter = formatters.get(stream);

			if (formatter == null) {
				formatter = new Formatter(stream);
				formatters.put(stream, formatter);
			}
		}

		return formatter;
	}

	public static PrintStream getStream(Arguments args) {
		String dest = args.getString(LOG_DEST, "stderr");

		if ("none".equals(dest)) {
			return new PrintStream(new OutputStream() {

				@Override
				public void write(byte[] b) throws IOException {
					// Just throw everything away.
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					// Just throw everything away.
				}

				@Override
				public void write(int b) throws IOException {
					// Just throw everything away.
				}
			});
		}

		if ("stdout".equals(dest)) {
			return System.out;
		}

		if ("stderr".equals(dest)) {
			return System.err;
		}

		synchronized (outputs) {
			PrintStream result = outputs.get(dest);

			if (result != null) {
				return result;
			}

			try {
				if (dest.startsWith("+")) {
					// Append if the file name starts with a +.
					result = new PrintStream(new FileOutputStream(dest.substring(1), true), true);
				} else {
					result = new PrintStream(new FileOutputStream(dest, false), true);
				}
			} catch (FileNotFoundException e) {
				System.err.println("Could not open file '" + dest + "' for output. Using stderr instead.");
				// Don't try this again.
				result = System.err;
			}

			outputs.put(dest, result);
			return result;
		}
	}

	public static void log(Arguments args, String msg) {
		getStream(args).println(msg);

		if (args.getBoolean(LOG_WITH_STACK, false)) {
			logCurrentStack(args, 1);
		}
	}

	public static void log(Arguments args, Object[] parts) {
		PrintStream stream = getStream(args);

		for (Object part : parts) {
			stream.print(part);
		}
	}

	public static void logWithFormat(Arguments args, String format, Object[] values) {
		Formatter formatter = getFormatter(args);
		formatter.format(format + "\n", values);

		if (args.getBoolean(LOG_WITH_STACK, false)) {
			logCurrentStack(args, 1);
		}
	}

	private static void logCurrentStack(Arguments args, int toSkip) {
		logWithStack(args, "", new Exception(), toSkip);
	}

	public static void logWithStack(Arguments args, String msg, int toSkip) {
		logWithStack(args, msg, new Exception(), toSkip);
	}

	public static void logWithStack(Arguments args, String msg, Exception stack, int toSkip) {
		PrintStream stream = getStream(args);

		if (msg.length() > 0) {
			stream.println(msg);
		}

		if (args.getBoolean(LOG_WITH_STACK, true)) {
			StackTraceElement[] frames = stack.getStackTrace();

			for (int i = toSkip; i < frames.length; ++i) {
				stream.println("\t" + frames[i]);
			}
		}
	}
}
