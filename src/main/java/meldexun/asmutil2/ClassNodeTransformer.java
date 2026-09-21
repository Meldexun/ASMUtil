/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.objectweb.asm.tree.ClassNode;

/**
 * Functional interface for transforming a {@link ClassNode}.
 * Transformers can be prioritized and are sorted by priority when applied.
 */
public interface ClassNodeTransformer extends Comparable<ClassNodeTransformer> {

	/**
	 * Applies transformation to a ClassNode.
	 *
	 * @param classNode the class to transform
	 * @return true if transformation was applied, false otherwise
	 */
	boolean transform(ClassNode classNode);

	/**
	 * Returns ClassWriter flags needed for bytecode generation.
	 *
	 * @return flags such as COMPUTE_FRAMES or COMPUTE_MAXS
	 */
	int writeFlags();

	/**
	 * Returns the priority of this transformer. Lower values execute first.
	 *
	 * @return the priority value
	 */
	int priority();

	@Override
	default int compareTo(ClassNodeTransformer o) {
		return Integer.compare(priority(), o.priority());
	}

	/**
	 * Creates a ClassNodeTransformer from a simple consumer.
	 *
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a new ClassNodeTransformer
	 */
	static ClassNodeTransformer create(int writeFlags, Consumer<ClassNode> transformer) {
		return create(writeFlags, classNode -> {
			if (!ASMUtil.DISABLE_LOGGING) {
				ASMUtil.LOGGER.info("Transforming class {}", classNode.name);
			}
			transformer.accept(classNode);
			return true;
		});
	}

	/**
	 * Creates a ClassNodeTransformer from a predicate.
	 *
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic that returns whether transformation occurred
	 * @return a new ClassNodeTransformer
	 */
	static ClassNodeTransformer create(int writeFlags, Predicate<ClassNode> transformer) {
		return create(writeFlags, 0, transformer);
	}

	/**
	 * Creates a ClassNodeTransformer with a specific priority.
	 *
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param priority the priority (lower values execute first)
	 * @param transformer the transformation logic
	 * @return a new ClassNodeTransformer
	 */
	static ClassNodeTransformer create(int writeFlags, int priority, Predicate<ClassNode> transformer) {
		return new ClassNodeTransformer() {

			@Override
			public boolean transform(ClassNode classNode) {
				if (!ASMUtil.DISABLE_LOGGING) {
					ASMUtil.LOGGER.info("Transforming class {}", classNode.name);
				}
				return transformer.test(classNode);
			}

			@Override
			public int writeFlags() {
				return writeFlags;
			}

			@Override
			public int priority() {
				return priority;
			}

		};
	}

}
