/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

class StringBuilderUtil {

	public static StringBuilder append(StringBuilder sb, String s, int l) {
		for (int i = l - s.length(); i > 0; i--) {
			sb.append(' ');
		}
		return sb.append(s);
	}

	public static StringBuilder append(StringBuilder sb, int x, int l) {
		for (int i = l - stringSize(x); i > 0; i--) {
			sb.append(' ');
		}
		return sb.append(x);
	}

	private static int stringSize(int x) {
		int d = 1;
		if (x >= 0) {
			d = 0;
			x = -x;
		}
		int p = -10;
		for (int i = 1; i < 10; i++) {
			if (x > p)
				return i + d;
			p = 10 * p;
		}
		return 10 + d;
	}

}
