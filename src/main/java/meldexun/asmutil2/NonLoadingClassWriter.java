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
 * ClassWriter that determines common super classes without loading classes.
 * Standard ClassWriter uses ClassLoader which can fail in transformation contexts.
 * This implementation reads class files directly to compute type hierarchies.
 */
public class NonLoadingClassWriter extends ClassWriter {

	private final ClassUtil classUtil;

	/**
	 * Creates a NonLoadingClassWriter with default class utilities.
	 *
	 * @param flags ClassWriter flags (e.g., COMPUTE_FRAMES, COMPUTE_MAXS)
	 */
	public NonLoadingClassWriter(int flags) {
		this(flags, ClassUtil.DEFAULT);
	}

	/**
	 * Creates a NonLoadingClassWriter with custom class utilities.
	 *
	 * @param flags ClassWriter flags (e.g., COMPUTE_FRAMES, COMPUTE_MAXS)
	 * @param classUtil utility for reading class hierarchies
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
