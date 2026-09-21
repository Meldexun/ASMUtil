/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * Class transformer that uses ASM's visitor pattern for class transformation.
 * Uses {@link ClassReader} to read bytecode and {@link ClassVisitor} to apply transformations.
 *
 * @param <T> the type of ClassVisitor used for transformation
 */
public abstract class ClassVisitorClassTransformer<T extends ClassVisitor> extends AbstractClassTransformer {

	@Override
	public byte[] transformOrNull(String obfName, String name, byte[] basicClass) {
		ITransformInfo<T> transformInfo = this.getTransformInfo(name);
		if (transformInfo == null) {
			return null;
		}
		if (basicClass == null) {
			ASMUtil.LOGGER.debug("Skipping transformation of non-existing class {}", name);
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

	/**
	 * Provides transformation information for a specific class.
	 *
	 * @param name the class name to transform
	 * @return the transform info, or null if this class should not be transformed
	 */
	protected abstract ITransformInfo<T> getTransformInfo(String name);

	/**
	 * Creates a ClassWriter for outputting transformed bytecode.
	 *
	 * @param flags ClassWriter flags (e.g., COMPUTE_FRAMES, COMPUTE_MAXS)
	 * @return a new ClassWriter instance
	 */
	protected ClassWriter createClassWriter(int flags) {
		return new NonLoadingClassWriter(flags);
	}

}
