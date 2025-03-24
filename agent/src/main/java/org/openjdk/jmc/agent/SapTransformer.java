/*
 * Copyright (c) 2024 SAP SE. All rights reserved.
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
