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
 * Encapsulates transformation information including the visitor to use,
 * transformation logic, and ASM read/write flags.
 *
 * @param <T> the type of ClassVisitor used for transformation
 */
public interface ITransformInfo<T extends ClassVisitor> {

	/**
	 * Creates the ClassVisitor used for transformation.
	 *
	 * @param classWriter lazy-initialized ClassWriter for output
	 * @return the ClassVisitor instance
	 */
	T visitor(Lazy<ClassWriter> classWriter);

	/**
	 * Performs the transformation using the visitor.
	 *
	 * @param classVisitor the visitor that received the class data
	 * @param classWriter lazy-initialized ClassWriter for output
	 * @return true if transformation was applied, false otherwise
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
