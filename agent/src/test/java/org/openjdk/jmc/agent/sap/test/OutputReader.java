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
package org.openjdk.jmc.agent.sap.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class OutputReader implements Runnable {
	private final StringBuilder out;
	private final InputStream is;

	public OutputReader(InputStream is, StringBuilder out) {
		this.is = is;
		this.out = out;
	}

	public static String[] getLines(CharSequence cs) {
		String raw;

		synchronized (cs) {
			raw = cs.toString();
		}

		return raw.split("[\r\n]+");
	}

	public String[] getLines() {
		return getLines(out);
	}

	public void run() {
		try {
			byte[] buf = new byte[8192];
			int read;

			while ((read = is.read(buf)) > 0) {

				if (read > 0) {
					byte[] part = new byte[read];
					System.arraycopy(buf, 0, part, 0, read);
					String toAppend = new String(part, StandardCharsets.ISO_8859_1);

					synchronized (out) {
						out.append(toAppend);
						out.notifyAll();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
