/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

/**
 * Base implementation of {@link IClassTransformer}. Subclasses implement
 * {@link #transformOrNull(String, String, byte[])}; this class takes care of the rest:
 * <ul>
 * <li>any exception thrown while transforming is logged and rethrown as a {@link ClassTransformException}
 * (crashing the game instead of silently loading an untransformed class)</li>
 * <li>a {@code null} result is turned back into the untouched input bytes</li>
 * <li>transformed classes are written to {@code .meldexun/asm/export} when the JVM is started with
 * {@code -Dmeldexun.asm.export=true}</li>
 * </ul>
 */
public abstract class AbstractClassTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String obfName, String name, byte[] basicClass) {
		byte[] transformedClass;
		try {
			transformedClass = this.transformOrNull(obfName, name, basicClass);
		} catch (Exception e) {
			String errorMessage = String.format("Failed transforming class: %s", name);
			ASMUtil.LOGGER.error(errorMessage, e);
			throw new ClassTransformException(errorMessage, e);
		}
		if (transformedClass == null) {
			return basicClass;
		}
		ASMUtil.exportIfEnabled(name, transformedClass);
		return transformedClass;
	}

	/**
	 * Performs the actual class transformation.
	 *
	 * @param obfName the untransformed class name (see {@link IClassTransformer#transform(String, String, byte[])})
	 * @param name the transformed (deobfuscated) binary class name with dots
	 * @param basicClass the class bytecode, or {@code null} if no class file exists for this name
	 * @return {@code null} if no transformation occurred, otherwise the transformed class bytecode
	 */
	protected abstract byte[] transformOrNull(String obfName, String name, byte[] basicClass);

}
