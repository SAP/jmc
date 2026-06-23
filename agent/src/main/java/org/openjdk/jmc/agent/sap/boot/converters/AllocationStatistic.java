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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.agent.sap.boot.util.Arguments;
import org.openjdk.jmc.agent.sap.boot.util.LoggingUtils;

public class AllocationStatistic {
	private HashMap<Long, AllocationSite> activeAllocations = new HashMap<>();
	private long totalSize = 0;
	private static long lastDumpSize = 0;

	public AllocationStatistic copy() {
		AllocationStatistic result = new AllocationStatistic();

		synchronized (activeAllocations) {
			result.activeAllocations = new HashMap<>(activeAllocations);
			result.totalSize = totalSize;
		}

		return result;
	}

	public void addAllocation(long addr, long size) {
		AllocationSite site = new AllocationSite(size);

		synchronized (activeAllocations) {
			assert !activeAllocations.containsKey(addr);
			totalSize += size;
			activeAllocations.put(addr, site);
		}
	}

	public void removeAllocation(long addr) {
		synchronized (activeAllocations) {
			assert activeAllocations.containsKey(addr);
			AllocationSite site = activeAllocations.remove(addr);

			if (site != null) {
				assert totalSize > site.size;
				totalSize -= site.size;
			}
		}
	}

	public boolean printActiveAllocations(Arguments args) {
		PrintStream ps = LoggingUtils.getStream(args);
		AllocationStatisticDumpFilter filter = new AllocationStatisticDumpFilter(args);

		synchronized (AllocationStatistic.class) {
			if (totalSize < filter.minSize) {
				return false;
			}

			if (totalSize < lastDumpSize * filter.minPercentageIncrease) {
				return false;
			}

			if ((filter.minIncrease >= 0) && (totalSize < lastDumpSize + filter.minIncrease)) {
				return false;
			}

			long printedSize = 0;
			long printedCount = 0;
			boolean dumped = false;

			for (Map.Entry<Long, AllocationSite> entry : activeAllocations.entrySet()) {
				if (entry.getValue().printOn(entry.getKey(), ps, filter)) {
					printedSize += entry.getValue().size;
					printedCount += 1;
				}
			}

			if (printedCount > 0) {
				ps.println("Printed " + printedCount + " of " + activeAllocations.size() + " allocations with "
						+ printedSize + " bytes (of " + totalSize + " bytes allocated in total).");
				lastDumpSize = totalSize;
				dumped = true;
			}

			return dumped;
		}
	}
}
