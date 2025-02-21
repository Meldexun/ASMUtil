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

	public static ClassNodeTransformer createObf(String name, String obfName, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return createObf(name, obfName, desc, 1, writeFlags, transformer);
	}

	public static ClassNodeTransformer createObf(String name, String desc, String obfName, String obfDesc,
			int writeFlags, Consumer<MethodNode> transformer) {
		return createObf(name, desc, obfName, obfDesc, 1, writeFlags, transformer);
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

	public static ClassNodeTransformer createObfOptional(String name, String obfName, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return createObf(name, obfName, desc, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer createObfOptional(String name, String desc, String obfName, String obfDesc,
			int writeFlags, Consumer<MethodNode> transformer) {
		return createObf(name, desc, obfName, obfDesc, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer createOptional(Predicate<MethodNode> predicate, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(predicate, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(MethodNodeUtil.matching(name), required, writeFlags, transformer,
				MethodNodeUtil.errorDetails(name));
	}

	public static ClassNodeTransformer createObf(String name, String obfName, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(MethodNodeUtil.matchingObf(name, obfName), required, writeFlags, transformer,
				MethodNodeUtil.errorDetailsObf(name, obfName));
	}

	public static ClassNodeTransformer create(String name, String desc, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(MethodNodeUtil.matching(name, desc), required, writeFlags, transformer,
				MethodNodeUtil.errorDetails(name, desc));
	}

	public static ClassNodeTransformer createObf(String name, String obfName, String desc, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(MethodNodeUtil.matchingObf(name, obfName, desc), required, writeFlags, transformer,
				MethodNodeUtil.errorDetailsObf(name, obfName, desc));
	}

	public static ClassNodeTransformer createObf(String name, String desc, String obfName, String obfDesc, int required,
			int writeFlags, Consumer<MethodNode> transformer) {
		return create(MethodNodeUtil.matchingObf(name, desc, obfName, obfDesc), required, writeFlags, transformer,
				MethodNodeUtil.errorDetailsObf(name, desc, obfName, obfDesc));
	}

	@Deprecated
	public static ClassNodeTransformer create(Predicate<MethodNode> predicate, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(predicate, required, writeFlags, transformer, null);
	}

	public static ClassNodeTransformer create(Predicate<MethodNode> predicate, int required, int writeFlags,
			Consumer<MethodNode> transformer, Consumer<StringBuilder> errorDetails) {
		return ClassNodeTransformer.create(writeFlags, classNode -> {
			int transformedMethodCount = 0;
			for (MethodNode method : classNode.methods) {
				if (predicate.test(method)) {
					if (required > 0 && transformedMethodCount >= required) {
						StringBuilder sb = new StringBuilder();
						sb.append("Found more method transform targets than expected!");
						sb.append(" ").append("expected=").append(required);
						if (errorDetails != null) {
							sb.append(" ");
							errorDetails.accept(sb);
						}
						throw new ClassTransformException(sb.toString());
					}
					if (!ASMUtil.DISABLE_LOGGING) {
						ASMUtil.LOGGER.info("Transforming method {}#{}{}", classNode.name, method.name, method.desc);
					}
					transformer.accept(method);
					transformedMethodCount++;
				}
			}
			if (required > 0 && transformedMethodCount < required) {
				StringBuilder sb = new StringBuilder();
				sb.append("Found less method transform targets than expected!");
				sb.append(" ").append("expected=").append(required);
				sb.append(" ").append("found=").append(transformedMethodCount);
				if (errorDetails != null) {
					sb.append(" ");
					errorDetails.accept(sb);
				}
				throw new ClassTransformException(sb.toString());
			}
			return transformedMethodCount > 0;
		});
	}

}
