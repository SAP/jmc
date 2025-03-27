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

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Predicate;

public class Dumps {
	public static final String DUMP_COUNT = "dumpCount";
	public static final String DUMP_INTERVAL = "dumpInterval";
	public static final String DUMP_DELAY = "dumpDelay";
	public static final String EXIT_AFTER_LAST_DUMP = "exitAfterLastDump";

	private static final HashMap<Command, Predicate<Arguments>> registeredDumps = new HashMap<>();

	public static void performDump(String arguments) throws IOException {
		if (arguments.equals("help")) {
			System.out.println("The following dumps are supported:");

			for (Command cmd : registeredDumps.keySet()) {
				if (registeredDumps.get(cmd) != null) {
					System.out.println(cmd.getName() + ": " + cmd.getDescription());
				}
			}

			System.out.println("Use dump=help:<dump> for all options for a specific dump.");
			return;
		} else if (arguments.startsWith("help:")) {
			String name = arguments.substring(5);

			for (Command cmd : registeredDumps.keySet()) {
				if (cmd.getName().equals(name)) {
					cmd.printHelp(System.out);
					return;
				}
			}

			System.err.println("Could not find dump type '" + name + "'. Use dump=help for a list of supported types.");
			return;
		}

		String[] parts = arguments.split(",");
		String type = parts[0];

		if (parts.length > 1) {
			arguments = arguments.substring(type.length() + 1);
		} else {
			arguments = "";
		}

		Arguments args = new Arguments(arguments);
		Predicate<Arguments> callback = null;

		synchronized (Dumps.class) {
			callback = registeredDumps.get(new Command(type, ""));
		}

		if (callback == null) {
			System.err.println("No dump registered for dumType='" + type + "'");
			return;
		}

		callback.test(args);
	}

	public static void registerPeriodicDump(Command command, String name, Predicate<Arguments> callback) {
		Arguments args = command.getArguments().get();
		long dumpCount = args.getLong(DUMP_COUNT, 0);

		if (dumpCount != 0) {
			long interval = args.getDurationInSeconds(DUMP_INTERVAL, 3600);
			long delay = args.getDurationInSeconds(DUMP_DELAY, interval);
			boolean exitAfterLastDump = args.getBoolean(EXIT_AFTER_LAST_DUMP, false);

			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(delay * 1000);

						long dumpsLeft = dumpCount;

						while (dumpsLeft > 0) {
							if (callback.test(args)) {
								dumpsLeft -= 1;
								LoggingUtils.log(args,
										name + " dump " + (dumpCount - dumpsLeft) + " of " + dumpCount + ".");
							}

							if (dumpsLeft > 0) {
								Thread.sleep(interval * 1000);
							}
						}

						if (exitAfterLastDump) {
							LoggingUtils.log(args, name + " dumps finished. Exiting VM.");
							System.exit(0);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}, "Dumper Thread");

			t.setDaemon(true);
			t.start();
		}
	}

	public static void registerOnDemandDump(Command command, Predicate<Arguments> callback) {
		synchronized (Dumps.class) {
			registeredDumps.put(command, callback);
		}
	}

	public static void addOptions(Command command) {
		command.addOption(DUMP_COUNT, "The maximum number of dumps to perform.");
		command.addOption(DUMP_INTERVAL,
				"The interval between successive dumps. Supports  s, m, h and d (e.g. 10s or 6m).");
		command.addOption(DUMP_DELAY, "The delay until the first dump is triggered. Supports s, m, h and d.");
		command.addOption(EXIT_AFTER_LAST_DUMP, "If true, the VM will be exited after the last dump is triggered.");
	}
}
