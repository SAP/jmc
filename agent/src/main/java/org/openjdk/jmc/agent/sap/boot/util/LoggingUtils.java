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
		command.addOption(LOG_WITH_STACK, "If not false, print a stack trace for every log output.");
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
