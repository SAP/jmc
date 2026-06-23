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
