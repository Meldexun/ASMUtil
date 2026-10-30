/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2.reader;

import java.io.DataInput;
import java.io.IOException;

/**
 * Functional interface for reading constants from a class file's constant pool.
 * Returns null to skip constants that aren't needed.
 */
@FunctionalInterface
interface FilteringConstantReader {

	/**
	 * Reads a constant from the input.
	 *
	 * @param in the input to read from
	 * @param type the constant type tag
	 * @return the constant value, or null to skip storing this constant
	 * @throws IOException if an I/O error occurs
	 */
	Object readConstant(DataInput in, byte type) throws IOException;

}
