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
