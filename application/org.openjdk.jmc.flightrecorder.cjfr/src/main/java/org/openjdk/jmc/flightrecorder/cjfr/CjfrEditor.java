/*
 * Copyright (c) 2026, SAP SE and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
 */
package org.openjdk.jmc.flightrecorder.cjfr;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.swt.widgets.Composite;
import org.openjdk.jmc.flightrecorder.ui.JfrEditor;
import org.openjdk.jmc.ui.MCPathEditorInput;

import me.bechberger.condensed.CondensedInputStream;
import me.bechberger.jfr.BasicJFRReader;
import me.bechberger.jfr.WritingJFRReader;

/**
 * Eclipse editor for .cjfr (condensed JFR) files. Inflates the recording to a temporary .jfr file
 * and delegates to the standard JFR loading machinery.
 */
public class CjfrEditor extends JfrEditor {

	public static final String EDITOR_ID = "org.openjdk.jmc.flightrecorder.cjfr.CjfrEditor"; //$NON-NLS-1$

	@Override
	public void createPartControl(Composite parent) {
		File cjfrFile = MCPathEditorInput.getFile(getEditorInput());
		try {
			File tempJfr = File.createTempFile("cjfr-inflate-", ".jfr"); //$NON-NLS-1$ //$NON-NLS-2$
			tempJfr.deleteOnExit();
			try (CondensedInputStream cin = new CondensedInputStream(
					new BufferedInputStream(new FileInputStream(cjfrFile)));
					FileOutputStream fos = new FileOutputStream(tempJfr)) {
				WritingJFRReader.toJFRStream(new BasicJFRReader(cin), fos);
			}
			setInput(new MCPathEditorInput(tempJfr, false));
		} catch (IOException e) {
			// Fall through: let RecordingLoader fail gracefully on the original file
		}
		super.createPartControl(parent);
	}
}
