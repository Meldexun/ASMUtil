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

public class NonLoadingClassWriter extends ClassWriter {

	private final ClassUtil classUtil;

	public NonLoadingClassWriter(int flags) {
		this(flags, ClassUtil.DEFAULT);
	}

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
