/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

public interface TriFunction<T, U, V, R> {

	R apply(T t, U u, V v);

}
