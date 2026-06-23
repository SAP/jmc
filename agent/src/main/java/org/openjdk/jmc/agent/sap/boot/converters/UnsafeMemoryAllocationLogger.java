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

import org.openjdk.jmc.agent.sap.boot.util.ArgumentsHolder;
import org.openjdk.jmc.agent.sap.boot.util.Command;
import org.openjdk.jmc.agent.sap.boot.util.Arguments;
import org.openjdk.jmc.agent.sap.boot.util.Dumps;
import org.openjdk.jmc.agent.sap.boot.util.LoggingUtils;

public class UnsafeMemoryAllocationLogger {
	public static final String MAX_FRAMES = "maxFrames";
	public static final String MIN_STACK_SIZE = "minStackSize";
	public static final String MIN_SIZE = "minSize";
	public static final String MIN_INCREASE = "minIncrease";
	public static final String MIN_PERCENTAGE = "minPercentage";
	public static final String MIN_AGE = "minAge";
	public static final String MAX_AGE = "maxAge";
	public static final String MUST_CONTAIN = "mustContain";
	public static final String MUST_NOT_CONTAIN = "mustNotContain";
	public static final Command dumpCommand;
	public static final Command command;

	private static final ThreadLocal<Long> sizeKey = new ThreadLocal<Long>();
	private static final ThreadLocal<Long> ptrKey = new ThreadLocal<Long>();
	private static final AllocationStatistic allocations = new AllocationStatistic();
	private static final ArgumentsHolder holder;

	static {
		// spotless:off
		dumpCommand = new Command(
				"unsafeAllocations", "Dump the currently active jdk.internal.misc.Unsafe allocatios.",
				MAX_FRAMES,	"The maximum number of frame to use for stack traces.",
				MIN_SIZE, "The minimum size of the live allocations to dump the result.",
				MIN_STACK_SIZE, "The minimum size of a stack to be included in a dump.",
				MIN_PERCENTAGE, "The minimum percentage compared to the last dump to print a dump.",
				MIN_AGE, "The minimum age to include an allocation in the output.",
				MAX_AGE, "The maximum age to include an allocation in the output.",
				MUST_CONTAIN, "A regexp which must match at least one frame to be printed.",
				MUST_NOT_CONTAIN, "A regexp which must not match any frame to be printed.");
		command = new Command(dumpCommand,
				"traceUnsafeAllocations", "Traces native memory allocation by jdk.internal.misc.Unsafe") {
			public void preTraceInit() {
				Dumps.registerPeriodicDump(command, "Unsafe native memory allocation",
						(Arguments args) -> printActiveAllocations(args));
			}
		};
		// spotless:on

		LoggingUtils.addOptions(command);
		Dumps.addOptions(command);
		Dumps.registerOnDemandDump(dumpCommand, (Arguments args) -> printActiveAllocations(args));
		holder = command.getArguments();
	}

	public static long logSize(long size) {
		assert sizeKey.get() == null;
		sizeKey.set(size);

		return size;
	}

	public static long logPtr(long ptr) {
		assert ptrKey.get() == null;
		assert sizeKey.get() != null;
		ptrKey.set(ptr);

		return ptr;
	}

	public static long logResult(long result) {
		Long toAdd = Long.valueOf(result);
		Long oldPtr = ptrKey.get();

		if ((oldPtr != null) && (oldPtr.longValue() != 0)) {
			// This is realloc.
			Long newSize = sizeKey.get();

			if (newSize != null) {
				allocations.removeAllocation(oldPtr);

				// Realloc with size 0 is a free.
				if (newSize > 0) {
					allocations.addAllocation(toAdd, newSize);
				}
			}
		} else {
			// This is malloc.
			Long size = sizeKey.get();

			if (size != null) {
				allocations.addAllocation(toAdd, size);
			}
		}

		sizeKey.remove();
		ptrKey.remove();

		return result;
	}

	public static long logFree(long ptr) {
		assert sizeKey.get() == null;
		assert ptrKey.get() == null;

		Long toRemove = Long.valueOf(ptr);

		if (toRemove != null) {
			allocations.removeAllocation(toRemove);
		}

		return ptr;
	}

	public static boolean printActiveAllocations() {
		return printActiveAllocations(holder.get());
	}

	public static boolean printActiveAllocations(Arguments args) {
		if (LoggingUtils.doesOutput(args)) {
			return allocations.copy().printActiveAllocations(args);
		}

		return false;
	}
}
