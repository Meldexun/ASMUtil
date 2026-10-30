/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

/**
 * A class transformer with the same signature as LaunchWrapper's
 * {@code net.minecraft.launchwrapper.IClassTransformer}, so that implementations can
 * implement both interfaces at once without this library depending on LaunchWrapper:
 *
 * <pre>
 * public class MyTransformer extends HashMapClassNodeClassTransformer
 *         implements net.minecraft.launchwrapper.IClassTransformer { ... }
 * </pre>
 */
public interface IClassTransformer {

	/**
	 * Transforms a class's bytecode.
	 *
	 * @param obfName the untransformed class name, i.e. the name the class file actually has
	 *        (in an obfuscated Minecraft environment this is the Notch name such as {@code bib})
	 * @param name the transformed (deobfuscated) binary class name with dots, such as
	 *        {@code net.minecraft.client.Minecraft}
	 * @param basicClass the class bytecode as produced by the previous transformer, or {@code null}
	 *        if no class file exists for this name
	 * @return the transformed class bytecode, or {@code basicClass} if nothing was changed
	 */
	byte[] transform(String obfName, String name, byte[] basicClass);

}
