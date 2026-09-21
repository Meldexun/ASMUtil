/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

/**
 * Internal utility class for StringBuilder operations with padding.
 */
class StringBuilderUtil {

	/**
	 * Appends a string to a StringBuilder with left padding to reach a minimum length.
	 *
	 * @param sb the StringBuilder to append to
	 * @param s the string to append
	 * @param l the minimum total length (pads with spaces if needed)
	 * @return the StringBuilder
	 */
	public static StringBuilder append(StringBuilder sb, String s, int l) {
		for (int i = l - s.length(); i > 0; i--) {
			sb.append(' ');
		}
		return sb.append(s);
	}

	/**
	 * Appends an integer to a StringBuilder with left padding to reach a minimum length.
	 *
	 * @param sb the StringBuilder to append to
	 * @param x the integer to append
	 * @param l the minimum total length (pads with spaces if needed)
	 * @return the StringBuilder
	 */
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
