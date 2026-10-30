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
 * Collects the transformers of a {@link HashMapClassNodeClassTransformer}. Only
 * {@link #add(String, ClassNodeTransformer)} has to be implemented; the other methods are shortcuts that create the
 * transformer via {@link ClassNodeTransformer} or {@link MethodNodeTransformer} (see there for the meaning of
 * {@code Obf}, {@code Optional} and {@code required}).
 * <p>
 * Class names are binary names with dots as passed to {@link IClassTransformer#transform(String, String, byte[])} as
 * {@code name}, e.g. {@code com.example.Foo} or {@code com.example.Foo$Inner}. Names inside the transformer
 * (owners, descriptors) are internal names with slashes, as everywhere in ASM.
 *
 * <pre>
 * registry.add("com.example.Foo", "bar", "(I)V", ClassWriter.COMPUTE_FRAMES, methodNode -&gt; {
 *     methodNode.instructions.insert(...);
 * });
 * </pre>
 */
public interface IClassTransformerRegistry {

	/**
	 * Adds a transformer for the specified class.
	 *
	 * @param className the binary class name, e.g. {@code com.example.Foo$Inner}
	 * @param transformer the transformer to apply
	 */
	void add(String className, ClassNodeTransformer transformer);

	/**
	 * Adds a class transformer that always reports the class as changed.
	 *
	 * @param className the binary class name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 */
	default void add(String className, int writeFlags, Consumer<ClassNode> transformer) {
		this.add(className, ClassNodeTransformer.create(writeFlags, transformer));
	}

	/**
	 * Adds a class transformer that returns whether it changed the class.
	 *
	 * @param className the binary class name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 */
	default void add(String className, int writeFlags, Predicate<ClassNode> transformer) {
		this.add(className, ClassNodeTransformer.create(writeFlags, transformer));
	}

	/**
	 * Adds a transformer for the single method with the given name.
	 *
	 * @param className the binary class name
	 * @param name the method name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 */
	default void add(String className, String name, int writeFlags, Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.create(name, writeFlags, transformer));
	}

	/**
	 * Adds a transformer for the single method named either {@code name} or {@code obfName}.
	 *
	 * @param className the binary class name
	 * @param name the deobfuscated method name
	 * @param obfName the obfuscated method name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 */
	default void addObf(String className, String name, String obfName, int writeFlags,
			Consumer<MethodNode> transformer) {
		this.add(className, MethodNodeTransformer.createObf(name, obfName, writeFlags, transformer));
	}

	/**
	 * Adds a transformer for the method with the given name and descriptor.
	 *
	 * @param className the binary class name
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
