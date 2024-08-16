/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import meldexun.asmutil2.reader.ClassUtil;

public class NonLoadingClassWriter extends ClassWriter {

	public static final ClassUtil.Configuration DEFAULT_CLASS_UTIL_CONFIGURATION = new ClassUtil.Configuration(
			NonLoadingClassWriter.class.getClassLoader());

	public NonLoadingClassWriter(int flags) {
		super(flags);
	}

	public NonLoadingClassWriter(ClassReader classReader, int flags) {
		super(classReader, flags);
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) {
		ClassUtil classUtil = ClassUtil.getInstance(this.getClassUtilConfiguration());
		List<String> classHierarchyType1 = new ArrayList<>();
		String result = classUtil.findInClassHierarchy(type1, type -> {
			if (type.equals(type2)) {
				return true;
			}
			classHierarchyType1.add(type);
			return false;
		});
		if (result != null) {
			return result;
		}
		result = classUtil.findInClassHierarchy(type2, classHierarchyType1::contains);
		if (result != null) {
			return result;
		}
		throw new IllegalStateException();
	}

	protected ClassUtil.Configuration getClassUtilConfiguration() {
		return DEFAULT_CLASS_UTIL_CONFIGURATION;
	}

}
