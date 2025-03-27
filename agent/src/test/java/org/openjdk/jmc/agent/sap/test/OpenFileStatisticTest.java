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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class OpenFileStatisticTest extends TestBase {

	public static void main(String[] args) {
		new OpenFileStatisticTest().dispatch(args);
	}

	private static void deleteTestFiles() {
		for (int i = 1; i <= 6; ++i) {
			File file = getFile(i);

			while (file.exists()) {
				file.delete();
			}
		}
	}

	@Override
	protected void runAllTests() throws Exception {
		JavaAgentRunner runner = getRunner("traceOpenFiles,logDest=stdout");
		runner.start("test");
		runner.waitForDone(1);
		runner.loadAgent("dump=openFiles,logDest=stderr");

		if (!smokeTestsOnly()) {
			runner.waitForDone();
			runner.loadAgent("dump=openFiles,logDest=stdout");
		}

		runner.kill();

		String[] stderr = runner.getStderrLines();
		assertLinesContains(stderr, getFileName(1) + "', mode 'w'");
		assertLinesContains(stderr, getFileName(2) + "', mode 'wa'");
		assertLinesContains(stderr, getFileName(1) + "', mode 'r'");
		assertLinesContains(stderr, getFileName(3) + "', mode 'w'");
		assertLinesContains(stderr, getFileName(4) + "', mode 'wa'");
		assertLinesContains(stderr, getFileName(2) + "', mode 'r'");
		assertLinesContains(stderr, getFileName(5) + "', mode 'rw'");
		assertLinesContains(stderr, getFileName(5) + "', mode 'r'");
		assertLinesContains(stderr, getFileName(6) + "', mode 'rw'");
		assertLinesContains(stderr, getFileName(6) + "', mode 'r'");
		assertLinesContainsRegExp(stderr, "Printed [0-9]+ of [0-9][0-9]+ file.* currently opened");

		if (!smokeTestsOnly()) {
			assertLinesNotContains(runner.getStdoutLines(), getFileName(1));
		}

		deleteTestFiles();
	}

	public static String getFileName(int index) {
		return "testopen" + index + ".txt";
	}

	public static File getFile(int index) {
		return new File(getFileName(index));
	}

	@SuppressWarnings("resource")
	public void test() throws IOException {
		FileOutputStream fos1 = new FileOutputStream(getFileName(1));
		FileOutputStream fos2 = new FileOutputStream(getFileName(2), true);
		FileInputStream fis1 = new FileInputStream(getFileName(1));
		FileOutputStream fos3 = new FileOutputStream(getFile(3));
		FileOutputStream fos4 = new FileOutputStream(getFile(4), true);
		FileInputStream fis2 = new FileInputStream(getFile(2));
		RandomAccessFile raf1 = new RandomAccessFile(getFileName(5), "rw");
		RandomAccessFile raf2 = new RandomAccessFile(getFileName(5), "r");
		RandomAccessFile raf3 = new RandomAccessFile(getFile(6), "rw");
		RandomAccessFile raf4 = new RandomAccessFile(getFile(6), "r");
		FileChannel fc = FileChannel.open(getFile(1).toPath());
		fc.read(ByteBuffer.allocate(10));

		done(1, 3000);

		fos1.close();
		fos2.close();
		fos3.close();
		fos4.close();
		fis1.close();
		fis2.close();
		raf1.close();
		raf2.close();
		raf3.close();
		raf4.close();
		fc.close();

		FileInputStream dummy = null;
		deleteTestFiles();

		try {
			dummy = new FileInputStream(getFileName(1));
			throw new RuntimeException("Should not be able to open the file");
		} catch (FileNotFoundException e) {
			// This is what we expect.
		}

		done();
		assertNotNull(dummy);
	}
}
