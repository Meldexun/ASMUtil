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
 * Fluent builder for finding a single instruction, usually created via {@link ASMUtil#first(MethodNode)},
 * {@link ASMUtil#last(MethodNode)}, {@link ASMUtil#next(MethodNode, AbstractInsnNode)} or
 * {@link ASMUtil#prev(MethodNode, AbstractInsnNode)}:
 *
 * <pre>
 * MethodInsnNode call = ASMUtil.first(methodNode)
 *         .opcode(Opcodes.INVOKEVIRTUAL)
 *         .methodInsn("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;")
 *         .ordinal(1)
 *         .find();
 * </pre>
 *
 * An instruction matches if it passes all three filters: <b>type</b> (instruction node class), <b>opcode</b> and
 * <b>predicate</b>. Each filter holds a single value, so a later call replaces an earlier one: every
 * {@code xyzInsn(...)} method sets both the type and the predicate, so e.g. {@code .methodInsn(..).ldcInsn(..)}
 * only searches for the LDC instruction. Use {@link #predicate(Predicate, Consumer)} to combine conditions.
 * <p>
 * The methods return this finder (cast to the new instruction type where needed), they do not create copies.
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
	 * @param method the method to search in (only used for error messages and local variable lookups)
	 * @param startInclusive the instruction to start at, must not be {@code null} (so the method must not be empty)
	 * @param advance returns the instruction to test after the given one, or {@code null} to stop
	 */
	public InsnFinder(MethodNode method, AbstractInsnNode startInclusive, UnaryOperator<AbstractInsnNode> advance) {
		this.method = Objects.requireNonNull(method);
		this.startInclusive = Objects.requireNonNull(startInclusive);
		this.advance = Objects.requireNonNull(advance);
	}

	/**
	 * Creates a finder searching forwards, starting at (and including) the first instruction.
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
	 * Finds the matching instruction and returns a new finder searching forwards from the instruction after it.
	 * Useful to find an instruction relative to another one:
	 * {@code ASMUtil.first(m).methodInsn("foo").findThenNextExclusive().type(JumpInsnNode.class).find()}.
	 *
	 * @return a new instruction finder
	 * @throws NoSuchElementException if no instruction matches
	 */
	public InsnFinder<AbstractInsnNode> findThenNextExclusive() {
		return ASMUtil.nextExclusive(this.method, this.find());
	}

	/**
	 * Finds the matching instruction and returns a new finder searching backwards from the instruction before it.
	 *
	 * @return a new instruction finder
	 * @throws NoSuchElementException if no instruction matches
	 */
	public InsnFinder<AbstractInsnNode> findThenPrevExclusive() {
		return ASMUtil.prevExclusive(this.method, this.find());
	}

	/**
	 * Finds the matching instruction and returns a new finder searching forwards, starting at (and including) it.
	 *
	 * @return a new instruction finder
	 * @throws NoSuchElementException if no instruction matches
	 */
	public InsnFinder<AbstractInsnNode> findThenNext() {
		return ASMUtil.next(this.method, this.find());
	}

	/**
	 * Finds the matching instruction and returns a new finder searching backwards, starting at (and including) it.
	 *
	 * @return a new instruction finder
	 * @throws NoSuchElementException if no instruction matches
	 */
	public InsnFinder<AbstractInsnNode> findThenPrev() {
		return ASMUtil.prev(this.method, this.find());
	}

	/**
	 * Only matches instructions of the given node class (e.g. {@code LabelNode.class}, {@code JumpInsnNode.class}).
	 * Replaces a previously set type.
	 *
	 * @param <R> the instruction type
	 * @param type the instruction node class to match
	 * @return this finder
	 */
	@SuppressWarnings("unchecked")
	public <R extends AbstractInsnNode> InsnFinder<R> type(Class<R> type) {
		InsnFinder<R> n = (InsnFinder<R>) this;
		n.type = type;
		return n;
	}

	/**
	 * Only matches instructions with the given opcode. Replaces a previously set opcode.
	 *
	 * @param opcode the opcode to match (e.g. {@code Opcodes.ALOAD})
	 * @return this finder
	 */
	public InsnFinder<T> opcode(int opcode) {
		this.opcode = opcode;
		return this;
	}

	/**
	 * Matches {@link TypeInsnNode}s (NEW, ANEWARRAY, CHECKCAST, INSTANCEOF) with the given type.
	 *
	 * @param desc the operand as stored in {@link TypeInsnNode#desc}: an internal name such as
	 *        {@code java/lang/String} (not {@code Ljava/lang/String;}), or an array descriptor
	 * @return this finder typed for TypeInsnNode
	 */
	public InsnFinder<TypeInsnNode> typeInsn(String desc) {
		return this.type(TypeInsnNode.class).predicate(insn -> insn.desc.equals(desc),
				sb -> sb.append("desc=").append(desc));
	}

	/**
	 * Matches LDC instructions loading the given constant, compared with {@link Objects#equals(Object, Object)},
	 * so the boxed type has to match too ({@code 1.5} matches a double constant, {@code 1.5F} a float constant).
	 * Small int constants are not loaded with LDC but with ICONST_*, BIPUSH or SIPUSH, see {@link #intInsn(int)}.
	 *
	 * @param cst the constant to match, e.g. a String, boxed number or {@link org.objectweb.asm.Type}
	 * @return this finder typed for LdcInsnNode
	 */
	public InsnFinder<LdcInsnNode> ldcInsn(Object cst) {
		return this.type(LdcInsnNode.class).predicate(insn -> Objects.equals(insn.cst, cst),
				sb -> sb.append("cst=").append(cst));
	}

	/**
	 * Matches {@link IntInsnNode}s (BIPUSH, SIPUSH, NEWARRAY) with the given operand. ICONST_M1 to ICONST_5 are
	 * plain {@link org.objectweb.asm.tree.InsnNode}s, search those with {@link #opcode(int)} instead.
	 *
	 * @param operand the operand value to match
	 * @return this finder typed for IntInsnNode
	 */
	public InsnFinder<IntInsnNode> intInsn(int operand) {
		return this.type(IntInsnNode.class).predicate(insn -> insn.operand == operand,
				sb -> sb.append("operand=").append(operand));
	}

	/**
	 * Matches load/store instructions of the local variable with the given name.
	 * <p>
	 * All name/descriptor based {@code varInsn*} methods resolve the variable index immediately through
	 * {@link ASMUtil#findLocalVariable(MethodNode, String, String, int)}, so they throw right away if the method has no
	 * matching local variable table entry. The resulting filter only compares the index; it does not check whether
	 * the instruction lies in the variable's scope.
	 *
	 * @param name the local variable name
	 * @return this finder typed for VarInsnNode
	 * @throws NoSuchElementException if there is no local variable with this name
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
	 * Matches method invocations of a method with the given name.
	 * <p>
	 * The {@code methodInsn*} and {@code fieldInsn*} overloads take the parts that have to match. The {@code Obf}
	 * variants accept either of two names, see {@link SignatureMatcher}. Note that the 6 argument variants take
	 * {@code (owner, name, desc, obfOwner, obfName, obfDesc)}, unlike the corresponding {@link SignatureMatcher}
	 * methods.
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
	 * Matches field accesses of a field with the given name.
	 *
	 * @param name the field name
	 * @return this finder typed for FieldInsnNode
	 * @see #methodInsn(String)
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
	 * Only matches instructions accepted by the given matcher. Replaces a previously set predicate.
	 *
	 * @param signatureMatcher the matcher, also used to describe the instruction in the error message
	 * @return this finder
	 */
	public InsnFinder<T> predicate(SignatureMatcher<T> signatureMatcher) {
		return this.predicate(signatureMatcher, signatureMatcher);
	}

	/**
	 * Only matches instructions accepted by the given predicate. Replaces a previously set predicate. The predicate
	 * is only called for instructions that passed the type and opcode filters, so it can safely cast to {@code T}.
	 *
	 * @param predicate the predicate to test instructions
	 * @param errorDetails appends a description of the searched instruction to the error message, may be
	 *        {@code null}
	 * @return this finder
	 */
	public InsnFinder<T> predicate(Predicate<T> predicate, Consumer<StringBuilder> errorDetails) {
		this.predicate = predicate;
		this.errorDetails = errorDetails;
		return this;
	}

	/**
	 * Skips the first {@code ordinal} matching instructions. Occurrences are counted in search direction, so with a
	 * backwards finder {@code ordinal(1)} returns the second to last match.
	 *
	 * @param ordinal the number of matches to skip (0 for the first match, 1 for the second, etc.)
	 * @return this finder
	 */
	public InsnFinder<T> ordinal(int ordinal) {
		this.ordinal = ordinal;
		return this;
	}

	/**
	 * Runs the search. Never returns {@code null}.
	 *
	 * @return the matching instruction
	 * @throws NoSuchElementException if no instruction matches, with a message listing the start index, direction
	 *         and all filters
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
