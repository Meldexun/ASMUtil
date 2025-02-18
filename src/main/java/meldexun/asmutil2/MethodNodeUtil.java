/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.function.Predicate;

import org.objectweb.asm.tree.MethodNode;

class MethodNodeUtil {

	static Predicate<MethodNode> matching(String name) {
		return method -> name.equals(method.name);
	}

	static Predicate<MethodNode> matchingObf(String name, String obfName) {
		return method -> obfName.equals(method.name) || name.equals(method.name);
	}

	static Predicate<MethodNode> matching(String name, String desc) {
		return method -> name.equals(method.name) && desc.equals(method.desc);
	}

	static Predicate<MethodNode> matchingObf(String name, String obfName, String desc) {
		return method -> (obfName.equals(method.name) || name.equals(method.name)) && desc.equals(method.desc);
	}

	static Predicate<MethodNode> matchingObf(String name, String desc, String obfName, String obfDesc) {
		return method -> obfName.equals(method.name) && obfDesc.equals(method.desc)
				|| name.equals(method.name) && desc.equals(method.desc);
	}

}
