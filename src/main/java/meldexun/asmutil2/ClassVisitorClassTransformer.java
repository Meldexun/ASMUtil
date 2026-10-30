/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * Class transformer that feeds the class into a {@link ClassVisitor} described by an {@link ITransformInfo}. The
 * class writer is created lazily with {@link #createClassWriter(int)}, so classes that end up unchanged cost no
 * writer.
 * <p>
 * Override {@link #createClassWriter(int)} to change how common super classes are computed for
 * {@link ClassWriter#COMPUTE_FRAMES}, e.g. to pass a {@link meldexun.asmutil2.reader.ClassUtil} that can map between
 * obfuscated and deobfuscated class names.
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
	 * Returns how to transform a class. Called for every class that is loaded, so it should be cheap.
	 *
	 * @param name the transformed binary class name with dots
	 * @return the transform info, or {@code null} if this class should not be transformed
	 */
	protected abstract ITransformInfo<T> getTransformInfo(String name);

	/**
	 * Creates the ClassWriter for a transformed class. Defaults to a {@link NonLoadingClassWriter} using
	 * {@link meldexun.asmutil2.reader.ClassUtil#DEFAULT}.
	 *
	 * @param flags ClassWriter flags (e.g. {@code COMPUTE_FRAMES}, {@code COMPUTE_MAXS})
	 * @return a new ClassWriter instance
	 */
	protected ClassWriter createClassWriter(int flags) {
		return new NonLoadingClassWriter(flags);
	}

}
