/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public interface IClassTransformerRegistry {

	void add(String className, ClassNodeTransformer transformer);

	default void add(String className, int writeFlags, Consumer<ClassNode> transformer) {
		this.add(className, ClassNodeTransformer.create(writeFlags, transformer));
	}

	default void add(String className, int writeFlags, Predicate<ClassNode> transformer) {
		this.add(className, ClassNodeTransformer.create(writeFlags, transformer));
	}

	default void add(String className, String name, int writeFlags, Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.create(name, writeFlags, transformer));
	}

	default void addObf(String className, String name, String obfName, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.createObf(name, obfName, writeFlags, transformer));
	}

	default void add(String className, String name, String desc, int writeFlags, Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.create(name, desc, writeFlags, transformer));
	}

	default void add(String className, String name, String obfName, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.create(name, obfName, desc, writeFlags, transformer));
	}

	default void add(String className, String name, String desc, String obfName, String obfDesc, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.create(name, desc, obfName, obfDesc, writeFlags, transformer));
	}

	default void addOptional(String className, String name, int writeFlags, Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.createOptional(name, writeFlags, transformer));
	}

	default void addObfOptional(String className, String name, String obfName, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.createObfOptional(name, obfName, writeFlags, transformer));
	}

	default void addOptional(String className, String name, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.createOptional(name, desc, writeFlags, transformer));
	}

	default void addOptional(String className, String name, String obfName, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.createOptional(name, obfName, desc, writeFlags, transformer));
	}

	default void addOptional(String className, String name, String desc, String obfName, String obfDesc, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className,
				MethodNodeTransformer.createOptional(name, desc, obfName, obfDesc, writeFlags, transformer));
	}

	default void add(String className, String name, String desc, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.create(name, desc, required, writeFlags, transformer));
	}

	default void add(String className, String name, String obfName, String desc, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.create(name, obfName, desc, required, writeFlags, transformer));
	}

	default void add(String className, String name, String desc, String obfName, String obfDesc, int required,
			int writeFlags, Consumer<MethodNode> transformer) {
		this.add(className,
				MethodNodeTransformer.create(name, desc, obfName, obfDesc, required, writeFlags, transformer));
	}

}
