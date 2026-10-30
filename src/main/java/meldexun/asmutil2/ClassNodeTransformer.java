/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.objectweb.asm.tree.ClassNode;

/**
 * A transformation of a {@link ClassNode}, together with the {@link org.objectweb.asm.ClassWriter} flags it needs
 * and a priority. Usually created with {@link #create(int, Consumer)}, {@link MethodNodeTransformer} or the
 * {@link IClassTransformerRegistry} methods rather than implemented directly.
 * <p>
 * When several transformers target the same class (see {@link ClassNodeClassTransformer}) they all work on the same
 * ClassNode, and the class is written once with the union of the write flags of all transformers that reported a
 * change.
 */
public interface ClassNodeTransformer extends Comparable<ClassNodeTransformer> {

	/**
	 * Transforms the class in place.
	 *
	 * @param classNode the class to transform
	 * @return {@code true} if the class was changed. If no transformer of a class returns {@code true}, the original
	 *         bytes are kept and any modifications made to the ClassNode are discarded.
	 */
	boolean transform(ClassNode classNode);

	/**
	 * Returns the {@link org.objectweb.asm.ClassWriter} flags needed after this transformation, e.g.
	 * {@code COMPUTE_FRAMES} when jumps were added or {@code COMPUTE_MAXS} when the stack got deeper. {@code 0}
	 * means the transformer keeps {@code maxStack}, {@code maxLocals} and the stack map frames valid itself.
	 *
	 * @return the write flags
	 */
	int writeFlags();

	/**
	 * Returns the priority of this transformer. Lower values run first.
	 *
	 * @return the priority
	 */
	int priority();

	@Override
	default int compareTo(ClassNodeTransformer o) {
		return Integer.compare(priority(), o.priority());
	}

	/**
	 * Creates a transformer with priority 0 that always reports the class as changed.
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
	 * Creates a transformer with priority 0.
	 *
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic, returning whether it changed the class
	 * @return a new ClassNodeTransformer
	 */
	static ClassNodeTransformer create(int writeFlags, Predicate<ClassNode> transformer) {
		return create(writeFlags, 0, transformer);
	}

	/**
	 * Creates a transformer with the given priority.
	 *
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param priority the priority (lower values run first)
	 * @param transformer the transformation logic, returning whether it changed the class
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
