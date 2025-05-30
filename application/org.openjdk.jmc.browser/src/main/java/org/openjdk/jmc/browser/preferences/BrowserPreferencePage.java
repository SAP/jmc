/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.browser.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.openjdk.jmc.browser.JVMBrowserPlugin;
import org.openjdk.jmc.ui.preferences.PreferencesToolkit;

/**
 * Preferences contribution for the Browser.
 */
public class BrowserPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Constructor.
	 */
	public BrowserPreferencePage() {
		super(FLAT);
		setPreferenceStore(JVMBrowserPlugin.getDefault().getPreferenceStore());
		setDescription(Messages.BrowserPreferencePage_BROWSER_PREFERENCES_DESCRIPTION);
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to
	 * manipulate various types of preferences. Each field editor knows how to save and restore
	 * itself.
	 */
	@Override
	public void createFieldEditors() {
		addField(new IntegerFieldEditor(PreferenceConstants.P_HIGHLIGHT_TIME,
				Messages.BrowserPreferencePage_CAPTION_HIGHLIGHT_TIME, getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_DELETES,
				Messages.BrowserPreferencePage_CAPTION_CONFIRM_DELETES, getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.P_WARN_NO_LOCAL_JVMs,
				org.openjdk.jmc.browser.views.Messages.JVMBrowserView_NO_LOCAL_JVMS_WARN_PREFERENCE,
				getFieldEditorParent()));
		PreferencesToolkit.fillNoteControl(getFieldEditorParent(),
				org.openjdk.jmc.browser.views.Messages.JVMBrowserView_NO_LOCAL_JVMS_WARN_CAUSE);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Not needed.
	}
}
