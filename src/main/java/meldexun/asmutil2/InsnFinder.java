/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class InsnFinder<T extends AbstractInsnNode> {

	private AbstractInsnNode startInclusive;
	private UnaryOperator<AbstractInsnNode> advance;
	private Class<T> type;
	private Predicate<T> predicate;
	private int ordinal;

	public static <T extends AbstractInsnNode> InsnFinder<T> create() {
		return new InsnFinder<>();
	}

	public InsnFinder<T> first(MethodNode methodNode) {
		return this.next(methodNode.instructions.getFirst());
	}

	public InsnFinder<T> last(MethodNode methodNode) {
		return this.prev(methodNode.instructions.getLast());
	}

	public InsnFinder<T> nextExclusive(AbstractInsnNode startExclusive) {
		return this.next(startExclusive.getNext());
	}

	public InsnFinder<T> prevExclusive(AbstractInsnNode startExclusive) {
		return this.prev(startExclusive.getPrevious());
	}

	public InsnFinder<T> next(AbstractInsnNode startInclusive) {
		this.startInclusive = startInclusive;
		this.advance = AbstractInsnNode::getNext;
		return this;
	}

	public InsnFinder<T> prev(AbstractInsnNode startInclusive) {
		this.startInclusive = startInclusive;
		this.advance = AbstractInsnNode::getPrevious;
		return this;
	}

	public InsnFinder<AbstractInsnNode> findThenNextExclusive() {
		return ASMUtil.nextExclusive(this.find());
	}

	public InsnFinder<AbstractInsnNode> findThenPrevExclusive() {
		return ASMUtil.prevExclusive(this.find());
	}

	public InsnFinder<AbstractInsnNode> findThenNext() {
		return ASMUtil.next(this.find());
	}

	public InsnFinder<AbstractInsnNode> findThenPrev() {
		return ASMUtil.prev(this.find());
	}

	@SuppressWarnings("unchecked")
	public <R extends AbstractInsnNode> InsnFinder<R> type(Class<R> type) {
		InsnFinder<R> n = (InsnFinder<R>) this;
		n.type = type;
		return n;
	}

	public InsnFinder<T> opcode(int opcode) {
		return this.predicate(insn -> insn.getOpcode() == opcode);
	}

	public InsnFinder<LdcInsnNode> ldcInsn(Object cst) {
		return this.type(LdcInsnNode.class).predicate(insn -> Objects.equals(insn.cst, cst));
	}

	public InsnFinder<IntInsnNode> intInsn(int opcode, int operand) {
		return this.type(IntInsnNode.class).predicate(insn -> insn.getOpcode() == opcode && insn.operand == operand);
	}

	public InsnFinder<MethodInsnNode> methodInsn(int opcode, String owner, String name, String desc) {
		return this.type(MethodInsnNode.class).predicate(insn -> {
			return insn.getOpcode() == opcode
					&& insn.owner.equals(owner)
					&& insn.name.equals(name)
					&& insn.desc.equals(desc);
		});
	}

	public InsnFinder<MethodInsnNode> methodInsn(int opcode, String owner, String name, String deobfName, String desc) {
		return this.type(MethodInsnNode.class).predicate(insn -> {
			return insn.getOpcode() == opcode
					&& insn.owner.equals(owner)
					&& (insn.name.equals(deobfName) || insn.name.equals(name))
					&& insn.desc.equals(desc);
		});
	}

	public InsnFinder<MethodInsnNode> methodInsn(int opcode, String owner, String name, String desc, String deobfOwner,
			String deobfName, String deobfDesc) {
		return this.type(MethodInsnNode.class).predicate(insn -> {
			return insn.getOpcode() == opcode
					&& (insn.owner.equals(owner) && insn.name.equals(name) && insn.desc.equals(desc)
							|| insn.owner.equals(deobfOwner) && insn.name.equals(deobfName) && insn.desc.equals(deobfDesc));
		});
	}

	public InsnFinder<FieldInsnNode> fieldInsn(int opcode, String owner, String name, String desc) {
		return this.type(FieldInsnNode.class).predicate(insn -> {
			return insn.getOpcode() == opcode
					&& insn.owner.equals(owner)
					&& insn.name.equals(name)
					&& insn.desc.equals(desc);
		});
	}

	public InsnFinder<FieldInsnNode> fieldInsn(int opcode, String owner, String name, String deobfName, String desc) {
		return this.type(FieldInsnNode.class).predicate(insn -> {
			return insn.getOpcode() == opcode
					&& insn.owner.equals(owner)
					&& (insn.name.equals(deobfName) || insn.name.equals(name))
					&& insn.desc.equals(desc);
		});
	}

	public InsnFinder<FieldInsnNode> fieldInsn(int opcode, String owner, String name, String desc, String deobfOwner,
			String deobfName, String deobfDesc) {
		return this.type(FieldInsnNode.class).predicate(insn -> {
			return insn.getOpcode() == opcode
					&& (insn.owner.equals(owner) && insn.name.equals(name) && insn.desc.equals(desc)
							|| insn.owner.equals(deobfOwner) && insn.name.equals(deobfName) && insn.desc.equals(deobfDesc));
		});
	}

	public InsnFinder<T> predicate(Predicate<T> predicate) {
		this.predicate = predicate;
		return this;
	}

	public InsnFinder<T> ordinal(int ordinal) {
		this.ordinal = ordinal;
		return this;
	}

	@SuppressWarnings("unchecked")
	public T find() {
		int i = 0;
		AbstractInsnNode insn = this.startInclusive;
		while (insn != null && (this.type != null && !this.type.isInstance(insn)
				|| this.predicate != null && !this.predicate.test((T) insn) || i++ != this.ordinal)) {
			insn = this.advance.apply(insn);
		}
		if (insn == null) {
			throw new NoSuchElementException();
		}
		return (T) insn;
	}

}
