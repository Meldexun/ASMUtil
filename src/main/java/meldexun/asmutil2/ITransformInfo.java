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

public interface ITransformInfo<T extends ClassVisitor> {

	T visitor(Lazy<ClassWriter> classWriter);

	boolean transform(T classVisitor, Lazy<ClassWriter> classWriter);

	int writeFlags();

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
