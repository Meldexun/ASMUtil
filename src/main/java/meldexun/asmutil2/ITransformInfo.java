/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * Describes how {@link ClassVisitorClassTransformer} transforms one class:
 * <ol>
 * <li>{@link #visitor(Lazy)} creates the visitor and the class is read into it with {@link #readFlags()},</li>
 * <li>{@link #transform(ClassVisitor, Lazy)} is called afterwards and decides whether the class changed,</li>
 * <li>if it did, the bytes of the class writer are returned.</li>
 * </ol>
 * The class writer is created on first access of the {@link Lazy}, with the flags returned by {@link #writeFlags()}
 * at that moment, so an implementation may compute its flags while transforming as long as it does so before
 * accessing the writer.
 *
 * @param <T> the type of ClassVisitor used for transformation
 */
public interface ITransformInfo<T extends ClassVisitor> {

	/**
	 * Creates the visitor the class is read into. A visitor that streams directly into the writer delegates to
	 * {@code classWriter.get()}; a {@link org.objectweb.asm.tree.ClassNode} would not touch it here.
	 *
	 * @param classWriter the lazily created ClassWriter for the output
	 * @return the ClassVisitor instance
	 */
	T visitor(Lazy<ClassWriter> classWriter);

	/**
	 * Called after the class has been read into the visitor. Has to make sure the class ends up in
	 * {@code classWriter} if it returns {@code true} (e.g. via {@code classNode.accept(classWriter.get())}).
	 *
	 * @param classVisitor the visitor that received the class
	 * @param classWriter the lazily created ClassWriter for the output
	 * @return {@code true} if the class was changed and the writer's bytes should be used
	 */
	boolean transform(T classVisitor, Lazy<ClassWriter> classWriter);

	/**
	 * Returns ClassWriter flags for bytecode generation.
	 *
	 * @return flags such as COMPUTE_FRAMES or COMPUTE_MAXS
	 */
	int writeFlags();

	/**
	 * Returns ClassReader flags for bytecode parsing.
	 *
	 * @return flags such as SKIP_FRAMES or SKIP_DEBUG
	 */
	int readFlags();

	static <T extends ClassVisitor> ITransformInfo<T> create(Supplier<T> classVisitorFactory,
			BiPredicate<T, Lazy<ClassWriter>> transformFunction, int writeFlags, int readFlags) {
		return createTransformInfo(classWriter -> classVisitorFactory.get(), transformFunction, writeFlags, readFlags);
	}

	static <T extends ClassVisitor> ITransformInfo<T> create(Function<ClassWriter, T> classVisitorFactory,
			BiPredicate<T, Lazy<ClassWriter>> transformFunction, int writeFlags, int readFlags) {
		return createTransformInfo(classVisitorFactory.compose(Lazy::get), transformFunction, writeFlags, readFlags);
	}

	static <T extends ClassVisitor> ITransformInfo<T> createTransformInfo(
			Function<Lazy<ClassWriter>, T> classVisitorFactory, BiPredicate<T, Lazy<ClassWriter>> transformFunction,
			int writeFlags, int readFlags) {
		return new ITransformInfo<T>() {

			@Override
			public T visitor(Lazy<ClassWriter> classWriter) {
				return classVisitorFactory.apply(classWriter);
			}

			@Override
			public boolean transform(T classVisitor, Lazy<ClassWriter> classWriter) {
				return transformFunction.test(classVisitor, classWriter);
			}

			@Override
			public int writeFlags() {
				return writeFlags;
			}

			@Override
			public int readFlags() {
				return readFlags;
			}

		};
	}

}
