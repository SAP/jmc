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
package org.openjdk.jmc.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;

public class SapTransformRegistry implements TransformRegistry {

	private TransformRegistry registry;
	private final HashMap<String, List<TransformDescriptor>> modifiedTransforms = new HashMap<>();

	private void modifyTransformations() {
		modifiedTransforms.clear();

		for (String className : getClassNames()) {
			String simpleName = className.substring(className.lastIndexOf('/') + 1);
			List<TransformDescriptor> descs = getTransformData(className);
			List<TransformDescriptor> modifiedDescs = null;

			for (int i = 0; i < descs.size(); ++i) {
				JFRTransformDescriptor desc = (JFRTransformDescriptor) descs.get(i);
				JFRTransformDescriptor modified = null;

				if (simpleName.equals(desc.getMethod().getName())) {
					Method modifiedMethod = new Method("<init>", desc.getMethod().getSignature());
					modified = new JFRTransformDescriptor(desc.getId(), desc.getClassName(), modifiedMethod,
							desc.getTransformationAttributes(), desc.getParameters(), desc.getReturnValue(),
							desc.getFields());
				}

				if (modified != null) {
					if (modifiedDescs == null) {
						modifiedDescs = new ArrayList<TransformDescriptor>(descs);
					}

					modifiedDescs.set(i, modified);
				}
			}

			if (modifiedDescs != null) {
				modifiedTransforms.put(className, modifiedDescs);
			}
		}
	}

	public SapTransformRegistry(TransformRegistry registry) {
		this.registry = registry;
		modifyTransformations();
	}

	@Override
	public boolean hasPendingTransforms(String className) {
		return registry.hasPendingTransforms(className);
	}

	@Override
	public List<TransformDescriptor> getTransformData(String className) {
		List<TransformDescriptor> modified = modifiedTransforms.get(className);

		if (modified != null) {
			return modified;
		}

		return registry.getTransformData(className);
	}

	@Override
	public Set<String> getClassNames() {
		return registry.getClassNames();
	}

	@Override
	public String getCurrentConfiguration() {
		return registry.getCurrentConfiguration();
	}

	@Override
	public void setCurrentConfiguration(String xmlDescription) {
		registry.setCurrentConfiguration(xmlDescription);
		modifyTransformations();
	}

	@Override
	public Set<String> modify(String xmlDescription) throws XMLValidationException {
		Set<String> result = registry.modify(xmlDescription);
		modifyTransformations();

		return result;
	}

	@Override
	public Set<String> clearAllTransformData() {
		Set<String> result = registry.clearAllTransformData();
		modifyTransformations();

		return result;
	}

	@Override
	public void setRevertInstrumentation(boolean shouldRevert) {
		registry.setRevertInstrumentation(shouldRevert);
	}

	@Override
	public boolean isRevertIntrumentation() {
		return registry.isRevertIntrumentation();
	}
}
