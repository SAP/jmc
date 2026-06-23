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

import org.openjdk.jmc.agent.sap.boot.converters.GenericLogger;
import org.openjdk.jmc.agent.sap.boot.converters.LocaleChangeLogger;
import org.openjdk.jmc.agent.sap.boot.converters.SystemPropChangeLogger;
import org.openjdk.jmc.agent.sap.boot.converters.TimeZoneChangeLogger;
import org.openjdk.jmc.agent.sap.boot.converters.UnsafeMemoryAllocationLogger;

public class Commands {

	public static final Command[] commands = new Command[] {LocaleChangeLogger.command, SystemPropChangeLogger.command,
			TimeZoneChangeLogger.command, UnsafeMemoryAllocationLogger.command, GenericLogger.commands[0],
			GenericLogger.commands[1], GenericLogger.commands[2], GenericLogger.commands[3], GenericLogger.commands[4],
			GenericLogger.commands[5]};

	public static void printAllCommands() {
		System.out.println("The following commands are supported:");

		for (Command command : commands) {
			System.out.println(command.getName() + ": " + command.getDescription());
		}

		System.out.println();
		System.out.println("Use <command>,help to get further help for a specific command.");
	}

	public static Command getCommand(String name) {
		for (Command command : commands) {
			if (command.getName().equals(name)) {
				return command;
			}
		}

		return null;
	}

	public static boolean checkCommands() {
		for (Command command : commands) {
			ArgumentsHolder holder = command.getArguments();

			if (holder.get() == null) {
				continue; // No arguments.
			}

			Arguments args = holder.get();

			if (args.hasHelpOption()) {
				printHelp(command);

				return false;
			}

			String unknownArgument = args.getUnknownArgument();

			if (unknownArgument != null) {
				// spotless:off
				System.err.println("Unknown argument '" + unknownArgument + "' for command '" + command.getName() + "'.");
				// spotless:on
				printHelp(command);

				return false;
			}
		}

		return true;
	}

	private static void printHelp(Command command) {
		command.printHelp(System.err);
	}
}
