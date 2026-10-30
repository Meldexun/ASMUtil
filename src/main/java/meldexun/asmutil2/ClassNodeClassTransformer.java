/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/**
 * Class transformer based on the ASM tree API. For every class, {@link #getClassNodeTransformers(String)} decides
 * which {@link ClassNodeTransformer}s apply. If there are any, the class is read into a single {@link ClassNode},
 * all transformers are applied in list order, and the class is written once if at least one of them reported a
 * change, using the union of their write flags. Classes without transformers are not even parsed.
 * <p>
 * Most users want {@link HashMapClassNodeClassTransformer}; extend this class directly to select transformers by
 * something other than the exact class name (e.g. return the same list for every class and decide inside the
 * transformer).
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
	 * Returns the transformers for a class. Called for every class that is loaded, so it should be cheap.
	 *
	 * @param className the transformed binary class name with dots
	 * @return the transformers in the order they are applied, or {@code null}/an empty list to skip the class
	 */
	protected abstract List<ClassNodeTransformer> getClassNodeTransformers(String className);

}
