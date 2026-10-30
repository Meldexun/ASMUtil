/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.ClassWriter;

import meldexun.asmutil2.reader.ClassUtil;

/**
 * A {@link ClassWriter} that computes common super classes (needed for {@link ClassWriter#COMPUTE_FRAMES}) by
 * reading class files with {@link ClassUtil} instead of loading them.
 * <p>
 * The default implementation calls {@code Class.forName}, which inside a class transformer loads the involved classes
 * early (before they could be transformed themselves), may recursively trigger the transformer, or fails because
 * the classes are not visible to the class loader of ASM.
 */
public class NonLoadingClassWriter extends ClassWriter {

	private final ClassUtil classUtil;

	/**
	 * Creates a writer reading class files through {@link ClassUtil#DEFAULT}.
	 *
	 * @param flags ClassWriter flags (e.g. {@code COMPUTE_FRAMES}, {@code COMPUTE_MAXS})
	 */
	public NonLoadingClassWriter(int flags) {
		this(flags, ClassUtil.DEFAULT);
	}

	/**
	 * Creates a writer reading class files through the given {@link ClassUtil}, e.g. one configured with another
	 * class loader or with obfuscation mappings.
	 *
	 * @param flags ClassWriter flags (e.g. {@code COMPUTE_FRAMES}, {@code COMPUTE_MAXS})
	 * @param classUtil used to look up super classes and interfaces
	 */
	public NonLoadingClassWriter(int flags, ClassUtil classUtil) {
		super(flags);
		this.classUtil = Objects.requireNonNull(classUtil);
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) {
		List<String> classHierarchyType1 = new ArrayList<>();
		String result = this.classUtil.findInClassHierarchy(type1, type -> {
			if (type.equals(type2)) {
				return true;
			}
			classHierarchyType1.add(type);
			return false;
		});
		if (result != null) {
			return result;
		}
		result = this.classUtil.findInClassHierarchy(type2, classHierarchyType1::contains);
		if (result != null) {
			return result;
		}
		throw new IllegalStateException();
	}

}
