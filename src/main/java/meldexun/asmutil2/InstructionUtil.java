/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.function.Consumer;

class InstructionUtil {

	static Consumer<StringBuilder> errorDetails(String name) {
		return MethodNodeUtil.errorDetails(name);
	}

	static Consumer<StringBuilder> errorDetailsObf(String name, String obfName) {
		return MethodNodeUtil.errorDetailsObf(name, obfName);
	}

	static Consumer<StringBuilder> errorDetails(String name, String desc) {
		return MethodNodeUtil.errorDetails(name, desc);
	}

	static Consumer<StringBuilder> errorDetailsObf(String name, String obfName, String desc) {
		return MethodNodeUtil.errorDetailsObf(name, obfName, desc);
	}

	static Consumer<StringBuilder> errorDetails(String owner, String name, String desc) {
		return sb -> {
			sb.append("owner=").append(owner);
			sb.append(" ");
			sb.append("name=").append(name);
			sb.append(" ");
			sb.append("desc=").append(desc);
		};
	}

	static Consumer<StringBuilder> errorDetailsObf(String owner, String name, String obfName, String desc) {
		return sb -> {
			sb.append("owner=").append(owner);
			sb.append(" ");
			sb.append("name=[").append(name).append(", ").append(obfName).append("]");
			sb.append(" ");
			sb.append("desc=").append(desc);
		};
	}

	static Consumer<StringBuilder> errorDetailsObf(String owner, String name, String desc, String obfOwner,
			String obfName, String obfDesc) {
		return sb -> {
			sb.append("owner=[").append(owner).append(", ").append(obfOwner).append("]");
			sb.append(" ");
			sb.append("name=[").append(name).append(", ").append(obfName).append("]");
			sb.append(" ");
			sb.append("desc=[").append(desc).append(", ").append(obfDesc).append("]");
		};
	}

}
