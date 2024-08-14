/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.objectweb.asm.tree.ClassNode;

public interface ClassNodeTransformer {

	boolean transform(ClassNode classNode);

	int writeFlags();

	static ClassNodeTransformer create(int writeFlags, Consumer<ClassNode> transformer) {
		return create(writeFlags, classNode -> {
			if (!ASMUtil.DISABLE_LOGGING) {
				ASMUtil.LOGGER.info("Transforming class {}", classNode.name);
			}
			transformer.accept(classNode);
			return true;
		});
	}

	static ClassNodeTransformer create(int writeFlags, Predicate<ClassNode> transformer) {
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

		};
	}

}
