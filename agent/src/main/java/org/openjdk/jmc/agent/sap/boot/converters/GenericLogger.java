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

package org.openjdk.jmc.agent.sap.boot.converters;

import java.util.ArrayList;

import org.openjdk.jmc.agent.sap.boot.util.Command;
import org.openjdk.jmc.agent.sap.boot.util.CommandArguments;
import org.openjdk.jmc.agent.sap.boot.util.LoggingUtils;

public class GenericLogger {

	public static final int MAX_FORMATS = 3;
	public static final String FORMAT = "format";
	public static final String GENERIC_COMMAND_PREFIX = "logGeneric";
	private static final ArrayList<ArrayList<ThreadLocal<Object>>> locals = new ArrayList<>();
	private static final int[] parameterIndices = new int[MAX_FORMATS];
	private static final String[] formats = new String[MAX_FORMATS];
	public static final Command[] commands = new Command[MAX_FORMATS];

	static {
		for (int i = 0; i < MAX_FORMATS; ++i) {
			commands[i] = new Command(GENERIC_COMMAND_PREFIX + (i + 1), "Generic logging format " + (i + 1), FORMAT,
					"Used to specify the output of the generic logging format " + (i + 1) + ".");
			LoggingUtils.addOptionsWithStack(commands[i]);
		}
	}

	private static void logValue(Object value, int index, boolean isLast) {
		int paramenterIndex;
		ArrayList<ThreadLocal<Object>> params;

		synchronized (GenericLogger.class) {
			while (index >= locals.size()) {
				locals.add(new ArrayList<>());
			}

			params = locals.get(index);
			paramenterIndex = parameterIndices[index]++;

			if (isLast) {
				parameterIndices[index] = 0;
			}

			while (paramenterIndex >= params.size()) {
				params.add(new ThreadLocal<Object>());
			}
		}

		params.get(paramenterIndex).set(value);

		if (isLast) {
			CommandArguments args = new CommandArguments(commands[index]);

			String format;
			Object[] values = new Object[paramenterIndex + 1];

			for (int i = 0; i <= paramenterIndex; ++i) {
				values[i] = params.get(i).get();
				params.get(i).remove();
			}

			synchronized (formats) {
				format = formats[index];

				if (format == null) {
					format = args.getString("format", null);

					if (format == null) {
						format = "Values for generic logger " + (index + 1) + ":";

						for (int i = 0; i < values.length; ++i) {
							format += " %" + (i + 1) + "$s";
						}
					}

					formats[index] = format;
				}
			}

			LoggingUtils.logWithFormat(args, format + "\n", values);
		}
	}

	private static void log(boolean v, int index, boolean isLast) {
		logValue(Boolean.valueOf(v), index, isLast);
	}

	private static void log(byte v, int index, boolean isLast) {
		logValue(Byte.valueOf(v), index, isLast);
	}

	private static void log(short v, int index, boolean isLast) {
		logValue(Short.valueOf(v), index, isLast);
	}

	private static void log(char v, int index, boolean isLast) {
		logValue(Character.valueOf(v), index, isLast);
	}

	private static void log(int v, int index, boolean isLast) {
		logValue(Integer.valueOf(v), index, isLast);
	}

	private static void log(long v, int index, boolean isLast) {
		logValue(Long.valueOf(v), index, isLast);
	}

	private static void log(float v, int index, boolean isLast) {
		logValue(Float.valueOf(v), index, isLast);
	}

	private static void log(double v, int index, boolean isLast) {
		logValue(Double.valueOf(v), index, isLast);
	}

	private static void log(Object v, int index, boolean isLast) {
		logValue(v, index, isLast);
	}

	private static String stringify(Object v) {
		return v == null ? "null" : v.toString();
	}

	public static boolean logFormat1(boolean v) {
		log(v, 0, false);
		return v;
	}

	public static byte logFormat1(byte v) {
		log(v, 0, false);
		return v;
	}

	public static short logFormat1(short v) {
		log(v, 0, false);
		return v;
	}

	public static char logFormat1(char v) {
		log(v, 0, false);
		return v;
	}

	public static int logFormat1(int v) {
		log(v, 0, false);
		return v;
	}

	public static long logFormat1(long v) {
		log(v, 0, false);
		return v;
	}

	public static float logFormat1(float v) {
		log(v, 0, false);
		return v;
	}

	public static double logFormat1(double v) {
		log(v, 0, false);
		return v;
	}

	public static String logFormat1(Object v) {
		log(v, 0, false);
		return stringify(v);
	}

	public static boolean logLastFormat1(boolean v) {
		log(v, 0, true);
		return v;
	}

	public static byte logLastFormat1(byte v) {
		log(v, 0, true);
		return v;
	}

	public static short logLastFormat1(short v) {
		log(v, 0, true);
		return v;
	}

	public static char logLastFormat1(char v) {
		log(v, 0, true);
		return v;
	}

	public static int logLastFormat1(int v) {
		log(v, 0, true);
		return v;
	}

	public static long logLastFormat1(long v) {
		log(v, 0, true);
		return v;
	}

	public static float logLastFormat1(float v) {
		log(v, 0, true);
		return v;
	}

	public static double logLastFormat1(double v) {
		log(v, 0, true);
		return v;
	}

	public static String logLastFormat1(Object v) {
		log(v, 0, true);
		return stringify(v);
	}

	public static boolean logFormat2(boolean v) {
		log(v, 1, false);
		return v;
	}

	public static byte logFormat2(byte v) {
		log(v, 1, false);
		return v;
	}

	public static short logFormat2(short v) {
		log(v, 1, false);
		return v;
	}

	public static char logFormat2(char v) {
		log(v, 1, false);
		return v;
	}

	public static int logFormat2(int v) {
		log(v, 1, false);
		return v;
	}

	public static long logFormat2(long v) {
		log(v, 1, false);
		return v;
	}

	public static float logFormat2(float v) {
		log(v, 1, false);
		return v;
	}

	public static double logFormat2(double v) {
		log(v, 1, false);
		return v;
	}

	public static String logFormat2(Object v) {
		log(v, 1, false);
		return stringify(v);
	}

	public static boolean logLastFormat2(boolean v) {
		log(v, 1, true);
		return v;
	}

	public static byte logLastFormat2(byte v) {
		log(v, 1, true);
		return v;
	}

	public static short logLastFormat2(short v) {
		log(v, 1, true);
		return v;
	}

	public static char logLastFormat2(char v) {
		log(v, 1, true);
		return v;
	}

	public static int logLastFormat2(int v) {
		log(v, 1, true);
		return v;
	}

	public static long logLastFormat2(long v) {
		log(v, 1, true);
		return v;
	}

	public static float logLastFormat2(float v) {
		log(v, 1, true);
		return v;
	}

	public static double logLastFormat2(double v) {
		log(v, 1, true);
		return v;
	}

	public static String logLastFormat2(Object v) {
		log(v, 1, true);
		return stringify(v);
	}

	public static boolean logFormat3(boolean v) {
		log(v, 2, false);
		return v;
	}

	public static byte logFormat3(byte v) {
		log(v, 2, false);
		return v;
	}

	public static short logFormat3(short v) {
		log(v, 2, false);
		return v;
	}

	public static char logFormat3(char v) {
		log(v, 2, false);
		return v;
	}

	public static int logFormat3(int v) {
		log(v, 2, false);
		return v;
	}

	public static long logFormat3(long v) {
		log(v, 2, false);
		return v;
	}

	public static float logFormat3(float v) {
		log(v, 2, false);
		return v;
	}

	public static double logFormat3(double v) {
		log(v, 2, false);
		return v;
	}

	public static String logFormat3(Object v) {
		log(v, 2, false);
		return stringify(v);
	}

	public static boolean logLastFormat3(boolean v) {
		log(v, 2, true);
		return v;
	}

	public static byte logLastFormat3(byte v) {
		log(v, 2, true);
		return v;
	}

	public static short logLastFormat3(short v) {
		log(v, 2, true);
		return v;
	}

	public static char logLastFormat3(char v) {
		log(v, 2, true);
		return v;
	}

	public static int logLastFormat3(int v) {
		log(v, 2, true);
		return v;
	}

	public static long logLastFormat3(long v) {
		log(v, 2, true);
		return v;
	}

	public static float logLastFormat3(float v) {
		log(v, 2, true);
		return v;
	}

	public static double logLastFormat3(double v) {
		log(v, 2, true);
		return v;
	}

	public static String logLastFormat3(Object v) {
		log(v, 2, true);
		return stringify(v);
	}
}
