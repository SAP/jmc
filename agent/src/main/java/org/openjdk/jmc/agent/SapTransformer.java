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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.util.TypeUtils;

public class SapTransformer implements ClassFileTransformer {

	private final Transformer impl;
	private final Module jfrModule;
	private TransformRegistry registry;

	public SapTransformer(TransformRegistry registry) {
		this.registry = registry;
		jfrModule = ModuleLayer.boot().findModule("jdk.jfr").get();
		impl = new Transformer(new SapTransformRegistry(registry));
	}

	private void grantJfrAccessToModule(
		Module module, ClassLoader loader, String className, ProtectionDomain protectionDomain)
			throws IllegalClassFormatException {
		// We need to access the jfr module.
		if (!module.canRead(jfrModule)) {
			// Create a class in the module which grants the access.
			ClassWriter cw = new ClassWriter(0);
			String name = className + "_$MakeJFRModuleReadable";
			cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, name, null, "java/lang/Object", null);

			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
			mv.visitCode();
			mv.visitLdcInsn(Type.getObjectType(name));
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getModule", "()Ljava/lang/Module;", false);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/ModuleLayer", "boot", "()Ljava/lang/ModuleLayer;",
					false);
			mv.visitLdcInsn("jdk.jfr");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ModuleLayer", "findModule",
					"(Ljava/lang/String;)Ljava/util/Optional;", false);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;", false);
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Module");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Module", "addReads",
					"(Ljava/lang/Module;)Ljava/lang/Module;", false);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(2, 0);
			mv.visitEnd();

			cw.visitEnd();
			byte[] bytes = cw.toByteArray();

			try {
				TypeUtils.defineClass(name.replace('/', '.'), bytes, 0, bytes.length, loader, protectionDomain);
				// Trigger clinit to invoke the code. Needs no special permissions.
				Class.forName(name.replace('/', '.'), true, loader);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public byte[] transform(
		ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
		byte[] classfileBuffer) throws IllegalClassFormatException {
		if (registry.getTransformData(className).isEmpty()) {
			return null;
		}

		if (classBeingRedefined != null) {
			grantJfrAccessToModule(classBeingRedefined.getModule(), loader, className, protectionDomain);
		}

		return impl.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
	}

	@Override
	public byte[] transform(
		Module module, ClassLoader loader, String className, Class<?> classBeingRedefined,
		ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if (registry.getTransformData(className).isEmpty()) {
			return null;
		}

		grantJfrAccessToModule(module, loader, className, protectionDomain);

		return impl.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
	}
}
