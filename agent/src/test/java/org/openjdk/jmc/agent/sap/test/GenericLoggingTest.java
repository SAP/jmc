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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;

public class GenericLoggingTest extends TestBase {

	public static void main(String[] args) {
		new GenericLoggingTest().dispatch(args);
	}

	protected void runAllTests() throws Exception {
		File xmlFile = new File("generic_logger.xml").getAbsoluteFile();

		try (InputStream is = GenericLoggingTest.class.getClassLoader()
				.getResourceAsStream("org/openjdk/jmc/agent/test/sap/generic.xml")) {
			Files.copy(is, xmlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

			JavaAgentRunner runner = getRunner(xmlFile.getPath()
					+ ",logGeneric1,logDest=stdout,maxLongValue2=100,maxDoubleValue2=2.1,"
					+ "logGeneric2,logDest=stdout,minLongValue2=0," + "logGeneric3,logDest=stdout,oncePerStack=true,"
					+ "logGeneric4,logDest=stdout," + "logGeneric5,logDest=stdout");
			runner.start("genericLogging");
			runner.waitForEnd();
		} finally {
			xmlFile.delete();
		}
	}

	public static void genericLogging() {
		log1(false, (byte) 2, 17, 0.2f, "TestMe", new Date(0));
		log2((short) 7, 0.3, "Yeah", 17, 0.3f);

		for (int i = 0; i < 10; ++i) {
			log3('A', -1, 0.1, (short) -1, true);
		}

		log4(Boolean.class, (byte) 2, 17, -0.2f, "TestMe", new Date(1));
		log5((short) 7, 0.3, "Yeah", 17, -0.3f);
		log1(false, (byte) 101, 107, 0.2f, "TestMe", new Date(0));
		log1(false, (byte) 99, 107, 0.2f, "TestMe", new Date(0));
		log2((short) 7, -0.3, "Yeah", 17, 0.3f);
		log1(false, (byte) 3, 17, 0.2f, "TestMe", new Date(0));
	}

	@SuppressWarnings("deprecation")
	public static String log1(boolean v1, byte v2, int v3, float v4, String v5, Date v6) {
		return v6.toGMTString();
	}

	public static int log2(short v1, double v2, String v3, Integer v4, float v5) {
		return (int) v2;
	}

	public static double log3(char v1, Integer v2, Double v3, Short v4, Boolean v5) {
		return v1 + v3;
	}

	@SuppressWarnings("deprecation")
	public static String log4(Class<?> v1, byte v2, int v3, float v4, CharSequence v5, Date v6) {
		return v6.toGMTString();
	}

	public static int log5(short v1, double v2, Object v3, Integer v4, float v5) {
		return (int) v2;
	}
}
