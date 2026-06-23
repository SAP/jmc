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
