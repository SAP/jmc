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

public class DuplicateTracedClasses {

	public static void main(String[] args) throws Exception {
		CloneClassLoader cl1 = new CloneClassLoader(DuplicateTracedClasses.class.getClassLoader());
		CloneClassLoader cl2 = new CloneClassLoader(DuplicateTracedClasses.class.getClassLoader());
		Class<?> clz1 = cl1.loadClass(DuplicateTracedClasses.class.getName());
		Class<?> clz2 = cl2.loadClass(DuplicateTracedClasses.class.getName());
		Method m1 = clz1.getDeclaredMethod("test", String.class);
		Method m2 = clz2.getDeclaredMethod("test", String.class);
		m1.invoke(null, "Loader 1");
		m2.invoke(null, "Loader 2");
		test("Default loader");
	}

	public static void test(String msg) {
		// Nothing to do.
	}
}
