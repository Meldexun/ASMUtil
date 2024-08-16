/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2.reader;

import java.io.DataInput;
import java.io.IOException;

@FunctionalInterface
interface FilteringConstantReader {

	Object readConstant(DataInput in, byte type) throws IOException;

}
