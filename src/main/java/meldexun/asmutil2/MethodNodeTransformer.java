/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.Objects;
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

	public static ClassNodeTransformer create(String name, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(SignatureMatcher.matchingMethodName(name), required, writeFlags, transformer);
	}

	public static ClassNodeTransformer createObf(String name, String obfName, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(SignatureMatcher.matchingMethodNameObf(name, obfName), required, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, String desc, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(SignatureMatcher.matchingMethodNameDesc(name, desc), required, writeFlags, transformer);
	}

	public static ClassNodeTransformer createObf(String name, String obfName, String desc, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(SignatureMatcher.matchingMethodNameDescObf(name, obfName, desc), required, writeFlags,
				transformer);
	}

	public static ClassNodeTransformer createObf(String name, String desc, String obfName, String obfDesc, int required,
			int writeFlags, Consumer<MethodNode> transformer) {
		return create(SignatureMatcher.matchingMethodNameDescObf(name, obfName, desc, obfDesc), required, writeFlags,
				transformer);
	}

	public static ClassNodeTransformer create(SignatureMatcher<MethodNode> signatureMatcher, int required,
			int writeFlags, Consumer<MethodNode> transformer) {
		return create(signatureMatcher, required, writeFlags, transformer, signatureMatcher);
	}

	public static ClassNodeTransformer create(Predicate<MethodNode> predicate, int required, int writeFlags,
			Consumer<MethodNode> transformer, Consumer<StringBuilder> errorDetails) {
		return builder(predicate, errorDetails).minMatches(required).maxMatches(required).writeFlags(writeFlags)
				.build(transformer);
	}

	public static Builder builder(String name) {
		return builder(SignatureMatcher.matchingMethodName(name));
	}

	public static Builder builder(String name, String desc) {
		return builder(SignatureMatcher.matchingMethodNameDesc(name, desc));
	}

	public static Builder builderObf(String name, String obfName) {
		return builder(SignatureMatcher.matchingMethodNameObf(name, obfName));
	}

	public static Builder builderObf(String name, String obfName, String desc) {
		return builder(SignatureMatcher.matchingMethodNameDescObf(name, obfName, desc));
	}

	public static Builder builderObf(String name, String desc, String obfName, String obfDesc) {
		return builder(SignatureMatcher.matchingMethodNameDescObf(name, obfName, desc, obfDesc));
	}

	public static Builder builder(SignatureMatcher<MethodNode> signatureMatcher) {
		return new Builder(signatureMatcher);
	}

	public static Builder builder(Predicate<MethodNode> methodMatcher, Consumer<StringBuilder> errorDetailAppender) {
		return new Builder(methodMatcher, errorDetailAppender);
	}

	public static class Builder {

		private final Predicate<MethodNode> methodMatcher;
		private final Consumer<StringBuilder> errorDetailAppender;
		private int minMatches = 1;
		private int maxMatches = 1;
		private int writeFlags;

		public Builder(SignatureMatcher<MethodNode> signatureMatcher) {
			this(signatureMatcher, signatureMatcher);
		}

		public Builder(Predicate<MethodNode> methodMatcher, Consumer<StringBuilder> errorDetailAppender) {
			this.methodMatcher = Objects.requireNonNull(methodMatcher);
			this.errorDetailAppender = Objects.requireNonNull(errorDetailAppender);
		}

		public Builder minMatches(int minMatches) {
			this.minMatches = minMatches;
			return this;
		}

		public Builder maxMatches(int maxMatches) {
			this.maxMatches = maxMatches;
			return this;
		}

		public Builder writeFlags(int writeFlags) {
			this.writeFlags = writeFlags;
			return this;
		}

		public ClassNodeTransformer build(Consumer<MethodNode> transformer) {
			return this.build(method -> {
				transformer.accept(method);
				return true;
			});
		}

		public ClassNodeTransformer build(Predicate<MethodNode> transformer) {
			Objects.requireNonNull(transformer);
			Predicate<MethodNode> methodMatcher = this.methodMatcher;
			Consumer<StringBuilder> errorDetailAppender = this.errorDetailAppender;
			int minMatches = this.minMatches;
			int maxMatches = this.maxMatches;
			int writeFlags = this.writeFlags;

			return ClassNodeTransformer.create(writeFlags, classNode -> {
				boolean transformed = false;
				int matches = 0;
				for (MethodNode method : classNode.methods) {
					if (methodMatcher.test(method)) {
						matches++;
						if (maxMatches > 0 && matches > maxMatches) {
							StringBuilder sb = new StringBuilder();
							sb.append("Found more method transform targets than expected!");
							sb.append(" ").append("minMatches=").append(minMatches);
							sb.append(" ").append("maxMatches=").append(maxMatches);
							if (errorDetailAppender != null) {
								sb.append(" ");
								errorDetailAppender.accept(sb);
							}
							throw new ClassTransformException(sb.toString());
						}
						if (!ASMUtil.DISABLE_LOGGING) {
							ASMUtil.LOGGER.info("Transforming method {}.{}{}", classNode.name, method.name,
									method.desc);
						}
						transformed |= transformer.test(method);
					}
				}
				if (matches < minMatches) {
					StringBuilder sb = new StringBuilder();
					sb.append("Found less method transform targets than expected!");
					sb.append(" ").append("minMatches").append(minMatches);
					sb.append(" ").append("maxMatches=").append(maxMatches);
					sb.append(" ").append("matches=").append(matches);
					if (errorDetailAppender != null) {
						sb.append(" ");
						errorDetailAppender.accept(sb);
					}
					throw new ClassTransformException(sb.toString());
				}
				return transformed;
			});
		}

	}

}
