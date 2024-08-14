/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

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
		return transformedClass;
	}

	/**
	 * @return {@code null} if no transformation occured, otherwise the transformed
	 *         class
	 */
	protected abstract byte[] transformOrNull(String obfName, String name, byte[] basicClass);

}
