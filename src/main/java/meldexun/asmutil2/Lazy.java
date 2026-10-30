/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Lazily computed value. The supplier is invoked on the first {@link #get()} and then released. Not thread-safe.
 *
 * @param <T> the type of the value
 */
class Lazy<T> implements Supplier<T> {

	private Supplier<T> supplier;
	private T value;

	/**
	 * Creates a lazy value with the given supplier.
	 *
	 * @param supplier the supplier to invoke for initialization
	 */
	public Lazy(Supplier<T> supplier) {
		this.supplier = Objects.requireNonNull(supplier);
	}

	/**
	 * Returns the value, initializing it on first access.
	 *
	 * @return the value
	 */
	@Override
	public T get() {
		if (this.supplier != null) {
			this.value = this.supplier.get();
			this.supplier = null;
		}
		return this.value;
	}

}
