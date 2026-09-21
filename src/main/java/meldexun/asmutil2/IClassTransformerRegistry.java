/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Registry interface for adding class and method transformers.
 * Provides convenient methods for registering transformers with various configurations
 * including support for obfuscated method names and optional transformations.
 */
public interface IClassTransformerRegistry {

	/**
	 * Adds a class transformer for the specified class.
	 *
	 * @param className the fully qualified class name
	 * @param transformer the transformer to apply
	 */
	void add(String className, ClassNodeTransformer transformer);

	/**
	 * Adds a class transformer using a simple consumer.
	 *
	 * @param className the fully qualified class name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 */
	default void add(String className, int writeFlags, Consumer<ClassNode> transformer) {
		this.add(className, ClassNodeTransformer.create(writeFlags, transformer));
	}

	/**
	 * Adds a class transformer using a predicate that returns whether transformation occurred.
	 *
	 * @param className the fully qualified class name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 */
	default void add(String className, int writeFlags, Predicate<ClassNode> transformer) {
		this.add(className, ClassNodeTransformer.create(writeFlags, transformer));
	}

	/**
	 * Adds a method transformer for a method with the specified name.
	 *
	 * @param className the fully qualified class name
	 * @param name the method name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 */
	default void add(String className, String name, int writeFlags, Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.create(name, writeFlags, transformer));
	}

	/**
	 * Adds a method transformer with support for obfuscated method names.
	 *
	 * @param className the fully qualified class name
	 * @param name the deobfuscated method name
	 * @param obfName the obfuscated (SRG) method name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 */
	default void addObf(String className, String name, String obfName, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.createObf(name, obfName, writeFlags, transformer));
	}

	/**
	 * Adds a method transformer matching both name and descriptor.
	 *
	 * @param className the fully qualified class name
	 * @param name the method name
	 * @param desc the method descriptor (e.g., "(Ljava/lang/String;)V")
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 */
	default void add(String className, String name, String desc, int writeFlags, Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.create(name, desc, writeFlags, transformer));
	}

	default void addObf(String className, String name, String obfName, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.createObf(name, obfName, desc, writeFlags, transformer));
	}

	default void addObf(String className, String name, String desc, String obfName, String obfDesc, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.createObf(name, desc, obfName, obfDesc, writeFlags, transformer));
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

	default void addObfOptional(String className, String name, String obfName, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.createObfOptional(name, obfName, desc, writeFlags, transformer));
	}

	default void addObfOptional(String className, String name, String desc, String obfName, String obfDesc,
			int writeFlags, Consumer<MethodNode> transformer) {
		this.add(className,
				MethodNodeTransformer.createObfOptional(name, desc, obfName, obfDesc, writeFlags, transformer));
	}

	default void add(String className, String name, int required, int writeFlags, Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.create(name, required, writeFlags, transformer));
	}

	default void addObf(String className, String name, String obfName, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.createObf(name, obfName, required, writeFlags, transformer));
	}

	default void add(String className, String name, String desc, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.create(name, desc, required, writeFlags, transformer));
	}

	default void addObf(String className, String name, String obfName, String desc, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.createObf(name, obfName, desc, required, writeFlags, transformer));
	}

	default void addObf(String className, String name, String desc, String obfName, String obfDesc, int required,
			int writeFlags, Consumer<MethodNode> transformer) {
		this.add(className,
				MethodNodeTransformer.createObf(name, desc, obfName, obfDesc, required, writeFlags, transformer));
	}

}
