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
