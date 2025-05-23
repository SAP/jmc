/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2025, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.rjmx.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.openjdk.jmc.rjmx.IPropertySyntheticAttribute;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.common.ISyntheticAttribute;
import org.openjdk.jmc.rjmx.common.ISyntheticNotification;
import org.openjdk.jmc.rjmx.common.internal.SyntheticAttributeEntry;
import org.openjdk.jmc.rjmx.common.internal.SyntheticNotificationEntry;
import org.openjdk.jmc.rjmx.common.subscription.MRI;

public class SyntheticRepositoryInitializer {
	public static List<SyntheticAttributeEntry> initializeAttributeEntries() {
		IExtensionRegistry er = Platform.getExtensionRegistry();
		IExtensionPoint ep = er.getExtensionPoint("org.openjdk.jmc.rjmx.syntheticattribute"); //$NON-NLS-1$
		IExtension[] extensions = ep.getExtensions();
		List<SyntheticAttributeEntry> attributeCandidates = new ArrayList<>();
		for (IExtension extension : extensions) {
			IConfigurationElement[] configs = extension.getConfigurationElements();
			for (IConfigurationElement config : configs) {
				if (config.getName().equals("syntheticAttribute")) { //$NON-NLS-1$
					try {
						ISyntheticAttribute attribute = (ISyntheticAttribute) config.createExecutableExtension("class"); //$NON-NLS-1$
						SyntheticAttributeEntry candidate = createAttributeEntry(attribute, config);
						attributeCandidates.add(candidate);
					} catch (CoreException e) {
						RJMXPlugin.getDefault().getLogger().log(Level.SEVERE,
								"Could not instantiate synthetic attribute!", e); //$NON-NLS-1$
					}
				}
			}
		}
		return attributeCandidates;
	}

	private static SyntheticAttributeEntry createAttributeEntry(
		ISyntheticAttribute attribute, IConfigurationElement config) {
		String attributeName = config.getAttribute("attributeName"); //$NON-NLS-1$
		String description = config.getAttribute("description"); //$NON-NLS-1$
		String type = config.getAttribute("type"); //$NON-NLS-1$
		boolean readable = Boolean.valueOf(config.getAttribute("readable")); //$NON-NLS-1$
		boolean writeable = Boolean.valueOf(config.getAttribute("writeable")); //$NON-NLS-1$
		boolean isIs = Boolean.valueOf(config.getAttribute("isIs")); //$NON-NLS-1$
		if (attribute instanceof IPropertySyntheticAttribute) {
			Map<String, Object> properties = parseProperties(config.getChildren("properties")); //$NON-NLS-1$
			((IPropertySyntheticAttribute) attribute).setProperties(properties);
		}
		MRI descriptor = MRI.createFromQualifiedName(attributeName);
		return new SyntheticAttributeEntry(attribute, descriptor, description, type, readable, writeable, isIs);
	}

	private static Map<String, Object> parseProperties(IConfigurationElement[] children) {
		if (children == null || children.length == 0) {
			return Collections.emptyMap();
		}
		Map<String, Object> properties = new HashMap<>();
		for (IConfigurationElement child : children[0].getChildren()) {
			parseProperty(child, properties);
		}
		return properties;
	}

	private static void parseProperty(IConfigurationElement child, Map<String, Object> properties) {
		String key = child.getAttribute("key"); //$NON-NLS-1$
		try {
			if ("string".equals(child.getName())) { //$NON-NLS-1$
				properties.put(key, child.getAttribute("value")); //$NON-NLS-1$
			} else if ("boolean".equals(child.getName())) { //$NON-NLS-1$
				properties.put(key, Boolean.valueOf(child.getAttribute("value"))); //$NON-NLS-1$
			} else if ("integer".equals(child.getName())) { //$NON-NLS-1$
				properties.put(key, Integer.parseInt(child.getAttribute("value"))); //$NON-NLS-1$
			} else if ("float".equals(child.getName())) { //$NON-NLS-1$
				properties.put(key, Float.parseFloat(child.getAttribute("value"))); //$NON-NLS-1$
			}
		} catch (NumberFormatException e) {
			properties.put(key, e.getMessage());
		}
	}

	public static List<SyntheticNotificationEntry> initializeNotificationEntries() {
		IExtensionRegistry er = Platform.getExtensionRegistry();
		IExtensionPoint ep = er.getExtensionPoint("org.openjdk.jmc.rjmx.syntheticnotification"); //$NON-NLS-1$
		IExtension[] extensions = ep.getExtensions();
		List<SyntheticNotificationEntry> notificationCandidates = new ArrayList<>();
		for (IExtension extension : extensions) {
			IConfigurationElement[] configs = extension.getConfigurationElements();
			for (IConfigurationElement config : configs) {
				if (config.getName().equals("syntheticNotification")) { //$NON-NLS-1$
					SyntheticNotificationEntry candidate = createNotificationEntry(config);
					if (candidate != null) {
						notificationCandidates.add(candidate);
					}
				}
			}
		}
		return notificationCandidates;
	}

	private static SyntheticNotificationEntry createNotificationEntry(IConfigurationElement config) {
		String notificationName = config.getAttribute("notificationName"); //$NON-NLS-1$
		try {
			ISyntheticNotification notification = (ISyntheticNotification) config.createExecutableExtension("class"); //$NON-NLS-1$
			String description = config.getAttribute("description"); //$NON-NLS-1$
			String type = config.getAttribute("type"); //$NON-NLS-1$
			String message = config.getAttribute("message"); //$NON-NLS-1$
			MRI descriptor = MRI.createFromQualifiedName(notificationName);
			return new SyntheticNotificationEntry(notification, descriptor, description, type, message);
		} catch (CoreException e) {
			RJMXPlugin.getDefault().getLogger().log(Level.SEVERE,
					"Could not create synthetic notification for " + notificationName, e); //$NON-NLS-1$
			return null;
		}
	}
}
