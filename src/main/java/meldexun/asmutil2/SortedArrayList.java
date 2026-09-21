/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * ArrayList that maintains elements in sorted order.
 * Elements are inserted at the correct position to maintain sort order.
 *
 * @param <E> the element type
 */
@SuppressWarnings("serial")
class SortedArrayList<E> extends ArrayList<E> {

	private final Comparator<E> comparator;

	/**
	 * Creates a sorted list with the given comparator.
	 *
	 * @param comparator the comparator for sorting elements
	 */
	SortedArrayList(Comparator<E> comparator) {
		this.comparator = comparator;
	}

	/**
	 * Creates a sorted list using natural ordering for comparable elements.
	 *
	 * @param <T> the element type
	 * @return a new sorted list
	 */
	static <T extends Comparable<T>> SortedArrayList<T> create() {
		return new SortedArrayList<T>(Comparator.naturalOrder());
	}

	@Override
	public boolean add(E e) {
		super.add(insertIndex(e), e);
		return true;
	}

	private int insertIndex(E e) {
		int low = 0;
		int high = size() - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			if (comparator.compare(get(mid), e) <= 0) {
				low = mid + 1;
			} else {
				high = mid - 1;
			}
		}

		return low;
	}

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		c.forEach(this::add);
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

}
