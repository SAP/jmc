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
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.openjdk.jmc.agent.sap.boot.util.ArgumentsHolder;
import org.openjdk.jmc.agent.sap.boot.util.Command;
import org.openjdk.jmc.agent.sap.boot.util.Arguments;
import org.openjdk.jmc.agent.sap.boot.util.LoggingUtils;

public class GenericLogger {

	public static final String GENERIC_COMMAND_PREFIX = "logGeneric";
	public static final int MAX_FORMATS = 5;
	public static final Command[] commands = new Command[MAX_FORMATS];

	private static final String ONCE_PER_STACK = "oncePerStack";
	private static final String FORMAT = "format";
	private static final String MAX_LONG = "maxLongValue";
	private static final String MIN_LONG = "minLongValue";
	private static final String MAX_DOUBLE = "maxDoubleValue";
	private static final String MIN_DOUBLE = "minDoubleValue";
	private static final String EQUALS = "equalsValue";
	private static final String STARTS_WITH = "valueStartsWith";
	private static final String ENDS_WITH = "valueStartsWith";
	private static final String CONTAINS = "valueContains";
	private static final String MATCHES_REGEXP = "valueMatchesRegexp";
	private static final String INSTANCEOF = "valueInstanceof";
	private static final ArrayList<ArrayList<ThreadLocal<Object>>> locals = new ArrayList<>();
	private static final int[] parameterIndices = new int[MAX_FORMATS];
	private static final String[] formats = new String[MAX_FORMATS];
	private static final HashSet<SeenStack> seenStacks = new HashSet<>();
	private static final ArgumentsHolder[] argsHolder = new ArgumentsHolder[MAX_FORMATS];

	static {
		for (int i = 0; i < MAX_FORMATS; ++i) {
			commands[i] = new Command(GENERIC_COMMAND_PREFIX + (i + 1),
					"Used to specify the logging options for generic logger " + (i + 1), FORMAT,
					"Used to specify the output of the generic logging format " + (i + 1) + ".", ONCE_PER_STACK,
					"If true we only log once per unique call stack.");
			LoggingUtils.addOptionsWithStack(commands[i]);
			addFilterOptions(commands[i]);
			argsHolder[i] = commands[i].getArguments();
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

	private static void addFilterOptions(Command cmd) {
		cmd.addOption(suffixValue(MAX_LONG, "<idx>"), "Traces only if value <idx> has the given maximum long value.");
		cmd.addOption(suffixValue(MIN_LONG, "<idx>"), "Traces only if value <idx> has the given minimum long value.");
		cmd.addOption(suffixValue(MAX_DOUBLE, "<idx>"),
				"Traces only if value <idx> has the given maximum double value.");
		cmd.addOption(suffixValue(MIN_DOUBLE, "<idx>"),
				"Traces only if value <idx> has the given minimum double value.");
		cmd.addOption(suffixValue(EQUALS, "<idx>"), "Traces only if value <idx> equals the given value.");
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
		String equals = getValueOption(EQUALS, idx);
		String startsWith = getValueOption(STARTS_WITH, idx);
		String endsWith = getValueOption(ENDS_WITH, idx);
		String contains = getValueOption(CONTAINS, idx);
		String matchesRegexp = getValueOption(MATCHES_REGEXP, idx);
		String instanceOf = getValueOption(INSTANCEOF, idx);

		if (args.hasOption(maxLong)) {
			result = addPredicate(result, new MaxLongValueFilter(args.getLong(maxLong, 0)));
		}

		if (args.hasOption(maxDouble)) {
			result = addPredicate(result, new MaxDoubleValueFilter(args.getDouble(maxDouble, 0)));
		}

		if (args.hasOption(minLong)) {
			result = addPredicate(result, new MinLongValueFilter(args.getLong(minDouble, 0)));
		}

		if (args.hasOption(minDouble)) {
			result = addPredicate(result, new MinDoubleValueFilter(args.getDouble(minDouble, 0)));
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
			result = addPredicate(result, new ContainsValueFilter(args.getString(contains, "")));
		}

		if (args.hasOption(matchesRegexp)) {
			result = addPredicate(result, new MatchesRegexpValueFilter(args.getString(matchesRegexp, "")));
		}

		if (args.hasOption(instanceOf)) {
			result = addPredicate(result, new InstanceofValueFilter(args.getString(instanceOf, "")));
		}

		return result;
	}

	private static final class MaxLongValueFilter implements Predicate<Object> {

		private final long max;

		MaxLongValueFilter(long max) {
			this.max = max;
		}

		@Override
		public boolean test(Object t) {
			if (t instanceof Number) {
				// Handle double and float separately, since we want to avoid treating 0.3 <= 0.
				if (t instanceof Double) {
					return ((Double) t).doubleValue() <= max;
				}

				if (t instanceof Float) {
					return ((Float) t).floatValue() <= max;
				}

				return ((Number) t).longValue() <= max;
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

			return false;
		}
	}

	private static final class EqualsValueFilter implements Predicate<Object> {

		private final String val;

		EqualsValueFilter(String val) {
			this.val = val;
		}

		@Override
		public boolean test(Object t) {
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
				} else if (t != null) {
					return val.equals(t.toString());
				} else {
					return val.equals("null");
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
				return instanceofImpl((Class<?>) t);
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
