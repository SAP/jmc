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

	public void addAllocastion(long addr, long size) {
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
