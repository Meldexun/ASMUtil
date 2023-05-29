/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

public interface IClassTransformer {

	byte[] transform(String obfName, String name, byte[] basicClass);

}
