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

package org.openjdk.jmc.agent.sap.boot.util;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class Command {
	private final String name;
	private final String description;
	private final HashMap<String, String> optionsWithHelp;

	public Command(String name, String description, String ... optionsWithHelp) {
		this.name = name;
		this.description = description;
		this.optionsWithHelp = new HashMap<>();

		for (int i = 0; i < optionsWithHelp.length; i += 2) {
			this.optionsWithHelp.put(optionsWithHelp[i], optionsWithHelp[i + 1]);
		}
	}

	public Command(Command parentCommand, String name, String description, String ... optionsWithHelp) {
		this.name = name;
		this.description = description;
		this.optionsWithHelp = new HashMap<>(parentCommand.optionsWithHelp);

		for (int i = 0; i < optionsWithHelp.length; i += 2) {
			this.optionsWithHelp.put(optionsWithHelp[i], optionsWithHelp[i + 1]);
		}
	}

	public void addOption(String option, String description) {
		optionsWithHelp.put(option, description);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String[] getOptions() {
		return optionsWithHelp.keySet().toArray(new String[optionsWithHelp.size()]);
	}

	public boolean hasOption(String name) {
		return optionsWithHelp.containsKey(name);
	}

	public String getOptionHJelp(String name) {
		return optionsWithHelp.get(name);
	}

	public void preTraceInit() {
		// Nothing to do by default.
	}

	public void printHelp(PrintStream str) {
		str.println("Help for command '" + getName() + "':");
		str.println("Description: " + getDescription());
		String[] options = getOptions();
		Arrays.sort(options, String.CASE_INSENSITIVE_ORDER);

		if (options.length > 0) {
			str.println("The following options are supported:");

			for (String option : options) {
				str.println(option + ": " + getOptionHJelp(option));
			}
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Command other = (Command) obj;
		return Objects.equals(name, other.name);
	}
}
