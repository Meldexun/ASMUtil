/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.objectweb.asm.tree.MethodNode;

/**
 * Factory class for creating {@link ClassNodeTransformer} instances that target specific methods.
 * Provides convenience methods for method transformations with support for obfuscated names,
 * optional methods, and flexible matching criteria.
 */
public class MethodNodeTransformer {

	/**
	 * Creates a transformer for a method by name.
	 *
	 * @param name the method name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching method
	 */
	public static ClassNodeTransformer create(String name, int writeFlags, Consumer<MethodNode> transformer) {
		return create(name, 1, writeFlags, transformer);
	}

	/**
	 * Creates a transformer for a method by name with obfuscation support.
	 *
	 * @param name the deobfuscated method name
	 * @param obfName the obfuscated (SRG) method name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching method
	 */
	public static ClassNodeTransformer createObf(String name, String obfName, int writeFlags,
			Consumer<MethodNode> transformer) {
		return createObf(name, obfName, 1, writeFlags, transformer);
	}

	/**
	 * Creates a transformer for a method by name and descriptor.
	 *
	 * @param name the method name
	 * @param desc the method descriptor (e.g., "(Ljava/lang/String;)V")
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching method
	 */
	public static ClassNodeTransformer create(String name, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(name, desc, 1, writeFlags, transformer);
	}

	/**
	 * Creates a transformer for a method by name and descriptor with obfuscation support.
	 *
	 * @param name the deobfuscated method name
	 * @param obfName the obfuscated (SRG) method name
	 * @param desc the method descriptor
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching method
	 */
	public static ClassNodeTransformer createObf(String name, String obfName, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return createObf(name, obfName, desc, 1, writeFlags, transformer);
	}

	/**
	 * Creates a transformer for a method with full obfuscation support for both name and descriptor.
	 *
	 * @param name the deobfuscated method name
	 * @param desc the deobfuscated method descriptor
	 * @param obfName the obfuscated (SRG) method name
	 * @param obfDesc the obfuscated method descriptor
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching method
	 */
	public static ClassNodeTransformer createObf(String name, String desc, String obfName, String obfDesc,
			int writeFlags, Consumer<MethodNode> transformer) {
		return createObf(name, desc, obfName, obfDesc, 1, writeFlags, transformer);
	}

	/**
	 * Creates a transformer for an optional method by name (does not fail if method is not found).
	 *
	 * @param name the method name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching method if found
	 */
	public static ClassNodeTransformer createOptional(String name, int writeFlags, Consumer<MethodNode> transformer) {
		return create(name, 0, writeFlags, transformer);
	}

	/**
	 * Creates a transformer for an optional method with obfuscation support.
	 *
	 * @param name the deobfuscated method name
	 * @param obfName the obfuscated (SRG) method name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching method if found
	 */
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

	/**
	 * Creates a transformer for a method with configurable required match count.
	 *
	 * @param name the method name
	 * @param required the required number of matches (0 for optional)
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching method
	 */
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

	/**
	 * Creates a transformer using a signature matcher with configurable required match count.
	 *
	 * @param signatureMatcher the matcher to find the method
	 * @param required the required number of matches (0 for optional)
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching method
	 */
	public static ClassNodeTransformer create(SignatureMatcher<MethodNode> signatureMatcher, int required,
			int writeFlags, Consumer<MethodNode> transformer) {
		return create(signatureMatcher, required, writeFlags, transformer, signatureMatcher);
	}

	/**
	 * Creates a transformer using a custom predicate with error details.
	 *
	 * @param predicate the predicate to match methods
	 * @param required the required number of matches (0 for optional)
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @param errorDetails consumer to append error details if match fails
	 * @return a ClassNodeTransformer that transforms the matching method
	 */
	public static ClassNodeTransformer create(Predicate<MethodNode> predicate, int required, int writeFlags,
			Consumer<MethodNode> transformer, Consumer<StringBuilder> errorDetails) {
		return builder(predicate, errorDetails).minMatches(required).maxMatches(required).writeFlags(writeFlags)
				.build(transformer);
	}

	/**
	 * Creates a builder for a method transformer by name.
	 *
	 * @param name the method name
	 * @return a builder instance
	 */
	public static Builder builder(String name) {
		return builder(SignatureMatcher.matchingMethodName(name));
	}

	/**
	 * Creates a builder for a method transformer by name and descriptor.
	 *
	 * @param name the method name
	 * @param desc the method descriptor
	 * @return a builder instance
	 */
	public static Builder builder(String name, String desc) {
		return builder(SignatureMatcher.matchingMethodNameDesc(name, desc));
	}

	/**
	 * Creates a builder for a method transformer with obfuscation support.
	 *
	 * @param name the deobfuscated method name
	 * @param obfName the obfuscated (SRG) method name
	 * @return a builder instance
	 */
	public static Builder builderObf(String name, String obfName) {
		return builder(SignatureMatcher.matchingMethodNameObf(name, obfName));
	}

	public static Builder builderObf(String name, String obfName, String desc) {
		return builder(SignatureMatcher.matchingMethodNameDescObf(name, obfName, desc));
	}

	public static Builder builderObf(String name, String desc, String obfName, String obfDesc) {
		return builder(SignatureMatcher.matchingMethodNameDescObf(name, obfName, desc, obfDesc));
	}

	/**
	 * Creates a builder for a method transformer using a signature matcher.
	 *
	 * @param signatureMatcher the matcher to use
	 * @return a builder instance
	 */
	public static Builder builder(SignatureMatcher<MethodNode> signatureMatcher) {
		return new Builder(signatureMatcher);
	}

	/**
	 * Creates a builder for a method transformer using a custom predicate.
	 *
	 * @param methodMatcher the predicate to match methods
	 * @param errorDetailAppender consumer to append error details
	 * @return a builder instance
	 */
	public static Builder builder(Predicate<MethodNode> methodMatcher, Consumer<StringBuilder> errorDetailAppender) {
		return new Builder(methodMatcher, errorDetailAppender);
	}

	/**
	 * Builder for creating method transformers with fine-grained control over matching behavior.
	 * Allows configuring min/max matches, write flags, and priority.
	 */
	public static class Builder {

		private final Predicate<MethodNode> methodMatcher;
		private final Consumer<StringBuilder> errorDetailAppender;
		private int minMatches = 1;
		private int maxMatches = 1;
		private int writeFlags;
		private int priority;

		/**
		 * Creates a builder with a signature matcher.
		 *
		 * @param signatureMatcher the matcher to use
		 */
		public Builder(SignatureMatcher<MethodNode> signatureMatcher) {
			this(signatureMatcher, signatureMatcher);
		}

		/**
		 * Creates a builder with a custom predicate.
		 *
		 * @param methodMatcher the predicate to match methods
		 * @param errorDetailAppender consumer to append error details
		 */
		public Builder(Predicate<MethodNode> methodMatcher, Consumer<StringBuilder> errorDetailAppender) {
			this.methodMatcher = Objects.requireNonNull(methodMatcher);
			this.errorDetailAppender = Objects.requireNonNull(errorDetailAppender);
		}

		/**
		 * Sets the minimum required number of matching methods.
		 *
		 * @param minMatches minimum matches (0 for optional)
		 * @return this builder
		 */
		public Builder minMatches(int minMatches) {
			this.minMatches = minMatches;
			return this;
		}

		/**
		 * Sets the maximum allowed number of matching methods.
		 *
		 * @param maxMatches maximum matches (0 for unlimited)
		 * @return this builder
		 */
		public Builder maxMatches(int maxMatches) {
			this.maxMatches = maxMatches;
			return this;
		}

		/**
		 * Sets ClassWriter flags for bytecode generation.
		 *
		 * @param writeFlags flags such as COMPUTE_FRAMES or COMPUTE_MAXS
		 * @return this builder
		 */
		public Builder writeFlags(int writeFlags) {
			this.writeFlags = writeFlags;
			return this;
		}

		/**
		 * Sets the transformer priority (lower values execute first).
		 *
		 * @param priority the priority value
		 * @return this builder
		 */
		public Builder priority(int priority) {
			this.priority = priority;
			return this;
		}

		/**
		 * Builds a ClassNodeTransformer with the configured settings.
		 *
		 * @param transformer the transformation logic
		 * @return a new ClassNodeTransformer
		 */
		public ClassNodeTransformer build(Consumer<MethodNode> transformer) {
			return this.build(method -> {
				transformer.accept(method);
				return true;
			});
		}

		/**
		 * Builds a ClassNodeTransformer with the configured settings using a predicate.
		 *
		 * @param transformer the transformation logic that returns whether transformation occurred
		 * @return a new ClassNodeTransformer
		 */
		public ClassNodeTransformer build(Predicate<MethodNode> transformer) {
			Objects.requireNonNull(transformer);
			Predicate<MethodNode> methodMatcher = this.methodMatcher;
			Consumer<StringBuilder> errorDetailAppender = this.errorDetailAppender;
			int minMatches = this.minMatches;
			int maxMatches = this.maxMatches;
			int writeFlags = this.writeFlags;
			int priority = this.priority;

			return ClassNodeTransformer.create(writeFlags, priority, classNode -> {
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
