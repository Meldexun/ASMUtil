/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
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

/**
 * Fluent API for finding instructions in a method's instruction list.
 * Supports searching by type, opcode, predicates, and specific instruction properties.
 * Can search forward or backward from a starting position.
 *
 * @param <T> the type of instruction being searched for
 */
public class InsnFinder<T extends AbstractInsnNode> {

	private static final UnaryOperator<AbstractInsnNode> NEXT = AbstractInsnNode::getNext;
	private static final UnaryOperator<AbstractInsnNode> PREVIOUS = AbstractInsnNode::getPrevious;
	private final MethodNode method;
	private final AbstractInsnNode startInclusive;
	private final UnaryOperator<AbstractInsnNode> advance;
	private Class<T> type;
	private int opcode = -1;
	private Predicate<T> predicate;
	private Consumer<StringBuilder> errorDetails;
	private int ordinal;

	/**
	 * Creates an instruction finder.
	 *
	 * @param method the method to search in
	 * @param startInclusive the instruction to start from
	 * @param advance function to advance to the next/previous instruction
	 */
	public InsnFinder(MethodNode method, AbstractInsnNode startInclusive, UnaryOperator<AbstractInsnNode> advance) {
		this.method = Objects.requireNonNull(method);
		this.startInclusive = Objects.requireNonNull(startInclusive);
		this.advance = Objects.requireNonNull(advance);
	}

	/**
	 * Creates a finder starting from the first instruction.
	 *
	 * @param method the method to search
	 * @return a new instruction finder
	 */
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
		return new InsnFinder<>(method, startInclusive, NEXT);
	}

	public static InsnFinder<AbstractInsnNode> prev(MethodNode method, AbstractInsnNode startInclusive) {
		return new InsnFinder<>(method, startInclusive, PREVIOUS);
	}

	/**
	 * Finds the matching instruction and creates a new finder starting after it.
	 *
	 * @return a new instruction finder
	 */
	public InsnFinder<AbstractInsnNode> findThenNextExclusive() {
		return ASMUtil.nextExclusive(this.method, this.find());
	}

	/**
	 * Finds the matching instruction and creates a new finder starting before it.
	 *
	 * @return a new instruction finder
	 */
	public InsnFinder<AbstractInsnNode> findThenPrevExclusive() {
		return ASMUtil.prevExclusive(this.method, this.find());
	}

	/**
	 * Finds the matching instruction and creates a new finder starting from it.
	 *
	 * @return a new instruction finder
	 */
	public InsnFinder<AbstractInsnNode> findThenNext() {
		return ASMUtil.next(this.method, this.find());
	}

	/**
	 * Finds the matching instruction and creates a new finder searching backwards from it.
	 *
	 * @return a new instruction finder
	 */
	public InsnFinder<AbstractInsnNode> findThenPrev() {
		return ASMUtil.prev(this.method, this.find());
	}

	/**
	 * Filters to match only instructions of a specific type.
	 *
	 * @param <R> the instruction type
	 * @param type the instruction class to match
	 * @return this finder
	 */
	@SuppressWarnings("unchecked")
	public <R extends AbstractInsnNode> InsnFinder<R> type(Class<R> type) {
		InsnFinder<R> n = (InsnFinder<R>) this;
		n.type = type;
		return n;
	}

	/**
	 * Filters to match only instructions with a specific opcode.
	 *
	 * @param opcode the opcode to match (e.g., Opcodes.ALOAD)
	 * @return this finder
	 */
	public InsnFinder<T> opcode(int opcode) {
		this.opcode = opcode;
		return this;
	}

	/**
	 * Finds a type instruction (NEW, INSTANCEOF, etc.) by descriptor.
	 *
	 * @param desc the type descriptor
	 * @return this finder typed for TypeInsnNode
	 */
	public InsnFinder<TypeInsnNode> typeInsn(String desc) {
		return this.type(TypeInsnNode.class).predicate(insn -> insn.desc.equals(desc),
				sb -> sb.append("desc=").append(desc));
	}

	/**
	 * Finds an LDC (load constant) instruction by constant value.
	 *
	 * @param cst the constant value to match
	 * @return this finder typed for LdcInsnNode
	 */
	public InsnFinder<LdcInsnNode> ldcInsn(Object cst) {
		return this.type(LdcInsnNode.class).predicate(insn -> Objects.equals(insn.cst, cst),
				sb -> sb.append("cst=").append(cst));
	}

	/**
	 * Finds an integer instruction (BIPUSH, SIPUSH) by operand value.
	 *
	 * @param operand the operand value to match
	 * @return this finder typed for IntInsnNode
	 */
	public InsnFinder<IntInsnNode> intInsn(int operand) {
		return this.type(IntInsnNode.class).predicate(insn -> insn.operand == operand,
				sb -> sb.append("operand=").append(operand));
	}

	/**
	 * Finds a variable instruction by local variable name.
	 *
	 * @param name the local variable name
	 * @return this finder typed for VarInsnNode
	 */
	public InsnFinder<VarInsnNode> varInsn(String name) {
		return this.varInsn(ASMUtil.findLocalVariable(this.method, name).index, sb -> {
			sb.append("varName=").append(name);
		});
	}

	public InsnFinder<VarInsnNode> varInsn(String name, int ordinal) {
		return this.varInsn(ASMUtil.findLocalVariable(this.method, name, ordinal).index, sb -> {
			sb.append("varName=").append(name);
			sb.append(" ");
			sb.append("varOrdinal=").append(ordinal);
		});
	}

	public InsnFinder<VarInsnNode> varInsnDesc(String desc) {
		return this.varInsn(ASMUtil.findLocalVariableDesc(this.method, desc).index, sb -> {
			sb.append("varDesc=").append(desc);
		});
	}

	public InsnFinder<VarInsnNode> varInsnDesc(String desc, int ordinal) {
		return this.varInsn(ASMUtil.findLocalVariableDesc(this.method, desc, ordinal).index, sb -> {
			sb.append("varDesc=").append(desc);
			sb.append(" ");
			sb.append("varOrdinal=").append(ordinal);
		});
	}

	public InsnFinder<VarInsnNode> varInsn(String name, String desc) {
		return this.varInsn(ASMUtil.findLocalVariable(this.method, name, desc).index, sb -> {
			sb.append("varName=").append(name);
			sb.append(" ");
			sb.append("varDesc=").append(desc);
		});
	}

	public InsnFinder<VarInsnNode> varInsn(String name, String desc, int ordinal) {
		return this.varInsn(ASMUtil.findLocalVariable(this.method, name, desc, ordinal).index, sb -> {
			sb.append("varName=").append(name);
			sb.append(" ");
			sb.append("varDesc=").append(desc);
			sb.append(" ");
			sb.append("varOrdinal=").append(ordinal);
		});
	}

	public InsnFinder<VarInsnNode> varInsn(int var) {
		return this.varInsn(var, null);
	}

	public InsnFinder<VarInsnNode> varInsn(int var, Consumer<StringBuilder> additionalErrorDetails) {
		return this.type(VarInsnNode.class).predicate(insn -> insn.var == var, sb -> {
			sb.append("var=").append(var);
			if (additionalErrorDetails != null) {
				sb.append(" ");
				additionalErrorDetails.accept(sb);
			}
		});
	}

	/**
	 * Finds a method invocation instruction by method name.
	 *
	 * @param name the method name
	 * @return this finder typed for MethodInsnNode
	 */
	public InsnFinder<MethodInsnNode> methodInsn(String name) {
		return this.type(MethodInsnNode.class).predicate(SignatureMatcher.matchingMethodInsnName(name));
	}

	public InsnFinder<MethodInsnNode> methodInsnObf(String name, String obfName) {
		return this.type(MethodInsnNode.class).predicate(SignatureMatcher.matchingMethodInsnNameObf(name, obfName));
	}

	public InsnFinder<MethodInsnNode> methodInsn(String name, String desc) {
		return this.type(MethodInsnNode.class).predicate(SignatureMatcher.matchingMethodInsnNameDesc(name, desc));
	}

	public InsnFinder<MethodInsnNode> methodInsnObf(String name, String obfName, String desc) {
		return this.type(MethodInsnNode.class)
				.predicate(SignatureMatcher.matchingMethodInsnNameDescObf(name, obfName, desc));
	}

	public InsnFinder<MethodInsnNode> methodInsn(String owner, String name, String desc) {
		return this.type(MethodInsnNode.class)
				.predicate(SignatureMatcher.matchingMethodInsnOwnerNameDesc(owner, name, desc));
	}

	public InsnFinder<MethodInsnNode> methodInsnObf(String owner, String name, String obfName, String desc) {
		return this.type(MethodInsnNode.class)
				.predicate(SignatureMatcher.matchingMethodInsnOwnerNameDescObf(owner, name, obfName, desc));
	}

	public InsnFinder<MethodInsnNode> methodInsnObf(String owner, String name, String desc, String obfOwner,
			String obfName, String obfDesc) {
		return this.type(MethodInsnNode.class).predicate(
				SignatureMatcher.matchingMethodInsnOwnerNameDescObf(owner, obfOwner, name, obfName, desc, obfDesc));
	}

	/**
	 * Finds a field access instruction by field name.
	 *
	 * @param name the field name
	 * @return this finder typed for FieldInsnNode
	 */
	public InsnFinder<FieldInsnNode> fieldInsn(String name) {
		return this.type(FieldInsnNode.class).predicate(SignatureMatcher.matchingFieldInsnName(name));
	}

	public InsnFinder<FieldInsnNode> fieldInsnObf(String name, String obfName) {
		return this.type(FieldInsnNode.class).predicate(SignatureMatcher.matchingFieldInsnNameObf(name, obfName));
	}

	public InsnFinder<FieldInsnNode> fieldInsn(String name, String desc) {
		return this.type(FieldInsnNode.class).predicate(SignatureMatcher.matchingFieldInsnNameDesc(name, desc));
	}

	public InsnFinder<FieldInsnNode> fieldInsnObf(String name, String obfName, String desc) {
		return this.type(FieldInsnNode.class)
				.predicate(SignatureMatcher.matchingFieldInsnNameDescObf(name, obfName, desc));
	}

	public InsnFinder<FieldInsnNode> fieldInsn(String owner, String name, String desc) {
		return this.type(FieldInsnNode.class)
				.predicate(SignatureMatcher.matchingFieldInsnOwnerNameDesc(owner, name, desc));
	}

	public InsnFinder<FieldInsnNode> fieldInsnObf(String owner, String name, String obfName, String desc) {
		return this.type(FieldInsnNode.class)
				.predicate(SignatureMatcher.matchingFieldInsnOwnerNameDescObf(owner, name, obfName, desc));
	}

	public InsnFinder<FieldInsnNode> fieldInsnObf(String owner, String name, String desc, String obfOwner,
			String obfName, String obfDesc) {
		return this.type(FieldInsnNode.class).predicate(
				SignatureMatcher.matchingFieldInsnOwnerNameDescObf(owner, obfOwner, name, obfName, desc, obfDesc));
	}

	/**
	 * Filters with a signature matcher.
	 *
	 * @param signatureMatcher the matcher to use
	 * @return this finder
	 */
	public InsnFinder<T> predicate(SignatureMatcher<T> signatureMatcher) {
		return this.predicate(signatureMatcher, signatureMatcher);
	}

	/**
	 * Filters with a custom predicate and error details.
	 *
	 * @param predicate the predicate to test instructions
	 * @param errorDetails consumer to append error details if not found
	 * @return this finder
	 */
	public InsnFinder<T> predicate(Predicate<T> predicate, Consumer<StringBuilder> errorDetails) {
		this.predicate = predicate;
		this.errorDetails = errorDetails;
		return this;
	}

	/**
	 * Selects a specific occurrence when multiple instructions match.
	 *
	 * @param ordinal the occurrence index (0 for first match, 1 for second, etc.)
	 * @return this finder
	 */
	public InsnFinder<T> ordinal(int ordinal) {
		this.ordinal = ordinal;
		return this;
	}

	/**
	 * Executes the search and returns the matching instruction.
	 *
	 * @return the matching instruction
	 * @throws NoSuchElementException if no matching instruction is found
	 */
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
			StringBuilder sb = new StringBuilder();
			sb.append("No matching instruction found!");
			sb.append(" ");
			sb.append("start=").append(this.method.instructions.indexOf(this.startInclusive));
			sb.append(" ");
			if (this.advance == NEXT) {
				sb.append("advance=").append("next");
			} else if (this.advance == PREVIOUS) {
				sb.append("advance=").append("previous");
			} else {
				sb.append("advance=").append(this.advance);
			}
			if (this.type != null) {
				sb.append(" ");
				sb.append("type=").append(this.type.getSimpleName());
			}
			if (this.opcode >= 0) {
				sb.append(" ");
				sb.append("opcode=").append(ASMUtil.opcodeName(this.opcode));
			}
			if (this.errorDetails != null) {
				sb.append(" ");
				this.errorDetails.accept(sb);
			}
			sb.append(" ");
			sb.append("ordinal=").append(this.ordinal);
			throw new NoSuchElementException(sb.toString());
		}
		return (T) insn;
	}

}
