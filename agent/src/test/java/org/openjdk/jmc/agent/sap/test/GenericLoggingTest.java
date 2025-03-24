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
import java.io.InputStream;
import java.io.Serializable;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class GenericLoggingTest extends TestBase {

	public static void main(String[] args) {
		new GenericLoggingTest().dispatch(args);
	}

	public static void genericLogging4() {
		Object[][] oa1 = new Object[][] {new Object[1], new Serializable[2][], new String[3]};
		String[] sa = new String[] {"just", "a", "test", null, "with", "many", "strings", "!"};
		boolean[] za = new boolean[] {true, false, true, false, true};
		boolean[] za1 = new boolean[] {true};
		HashMap<String, Object> map1 = new HashMap<>();
		map1.put("test", new C1());
		LinkedHashMap<String, Object> map2 = new LinkedHashMap<>(); // Must be linked to stable order.
		map2.put("test1", new C1());
		map2.put("test2", new C2());
		map2.put("test3", new C3());
		map2.put(null, new C1());
		map2.put("test5", new C2());
		map2.put("test6", new C3());
		DummyList<Object> dl1 = new DummyList<Object>(map2.values(), true);
		DummyList<Object> dl2 = new DummyList<Object>(map2.values(), false);
		LinkedHashMap<String, Object> map3 = new LinkedHashMap<>();
		map3.put("map1", map1);
		map3.put("map2", map2);
		Object[] oa2 = new Object[] {oa1, sa, map1, map3, za, za1};

		// The calls we expect to be logged
		traceObjects1(null, new byte[3], new byte[4][], new Integer[10], new C1(), new C1(), new C3(), "Hehe!");
		tracePrimitives1(false, (byte) -1, (short) -7, 'C', 128, -7, 0.2f, -2.2);
		tracePrimitives2('C', (byte) 1, (short) 7, 'C', -2, 7, -0.2f, 2.2);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -222, 77132, -0.22387f, 2.1332);
		traceObjects1(null, new byte[3], new byte[4][], new Integer[10], C1.class, new C1(), new C3(), "Hehe!");
		traceObjects1(null, new byte[3], new byte[4][], new Integer[10], C1.class, new C1(), new C3(),
				new StringBuilder("Hehe!"));
		traceObjects1(null, new byte[3], new byte[4][], new Integer[10], C1.class, new C1(), new C3(),
				new StringBuffer("Hehe!"));
		traceObjects1(null, new byte[3], new byte[4][], new Integer[10], C1.class, new C1(), new C3(),
				CharBuffer.allocate(5).append("Hele!").position(0));
		traceObjects2(map1, map2, za, null, dl1, dl2, map3, oa2);
		traceObjects3(dl2, sa, za, map1, map2, oa1, Class.class, new StringBuilder("Jeje"));

		// The calls we expect to be filtered out.
		tracePrimitives1(true, (byte) -1, (short) -7, 'C', 128, -7, 0.2f, -2.2);
		tracePrimitives1(false, (byte) -2, (short) -7, 'C', 128, -7, 0.2f, -2.2);
		tracePrimitives1(false, (byte) 3, (short) -7, 'C', 128, -7, 0.2f, -2.2);
		tracePrimitives1(false, (byte) -1, (short) -8, 'C', 128, -7, 0.2f, -2.2);
		tracePrimitives1(false, (byte) -1, (short) 2, 'C', 128, -7, 0.2f, -2.2);
		tracePrimitives1(false, (byte) -1, (short) -7, 'B', 128, -7, 0.2f, -2.2);
		tracePrimitives1(false, (byte) -1, (short) -7, 'D', 128, -7, 0.2f, -2.2);
		tracePrimitives1(false, (byte) -1, (short) -7, 'C', 127, -7, 0.2f, -2.2);
		tracePrimitives1(false, (byte) -1, (short) -7, 'C', 129, -7, 0.2f, -2.2);
		tracePrimitives1(false, (byte) -1, (short) -7, 'C', 128, -8, 0.2f, -2.2);
		tracePrimitives1(false, (byte) -1, (short) -7, 'C', 128, -6, 0.2f, -2.2);
		tracePrimitives1(false, (byte) -1, (short) -7, 'C', 128, -7, 0.1f, -2.2);
		tracePrimitives1(false, (byte) -1, (short) -7, 'C', 128, -7, 0.3f, -2.2);
		tracePrimitives2('D', (byte) 1, (short) 7, 'C', -2, 7, -0.2f, 2.2);
		tracePrimitives2('C', (byte) 0, (short) 7, 'C', -2, 7, -0.2f, 2.2);
		tracePrimitives2('C', (byte) 2, (short) 7, 'C', -2, 7, -0.2f, 2.2);
		tracePrimitives2('C', (byte) 1, (short) 6, 'C', -2, 7, -0.2f, 2.2);
		tracePrimitives2('C', (byte) 1, (short) 8, 'C', -2, 7, -0.2f, 2.2);
		tracePrimitives2('C', (byte) 1, (short) 7, 'B', -2, 7, -0.2f, 2.2);
		tracePrimitives2('C', (byte) 1, (short) 7, 'D', -2, 7, -0.2f, 2.2);
		tracePrimitives2('C', (byte) 1, (short) 7, 'C', -4, 7, -0.2f, 2.2);
		tracePrimitives2('C', (byte) 1, (short) 7, 'C', 0, 7, -0.2f, 2.2);
		tracePrimitives2('C', (byte) 1, (short) 7, 'C', -2, 6, -0.2f, 2.2);
		tracePrimitives2('C', (byte) 1, (short) 7, 'C', -2, 8, -0.2f, 2.2);
		tracePrimitives2('C', (byte) 1, (short) 7, 'C', -2, 7, -1.2f, 2.2);
		tracePrimitives2('C', (byte) 1, (short) 7, 'C', -2, 7, 0.2f, 2.2);
		tracePrimitives2('C', (byte) 1, (short) 7, 'C', -2, 7, -0.2f, 2.1);
		tracePrimitives2('C', (byte) 1, (short) 7, 'C', -2, 7, -0.2f, 2.3);
		tracePrimitives3('E', (byte) 127, (short) 15, 'E', -222, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 126, (short) 15, 'E', -222, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 107, (short) 15, 'E', -222, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 27, (short) 15, 'E', -222, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 215, 'E', -222, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 151, 'E', -222, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15315, 'E', -222, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15715, 'E', -222, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15, 'e', -222, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15, 'f', -222, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', 222, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -223, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -232, 77132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -222, 67132, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -222, 77133, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -222, 77232, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -222, 76232, -0.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -222, 77132, -1.22387f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -222, 77132, -0.22367f, 2.1332);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -222, 77132, -0.22387f, 2.1432);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -222, 77132, -0.22387f, 2.1335);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -222, 77132, -0.22387f, 2.1533);
		tracePrimitives3('e', (byte) 127, (short) 15, 'E', -222, 77132, -0.22387f, -2.1332);
		traceObjects1("", new byte[3], new byte[4][], new Integer[10], C1.class, new C1(), new C3(), "Hehe!");
		traceObjects1(null, new Byte[3], new byte[4][], new Integer[10], C1.class, new C1(), new C3(), "Hehe!");
		traceObjects1(null, new byte[2], new byte[4][], new Integer[10], C1.class, new C1(), new C3(), "Hehe!");
		traceObjects1(null, new byte[5], new byte[4][], new Integer[10], C1.class, new C1(), new C3(), "Hehe!");
		traceObjects1(null, new byte[3], new byte[4], new Integer[10], C1.class, new C1(), new C3(), "Hehe!");
		traceObjects1(null, new byte[3], new byte[3][], new Integer[10], C1.class, new C1(), new C3(), "Hehe!");
		traceObjects1(null, new byte[3], new byte[6][], new Integer[10], C1.class, new C1(), new C3(), "Hehe!");
		traceObjects1(null, new byte[3], new byte[4][], new byte[10], C1.class, new C1(), new C3(), "Hehe!");
		traceObjects1(null, new byte[3], new byte[4][], new Integer[9], C1.class, new C1(), new C3(), "Hehe!");
		traceObjects1(null, new byte[3], new byte[4][], new Integer[11], C1.class, new C1(), new C3(), "Hehe!");
		traceObjects1(null, new byte[3], new byte[4][], new Integer[10], null, new C1(), new C3(), "Hehe!");
		traceObjects1(null, new byte[3], new byte[4][], new Integer[10], C1.class, C1.class, new C3(), "Hehe!");
		traceObjects1(null, new byte[3], new byte[4][], new Integer[10], C1.class, new C2(), new C3(), "Hehe!");
		traceObjects1(null, new byte[3], new byte[4][], new Integer[10], C1.class, new C1(), new C2(), "Hehe!");
		traceObjects1(null, new byte[3], new byte[4][], new Integer[10], C1.class, new C1(), new C3(), "Hehe!!");
		traceObjects1(null, new byte[3], new byte[4][], new Integer[10], C1.class, new C1(), new C3(), "Heha!");
		traceObjects2(za1, map2, za, null, dl1, dl2, map3, oa2);
		traceObjects2(map2, map2, za, null, dl1, dl2, map3, oa2);
		traceObjects2(map1, map1, za, null, dl1, dl2, map3, oa2);
		traceObjects2(map1, map2, new Object[0], "null", dl1, dl2, map3, oa2);
		traceObjects2(map1, map2, za, "null", dl1, dl2, map3, oa2);
		traceObjects2(map1, map2, za, null, new int[10], dl2, map3, oa2);
		traceObjects2(map1, map2, za, null, map1, dl2, map3, oa2);
		traceObjects2(map1, map2, za, null, dl1, dl1, map3, oa2);
		traceObjects2(map1, map2, za, null, dl1, dl2, map2, oa2);
	}

	protected void runAllTests() throws Exception {
		File xmlFile = new File("generic_logger.xml").getAbsoluteFile();

		try (InputStream is = GenericLoggingTest.class.getClassLoader()
				.getResourceAsStream("org/openjdk/jmc/agent/test/sap/generic.xml")) {
			Files.copy(is, xmlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

			String filter1 = "value1Equals=false,value1NotEquals=true,value1StartsWith=fal,value1EndsWith=lse,"
					+ "value1NotStartsWith=tr,value1NotEndsWith=ue,value1Contains=ls,"
					+ "value1NotContains=ru,value1MatchesRegexp=a.s,value1NotMatchesRegexp=r.e,"
					+ "minLongValue2=-1,maxLongValue2=2,minDoubleValue3=-7.2,maxDoubleValue3=1.1,"
					+ "minLongValue4=67,maxLongValue4=67,minDoubleValue5=127.2,maxDoubleValue5=128.1,"
					+ "minLongValue6=-7,maxLongValue6=-7,minDoubleValue7=0.15,maxDoubleValue7=0.25";
			String filter2 = "value1Equals=C,value1StartsWith=C,value1EndsWith=C,value1Contains=C,"
					+ "value1MatchesRegexp=^[C]$,minDoubleValue2=0.9,maxDoubleValue2=1.1,"
					+ "minLongValue3=7,maxLongValue3=7,minDoubleValue4=66.3,maxDoubleValue4=67.1,"
					+ "minLongValue5=-3,maxLongValue5=-1,minDoubleValue6=6.9,maxDoubleValue6=7.1,"
					+ "minLongValue7=-1,maxLongValue7=0,minDoubleValue8=2.15,maxDoubleValue8=2.25";
			String filter3 = "value1NotMatchesRegexp=E,value2MatchesRegexp=1?[12][76],value2EndsWith=7,"
					+ "value2Contains=1,value3StartsWith=15,value3EndsWith=15,value3NotContains=3,"
					+ "value3NotMatchesRegexp=7,value3Instanceof=java.lang.Short,"
					+ "value3Instanceof=java.lang.Number,value3Instanceof=java.lang.Object,"
					+ "value4Instanceof=java.lang.Character,value4NotInstanceof=java.lang.Number,"
					+ "value4NotContains=e,value4MatchesRegexp=[eE],value5StartsWith=-,"
					+ "value5EndsWith=2,value5MatchesRegexp=22,value6NotStartsWith=6,value6NotEndsWith=3,"
					+ "value6NotContains=23,value6MatchesRegexp=.7.3,value7NotContains=6,value7MatchesRegexp=0[.],"
					+ "value7Instanceof=java.lang.Float,value8Instanceof=java.lang.Number,"
					+ "value8Instanceof=java.lang.Double,value8NotInstanceof=java.lang.Float,"
					+ "value8NotEquals=2.1432,value8NotContains=35,value8NotEndsWith=3,value8NotStartsWith=-";
			String filter4 = "value1IsType=null,value2IsNotType=object_array,value2IsType=array,"
					+ "value2IsType=primitive_array,value2MinLength=3,value2MaxLength=4,value3IsType=array,"
					+ "value3IsNotType=primitive_array,value3IsType=object_array,value3MinLength=4,"
					+ "value3MaxLength=5,value4IsType=array,value4IsNotType=primitive_array,"
					+ "value4IsType=object_array,value4MinLength=10,value4MaxLength=10,value5IsNotType=null,"
					+ "value5Instanceof=org.openjdk.jmc.agent.sap.test.C1,value5Instanceof=java.lang.Object,"
					+ "value5Instanceof=java.lang.Class,value5Instanceof=java.lang.Comparable,"
					+ "value6Instanceof=org.openjdk.jmc.agent.sap.test.C1,value6Instanceof=java.lang.Object,"
					+ "value6NotInstanceof=java.lang.Class,value6NotInstanceof=org.openjdk.jmc.agent.sap.test.C2,"
					+ "value7Instanceof=org.openjdk.jmc.agent.sap.test.I3,value7Instanceof=org.openjdk.jmc.agent.sap.test.C2,"
					+ "value7Instanceof=org.openjdk.jmc.agent.sap.test.C1,value7Instanceof=org.openjdk.jmc.agent.sap.test.I2,"
					+ "value7Instanceof=org.openjdk.jmc.agent.sap.test.I1,value8StartsWith=He,value8EndsWith=!,"
					+ "value8MatchesRegexp=.e.e,value8MinLength=5,value8MaxLength=5,value8Contains=e!";
			String filter5 = "printCollectionContent=true,value1MaxLength=1,value1MinLength=1,"
					+ "value1IsNotType=array,value2MinLength=6,value2IsNotType=primitive_array,"
					+ "value2IsNotType=object_array,value2IsNotType=null,value3IsType=primitive_array,"
					+ "value4IsType=null,value4IsNotType=object_array,value4IsNotType=array,"
					+ "value5MinLength=6,value5IsNotType=array,value6StartsWith=JustA,value6EndsWith=Dummy,"
					+ "value6MatchesRegexp=.ust.Du[m-n].y,value6Contains=ADum,value6Equals=JustADummy,"
					+ "value7MaxLength=2";
			String filter6 = "printCollectionContent=false";
			JavaAgentRunner runner = getRunner(xmlFile.getPath() + ",logGeneric1,logDest=stdout," + filter1
					+ ",logGeneric2,logDest=stdout," + filter2 + ",logGeneric3,logDest=stdout," + filter3
					+ ",logGeneric4,logDest=stdout," + filter4 + ",logGeneric5,logDest=stdout," + filter5
					+ ",logGeneric6,logDest=stdout," + filter6);
			runner.start("genericLogging4");
			runner.waitForEnd();
			assertLinesContains(runner.getStdoutLines(), "Values for generic logger 1: false -1 -7 C 128 -7 0.2 -2.2",
					"Values for generic logger 2: C 1 7 C -2 7 -0.2 2.2",
					"Values for generic logger 3: e 127 15 E -222 77132 -0.22387 2.1332",
					"Values for generic logger 4: null {0, 0, 0} {null, null, null, null} "
							+ "{null, null, null, null, null, 5 skipped ...} "
							+ "class org.openjdk.jmc.agent.sap.test.C1 C1 C3 \"Hehe!\"",
					"Values for generic logger 4: null {0, 0, 0} {null, null, null, null} "
							+ "{null, null, null, null, null, 5 skipped ...} "
							+ "class org.openjdk.jmc.agent.sap.test.C1 C1 C3 \"Hele!\"",
					"Values for generic logger 5: {\"test\": C1} {\"test1\": C1, \"test2\": C2, "
							+ "\"test3\": C3, null: C1, \"test5\": C2, 1 skipped ...} "
							+ "{true, false, true, false, true} null {C1, C2, C3, C1, C2, 1 skipped ...} "
							+ "org.openjdk.jmc.agent.sap.test.DummyList(size 6) "
							+ "{\"map1\": {\"test\": C1}, \"map2\": {\"test1\": C1, \"test2\": C2, "
							+ "\"test3\": C3, null: C1, \"test5\": C2, 1 skipped ...}} "
							+ "{java.lang.Object[3][], java.lang.String[8], {\"test\": C1}, "
							+ "{\"map1\": {\"test\": C1}, \"map2\": {\"test1\": C1, \"test2\": C2, "
							+ "\"test3\": C3, null: C1, \"test5\": C2, 1 skipped ...}}, boolean[5], 1 skipped ...}",
					"Values for generic logger 6: org.openjdk.jmc.agent.sap.test.DummyList(size 6) "
							+ "{\"just\", \"a\", \"test\", null, \"with\", 3 skipped ...} "
							+ "{true, false, true, false, true} java.util.HashMap(size 1) "
							+ "java.util.LinkedHashMap(size 6) {java.lang.Object[1], java.io.Serializable[2][], "
							+ "java.lang.String[3]} class java.lang.Class \"Jeje\"");
			assertNrOfLines(runner.getStdoutLines(), 9); // Should contain no other than the lines check above.
		} finally {
			while (xmlFile.exists()) {
				xmlFile.delete();
			}
		}
	}

	public static int tracePrimitives1(boolean v1, byte v2, short v3, char c4, int v5, long v6, float v7, double v8) {
		return 0;
	}

	public static int tracePrimitives2(char v1, byte v2, short v3, char c4, int v5, long v6, float v7, double v8) {
		return 0;
	}

	public static int tracePrimitives3(char v1, byte v2, short v3, char c4, int v5, long v6, float v7, double v8) {
		return 0;
	}

	public static int traceObjects1(
		Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
		if (o2 != null) {
			return 1;
		}

		return 0;
	}

	public static int traceObjects2(
		Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
		return 0;
	}

	public static int traceObjects3(
		Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
		return 0;
	}
}

class C1 implements Comparable<Object> {

	@Override
	public int compareTo(Object o) {
		return 0;
	}

	@Override
	public String toString() {
		return "C1";
	}
}

interface I1 {
	public void if1();
}

interface I2 extends I1 {
	public void if2();
}

interface I3 {
	public void if3();
}

class C2 extends C1 implements I2, I1 {

	@Override
	public void if1() {
	}

	@Override
	public void if2() {
	}

	@Override
	public String toString() {
		return "C2";
	}
}

class C3 extends C2 implements I3 {

	@Override
	public void if3() {
	}

	@Override
	public String toString() {
		return "C3";
	}
}

@SuppressWarnings("serial")
class DummyList<E> extends ArrayList<E> {

	private final boolean allowInspection;

	public DummyList(Collection<E> toCopy, boolean allowInspection) {
		super(toCopy);
		this.allowInspection = allowInspection;
	}

	@Override
	public Iterator<E> iterator() {
		if (allowInspection) {
			return super.iterator();
		}

		return new Iterator<E>() {

			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public E next() {
				throw new ConcurrentModificationException();
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public E[] toArray() {
		if (allowInspection) {
			return (E[]) super.toArray();
		}

		throw new RuntimeException("Chould not be called.");
	}

	@Override
	public <T> T[] toArray(T[] a) {
		if (allowInspection) {
			return super.toArray(a);
		}

		throw new RuntimeException("Chould not be called.");
	}

	@Override
	public String toString() {
		if (allowInspection) {
			return super.toString();
		}

		return "JustADummy";
	}
}
