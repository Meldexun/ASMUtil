/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.function.Consumer;
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

	static Consumer<StringBuilder> errorDetails(String name) {
		return sb -> {
			sb.append("name=").append(name);
		};
	}

	static Consumer<StringBuilder> errorDetailsObf(String name, String obfName) {
		return sb -> {
			sb.append("name=[").append(name).append(", ").append(obfName).append("]");
		};
	}

	static Consumer<StringBuilder> errorDetails(String name, String desc) {
		return sb -> {
			sb.append("name=").append(name);
			sb.append(" ");
			sb.append("desc=").append(desc);
		};
	}

	static Consumer<StringBuilder> errorDetailsObf(String name, String obfName, String desc) {
		return sb -> {
			sb.append("name=[").append(name).append(", ").append(obfName).append("]");
			sb.append(" ");
			sb.append("desc=").append(desc);
		};
	}

	static Consumer<StringBuilder> errorDetailsObf(String name, String desc, String obfName, String obfDesc) {
		return sb -> {
			sb.append("name=[").append(name).append(", ").append(obfName).append("]");
			sb.append(" ");
			sb.append("desc=[").append(desc).append(", ").append(obfDesc).append("]");
		};
	}

}
