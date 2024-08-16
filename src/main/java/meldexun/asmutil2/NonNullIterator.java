/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.Iterator;
import java.util.NoSuchElementException;

abstract class NonNullIterator<E> implements Iterator<E> {

	private enum State {
		COMPUTE_NEXT, HAS_NEXT, END
	}

	private State state = State.COMPUTE_NEXT;
	private E next;

	@Override
	public boolean hasNext() {
		if (this.state == State.COMPUTE_NEXT) {
			this.next = this.computeNext(this.next);
			this.state = this.next != null ? State.HAS_NEXT : State.END;
		}
		return this.state == State.HAS_NEXT;
	}

	@Override
	public E next() {
		if (!this.hasNext()) {
			throw new NoSuchElementException();
		}
		this.state = State.COMPUTE_NEXT;
		return this.next;
	}

	protected abstract E computeNext(E current);

}
