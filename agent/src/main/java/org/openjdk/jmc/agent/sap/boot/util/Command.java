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
package org.openjdk.jmc.agent.sap.boot.util;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class Command {
	private final String name;
	private final String description;
	private final HashMap<String, String> optionsWithHelp;
	public static final HashMap<String, ArgumentsHolder> holders = new HashMap<>();

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
		if (optionsWithHelp.containsKey(name)) {
			return true;
		}

		for (String option : optionsWithHelp.keySet()) {
			if (option.contains("<idx>")) {
				if (name.matches(option.replace("<idx>", "[0-9]+"))) {
					return true;
				}
			}
		}

		return false;
	}

	public String getOptionHelp(String name) {
		return optionsWithHelp.get(name);
	}

	public void preTraceInit() {
		// Nothing to do by default
	}

	public void addCommandArgs(String options) {
		Arguments args = new Arguments(options, this);
		boolean seenFirst = false;

		synchronized (Command.class) {
			ArgumentsHolder holder = holders.get(name);

			if (holder == null) {
				holder = new ArgumentsHolder(args);
				holders.put(name, holder);
				seenFirst = true;
			} else {
				if (holder.get() == null) {
					seenFirst = true;
				}

				holder.set(args);
			}
		}

		if (seenFirst) {
			preTraceInit();
		}
	}

	public ArgumentsHolder getArguments() {
		synchronized (Command.class) {
			ArgumentsHolder holder = holders.get(name);

			if (holder == null) {
				holder = new ArgumentsHolder(null);
				holders.put(name, holder);
			}

			return holder;
		}
	}

	public void printHelp(PrintStream str) {
		str.println("Help for command '" + getName() + "':");
		str.println("Description: " + getDescription());
		String[] options = getOptions();
		Arrays.sort(options, String.CASE_INSENSITIVE_ORDER);

		if (options.length > 0) {
			str.println();
			str.println("The following options are supported:");

			for (String option : options) {
				str.println(option + ": " + getOptionHelp(option));
			}

			str.println();
			str.println("In order to specify options, add them separated by commas after the command:");
			str.println(getName() + "[,<option1=value>[,<option2=value[,...]]]");
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
