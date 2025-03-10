/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

class InsnSpliterator implements Spliterator<AbstractInsnNode> {

	private final InsnList instructions;
	private final int estimatedSize;
	private int index;
	private int end;
	private AbstractInsnNode node;

	InsnSpliterator(InsnList instructions) {
		this(instructions, 0, instructions.size());
	}

	InsnSpliterator(InsnList instructions, int start, int end) {
		Objects.requireNonNull(instructions);
		if (start < 0) throw new IndexOutOfBoundsException();
		if (start > end) throw new IndexOutOfBoundsException();
		if (end > instructions.size()) throw new IndexOutOfBoundsException();
		this.instructions = instructions;
		this.estimatedSize = instructions.size();
		this.index = start;
		this.end = end;
		this.node = start < end ? instructions.get(start) : null;
	}

	@Override
	public boolean tryAdvance(Consumer<? super AbstractInsnNode> action) {
		if (index >= end)
			return false;
		if (instructions.size() != estimatedSize)
			throw new ConcurrentModificationException();
		action.accept(node);
		node = node.getNext();
		index++;
		return true;
	}

	@Override
	public Spliterator<AbstractInsnNode> trySplit() {
		if (end - index < 2)
			return null;
		return new InsnSpliterator(instructions, index, index = index + (end - index) / 2);
	}

	@Override
	public long estimateSize() {
		return end - index;
	}

	@Override
	public int characteristics() {
		return ORDERED | DISTINCT | NONNULL | SIZED | SUBSIZED;
	}

}
