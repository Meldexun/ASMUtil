/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.List;
import java.util.function.Supplier;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public abstract class ClassNodeClassTransformer extends ClassVisitorClassTransformer<ClassNode> {

	@Override
	protected ITransformInfo<ClassNode> getTransformInfo(String name) {
		List<ClassNodeTransformer> transformers = this.getClassNodeTransformers(name);
		if (transformers == null || transformers.isEmpty()) {
			return null;
		}
		int writeFlags = ReductionUtil.intOr(transformers, ClassNodeTransformer::writeFlags);
		int readFlags = (writeFlags & ClassWriter.COMPUTE_FRAMES) != 0 ? ClassReader.SKIP_FRAMES : 0;
		return ITransformInfo.create((Supplier<ClassNode>) ClassNode::new,
				classNode -> ReductionUtil.booleanOr(transformers, classNode, ClassNodeTransformer::transform),
				writeFlags, readFlags);
	}

	protected abstract List<ClassNodeTransformer> getClassNodeTransformers(String className);

}
