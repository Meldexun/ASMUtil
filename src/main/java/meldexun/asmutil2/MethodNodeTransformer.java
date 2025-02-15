/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.objectweb.asm.tree.MethodNode;

public class MethodNodeTransformer {

	public static ClassNodeTransformer create(String name, int writeFlags, Consumer<MethodNode> transformer) {
		return create(name, 1, writeFlags, transformer);
	}

	public static ClassNodeTransformer createObf(String name, String obfName, int writeFlags,
			Consumer<MethodNode> transformer) {
		return createObf(name, obfName, 1, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(name, desc, 1, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, String obfName, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(name, obfName, desc, 1, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, String desc, String obfName, String obfDesc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(name, desc, obfName, obfDesc, 1, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(Predicate<MethodNode> predicate, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(predicate, 1, writeFlags, transformer);
	}

	public static ClassNodeTransformer createOptional(String name, int writeFlags, Consumer<MethodNode> transformer) {
		return create(name, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer createObfOptional(String name, String obfName, int writeFlags,
			Consumer<MethodNode> transformer) {
		return createObf(name, obfName, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer createOptional(String name, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(name, desc, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer createOptional(String name, String obfName, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(name, obfName, desc, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer createOptional(String name, String desc, String obfName, String obfDesc,
			int writeFlags, Consumer<MethodNode> transformer) {
		return create(name, desc, obfName, obfDesc, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer createOptional(Predicate<MethodNode> predicate, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(predicate, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(MethodNodeUtil.matching(name), required, writeFlags, transformer);
	}

	public static ClassNodeTransformer createObf(String name, String obfName, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(MethodNodeUtil.matchingObf(name, obfName), required, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, String desc, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(MethodNodeUtil.matching(name, desc), required, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, String obfName, String desc, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(MethodNodeUtil.matching(name, obfName, desc), required, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, String desc, String obfName, String obfDesc, int required,
			int writeFlags, Consumer<MethodNode> transformer) {
		return create(MethodNodeUtil.matching(name, desc, obfName, obfDesc), required, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(Predicate<MethodNode> predicate, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return ClassNodeTransformer.create(writeFlags, classNode -> {
			int transformedMethodCount = 0;
			for (MethodNode method : classNode.methods) {
				if (predicate.test(method)) {
					if (required > 0 && transformedMethodCount >= required) {
						throw new ClassTransformException(
								String.format("Found more method transform targets than expected (%s)", required));
					}
					if (!ASMUtil.DISABLE_LOGGING) {
						ASMUtil.LOGGER.info("Transforming method {}#{}{}", classNode.name, method.name, method.desc);
					}
					transformer.accept(method);
					transformedMethodCount++;
				}
			}
			if (required > 0 && transformedMethodCount < required) {
				throw new ClassTransformException(
						String.format("Found less method transform targets (%s) than expected (%s)",
								transformedMethodCount, required));
			}
			return transformedMethodCount > 0;
		});
	}

}
