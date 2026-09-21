/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

/**
 * Functional interface for a function with four arguments.
 *
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 * @param <V> the type of the third argument
 * @param <W> the type of the fourth argument
 * @param <R> the type of the result
 */
public interface QuadFunction<T, U, V, W, R> {

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t the first argument
	 * @param u the second argument
	 * @param v the third argument
	 * @param w the fourth argument
	 * @return the function result
	 */
	R apply(T t, U u, V v, W w);

}
