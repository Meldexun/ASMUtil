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
 * Factory for {@link ClassNodeTransformer}s that transform methods. The created transformer applies
 * {@code transformer} to every method of the class that matches, and fails with a {@link ClassTransformException} if
 * the number of matching methods is not as expected.
 * <p>
 * The factory methods follow the scheme used throughout the library:
 * <ul>
 * <li>{@code create} matches by name or by name and descriptor. {@code createObf} accepts either of two names (and
 * optionally descriptors), see {@link SignatureMatcher}.</li>
 * <li>Without {@code required}, exactly one method has to match.</li>
 * <li>With {@code required}, exactly {@code required} methods have to match, except for {@code 0} which means any
 * number of methods, including none.</li>
 * <li>{@code createOptional}/{@code createObfOptional} are the same as {@code required = 0}, so they also transform
 * <b>all</b> matching methods if there are several.</li>
 * </ul>
 * For anything else (e.g. "at least one", or a priority) use {@link #builder(SignatureMatcher)}.
 */
public class MethodNodeTransformer {

	/**
	 * Creates a transformer for the single method with the given name.
	 *
	 * @param name the method name
	 * @param writeFlags {@link org.objectweb.asm.ClassWriter} flags such as {@code COMPUTE_FRAMES}
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching method
	 */
	public static ClassNodeTransformer create(String name, int writeFlags, Consumer<MethodNode> transformer) {
		return create(name, 1, writeFlags, transformer);
	}

	/**
	 * Creates a transformer for the single method named either {@code name} or {@code obfName}.
	 *
	 * @param name the deobfuscated method name
	 * @param obfName the obfuscated method name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching method
	 */
	public static ClassNodeTransformer createObf(String name, String obfName, int writeFlags,
			Consumer<MethodNode> transformer) {
		return createObf(name, obfName, 1, writeFlags, transformer);
	}

	/**
	 * Creates a transformer for the method with the given name and descriptor.
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
	 * Creates a transformer for the method named either {@code name} or {@code obfName} with the given descriptor.
	 *
	 * @param name the deobfuscated method name
	 * @param obfName the obfuscated method name
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
	 * Creates a transformer for the method named either {@code name} or {@code obfName} whose descriptor is either
	 * {@code desc} or {@code obfDesc}. Note the parameter order {@code (name, desc, obfName, obfDesc)}.
	 *
	 * @param name the deobfuscated method name
	 * @param desc the deobfuscated method descriptor
	 * @param obfName the obfuscated method name
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
	 * Creates a transformer for all methods with the given name, which does not fail if there are none.
	 *
	 * @param name the method name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching methods, if any
	 */
	public static ClassNodeTransformer createOptional(String name, int writeFlags, Consumer<MethodNode> transformer) {
		return create(name, 0, writeFlags, transformer);
	}

	/**
	 * Creates a transformer for all methods named either {@code name} or {@code obfName}, which does not fail if
	 * there are none.
	 *
	 * @param name the deobfuscated method name
	 * @param obfName the obfuscated method name
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching methods, if any
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
	 * Creates a transformer for methods with the given name, requiring an exact number of matches.
	 *
	 * @param name the method name
	 * @param required the exact number of methods that have to match, or 0 for any number (including none)
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
	 * Creates a transformer for methods accepted by the matcher, requiring an exact number of matches.
	 *
	 * @param signatureMatcher the matcher, also used to describe the method in error messages
	 * @param required the exact number of methods that have to match, or 0 for any number (including none)
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @return a ClassNodeTransformer that transforms the matching method
	 */
	public static ClassNodeTransformer create(SignatureMatcher<MethodNode> signatureMatcher, int required,
			int writeFlags, Consumer<MethodNode> transformer) {
		return create(signatureMatcher, required, writeFlags, transformer, signatureMatcher);
	}

	/**
	 * Creates a transformer for methods accepted by the predicate, requiring an exact number of matches.
	 *
	 * @param predicate the predicate to match methods
	 * @param required the exact number of methods that have to match, or 0 for any number (including none)
	 * @param writeFlags ClassWriter flags for bytecode generation
	 * @param transformer the transformation logic
	 * @param errorDetails appends a description of the searched method to error messages
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
	 * Creates a builder for a method transformer matching methods named either {@code name} or {@code obfName}.
	 *
	 * @param name the deobfuscated method name
	 * @param obfName the obfuscated method name
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
	 * @param errorDetailAppender appends a description of the searched method to error messages
	 * @return a builder instance
	 */
	public static Builder builder(Predicate<MethodNode> methodMatcher, Consumer<StringBuilder> errorDetailAppender) {
		return new Builder(methodMatcher, errorDetailAppender);
	}

	/**
	 * Builder for method transformers. Defaults: exactly one match ({@code minMatches = maxMatches = 1}), no write
	 * flags, priority 0.
	 *
	 * <pre>
	 * registry.add("com.example.Foo", MethodNodeTransformer.builder("update")
	 *         .minMatches(1).maxMatches(0) // at least one overload
	 *         .writeFlags(ClassWriter.COMPUTE_FRAMES)
	 *         .build(methodNode -&gt; { ... }));
	 * </pre>
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
		 * @param errorDetailAppender appends a description of the searched method to error messages
		 */
		public Builder(Predicate<MethodNode> methodMatcher, Consumer<StringBuilder> errorDetailAppender) {
			this.methodMatcher = Objects.requireNonNull(methodMatcher);
			this.errorDetailAppender = Objects.requireNonNull(errorDetailAppender);
		}

		/**
		 * Sets the minimum number of matching methods. Fewer matches fail the transformation.
		 *
		 * @param minMatches minimum matches (0 if the method does not have to exist)
		 * @return this builder
		 */
		public Builder minMatches(int minMatches) {
			this.minMatches = minMatches;
			return this;
		}

		/**
		 * Sets the maximum number of matching methods. More matches fail the transformation (before the
		 * transformer is applied to the excess method).
		 *
		 * @param maxMatches maximum matches (0 for unlimited)
		 * @return this builder
		 */
		public Builder maxMatches(int maxMatches) {
			this.maxMatches = maxMatches;
			return this;
		}

		/**
		 * Sets the {@link org.objectweb.asm.ClassWriter} flags this transformation needs. They are only applied if
		 * the transformer reports that it changed something.
		 *
		 * @param writeFlags flags such as {@code COMPUTE_FRAMES} or {@code COMPUTE_MAXS}
		 * @return this builder
		 */
		public Builder writeFlags(int writeFlags) {
			this.writeFlags = writeFlags;
			return this;
		}

		/**
		 * Sets the priority. Transformers registered for the same class run in ascending priority order (lower
		 * values first), and in registration order for equal priorities.
		 *
		 * @param priority the priority value
		 * @return this builder
		 */
		public Builder priority(int priority) {
			this.priority = priority;
			return this;
		}

		/**
		 * Builds a ClassNodeTransformer with the configured settings. The class is always considered changed when
		 * at least one method matched.
		 *
		 * @param transformer the transformation logic, called once per matching method
		 * @return a new ClassNodeTransformer
		 */
		public ClassNodeTransformer build(Consumer<MethodNode> transformer) {
			return this.build(method -> {
				transformer.accept(method);
				return true;
			});
		}

		/**
		 * Builds a ClassNodeTransformer with the configured settings.
		 *
		 * @param transformer the transformation logic, called once per matching method, returning whether it
		 *        changed the method. If it returns {@code false} for all methods, this transformer counts as not
		 *        having changed the class (the match count is still checked).
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
