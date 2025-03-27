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

import java.util.regex.Pattern;

import org.openjdk.jmc.agent.sap.boot.util.Arguments;

public class AllocationStatisticDumpFilter {
	public final int maxFrames;
	public final long minSize;
	public final long minStackSize;
	public final long minIncrease;
	public final double minPercentageIncrease;
	public final long minAge;
	public final long maxAge;
	public final Pattern mustContain;
	public final Pattern mustNotContain;

	public AllocationStatisticDumpFilter(Arguments args) {
		this.maxFrames = args.getInt(UnsafeMemoryAllocationLogger.MAX_FRAMES, 16);
		this.minSize = args.getSize(UnsafeMemoryAllocationLogger.MIN_SIZE, 0);
		this.minStackSize = args.getSize(UnsafeMemoryAllocationLogger.MIN_STACK_SIZE, 0);
		this.minIncrease = args.getSize(UnsafeMemoryAllocationLogger.MIN_INCREASE, -1);
		this.minPercentageIncrease = 0.01 * args.getLong(UnsafeMemoryAllocationLogger.MIN_PERCENTAGE, 0);
		this.minAge = 1000 * args.getDurationInSeconds(UnsafeMemoryAllocationLogger.MIN_AGE, 0);
		this.maxAge = 1000 * args.getDurationInSeconds(UnsafeMemoryAllocationLogger.MAX_AGE, 365 * 24 * 3600);
		this.mustContain = args.getPattern(UnsafeMemoryAllocationLogger.MUST_CONTAIN, null);
		this.mustNotContain = args.getPattern(UnsafeMemoryAllocationLogger.MUST_NOT_CONTAIN, null);
	}
}
