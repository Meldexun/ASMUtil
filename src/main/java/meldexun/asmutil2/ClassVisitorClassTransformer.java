/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public abstract class ClassVisitorClassTransformer<T extends ClassVisitor> extends AbstractClassTransformer {

	@Override
	public byte[] transformOrNull(String obfName, String name, byte[] basicClass) {
		ITransformInfo<T> transformInfo = this.getTransformInfo(name);
		if (transformInfo == null) {
			return null;
		}
		ClassReader classReader = new ClassReader(basicClass);
		Lazy<ClassWriter> classWriter = new Lazy<>(() -> this.createClassWriter(transformInfo.writeFlags()));
		T classVisitor = transformInfo.visitor(classWriter);
		classReader.accept(classVisitor, transformInfo.readFlags());
		if (!transformInfo.transform(classVisitor, classWriter)) {
			return null;
		}
		return classWriter.get().toByteArray();
	}

	protected abstract ITransformInfo<T> getTransformInfo(String name);

	protected ClassWriter createClassWriter(int flags) {
		return new NonLoadingClassWriter(flags);
	}

}
