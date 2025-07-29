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

import java.util.Locale;
import java.util.Locale.Category;

public class LocaleChangeTest extends TestBase {

	public static void main(String[] args) {
		new LocaleChangeTest().dispatch(args);
	}

	@Override
	protected void runAllTests() throws Exception {
		JavaAgentRunner runner = getRunner("traceLocaleChange,logDest=stdout");
		runner.start("changeLocale");
		runner.waitForEnd();
		assertLinesContainsRegExp(runner.getStdoutLines(),
				"Changed default locale for category 'DISPLAY' from .+ to 'English [(]Canada[)]'",
				"Changed default locale for category 'FORMAT' from .* to 'English [(]Canada[)]'");
		assertLinesContains(runner.getStdoutLines(),
				"Changed default locale for category 'DISPLAY' from 'English (Canada)' to 'Chinese (Taiwan)'.",
				"Changed default locale for category 'FORMAT' from 'English (Canada)' to 'Chinese (Taiwan)'.",
				"Changed default locale for category 'DISPLAY' from 'Chinese (Taiwan)' to 'English (Canada)'",
				"Changed default locale for category 'FORMAT' from 'Chinese (Taiwan)' to 'English (Canada)'.",
				"Changed default locale for category 'DISPLAY' from 'English (Canada)' to 'Chinese (China)'.",
				"Changed default locale for category 'DISPLAY' from 'Chinese (China)' to 'French (France)'.",
				"Changed default locale for category 'FORMAT' from 'English (Canada)' to 'Italian'.",
				"Changed default locale for category 'FORMAT' from 'Italian' to 'French (Canada)'.");
		assertLinesNotContains(runner.getStdoutLines(),
				"Changed default locale for category 'DISPLAY' from 'Chinese (Taiwan)' to 'Chinese (Taiwan)'.",
				"Changed default locale for category 'FORMAT' from 'Chinese (Taiwan)' to 'Chinese (Taiwan)'.",
				"Changed default locale for category 'DISPLAY' from 'Chinese (China)' to 'Chinese (China)'.",
				"Changed default locale for category 'FORMAT' from 'Italian' to 'Italian'.");
	}

	public static void changeLocale() {
		Locale.setDefault(Locale.CANADA);
		Locale.setDefault(Locale.TAIWAN);
		Locale.setDefault(Locale.TAIWAN);
		Locale.setDefault(Locale.CANADA);
		Locale.setDefault(Category.DISPLAY, Locale.CHINA);
		Locale.setDefault(Category.DISPLAY, Locale.CHINA);
		Locale.setDefault(Category.DISPLAY, Locale.FRANCE);
		Locale.setDefault(Category.FORMAT, Locale.ITALIAN);
		Locale.setDefault(Category.FORMAT, Locale.ITALIAN);
		Locale.setDefault(Category.FORMAT, Locale.CANADA_FRENCH);
	}
}
