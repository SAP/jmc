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
		System.out.println("The following commands are suppored:");

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
				System.err
						.println("Unknown argument '" + unknownArgument + "' for command '" + command.getName() + "'.");
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
