/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

public interface QuadFunction<T, U, V, W, R> {

	R apply(T t, U u, V v, W w);

}
