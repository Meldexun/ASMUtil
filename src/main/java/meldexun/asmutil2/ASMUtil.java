/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Static helpers for working with the ASM tree API: finding methods, local variables and instructions,
 * building and splicing {@link InsnList}s, and printing classes/methods for debugging.
 * <p>
 * All {@code find*} methods throw a {@link NoSuchElementException} with a description of what was searched for
 * instead of returning {@code null}.
 */
public class ASMUtil {

	/** Logger used for all output of this library. */
	public static final Logger LOGGER = LogManager.getLogger("ASMUtil");
	/** {@code -Dmeldexun.asm.disableLogging=true} disables the "Transforming class/method ..." log messages. */
	public static final boolean DISABLE_LOGGING = Boolean.parseBoolean(System.getProperty("meldexun.asm.disableLogging"));
	/**
	 * {@code -Dmeldexun.asm.export=true} writes every class transformed by an {@link AbstractClassTransformer} to
	 * {@code .meldexun/asm/export/} (the directory is cleared on the first export of each run).
	 */
	public static final boolean EXPORT = Boolean.parseBoolean(System.getProperty("meldexun.asm.export"));
	private static final Path EXPORT_DIR = Paths.get(".meldexun/asm/export");
	private static boolean exportDirCleaned;

	private static final Map<Class<? extends AbstractInsnNode>, Function<? extends AbstractInsnNode, String>> INSN_TO_STRING_SERIALIZERS = new HashMap<>();
	static {
		register(IntInsnNode.class, insn -> insn.operand);
		register(VarInsnNode.class, insn -> insn.var);
		register(TypeInsnNode.class, insn -> insn.desc);
		register(FieldInsnNode.class, insn -> (insn.owner + " " + insn.name + " " + insn.desc));
		register(MethodInsnNode.class, insn -> (insn.owner + " " + insn.name + " " + insn.desc));
		register(JumpInsnNode.class, insn -> insn.label.getLabel());
		register(LabelNode.class, insn -> insn.getLabel());
		register(LdcInsnNode.class, insn -> insn.cst);
		register(FrameNode.class, insn -> (insn.local + " " + insn.stack));
		register(LineNumberNode.class, insn -> insn.line);
	}

	private static <T extends AbstractInsnNode> void register(Class<T> type, Function<T, ?> insnToStringSerializer) {
		INSN_TO_STRING_SERIALIZERS.compute(type, (k, v) -> {
			if (v != null) {
				throw new IllegalArgumentException();
			}
			return insnToStringSerializer.andThen(Object::toString);
		});
	}

	@SuppressWarnings("unchecked")
	private static <T extends AbstractInsnNode> Function<T, String> get(T insn) {
		return (Function<T, String>) INSN_TO_STRING_SERIALIZERS.get(insn.getClass());
	}

	static void exportIfEnabled(String name, byte[] data) {
		if (EXPORT) {
			try {
				export(name, data);
			} catch (IOException e) {
				String errorMessage = String.format("Failed exporting class: %s", name);
				ASMUtil.LOGGER.error(errorMessage, e);
				throw new ClassTransformException(errorMessage, e);
			}
		}
	}

	private static void export(String name, byte[] data) throws IOException {
		if (!exportDirCleaned) {
			synchronized (ASMUtil.class) {
				if (!exportDirCleaned) {
					FileUtil.deleteDirectory(EXPORT_DIR);
					exportDirCleaned = true;
				}
			}
		}
		FileUtil.writeFile(EXPORT_DIR.resolve(name.replace('.', '/') + ".class"), data);
	}

	/**
	 * Converts a class to a human-readable listing for debugging: the class name followed by
	 * {@link #methodToString(MethodNode)} of every method. Fields and attributes are not included.
	 *
	 * @param classNode the class to convert
	 * @return a multi-line listing
	 */
	public static String classToString(ClassNode classNode) {
		StringBuilder sb = new StringBuilder();
		sb.append(classNode.name);
		sb.append('\n');

		sb.append(classNode.methods.stream()
				.map(ASMUtil::methodToString)
				.collect(Collectors.joining("\n\n")));

		return sb.toString();
	}

	/**
	 * Converts a method to a human-readable listing for debugging: name and descriptor followed by one line per
	 * instruction (see {@link #instructionToString(AbstractInsnNode)}), prefixed with its index in the instruction
	 * list. Typically used as {@code ASMUtil.LOGGER.info(ASMUtil.methodToString(methodNode))} while writing a
	 * transformer.
	 *
	 * @param methodNode the method to convert
	 * @return a multi-line listing
	 */
	public static String methodToString(MethodNode methodNode) {
		StringBuilder sb = new StringBuilder();
		sb.append(methodNode.name);
		sb.append(' ');
		sb.append(methodNode.desc);
		sb.append('\n');

		int i = 0;
		for (Iterator<AbstractInsnNode> iterator = methodNode.instructions.iterator(); iterator.hasNext();) {
			AbstractInsnNode insn = iterator.next();
			StringBuilderUtil.append(sb, i, 3);
			sb.append(' ');
			sb.append(instructionToString(insn));
			if (iterator.hasNext()) {
				sb.append('\n');
			}
			i++;
		}

		return sb.toString();
	}

	/**
	 * Converts an instruction to a single line consisting of the opcode name (empty for labels, line numbers and
	 * frames), the instruction node type and its operands.
	 *
	 * @param insn the instruction to convert
	 * @return a single line describing the instruction
	 */
	public static <T extends AbstractInsnNode> String instructionToString(T insn) {
		StringBuilder sb = new StringBuilder();
		StringBuilderUtil.append(sb, opcodeName(insn), 15);
		sb.append(' ');
		StringBuilderUtil.append(sb, insn.getClass().getSimpleName(), 22);
		sb.append(' ');

		Function<T, String> insnToStringSerializer = get(insn);
		if (insnToStringSerializer != null) {
			sb.append(insnToStringSerializer.apply(insn));
		}

		return sb.toString();
	}

	private static String opcodeName(AbstractInsnNode insn) {
		if (insn instanceof LabelNode) {
			return "";
		}
		if (insn instanceof LineNumberNode) {
			return "";
		}
		if (insn instanceof FrameNode) {
			return "";
		}
		return opcodeName(insn.getOpcode());
	}

	static String opcodeName(int opcode) {
		return Arrays.stream(Opcodes.class.getFields())
				.filter(field -> Modifier.isStatic(field.getModifiers()))
				.filter(field -> field.getType() == int.class)
				.filter(field -> !field.getName().startsWith("ASM"))
				.filter(field -> !field.getName().startsWith("V"))
				.filter(field -> !field.getName().startsWith("ACC_"))
				.filter(field -> !field.getName().startsWith("T_"))
				.filter(field -> !field.getName().startsWith("H_"))
				.filter(field -> !field.getName().startsWith("F_"))
				.filter(field -> {
					try {
						return field.getInt(null) == opcode;
					} catch (IllegalArgumentException | IllegalAccessException e) {
						// ignore
						return false;
					}
				})
				.map(Field::getName)
				.findFirst()
				.orElse(Integer.toString(opcode));
	}

	/**
	 * Finds the first method with the given name. If the method is overloaded, use an overload that also takes
	 * the descriptor.
	 *
	 * @param classNode the class to search
	 * @param name the method name
	 * @return the first matching method
	 * @throws NoSuchElementException if no method matches
	 */
	public static MethodNode find(ClassNode classNode, String name) {
		return find(classNode, SignatureMatcher.matchingMethodName(name));
	}

	/**
	 * Finds the first method whose name is equal to <b>either</b> {@code name} or {@code obfName}, so the same
	 * transformer works against both the deobfuscated and the obfuscated version of a class (e.g. MCP and SRG names
	 * in Minecraft). The environment is not detected, both names are always accepted.
	 *
	 * @param classNode the class to search
	 * @param name the deobfuscated method name
	 * @param obfName the obfuscated method name
	 * @return the first matching method
	 * @throws NoSuchElementException if no method matches
	 */
	public static MethodNode findObf(ClassNode classNode, String name, String obfName) {
		return find(classNode, SignatureMatcher.matchingMethodNameObf(name, obfName));
	}

	/**
	 * Finds the method with the given name and descriptor.
	 *
	 * @param classNode the class to search
	 * @param name the method name
	 * @param desc the method descriptor, e.g. {@code (Ljava/lang/String;)V}
	 * @return the matching method
	 * @throws NoSuchElementException if no method matches
	 */
	public static MethodNode find(ClassNode classNode, String name, String desc) {
		return find(classNode, SignatureMatcher.matchingMethodNameDesc(name, desc));
	}

	/**
	 * Finds the method whose name is equal to either {@code name} or {@code obfName} and whose descriptor is equal
	 * to {@code desc}. Use this when only member names are obfuscated but class names are not (as is the case for
	 * Forge coremod transformers, which run after Forge remapped class names).
	 *
	 * @param classNode the class to search
	 * @param name the deobfuscated method name
	 * @param obfName the obfuscated method name
	 * @param desc the method descriptor
	 * @return the matching method
	 * @throws NoSuchElementException if no method matches
	 */
	public static MethodNode findObf(ClassNode classNode, String name, String obfName, String desc) {
		return find(classNode, SignatureMatcher.matchingMethodNameDescObf(name, obfName, desc));
	}

	/**
	 * Finds the method whose name is equal to either {@code name} or {@code obfName} and whose descriptor is equal
	 * to either {@code desc} or {@code obfDesc}. Note the parameter order {@code (name, desc, obfName, obfDesc)},
	 * which differs from {@link SignatureMatcher#matchingMethodNameDescObf(String, String, String, String)}.
	 *
	 * @param classNode the class to search
	 * @param name the deobfuscated method name
	 * @param desc the deobfuscated descriptor
	 * @param obfName the obfuscated method name
	 * @param obfDesc the obfuscated descriptor
	 * @return the matching method
	 * @throws NoSuchElementException if no method matches
	 */
	public static MethodNode findObf(ClassNode classNode, String name, String desc, String obfName, String obfDesc) {
		return find(classNode, SignatureMatcher.matchingMethodNameDescObf(name, obfName, desc, obfDesc));
	}

	/**
	 * Finds the first method accepted by the given matcher.
	 *
	 * @param classNode the class to search
	 * @param signatureMatcher the matcher, also used to describe the method in the error message
	 * @return the first matching method
	 * @throws NoSuchElementException if no method matches
	 */
	public static MethodNode find(ClassNode classNode, SignatureMatcher<MethodNode> signatureMatcher) {
		return find(classNode, signatureMatcher, signatureMatcher);
	}

	/**
	 * Finds the first method accepted by the given predicate.
	 *
	 * @param classNode the class to search
	 * @param predicate the predicate to test methods
	 * @param errorDetails appends a description of the searched method to the error message, may be {@code null}
	 * @return the first matching method
	 * @throws NoSuchElementException if no method matches
	 */
	public static MethodNode find(ClassNode classNode, Predicate<MethodNode> predicate,
			Consumer<StringBuilder> errorDetails) {
		for (MethodNode methodNode : classNode.methods) {
			if (predicate.test(methodNode)) {
				return methodNode;
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("No matching method found!");
		if (errorDetails != null) {
			sb.append(" ");
			errorDetails.accept(sb);
		}
		throw new NoSuchElementException(sb.toString());
	}

	/**
	 * Creates an {@link InsnFinder} searching forwards, starting at (and including) the first instruction.
	 *
	 * @param methodNode the method to search
	 * @return a new instruction finder
	 */
	public static InsnFinder<AbstractInsnNode> first(MethodNode methodNode) {
		return InsnFinder.first(methodNode);
	}

	/**
	 * Creates an {@link InsnFinder} searching backwards, starting at (and including) the last instruction.
	 *
	 * @param methodNode the method to search
	 * @return a new instruction finder
	 */
	public static InsnFinder<AbstractInsnNode> last(MethodNode methodNode) {
		return InsnFinder.last(methodNode);
	}

	/**
	 * Creates an {@link InsnFinder} searching forwards, starting at the instruction after {@code startExclusive}.
	 *
	 * @param methodNode the method to search
	 * @param startExclusive the instruction to start after
	 * @return a new instruction finder
	 */
	public static InsnFinder<AbstractInsnNode> nextExclusive(MethodNode methodNode, AbstractInsnNode startExclusive) {
		return InsnFinder.nextExclusive(methodNode, startExclusive);
	}

	/**
	 * Creates an {@link InsnFinder} searching backwards, starting at the instruction before {@code startExclusive}.
	 *
	 * @param methodNode the method to search
	 * @param startExclusive the instruction to start before
	 * @return a new instruction finder
	 */
	public static InsnFinder<AbstractInsnNode> prevExclusive(MethodNode methodNode, AbstractInsnNode startExclusive) {
		return InsnFinder.prevExclusive(methodNode, startExclusive);
	}

	/**
	 * Creates an {@link InsnFinder} searching forwards, starting at (and including) {@code startInclusive}.
	 *
	 * @param methodNode the method to search
	 * @param startInclusive the instruction to start at
	 * @return a new instruction finder
	 */
	public static InsnFinder<AbstractInsnNode> next(MethodNode methodNode, AbstractInsnNode startInclusive) {
		return InsnFinder.next(methodNode, startInclusive);
	}

	/**
	 * Creates an {@link InsnFinder} searching backwards, starting at (and including) {@code startInclusive}.
	 *
	 * @param methodNode the method to search
	 * @param startInclusive the instruction to start at
	 * @return a new instruction finder
	 */
	public static InsnFinder<AbstractInsnNode> prev(MethodNode methodNode, AbstractInsnNode startInclusive) {
		return InsnFinder.prev(methodNode, startInclusive);
	}

	/*
	 * Fixed arity overloads of listOf(AbstractInsnNode...) that avoid allocating an array.
	 */

	public static InsnList listOf(AbstractInsnNode insn0) {
		InsnList list = new InsnList();
		list.add(insn0);
		return list;
	}

	public static InsnList listOf(AbstractInsnNode insn0, AbstractInsnNode insn1) {
		InsnList list = new InsnList();
		list.add(insn0);
		list.add(insn1);
		return list;
	}

	public static InsnList listOf(AbstractInsnNode insn0, AbstractInsnNode insn1, AbstractInsnNode insn2) {
		InsnList list = new InsnList();
		list.add(insn0);
		list.add(insn1);
		list.add(insn2);
		return list;
	}

	public static InsnList listOf(AbstractInsnNode insn0, AbstractInsnNode insn1, AbstractInsnNode insn2,
			AbstractInsnNode insn3) {
		InsnList list = new InsnList();
		list.add(insn0);
		list.add(insn1);
		list.add(insn2);
		list.add(insn3);
		return list;
	}

	public static InsnList listOf(AbstractInsnNode insn0, AbstractInsnNode insn1, AbstractInsnNode insn2,
			AbstractInsnNode insn3, AbstractInsnNode insn4) {
		InsnList list = new InsnList();
		list.add(insn0);
		list.add(insn1);
		list.add(insn2);
		list.add(insn3);
		list.add(insn4);
		return list;
	}

	public static InsnList listOf(AbstractInsnNode insn0, AbstractInsnNode insn1, AbstractInsnNode insn2,
			AbstractInsnNode insn3, AbstractInsnNode insn4, AbstractInsnNode insn5) {
		InsnList list = new InsnList();
		list.add(insn0);
		list.add(insn1);
		list.add(insn2);
		list.add(insn3);
		list.add(insn4);
		list.add(insn5);
		return list;
	}

	public static InsnList listOf(AbstractInsnNode insn0, AbstractInsnNode insn1, AbstractInsnNode insn2,
			AbstractInsnNode insn3, AbstractInsnNode insn4, AbstractInsnNode insn5, AbstractInsnNode insn6) {
		InsnList list = new InsnList();
		list.add(insn0);
		list.add(insn1);
		list.add(insn2);
		list.add(insn3);
		list.add(insn4);
		list.add(insn5);
		list.add(insn6);
		return list;
	}

	public static InsnList listOf(AbstractInsnNode insn0, AbstractInsnNode insn1, AbstractInsnNode insn2,
			AbstractInsnNode insn3, AbstractInsnNode insn4, AbstractInsnNode insn5, AbstractInsnNode insn6,
			AbstractInsnNode insn7) {
		InsnList list = new InsnList();
		list.add(insn0);
		list.add(insn1);
		list.add(insn2);
		list.add(insn3);
		list.add(insn4);
		list.add(insn5);
		list.add(insn6);
		list.add(insn7);
		return list;
	}

	/**
	 * Creates a new instruction list containing the given instructions in order. An instruction can only be part
	 * of one list, so the instructions must not already be in another list (e.g. the method's instructions).
	 *
	 * @param insns the instructions to add
	 * @return a new instruction list
	 */
	public static InsnList listOf(AbstractInsnNode... insns) {
		InsnList list = new InsnList();
		for (AbstractInsnNode insn : insns) {
			list.add(insn);
		}
		return list;
	}

	/**
	 * Creates a new {@link LabelNode} and passes it to {@code mappingFunction}. This allows building a list with
	 * jumps inline, without declaring the label as a local variable first:
	 *
	 * <pre>
	 * ASMUtil.listWithLabel(skip -&gt; ASMUtil.listOf(
	 *         new VarInsnNode(Opcodes.ALOAD, 1),
	 *         new JumpInsnNode(Opcodes.IFNULL, skip),
	 *         ...,
	 *         skip));
	 * </pre>
	 *
	 * The label still has to be added to the returned list by {@code mappingFunction}.
	 *
	 * @param mappingFunction function that creates the instruction list using the label
	 * @return the list returned by {@code mappingFunction}
	 */
	public static InsnList listWithLabel(Function<LabelNode, InsnList> mappingFunction) {
		return mappingFunction.apply(new LabelNode());
	}

	/**
	 * Like {@link #listWithLabel(Function)}, with two new labels.
	 *
	 * @param mappingFunction function that creates the instruction list using the labels
	 * @return the list returned by {@code mappingFunction}
	 */
	public static InsnList listWithLabels(BiFunction<LabelNode, LabelNode, InsnList> mappingFunction) {
		return mappingFunction.apply(new LabelNode(), new LabelNode());
	}

	/**
	 * Like {@link #listWithLabel(Function)}, with three new labels.
	 *
	 * @param mappingFunction function that creates the instruction list using the labels
	 * @return the list returned by {@code mappingFunction}
	 */
	public static InsnList listWithLabels(TriFunction<LabelNode, LabelNode, LabelNode, InsnList> mappingFunction) {
		return mappingFunction.apply(new LabelNode(), new LabelNode(), new LabelNode());
	}

	/**
	 * Like {@link #listWithLabel(Function)}, with four new labels.
	 *
	 * @param mappingFunction function that creates the instruction list using the labels
	 * @return the list returned by {@code mappingFunction}
	 */
	public static InsnList listWithLabels(QuadFunction<LabelNode, LabelNode, LabelNode, LabelNode, InsnList> mappingFunction) {
		return mappingFunction.apply(new LabelNode(), new LabelNode(), new LabelNode(), new LabelNode());
	}

	/**
	 * Removes all instructions from a method. Like {@link #replaceAll(MethodNode, InsnList)} this does not touch
	 * {@link MethodNode#tryCatchBlocks} or {@link MethodNode#localVariables}, which may reference labels that were
	 * removed.
	 *
	 * @param methodNode the method to modify
	 * @return a list containing the removed instructions, which can be inserted somewhere else
	 */
	public static InsnList removeAll(MethodNode methodNode) {
		return remove(methodNode, methodNode.instructions.getFirst(), methodNode.instructions.getLast());
	}

	/**
	 * Removes all instructions from {@code start} to {@code end} (both inclusive).
	 *
	 * @param methodNode the method to modify
	 * @param start the first instruction to remove
	 * @param end the last instruction to remove, must be {@code start} or come after it
	 * @return a list containing the removed instructions, which can be inserted somewhere else
	 */
	public static InsnList remove(MethodNode methodNode, AbstractInsnNode start, AbstractInsnNode end) {
		InsnList list = new InsnList();
		AbstractInsnNode insn = start;
		while (true) {
			AbstractInsnNode next = insn.getNext();
			methodNode.instructions.remove(insn);
			list.add(insn);
			if (insn == end) {
				break;
			}
			insn = next;
		}
		return list;
	}

	/**
	 * Replaces the whole method body with a single instruction.
	 *
	 * @param methodNode the method to modify
	 * @param replacement the new instruction
	 * @see #replaceAll(MethodNode, InsnList)
	 */
	public static void replaceAll(MethodNode methodNode, AbstractInsnNode replacement) {
		methodNode.instructions.clear();
		methodNode.instructions.insert(replacement);
	}

	/**
	 * Replaces the whole method body. Only the instructions are replaced: if the method had try-catch blocks or a
	 * local variable table, clear {@link MethodNode#tryCatchBlocks} and {@link MethodNode#localVariables} as well,
	 * since they reference labels that are no longer part of the method.
	 *
	 * @param methodNode the method to modify
	 * @param replacement the new instructions
	 */
	public static void replaceAll(MethodNode methodNode, InsnList replacement) {
		methodNode.instructions.clear();
		methodNode.instructions.insert(replacement);
	}

	/**
	 * Replaces a single instruction.
	 *
	 * @param methodNode the method to modify
	 * @param target the instruction to remove
	 * @param replacement the instruction inserted where {@code target} was
	 */
	public static void replace(MethodNode methodNode, AbstractInsnNode target, AbstractInsnNode replacement) {
		AbstractInsnNode previous = target.getPrevious();
		methodNode.instructions.remove(target);
		if (previous != null) {
			methodNode.instructions.insert(previous, replacement);
		} else {
			methodNode.instructions.insert(replacement);
		}
	}

	/**
	 * Replaces a single instruction with a list of instructions.
	 *
	 * @param methodNode the method to modify
	 * @param target the instruction to remove
	 * @param replacement the instructions inserted where {@code target} was
	 */
	public static void replace(MethodNode methodNode, AbstractInsnNode target, InsnList replacement) {
		AbstractInsnNode previous = target.getPrevious();
		methodNode.instructions.remove(target);
		if (previous != null) {
			methodNode.instructions.insert(previous, replacement);
		} else {
			methodNode.instructions.insert(replacement);
		}
	}

	/**
	 * Replaces all instructions from {@code start} to {@code end} (both inclusive) with a single instruction.
	 *
	 * @param methodNode the method to modify
	 * @param start the first instruction to remove
	 * @param end the last instruction to remove, must be {@code start} or come after it
	 * @param replacement the instruction inserted where the range was
	 */
	public static void replace(MethodNode methodNode, AbstractInsnNode start, AbstractInsnNode end,
			AbstractInsnNode replacement) {
		AbstractInsnNode previous = start.getPrevious();
		ASMUtil.removeNoResult(methodNode, start, end);
		if (previous != null) {
			methodNode.instructions.insert(previous, replacement);
		} else {
			methodNode.instructions.insert(replacement);
		}
	}

	/**
	 * Replaces all instructions from {@code start} to {@code end} (both inclusive) with a list of instructions.
	 *
	 * @param methodNode the method to modify
	 * @param start the first instruction to remove
	 * @param end the last instruction to remove, must be {@code start} or come after it
	 * @param replacement the instructions inserted where the range was
	 */
	public static void replace(MethodNode methodNode, AbstractInsnNode start, AbstractInsnNode end,
			InsnList replacement) {
		AbstractInsnNode previous = start.getPrevious();
		ASMUtil.removeNoResult(methodNode, start, end);
		if (previous != null) {
			methodNode.instructions.insert(previous, replacement);
		} else {
			methodNode.instructions.insert(replacement);
		}
	}

	private static void removeNoResult(MethodNode methodNode, AbstractInsnNode start, AbstractInsnNode end) {
		AbstractInsnNode insn = start;
		while (true) {
			AbstractInsnNode next = insn.getNext();
			methodNode.instructions.remove(insn);
			if (insn == end) {
				break;
			}
			insn = next;
		}
	}

	/**
	 * Finds the first local variable with the given name.
	 * <p>
	 * All {@code findLocalVariable*} methods search {@link MethodNode#localVariables}, i.e. the
	 * {@code LocalVariableTable} debug attribute. Classes compiled without debug information (or with stripped
	 * debug information) have no entries, in which case the variable index has to be hardcoded instead. Entries
	 * are searched in table order, which is not necessarily declaration order, and a variable that is declared in
	 * several scopes (e.g. two loops each declaring {@code i}) has one entry per scope.
	 *
	 * @param methodNode the method to search
	 * @param name the variable name
	 * @return the matching local variable, use {@link LocalVariableNode#index} for load/store instructions
	 * @throws NoSuchElementException if no variable matches
	 */
	public static LocalVariableNode findLocalVariable(MethodNode methodNode, String name) {
		return findLocalVariable(methodNode, name, 0);
	}

	/**
	 * Finds the {@code ordinal}-th local variable with the given name.
	 *
	 * @param methodNode the method to search
	 * @param name the variable name
	 * @param ordinal the number of matching variables to skip (0 for the first match)
	 * @return the matching local variable
	 * @throws NoSuchElementException if no variable matches
	 * @see #findLocalVariable(MethodNode, String)
	 */
	public static LocalVariableNode findLocalVariable(MethodNode methodNode, String name, int ordinal) {
		return findLocalVariable(methodNode, name, null, ordinal);
	}

	/**
	 * Finds the first local variable with the given type.
	 *
	 * @param methodNode the method to search
	 * @param desc the variable type descriptor, e.g. {@code Lnet/minecraft/world/World;} or {@code I}
	 * @return the matching local variable
	 * @throws NoSuchElementException if no variable matches
	 * @see #findLocalVariable(MethodNode, String)
	 */
	public static LocalVariableNode findLocalVariableDesc(MethodNode methodNode, String desc) {
		return findLocalVariableDesc(methodNode, desc, 0);
	}

	/**
	 * Finds the {@code ordinal}-th local variable with the given type.
	 *
	 * @param methodNode the method to search
	 * @param desc the variable type descriptor
	 * @param ordinal the number of matching variables to skip (0 for the first match)
	 * @return the matching local variable
	 * @throws NoSuchElementException if no variable matches
	 * @see #findLocalVariable(MethodNode, String)
	 */
	public static LocalVariableNode findLocalVariableDesc(MethodNode methodNode, String desc, int ordinal) {
		return findLocalVariable(methodNode, null, desc, ordinal);
	}

	/**
	 * Finds the first local variable with the given name and type.
	 *
	 * @param methodNode the method to search
	 * @param name the variable name
	 * @param desc the variable type descriptor
	 * @return the matching local variable
	 * @throws NoSuchElementException if no variable matches
	 * @see #findLocalVariable(MethodNode, String)
	 */
	public static LocalVariableNode findLocalVariable(MethodNode methodNode, String name, String desc) {
		return findLocalVariable(methodNode, name, desc, 0);
	}

	/**
	 * Finds the {@code ordinal}-th local variable with the given name and type.
	 *
	 * @param methodNode the method to search
	 * @param name the variable name, or {@code null} to match any name
	 * @param desc the variable type descriptor, or {@code null} to match any type
	 * @param ordinal the number of matching variables to skip (0 for the first match)
	 * @return the matching local variable
	 * @throws NoSuchElementException if no variable matches
	 * @see #findLocalVariable(MethodNode, String)
	 */
	public static LocalVariableNode findLocalVariable(MethodNode methodNode, String name, String desc, int ordinal) {
		int i = 0;
		for (LocalVariableNode localVariable : methodNode.localVariables) {
			if ((name == null || localVariable.name.equals(name)) && (desc == null || localVariable.desc.equals(desc))
					&& i++ == ordinal) {
				return localVariable;
			}
		}
		throw new NoSuchElementException(
				String.format("No local variable with name=%s desc=%s ordinal=%s found!", name, desc, ordinal));
	}

	/**
	 * Adds an entry to the local variable table, using the index after the highest index currently in the table.
	 * Afterwards the index can be looked up with {@link #findLocalVariable(MethodNode, String)}.
	 * <p>
	 * Things to be aware of:
	 * <ul>
	 * <li>{@code start} and {@code end} are only referenced, they still have to be inserted into the method's
	 * instructions (with {@code start} before {@code end}), otherwise writing the class fails.</li>
	 * <li>The index is derived from the local variable table only. If the table is empty or incomplete (no debug
	 * information) or the last variable is a {@code long}/{@code double} (which occupy two slots), the chosen index
	 * may collide with an existing variable.</li>
	 * <li>{@link MethodNode#maxLocals} is not updated, use {@link org.objectweb.asm.ClassWriter#COMPUTE_MAXS} or
	 * {@link org.objectweb.asm.ClassWriter#COMPUTE_FRAMES}.</li>
	 * </ul>
	 *
	 * @param methodNode the method to modify
	 * @param name the variable name
	 * @param desc the variable type descriptor
	 * @param start the label at which the variable's scope starts
	 * @param end the label at which the variable's scope ends
	 */
	public static void addLocalVariable(MethodNode methodNode, String name, String desc, LabelNode start, LabelNode end) {
		int i = 0;
		for (LocalVariableNode localVariable : methodNode.localVariables) {
			if (localVariable.index >= i) {
				i = localVariable.index + 1;
			}
		}
		methodNode.localVariables.add(new LocalVariableNode(name, desc, null, start, end, i));
	}

	/**
	 * Returns a stream of all methods of a class.
	 *
	 * @param classNode the class
	 * @return a stream of methods
	 */
	public static Stream<MethodNode> stream(ClassNode classNode) {
		return classNode.methods.stream();
	}

	/**
	 * Returns a stream of all instructions of a method.
	 *
	 * @param methodNode the method
	 * @return a stream of instructions
	 * @see #stream(InsnList)
	 */
	public static Stream<AbstractInsnNode> stream(MethodNode methodNode) {
		return stream(methodNode.instructions);
	}

	/**
	 * Returns a stream of all instructions in an instruction list. Adding or removing instructions while the
	 * stream is being consumed causes a {@link java.util.ConcurrentModificationException}; collect the instructions
	 * into a list first (or use {@link #forEach(InsnList, BiConsumer)}). Modifying the instructions themselves (e.g.
	 * changing the owner of a {@link MethodInsnNode}) is fine.
	 *
	 * @param instructions the instruction list
	 * @return a stream of instructions
	 */
	public static Stream<AbstractInsnNode> stream(InsnList instructions) {
		return StreamSupport.stream(new InsnSpliterator(instructions), false);
	}

	/*
	 * The forEach overloads iterate over an instruction list, optionally only visiting instructions of a given type
	 * and/or accepted by a filter. The variants taking a BiConsumer additionally pass the list iterator, which can
	 * be used to remove, replace (set) or add instructions at the current position while iterating.
	 */

	/**
	 * Calls {@code action} for every instruction.
	 *
	 * @param instructions the instruction list to iterate
	 * @param action the action to perform on each instruction
	 */
	public static void forEach(InsnList instructions, Consumer<AbstractInsnNode> action) {
		forEach(instructions, null, null, action);
	}

	/**
	 * Calls {@code action} for every instruction of the given type.
	 *
	 * @param instructions the instruction list to iterate
	 * @param type the instruction node class, e.g. {@code MethodInsnNode.class}
	 * @param action the action to perform on each matching instruction
	 */
	public static <T extends AbstractInsnNode> void forEach(InsnList instructions, Class<T> type, Consumer<T> action) {
		forEach(instructions, type, null, action);
	}

	/**
	 * Calls {@code action} for every instruction accepted by {@code filter}.
	 *
	 * @param instructions the instruction list to iterate
	 * @param filter the filter predicate
	 * @param action the action to perform on each matching instruction
	 */
	public static void forEach(InsnList instructions, Predicate<AbstractInsnNode> filter,
			Consumer<AbstractInsnNode> action) {
		forEach(instructions, null, filter, action);
	}

	/**
	 * Calls {@code action} for every instruction of the given type that is accepted by {@code filter}.
	 *
	 * @param instructions the instruction list to iterate
	 * @param type the instruction node class, or {@code null} for any type
	 * @param filter the filter predicate, or {@code null} to accept all instructions
	 * @param action the action to perform on each matching instruction
	 */
	public static <T extends AbstractInsnNode> void forEach(InsnList instructions, Class<T> type, Predicate<T> filter,
			Consumer<T> action) {
		forEach(instructions, type, filter, (iterator, insn) -> action.accept(insn));
	}

	/**
	 * Calls {@code action} for every instruction, passing the iterator positioned after the instruction.
	 *
	 * @param instructions the instruction list to iterate
	 * @param action the action receiving the iterator and the current instruction
	 */
	public static void forEach(InsnList instructions,
			BiConsumer<ListIterator<AbstractInsnNode>, AbstractInsnNode> action) {
		forEach(instructions, null, null, action);
	}

	/**
	 * Calls {@code action} for every instruction of the given type, passing the iterator positioned after the
	 * instruction.
	 *
	 * @param instructions the instruction list to iterate
	 * @param type the instruction node class
	 * @param action the action receiving the iterator and the current instruction
	 */
	public static <T extends AbstractInsnNode> void forEach(InsnList instructions, Class<T> type,
			BiConsumer<ListIterator<AbstractInsnNode>, T> action) {
		forEach(instructions, type, null, action);
	}

	/**
	 * Calls {@code action} for every instruction accepted by {@code filter}, passing the iterator positioned after
	 * the instruction.
	 *
	 * @param instructions the instruction list to iterate
	 * @param filter the filter predicate
	 * @param action the action receiving the iterator and the current instruction
	 */
	public static void forEach(InsnList instructions, Predicate<AbstractInsnNode> filter,
			BiConsumer<ListIterator<AbstractInsnNode>, AbstractInsnNode> action) {
		forEach(instructions, null, filter, action);
	}

	/**
	 * Calls {@code action} for every instruction of the given type that is accepted by {@code filter}, passing the
	 * iterator positioned after the instruction.
	 *
	 * @param instructions the instruction list to iterate
	 * @param type the instruction node class, or {@code null} for any type
	 * @param filter the filter predicate, or {@code null} to accept all instructions
	 * @param action the action receiving the iterator and the current instruction
	 */
	@SuppressWarnings("unchecked")
	public static <T extends AbstractInsnNode> void forEach(InsnList instructions, Class<T> type, Predicate<T> filter,
			BiConsumer<ListIterator<AbstractInsnNode>, T> action) {
		ListIterator<AbstractInsnNode> iterator = instructions.iterator();
		while (iterator.hasNext()) {
			AbstractInsnNode insn = iterator.next();
			if ((type == null || type.isInstance(insn)) && (filter == null || filter.test((T) insn))) {
				action.accept(iterator, (T) insn);
			}
		}
	}

}
