/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

/**
 * Base implementation of {@link IClassTransformer} that provides error handling,
 * logging, and optional class export functionality.
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
	 * Performs the actual class transformation. Subclasses implement this to define transformation logic.
	 *
	 * @param obfName the obfuscated (SRG) name of the class
	 * @param name the deobfuscated name of the class
	 * @param basicClass the original class bytecode
	 * @return {@code null} if no transformation occurred, otherwise the transformed class bytecode
	 */
	protected abstract byte[] transformOrNull(String obfName, String name, byte[] basicClass);

}
