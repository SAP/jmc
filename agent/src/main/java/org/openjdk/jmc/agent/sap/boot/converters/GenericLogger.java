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

import java.lang.reflect.Array;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.openjdk.jmc.agent.sap.boot.util.Arguments;
import org.openjdk.jmc.agent.sap.boot.util.ArgumentsHolder;
import org.openjdk.jmc.agent.sap.boot.util.Command;
import org.openjdk.jmc.agent.sap.boot.util.LoggingUtils;

public class GenericLogger {

	public static final String GENERIC_COMMAND_PREFIX = "logGeneric";
	public static final int MAX_FORMATS = 6;
	public static final Command[] commands = new Command[MAX_FORMATS];

	private static final int DEFAULT_MAX_PRINT_LENGTH = 256;
	private static final int DEFAULT_MAX_ARRAY_PRINT_LENGTH = 5;
	private static final String ONCE_PER_STACK = "oncePerStack";
	private static final String FORMAT = "format";
	private static final String PRINT_COLLECTION_CONTENT = "printCollectionContent";
	private static final String MAX_PRINT_LENGTH = "maxPrintLength";
	private static final String MAX_ARRAY_PRINT_LENGTH = "maxArrayPrintLength";
	private static final String MAX_LONG = "maxLongValue";
	private static final String MIN_LONG = "minLongValue";
	private static final String MAX_DOUBLE = "maxDoubleValue";
	private static final String MIN_DOUBLE = "minDoubleValue";
	private static final String MIN_LENGTH = "valueMinLength";
	private static final String MAX_LENGTH = "valueMaxLength";
	private static final String EQUALS = "valueEquals";
	private static final String STARTS_WITH = "valueStartsWith";
	private static final String ENDS_WITH = "valueEndsWith";
	private static final String CONTAINS = "valueContains";
	private static final String MATCHES_REGEXP = "valueMatchesRegexp";
	private static final String INSTANCEOF = "valueInstanceof";
	private static final String IS_TYPE = "valueIsType";
	private static final String NOT_EQUALS = "valueNotEquals";
	private static final String NOT_STARTS_WITH = "valueNotStartsWith";
	private static final String NOT_ENDS_WITH = "valueNotEndsWith";
	private static final String NOT_CONTAINS = "valueNotContains";
	private static final String NOT_MATCHES_REGEXP = "valueNotMatchesRegexp";
	private static final String NOT_INSTANCEOF = "valueNotInstanceof";
	private static final String IS_NOT_TYPE = "valueIsNotType";
	private static final ArrayList<ArrayList<ThreadLocal<Object>>> locals = new ArrayList<>();
	private static final int[] parameterIndices = new int[MAX_FORMATS];
	private static final HashSet<SeenStack> seenStacks = new HashSet<>();
	private static final ArgumentsHolder[] argsHolder = new ArgumentsHolder[MAX_FORMATS];

	static {
		for (int i = 0; i < MAX_FORMATS; ++i) {
			commands[i] = new Command(GENERIC_COMMAND_PREFIX + (i + 1),
					"Used to specify the logging options for generic logger " + (i + 1), FORMAT,
					"Used to specify the output of the generic logging format " + (i + 1) + ".", ONCE_PER_STACK,
					"If true we only log once per unique call stack.", MAX_PRINT_LENGTH,
					"The maximum number of characters to print.", MAX_ARRAY_PRINT_LENGTH,
					"The maximum number of elements in an array to print.", PRINT_COLLECTION_CONTENT,
					"If true we dump part of the contents of a supported Java collection type. "
							+ "This is disabled by default because of potential multi-threading issues.");
			LoggingUtils.addOptionsWithStack(commands[i]);
			addFilterOptions(commands[i]);
			argsHolder[i] = commands[i].getArguments();
		}
	}

	private static StringBuilder sanitize(CharSequence cs) {
		StringBuilder sb = new StringBuilder(cs.length());

		for (int i = 0; i < cs.length(); ++i) {
			char c = cs.charAt(i);

			switch (c) {
			case '\n':
				sb.append("\\n");
				break;

			case '\t':
				sb.append("\\t");
				break;

			case '"':
				sb.append("\\\"");
				break;

			case '\'':
				sb.append("\\'");
				break;

			case '\\':
				sb.append("\\\\");
				break;

			default:
				if (c < 32 || c >= 127) {
					sb.append('.');
				} else {
					sb.append(c);
				}
			}
		}

		return sb;
	}

	private static StringBuilder convertToString(int format, boolean allowInspection, Object o) {
		if (o == null) {
			return new StringBuilder("null");
		}

		int maxPrintLen = argsHolder[format].get().getInt(MAX_PRINT_LENGTH, DEFAULT_MAX_PRINT_LENGTH);
		boolean isCollection = o instanceof Collection<?> || o instanceof Map<?, ?>;

		if (o.getClass().isArray() || (isCollection && allowInspection)) {
			int maxArrayLen = argsHolder[format].get().getInt(MAX_ARRAY_PRINT_LENGTH, DEFAULT_MAX_ARRAY_PRINT_LENGTH);
			int len;
			Iterator<?> it = null;

			StringBuilder sb = new StringBuilder("{");
			boolean success = true;

			if (o.getClass().isArray()) {
				len = Array.getLength(o);
			} else if (o instanceof Map<?, ?>) {
				len = ((Map<?, ?>) o).size();
				it = ((Map<?, ?>) o).entrySet().iterator();
			} else {
				len = ((Collection<?>) o).size();
				it = ((Collection<?>) o).iterator();
			}

			for (int i = 0; i < len && i < maxArrayLen; ++i) {
				if (i > 0) {
					sb.append(", ");
				}

				if (o instanceof Collection<?>) {
					if (it.hasNext()) {
						try {
							Object v = it.next();
							sb.append(convertToString(format, allowInspection, v));
						} catch (ConcurrentModificationException | NoSuchElementException e) {
							success = false;
							break;
						}
					}
				} else if (o instanceof Map<?, ?>) {
					if (it.hasNext()) {
						try {
							Map.Entry<?, ?> v = (Map.Entry<?, ?>) it.next();
							sb.append(convertToString(format, allowInspection, v.getKey()) + ": "
									+ convertToString(format, allowInspection, v.getValue()));
						} catch (ConcurrentModificationException | NoSuchElementException e) {
							success = false;
							break;
						}
					}
				} else if (o instanceof boolean[]) {
					sb.append(Boolean.toString(((boolean[]) o)[i]));
				} else if (o instanceof byte[]) {
					sb.append(Byte.toString(((byte[]) o)[i]));
				} else if (o instanceof short[]) {
					sb.append(Short.toString(((short[]) o)[i]));
				} else if (o instanceof char[]) {
					sb.append("'" + sanitize(Character.toString(((char[]) o)[i])) + "'");
				} else if (o instanceof int[]) {
					sb.append(Integer.toString(((int[]) o)[i]));
				} else if (o instanceof long[]) {
					sb.append(Long.toString(((long[]) o)[i]));
				} else if (o instanceof float[]) {
					sb.append(Float.toString(((float[]) o)[i]));
				} else if (o instanceof double[]) {
					sb.append(Double.toString(((double[]) o)[i]));
				} else {
					Class<?> compType = o.getClass().getComponentType();
					Object oi = ((Object[]) o)[i];

					if (oi == null) {
						sb.append("null");
					} else if (oi.getClass().isArray()) {
						int depth = 0;
						compType = oi.getClass().getComponentType();

						while (compType.isArray()) {
							depth += 1;
							compType = compType.getComponentType();
						}

						sb.append(compType.getName() + "[" + Array.getLength(oi) + "]");

						for (int j = 0; j < depth; ++j) {
							sb.append("[]");
						}
					} else {
						sb.append(convertToString(format, allowInspection, ((Object[]) o)[i]));
					}
				}
			}

			if (len > maxArrayLen) {
				sb.append(", " + (len - maxArrayLen) + " skipped ...");
			}

			sb.append("}");

			if (success) {
				return sb;
			}
		}

		StringBuilder sb = new StringBuilder();

		if (isCollection) {
			if (o instanceof Collection<?>) {
				sb.append(o.getClass().getName() + "(size " + ((Collection<?>) o).size() + ")");

				return sb;
			}

			if (o instanceof Map<?, ?>) {
				sb.append(o.getClass().getName() + "(size " + ((Map<?, ?>) o).size() + ")");

				return sb;
			}
		}

		if (o instanceof CharSequence) {
			StringBuilder cs = sanitize(new StringBuilder((CharSequence) o));
			sb = new StringBuilder(cs.length() + 2);
			sb.append('"');
			sb.append(cs);
			sb.append('"');
		} else {
			sb = new StringBuilder(sanitize(o.toString()));

			if (sb.length() > maxPrintLen) {
				return new StringBuilder(sb.substring(0, maxPrintLen));
			}
		}

		return sb;
	}

	@SuppressWarnings("deprecation")
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
			Arguments args = argsHolder[index].get();

			if (args.getBoolean(ONCE_PER_STACK, false)) {
				SeenStack stack = new SeenStack();

				synchronized (GenericLogger.class) {
					if (seenStacks.contains(stack)) {
						return;
					}

					seenStacks.add(stack);
				}
			}

			Object[] values = new Object[paramenterIndex + 1];

			for (int i = 0; i <= paramenterIndex; ++i) {
				values[i] = params.get(i).get();

				// Make sure we have a standard format for dates.
				if (values[i] instanceof Date) {
					values[i] = ((Date) values[i]).toGMTString();
				}

				params.get(i).remove();
			}

			@SuppressWarnings("unchecked")
			ArrayList<Predicate<Object>> filters = (ArrayList<Predicate<Object>>) args.getCustomData();

			if (filters == null) {
				filters = new ArrayList<>(values.length);

				for (int i = 0; i < values.length; ++i) {
					filters.add(getFilter(args, i + 1));
				}

				args.setCustomData(filters);
			}

			for (int i = 0; i < values.length; ++i) {
				Predicate<Object> filter = filters.get(i);

				if ((filter != null) && !filter.test(values[i])) {
					return;
				}
			}

			StringBuilder sb = new StringBuilder("Values for generic logger " + (index + 1) + ":");

			for (Object o : values) {
				sb.append(' ');
				sb.append(convertToString(index, args.getBoolean(PRINT_COLLECTION_CONTENT, false), o));
			}

			LoggingUtils.log(args, sb.toString());
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

	public static boolean logFormat4(boolean v) {
		log(v, 3, false);
		return v;
	}

	public static byte logFormat4(byte v) {
		log(v, 3, false);
		return v;
	}

	public static short logFormat4(short v) {
		log(v, 3, false);
		return v;
	}

	public static char logFormat4(char v) {
		log(v, 3, false);
		return v;
	}

	public static int logFormat4(int v) {
		log(v, 3, false);
		return v;
	}

	public static long logFormat4(long v) {
		log(v, 3, false);
		return v;
	}

	public static float logFormat4(float v) {
		log(v, 3, false);
		return v;
	}

	public static double logFormat4(double v) {
		log(v, 3, false);
		return v;
	}

	public static String logFormat4(Object v) {
		log(v, 3, false);
		return stringify(v);
	}

	public static boolean logLastFormat4(boolean v) {
		log(v, 3, true);
		return v;
	}

	public static byte logLastFormat4(byte v) {
		log(v, 3, true);
		return v;
	}

	public static short logLastFormat4(short v) {
		log(v, 3, true);
		return v;
	}

	public static char logLastFormat4(char v) {
		log(v, 3, true);
		return v;
	}

	public static int logLastFormat4(int v) {
		log(v, 3, true);
		return v;
	}

	public static long logLastFormat4(long v) {
		log(v, 3, true);
		return v;
	}

	public static float logLastFormat4(float v) {
		log(v, 3, true);
		return v;
	}

	public static double logLastFormat4(double v) {
		log(v, 3, true);
		return v;
	}

	public static String logLastFormat4(Object v) {
		log(v, 3, true);
		return stringify(v);
	}

	public static boolean logFormat5(boolean v) {
		log(v, 4, false);
		return v;
	}

	public static byte logFormat5(byte v) {
		log(v, 4, false);
		return v;
	}

	public static short logFormat5(short v) {
		log(v, 4, false);
		return v;
	}

	public static char logFormat5(char v) {
		log(v, 4, false);
		return v;
	}

	public static int logFormat5(int v) {
		log(v, 4, false);
		return v;
	}

	public static long logFormat5(long v) {
		log(v, 4, false);
		return v;
	}

	public static float logFormat5(float v) {
		log(v, 4, false);
		return v;
	}

	public static double logFormat5(double v) {
		log(v, 4, false);
		return v;
	}

	public static String logFormat5(Object v) {
		log(v, 4, false);
		return stringify(v);
	}

	public static boolean logLastFormat5(boolean v) {
		log(v, 4, true);
		return v;
	}

	public static byte logLastFormat5(byte v) {
		log(v, 4, true);
		return v;
	}

	public static short logLastFormat5(short v) {
		log(v, 4, true);
		return v;
	}

	public static char logLastFormat5(char v) {
		log(v, 4, true);
		return v;
	}

	public static int logLastFormat5(int v) {
		log(v, 4, true);
		return v;
	}

	public static long logLastFormat5(long v) {
		log(v, 4, true);
		return v;
	}

	public static float logLastFormat5(float v) {
		log(v, 4, true);
		return v;
	}

	public static double logLastFormat5(double v) {
		log(v, 4, true);
		return v;
	}

	public static String logLastFormat5(Object v) {
		log(v, 4, true);
		return stringify(v);
	}

	public static boolean logFormat6(boolean v) {
		log(v, 5, false);
		return v;
	}

	public static byte logFormat6(byte v) {
		log(v, 5, false);
		return v;
	}

	public static short logFormat6(short v) {
		log(v, 5, false);
		return v;
	}

	public static char logFormat6(char v) {
		log(v, 5, false);
		return v;
	}

	public static int logFormat6(int v) {
		log(v, 5, false);
		return v;
	}

	public static long logFormat6(long v) {
		log(v, 5, false);
		return v;
	}

	public static float logFormat6(float v) {
		log(v, 5, false);
		return v;
	}

	public static double logFormat6(double v) {
		log(v, 5, false);
		return v;
	}

	public static String logFormat6(Object v) {
		log(v, 5, false);
		return stringify(v);
	}

	public static boolean logLastFormat6(boolean v) {
		log(v, 5, true);
		return v;
	}

	public static byte logLastFormat6(byte v) {
		log(v, 5, true);
		return v;
	}

	public static short logLastFormat6(short v) {
		log(v, 5, true);
		return v;
	}

	public static char logLastFormat6(char v) {
		log(v, 5, true);
		return v;
	}

	public static int logLastFormat6(int v) {
		log(v, 5, true);
		return v;
	}

	public static long logLastFormat6(long v) {
		log(v, 5, true);
		return v;
	}

	public static float logLastFormat6(float v) {
		log(v, 5, true);
		return v;
	}

	public static double logLastFormat6(double v) {
		log(v, 5, true);
		return v;
	}

	public static String logLastFormat6(Object v) {
		log(v, 5, true);
		return stringify(v);
	}

	private static void addFilterOptions(Command cmd) {
		cmd.addOption(suffixValue(MAX_LONG, "<idx>"), "Traces only if value <idx> has the given maximum long value.");
		cmd.addOption(suffixValue(MIN_LONG, "<idx>"), "Traces only if value <idx> has the given minimum long value.");
		cmd.addOption(suffixValue(MAX_DOUBLE, "<idx>"),
				"Traces only if value <idx> has the given maximum double value.");
		cmd.addOption(suffixValue(MIN_DOUBLE, "<idx>"),
				"Traces only if value <idx> has the given minimum double value.");
		cmd.addOption(suffixValue(MIN_LENGTH, "<idx>"),
				"Traces only if array or similar type has the given minimum length.");
		cmd.addOption(suffixValue(MAX_LENGTH, "<idx>"),
				"Traces only if array or similar type has the given minimum length.");
		cmd.addOption(suffixValue(EQUALS, "<idx>"), "Traces only if value <idx> equals the given value.");
		cmd.addOption(suffixValue(CONTAINS, "<idx>"),
				"Traces only if the string representation of value <idx> contains the given string.");
		cmd.addOption(suffixValue(STARTS_WITH, "<idx>"),
				"Traces only if the string representation of value <idx> starts with the given string.");
		cmd.addOption(suffixValue(ENDS_WITH, "<idx>"),
				"Traces only if the string representation of value <idx> ends with the given string.");
		cmd.addOption(suffixValue(MATCHES_REGEXP, "<idx>"),
				"Traces only if the string representation of value <idx> matches the given regexp.");
		cmd.addOption(suffixValue(INSTANCEOF, "<idx>"),
				"Traces only if value <idx> is a class and is an instance of the given type.");
		cmd.addOption(suffixValue(IS_TYPE, "<idx>"),
				"Traces only if the value <idx> is of the given type (null, array, primitive_array or object_array).");
		cmd.addOption(suffixValue(NOT_EQUALS, "<idx>"), "Traces only if value <idx> does NOT equals the given value.");
		cmd.addOption(suffixValue(NOT_CONTAINS, "<idx>"),
				"Traces only if the string representation of value <idx> does NOT contains the given string.");
		cmd.addOption(suffixValue(NOT_STARTS_WITH, "<idx>"),
				"Traces only if the string representation of value <idx> does NOT starts with the given string.");
		cmd.addOption(suffixValue(NOT_ENDS_WITH, "<idx>"),
				"Traces only if the string representation of value <idx> does NOT ends with the given string.");
		cmd.addOption(suffixValue(NOT_MATCHES_REGEXP, "<idx>"),
				"Traces only if the string representation of value <idx> does NOT matches the given regexp.");
		cmd.addOption(suffixValue(NOT_INSTANCEOF, "<idx>"),
				"Traces only if value <idx> is a class and is NOT an instance of the given type.");
		cmd.addOption(suffixValue(IS_NOT_TYPE, "<idx>"),
				"Traces only if the value <idx> is not of the given type (null, array, primitive_array or object_array).");
	}

	private static String suffixValue(String option, String suffix) {
		if (option.startsWith("value")) {
			return "value" + suffix + option.substring(5);
		}

		int pos = option.indexOf("Value");

		return option.substring(0, pos) + "Value" + suffix + option.substring(pos + 5);
	}

	private static String getValueOption(String option, int idx) {
		return suffixValue(option, Integer.toString(idx));
	}

	private static Predicate<Object> addPredicate(Predicate<Object> predicate, Predicate<Object> toAdd) {
		if (predicate == null) {
			return toAdd;
		} else if (toAdd == null) {
			return predicate;
		} else {
			return predicate.and(toAdd);
		}
	}

	private static Predicate<Object> getFilter(Arguments args, int idx) {
		Predicate<Object> result = null;
		String maxLong = getValueOption(MAX_LONG, idx);
		String maxDouble = getValueOption(MAX_DOUBLE, idx);
		String minLong = getValueOption(MIN_LONG, idx);
		String minDouble = getValueOption(MIN_DOUBLE, idx);
		String minLength = getValueOption(MIN_LENGTH, idx);
		String maxLength = getValueOption(MAX_LENGTH, idx);
		String equals = getValueOption(EQUALS, idx);
		String startsWith = getValueOption(STARTS_WITH, idx);
		String endsWith = getValueOption(ENDS_WITH, idx);
		String contains = getValueOption(CONTAINS, idx);
		String matchesRegexp = getValueOption(MATCHES_REGEXP, idx);
		String instanceOf = getValueOption(INSTANCEOF, idx);
		String isType = getValueOption(IS_TYPE, idx);
		String notEquals = getValueOption(NOT_EQUALS, idx);
		String notStartsWith = getValueOption(NOT_STARTS_WITH, idx);
		String notEndsWith = getValueOption(NOT_ENDS_WITH, idx);
		String notContains = getValueOption(NOT_CONTAINS, idx);
		String notMatchesRegexp = getValueOption(NOT_MATCHES_REGEXP, idx);
		String notInstanceOf = getValueOption(NOT_INSTANCEOF, idx);
		String isNotType = getValueOption(IS_NOT_TYPE, idx);

		if (args.hasOption(maxLong)) {
			result = addPredicate(result, new MaxLongValueFilter(args.getLong(maxLong, 0)));
		}

		if (args.hasOption(maxDouble)) {
			result = addPredicate(result, new MaxDoubleValueFilter(args.getDouble(maxDouble, 0)));
		}

		if (args.hasOption(minLong)) {
			result = addPredicate(result, new MinLongValueFilter(args.getLong(minLong, 0)));
		}

		if (args.hasOption(minDouble)) {
			result = addPredicate(result, new MinDoubleValueFilter(args.getDouble(minDouble, 0)));
		}

		if (args.hasOption(minLength)) {
			result = addPredicate(result, new MinLengthFilter(args.getInt(minLength, 0)));
		}

		if (args.hasOption(maxLength)) {
			result = addPredicate(result, new MaxLengthFilter(args.getInt(maxLength, 0)));
		}

		if (args.hasOption(equals)) {
			result = addPredicate(result, new EqualsValueFilter(args.getString(equals, "")));
		}

		if (args.hasOption(startsWith)) {
			result = addPredicate(result, new StartsWithValueFilter(args.getString(startsWith, "")));
		}

		if (args.hasOption(endsWith)) {
			result = addPredicate(result, new EndsWithValueFilter(args.getString(endsWith, "")));
		}

		if (args.hasOption(contains)) {
			for (String str : args.getStrings(contains)) {
				result = addPredicate(result, new ContainsValueFilter(str));
			}
		}

		if (args.hasOption(matchesRegexp)) {
			for (String str : args.getStrings(matchesRegexp)) {
				result = addPredicate(result, new MatchesRegexpValueFilter(str));
			}
		}

		if (args.hasOption(instanceOf)) {
			for (String str : args.getStrings(instanceOf)) {
				result = addPredicate(result, new InstanceofValueFilter(str));
			}
		}

		if (args.hasOption(isType)) {
			for (String str : args.getStrings(isType)) {
				result = addPredicate(result, new IsTypeFilter(str));
			}
		}

		if (args.hasOption(notEquals)) {
			for (String str : args.getStrings(notEquals)) {
				result = addPredicate(result, new EqualsValueFilter(str).negate());
			}
		}

		if (args.hasOption(notStartsWith)) {
			for (String str : args.getStrings(notStartsWith)) {
				result = addPredicate(result, new StartsWithValueFilter(str).negate());
			}
		}

		if (args.hasOption(notEndsWith)) {
			for (String str : args.getStrings(notEndsWith)) {
				result = addPredicate(result, new EndsWithValueFilter(str).negate());
			}
		}

		if (args.hasOption(notContains)) {
			for (String str : args.getStrings(notContains)) {
				result = addPredicate(result, new ContainsValueFilter(str).negate());
			}
		}

		if (args.hasOption(notMatchesRegexp)) {
			for (String str : args.getStrings(notMatchesRegexp)) {
				result = addPredicate(result, new MatchesRegexpValueFilter(str).negate());
			}
		}

		if (args.hasOption(notInstanceOf)) {
			for (String str : args.getStrings(notInstanceOf)) {
				result = addPredicate(result, new InstanceofValueFilter(str).negate());
			}
		}

		if (args.hasOption(isNotType)) {
			for (String str : args.getStrings(isNotType)) {
				result = addPredicate(result, new IsTypeFilter(str).negate());
			}
		}

		return result;
	}

	private static final class IsTypeFilter implements Predicate<Object> {
		private final String type;

		public IsTypeFilter(String type) {
			this.type = type;
		}

		@Override
		public boolean test(Object t) {
			if (t == null) {
				return "null".equals(type);
			}

			if (t.getClass().isArray()) {
				if ("array".equals(type)) {
					return true;
				}

				if (t.getClass().getComponentType().isPrimitive()) {
					return "primitive_array".equals(type);
				}

				return "object_array".equals(type);
			}

			return false;
		}
	}

	private static final class MaxLongValueFilter implements Predicate<Object> {

		private final long max;

		MaxLongValueFilter(long max) {
			this.max = max;
		}

		@Override
		public boolean test(Object t) {
			// Handle double and float separately, since we want to avoid treating 0.3 <= 0.
			if (t instanceof Double) {
				return ((Double) t).doubleValue() <= max;
			}

			if (t instanceof Float) {
				return ((Float) t).floatValue() <= max;
			}

			if (t instanceof Number) {
				return ((Number) t).longValue() <= max;
			}

			if (t instanceof Character) {
				return ((Character) t).charValue() <= max;
			}

			return false;
		}
	}

	private static final class MaxDoubleValueFilter implements Predicate<Object> {

		private final double max;

		MaxDoubleValueFilter(double max) {
			this.max = max;
		}

		@Override
		public boolean test(Object t) {
			if (t instanceof Number) {
				return ((Number) t).doubleValue() <= max;
			}

			if (t instanceof Character) {
				return ((Character) t).charValue() <= max;
			}

			return false;
		}
	}

	private static final class MinLongValueFilter implements Predicate<Object> {

		private final long min;

		MinLongValueFilter(long min) {
			this.min = min;
		}

		@Override
		public boolean test(Object t) {
			// Handle double and float separately, since we want to avoid treating -0.3 >= 0.
			if (t instanceof Double) {
				return ((Double) t).doubleValue() >= min;
			}

			if (t instanceof Float) {
				return ((Float) t).floatValue() >= min;
			}

			if (t instanceof Number) {
				return ((Number) t).longValue() >= min;
			}

			if (t instanceof Character) {
				return ((Character) t).charValue() >= min;
			}

			return false;
		}
	}

	private static final class MinDoubleValueFilter implements Predicate<Object> {

		private final double min;

		MinDoubleValueFilter(double min) {
			this.min = min;
		}

		@Override
		public boolean test(Object t) {
			if (t instanceof Number) {
				return ((Number) t).doubleValue() >= min;
			}

			if (t instanceof Character) {
				return ((Character) t).charValue() >= min;
			}

			return false;
		}
	}

	private static int getLength(Object o) {
		if (o == null) {
			return -1;
		}

		if (o.getClass().isArray()) {
			return Array.getLength(o);
		}

		// Support some of the common types with lengths.
		if (o instanceof CharSequence) {
			return ((CharSequence) o).length();
		}

		if (o instanceof Collection<?>) {
			return ((Collection<?>) o).size();
		}

		if (o instanceof Map<?, ?>) {
			return ((Map<?, ?>) o).size();
		}

		return -1;
	}

	private static final class MinLengthFilter implements Predicate<Object> {

		private final int min;

		MinLengthFilter(int min) {
			this.min = min;
		}

		@Override
		public boolean test(Object t) {
			int len = getLength(t);

			if (len < 0) {
				return false;
			}

			return len >= min;
		}
	}

	private static final class MaxLengthFilter implements Predicate<Object> {

		private final int max;

		MaxLengthFilter(int max) {
			this.max = max;
		}

		@Override
		public boolean test(Object t) {
			int len = getLength(t);

			if (len < 0) {
				return false;
			}

			return len <= max;
		}
	}

	private static final class EqualsValueFilter implements Predicate<Object> {

		private final String val;

		EqualsValueFilter(String val) {
			this.val = val;
		}

		@Override
		public boolean test(Object t) {
			if (t == null) {
				return false;
			}

			try {
				if (t instanceof Number) {
					if ((t instanceof Double) || (t instanceof Float)) {
						return Double.parseDouble(val) == ((Number) t).doubleValue();
					} else {
						return Long.parseLong(val) == ((Number) t).longValue();
					}
				} else if (t instanceof CharSequence) {
					return val.equals((CharSequence) t);
				} else if (t instanceof Class) {
					return val.equals(((Class<?>) t).getName());
				} else {
					return val.equals(t.toString());
				}
			} catch (NumberFormatException e) {
				// Ignore and return false.
			}

			return false;
		}
	}

	private static final class StartsWithValueFilter implements Predicate<Object> {

		private final String prefix;

		StartsWithValueFilter(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public boolean test(Object t) {
			if (t != null) {
				if (t instanceof Class) {
					return ((Class<?>) t).getName().startsWith(prefix);
				}

				if (t instanceof CharBuffer) {
					return t.toString().startsWith(prefix);
				}

				return t.toString().startsWith(prefix);
			}

			return false;
		}
	}

	private static final class EndsWithValueFilter implements Predicate<Object> {

		private final String suffix;

		EndsWithValueFilter(String suffix) {
			this.suffix = suffix;
		}

		@Override
		public boolean test(Object t) {
			if (t != null) {
				if (t instanceof Class) {
					return ((Class<?>) t).getName().endsWith(suffix);
				}

				return t.toString().endsWith(suffix);
			}

			return false;
		}
	}

	private static final class ContainsValueFilter implements Predicate<Object> {

		private final String part;

		ContainsValueFilter(String part) {
			this.part = part;
		}

		@Override
		public boolean test(Object t) {
			if (t != null) {
				if (t instanceof Class) {
					return ((Class<?>) t).getName().contains(part);
				}

				return t.toString().contains(part);
			}

			return false;
		}
	}

	private static final class MatchesRegexpValueFilter implements Predicate<Object> {

		private final Pattern pattern;

		MatchesRegexpValueFilter(String pattern) {
			this.pattern = Pattern.compile(pattern);
		}

		@Override
		public boolean test(Object t) {
			if (t != null) {
				if (t instanceof Class) {
					return pattern.matcher(((Class<?>) t).getName()).find();
				} else if (t instanceof CharSequence) {
					return pattern.matcher((CharSequence) t).find();
				}

				return pattern.matcher(t.toString()).find();
			}

			return false;
		}
	}

	private static final class InstanceofValueFilter implements Predicate<Object> {

		private final String clazz;

		InstanceofValueFilter(String clazz) {
			this.clazz = clazz;
		}

		private boolean instanceofImpl(Class<?> base) {
			if (base == null) {
				return false;
			}

			if (base.getName().equals(clazz)) {
				return true;
			}

			if (instanceofImpl(base.getSuperclass())) {
				return true;
			}

			for (Class<?> interf : base.getInterfaces()) {
				if (instanceofImpl(interf)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public boolean test(Object t) {
			if (t instanceof Class) {
				if ("java.lang.Class".equals(clazz)) {
					return true;
				}

				return instanceofImpl((Class<?>) t);
			} else if (t != null) {
				return instanceofImpl(t.getClass());
			}

			return false;
		}
	}

	private static class SeenStack {
		private final int hashCode;
		private final StackTraceElement[] stack;

		public SeenStack() {
			stack = new Throwable().fillInStackTrace().getStackTrace();
			hashCode = Arrays.hashCode(stack);
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof SeenStack) {
				SeenStack otherStack = (SeenStack) other;

				if (otherStack.hashCode != hashCode) {
					return false;
				}

				return Arrays.equals(stack, otherStack.stack);
			}

			return false;
		}
	}
}
