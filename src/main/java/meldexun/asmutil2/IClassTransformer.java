/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

/**
 * Base interface for transforming Java classes at runtime in Minecraft 1.12.2.
 * Implementations modify class bytecode before classes are loaded by the JVM.
 */
public interface IClassTransformer {

	/**
	 * Transforms a class's bytecode.
	 *
	 * @param obfName the obfuscated (SRG) name of the class
	 * @param name the deobfuscated name of the class
	 * @param basicClass the original class bytecode, or null if class doesn't exist
	 * @return the transformed class bytecode, or the original if no transformation occurred
	 */
	byte[] transform(String obfName, String name, byte[] basicClass);

}
