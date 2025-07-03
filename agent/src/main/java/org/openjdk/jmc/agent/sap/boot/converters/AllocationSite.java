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
import java.util.Date;

public class AllocationSite {
	public final Exception stack;
	public final long timestamp;
	public final long size;

	public AllocationSite(long size) {
		this.stack = new Exception();
		this.timestamp = System.currentTimeMillis();
		this.size = size;
	}

	public boolean printOn(long address, PrintStream ps, AllocationStatisticDumpFilter filter) {
		if (size < filter.minStackSize) {
			return false;
		}

		long age = System.currentTimeMillis() - timestamp;

		if (age > filter.maxAge) {
			return false;
		}

		if (age < filter.minAge) {
			return false;
		}

		StackTraceElement[] frames = stack.getStackTrace();
		int framesToSkip = 3;
		int maxFrames = Math.min(filter.maxFrames + framesToSkip, frames.length);

		if (filter.mustContain != null) {
			boolean matchFound = false;

			for (int i = framesToSkip; i < maxFrames; ++i) {
				if (filter.mustContain.matcher(frames[i].toString()).find()) {
					matchFound = true;

					break;
				}
			}

			if (!matchFound) {
				return false;
			}
		}

		if (filter.mustNotContain != null) {
			for (int i = framesToSkip; i < maxFrames; ++i) {
				if (filter.mustNotContain.matcher(frames[i].toString()).find()) {
					return false;
				}
			}
		}

		ps.println("Allocated " + size + " bytes at 0x" + Long.toUnsignedString(address, 16));
		ps.println("Timestamp: " + new Date(timestamp).toString());
		ps.println("Allocated at:");

		for (int i = framesToSkip; i < maxFrames; ++i) {
			ps.println("\t" + frames[i]);
		}

		return true;
	}
}
