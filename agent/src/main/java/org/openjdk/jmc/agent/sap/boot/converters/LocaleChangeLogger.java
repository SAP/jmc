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
package org.openjdk.jmc.agent.sap.boot.converters;

import java.util.Locale;

import org.openjdk.jmc.agent.sap.boot.util.ArgumentsHolder;
import org.openjdk.jmc.agent.sap.boot.util.Command;
import org.openjdk.jmc.agent.sap.boot.util.LoggingUtils;
import org.openjdk.jmc.agent.sap.boot.util.OutputCommand;

public class LocaleChangeLogger {

	private static final ThreadLocal<Locale.Category> categoryKey = new ThreadLocal<Locale.Category>();

	public static Command command = new OutputCommand("traceLocaleChange",
			"Traces when the default locale is changed.");

	private static final ArgumentsHolder holder = command.getArguments();

	public static String logDefaultLocaleCategoryChange(Locale.Category newCategory) {
		assert categoryKey.get() == null;
		categoryKey.set(newCategory);

		return newCategory.name();
	}

	public static String logDefaultLocale(Locale.Category newCategory) {
		assert categoryKey.get() != null;

		return Locale.getDefault(newCategory).getDisplayName(Locale.ENGLISH);
	}

	public static String logDefaultLocalChange(Locale newLocale) {
		assert categoryKey.get() != null;

		return newLocale.getDisplayName(Locale.ENGLISH);
	}

	public static boolean changesDefaultLocale(Locale newLocale) {
		assert categoryKey.get() != null;
		Locale oldLocale = Locale.getDefault(categoryKey.get());
		boolean result = !oldLocale.equals(newLocale);

		if (result) {
			LoggingUtils.logWithStack(holder.get(),
					"Changed default locale for category '" + categoryKey.get().name() + "' from '"
							+ oldLocale.getDisplayName(Locale.ENGLISH) + "' to '"
							+ newLocale.getDisplayName(Locale.ENGLISH) + "'.",
					2);
		}

		categoryKey.remove();

		return result;
	}
}
