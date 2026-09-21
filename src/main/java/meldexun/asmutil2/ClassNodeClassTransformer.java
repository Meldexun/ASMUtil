/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/**
 * Class transformer that uses ASM's tree API for class transformation.
 * Works with {@link ClassNode} objects which provide a complete in-memory representation
 * of a class that can be easily inspected and modified.
 */
public abstract class ClassNodeClassTransformer extends ClassVisitorClassTransformer<ClassNode> {

	@Override
	protected ITransformInfo<ClassNode> getTransformInfo(String name) {
		List<ClassNodeTransformer> transformers = this.getClassNodeTransformers(name);
		if (transformers == null || transformers.isEmpty()) {
			return null;
		}
		return new ITransformInfo<ClassNode>() {
			private int writeFlags;

			@Override
			public ClassNode visitor(Lazy<ClassWriter> classWriter) {
				return new ClassNode();
			}

			@Override
			public boolean transform(ClassNode classVisitor, Lazy<ClassWriter> classWriter) {
				boolean transformed = false;
				for (ClassNodeTransformer transformer : transformers) {
					if (transformer.transform(classVisitor)) {
						this.writeFlags |= transformer.writeFlags();
						transformed = true;
					}
				}
				if (transformed) {
					classVisitor.accept(classWriter.get());
				}
				return transformed;
			}

			@Override
			public int writeFlags() {
				return this.writeFlags;
			}

			@Override
			public int readFlags() {
				return 0;
			}
		};
	}

	/**
	 * Provides the list of transformers to apply to a specific class.
	 *
	 * @param className the class name to get transformers for
	 * @return list of transformers, or null/empty if no transformations needed
	 */
	protected abstract List<ClassNodeTransformer> getClassNodeTransformers(String className);

}
