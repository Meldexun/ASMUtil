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
import org.objectweb.asm.tree.TypeInsnNode;

public class InsnFinder<T extends AbstractInsnNode> {

	private AbstractInsnNode startInclusive;
	private UnaryOperator<AbstractInsnNode> advance;
	private Class<T> type;
	private int opcode = -1;
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
		this.opcode = opcode;
		return this;
	}

	public InsnFinder<TypeInsnNode> typeInsn(String desc) {
		return this.type(TypeInsnNode.class).predicate(insn -> insn.desc.equals(desc));
	}

	public InsnFinder<LdcInsnNode> ldcInsn(Object cst) {
		return this.type(LdcInsnNode.class).predicate(insn -> Objects.equals(insn.cst, cst));
	}

	public InsnFinder<IntInsnNode> intInsn(int operand) {
		return this.type(IntInsnNode.class).predicate(insn -> insn.operand == operand);
	}

	public InsnFinder<MethodInsnNode> methodInsn(String name) {
		return this.type(MethodInsnNode.class).predicate(insn -> {
			return insn.name.equals(name);
		});
	}

	public InsnFinder<MethodInsnNode> methodInsnObf(String name, String obfName) {
		return this.type(MethodInsnNode.class).predicate(insn -> {
			return insn.name.equals(obfName) || insn.name.equals(name);
		});
	}

	public InsnFinder<MethodInsnNode> methodInsn(String name, String desc) {
		return this.type(MethodInsnNode.class).predicate(insn -> {
			return insn.name.equals(name) && insn.desc.equals(desc);
		});
	}

	public InsnFinder<MethodInsnNode> methodInsnObf(String name, String obfName, String desc) {
		return this.type(MethodInsnNode.class).predicate(insn -> {
			return (insn.name.equals(obfName) || insn.name.equals(name)) && insn.desc.equals(desc);
		});
	}

	public InsnFinder<MethodInsnNode> methodInsn(String owner, String name, String desc) {
		return this.type(MethodInsnNode.class).predicate(insn -> {
			return insn.owner.equals(owner) && insn.name.equals(name) && insn.desc.equals(desc);
		});
	}

	public InsnFinder<MethodInsnNode> methodInsnObf(String owner, String name, String obfName, String desc) {
		return this.type(MethodInsnNode.class).predicate(insn -> {
			return insn.owner.equals(owner) && (insn.name.equals(obfName) || insn.name.equals(name))
					&& insn.desc.equals(desc);
		});
	}

	public InsnFinder<MethodInsnNode> methodInsnObf(String owner, String name, String desc, String obfOwner,
			String obfName, String obfDesc) {
		return this.type(MethodInsnNode.class).predicate(insn -> {
			return insn.owner.equals(obfOwner) && insn.name.equals(obfName) && insn.desc.equals(obfDesc)
					|| insn.owner.equals(owner) && insn.name.equals(name) && insn.desc.equals(desc);
		});
	}

	public InsnFinder<FieldInsnNode> fieldInsn(String name) {
		return this.type(FieldInsnNode.class).predicate(insn -> {
			return insn.name.equals(name);
		});
	}

	public InsnFinder<FieldInsnNode> fieldInsnObf(String name, String obfName) {
		return this.type(FieldInsnNode.class).predicate(insn -> {
			return insn.name.equals(obfName) || insn.name.equals(name);
		});
	}

	public InsnFinder<FieldInsnNode> fieldInsn(String name, String desc) {
		return this.type(FieldInsnNode.class).predicate(insn -> {
			return insn.name.equals(name) && insn.desc.equals(desc);
		});
	}

	public InsnFinder<FieldInsnNode> fieldInsnObf(String name, String obfName, String desc) {
		return this.type(FieldInsnNode.class).predicate(insn -> {
			return (insn.name.equals(obfName) || insn.name.equals(name)) && insn.desc.equals(desc);
		});
	}

	public InsnFinder<FieldInsnNode> fieldInsn(String owner, String name, String desc) {
		return this.type(FieldInsnNode.class).predicate(insn -> {
			return insn.owner.equals(owner) && insn.name.equals(name) && insn.desc.equals(desc);
		});
	}

	public InsnFinder<FieldInsnNode> fieldInsnObf(String owner, String name, String obfName, String desc) {
		return this.type(FieldInsnNode.class).predicate(insn -> {
			return insn.owner.equals(owner) && (insn.name.equals(obfName) || insn.name.equals(name))
					&& insn.desc.equals(desc);
		});
	}

	public InsnFinder<FieldInsnNode> fieldInsnObf(String owner, String name, String desc, String obfOwner,
			String obfName, String obfDesc) {
		return this.type(FieldInsnNode.class).predicate(insn -> {
			return insn.owner.equals(obfOwner) && insn.name.equals(obfName) && insn.desc.equals(obfDesc)
					|| insn.owner.equals(owner) && insn.name.equals(name) && insn.desc.equals(desc);
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
				|| this.opcode >= 0 && insn.getOpcode() != this.opcode
				|| this.predicate != null && !this.predicate.test((T) insn) || i++ != this.ordinal)) {
			insn = this.advance.apply(insn);
		}
		if (insn == null) {
			throw new NoSuchElementException();
		}
		return (T) insn;
	}

}
