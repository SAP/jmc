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
