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
import org.objectweb.asm.tree.VarInsnNode;

public class InsnFinder<T extends AbstractInsnNode> {

	private final MethodNode method;
	private final AbstractInsnNode startInclusive;
	private final UnaryOperator<AbstractInsnNode> advance;
	private Class<T> type;
	private int opcode = -1;
	private Predicate<T> predicate;
	private int ordinal;

	public InsnFinder(MethodNode method, AbstractInsnNode startInclusive, UnaryOperator<AbstractInsnNode> advance) {
		this.method = Objects.requireNonNull(method);
		this.startInclusive = Objects.requireNonNull(startInclusive);
		this.advance = Objects.requireNonNull(advance);
	}

	public static InsnFinder<AbstractInsnNode> first(MethodNode method) {
		return next(method, method.instructions.getFirst());
	}

	public static InsnFinder<AbstractInsnNode> last(MethodNode method) {
		return prev(method, method.instructions.getLast());
	}

	public static InsnFinder<AbstractInsnNode> nextExclusive(MethodNode method, AbstractInsnNode startExclusive) {
		return next(method, startExclusive.getNext());
	}

	public static InsnFinder<AbstractInsnNode> prevExclusive(MethodNode method, AbstractInsnNode startExclusive) {
		return prev(method, startExclusive.getPrevious());
	}

	public static InsnFinder<AbstractInsnNode> next(MethodNode method, AbstractInsnNode startInclusive) {
		return new InsnFinder<>(method, startInclusive, AbstractInsnNode::getNext);
	}

	public static InsnFinder<AbstractInsnNode> prev(MethodNode method, AbstractInsnNode startInclusive) {
		return new InsnFinder<>(method, startInclusive, AbstractInsnNode::getPrevious);
	}

	public InsnFinder<AbstractInsnNode> findThenNextExclusive() {
		return ASMUtil.nextExclusive(this.method, this.find());
	}

	public InsnFinder<AbstractInsnNode> findThenPrevExclusive() {
		return ASMUtil.prevExclusive(this.method, this.find());
	}

	public InsnFinder<AbstractInsnNode> findThenNext() {
		return ASMUtil.next(this.method, this.find());
	}

	public InsnFinder<AbstractInsnNode> findThenPrev() {
		return ASMUtil.prev(this.method, this.find());
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

	public InsnFinder<VarInsnNode> varInsn(String name) {
		return this.varInsn(ASMUtil.findLocalVariable(this.method, name).index);
	}

	public InsnFinder<VarInsnNode> varInsn(String name, int ordinal) {
		return this.varInsn(ASMUtil.findLocalVariable(this.method, name, ordinal).index);
	}

	public InsnFinder<VarInsnNode> varInsnDesc(String desc) {
		return this.varInsn(ASMUtil.findLocalVariableDesc(this.method, desc).index);
	}

	public InsnFinder<VarInsnNode> varInsnDesc(String desc, int ordinal) {
		return this.varInsn(ASMUtil.findLocalVariableDesc(this.method, desc, ordinal).index);
	}

	public InsnFinder<VarInsnNode> varInsn(String name, String desc) {
		return this.varInsn(ASMUtil.findLocalVariable(this.method, name, desc).index);
	}

	public InsnFinder<VarInsnNode> varInsn(String name, String desc, int ordinal) {
		return this.varInsn(ASMUtil.findLocalVariable(this.method, name, desc, ordinal).index);
	}

	public InsnFinder<VarInsnNode> varInsn(int var) {
		return this.type(VarInsnNode.class).predicate(insn -> insn.var == var);
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
