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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
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

public class ASMUtil {

	public static final Logger LOGGER = LogManager.getLogger("ASMUtil");
	public static final boolean DISABLE_LOGGING = Boolean.parseBoolean(System.getProperty("meldexun.asm.disableLogging"));
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

	public static String classToString(ClassNode classNode) {
		StringBuilder sb = new StringBuilder();
		sb.append(classNode.name);
		sb.append('\n');

		sb.append(classNode.methods.stream()
				.map(ASMUtil::methodToString)
				.collect(Collectors.joining("\n\n")));

		return sb.toString();
	}

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
						return field.getInt(null) == insn.getOpcode();
					} catch (IllegalArgumentException | IllegalAccessException e) {
						// ignore
						return false;
					}
				})
				.map(Field::getName)
				.findFirst()
				.orElse("UNKNOWN");
	}

	public static MethodNode find(ClassNode classNode, String name) {
		return find(classNode, MethodNodeUtil.matching(name));
	}

	public static MethodNode findObf(ClassNode classNode, String name, String obfName) {
		return find(classNode, MethodNodeUtil.matching(name, obfName));
	}

	public static MethodNode find(ClassNode classNode, String name, String desc) {
		return find(classNode, MethodNodeUtil.matching(name, desc));
	}

	public static MethodNode find(ClassNode classNode, String name, String obfName, String desc) {
		return find(classNode, MethodNodeUtil.matching(name, obfName, desc));
	}

	public static MethodNode find(ClassNode classNode, String name, String desc, String obfName, String obfDesc) {
		return find(classNode, MethodNodeUtil.matching(name, desc, obfName, obfDesc));
	}

	public static MethodNode find(ClassNode classNode, Predicate<MethodNode> predicate) {
		for (MethodNode methodNode : classNode.methods) {
			if (predicate.test(methodNode)) {
				return methodNode;
			}
		}
		throw new NoSuchElementException();
	}

	public static <T extends AbstractInsnNode> InsnFinder<T> first(MethodNode methodNode) {
		return InsnFinder.<T>create().first(methodNode);
	}

	public static <T extends AbstractInsnNode> InsnFinder<T> last(MethodNode methodNode) {
		return InsnFinder.<T>create().last(methodNode);
	}

	public static <T extends AbstractInsnNode> InsnFinder<T> nextExclusive(AbstractInsnNode startExclusive) {
		return InsnFinder.<T>create().nextExclusive(startExclusive);
	}

	public static <T extends AbstractInsnNode> InsnFinder<T> prevExclusive(AbstractInsnNode startExclusive) {
		return InsnFinder.<T>create().prevExclusive(startExclusive);
	}

	public static <T extends AbstractInsnNode> InsnFinder<T> next(AbstractInsnNode startInclusive) {
		return InsnFinder.<T>create().next(startInclusive);
	}

	public static <T extends AbstractInsnNode> InsnFinder<T> prev(AbstractInsnNode startInclusive) {
		return InsnFinder.<T>create().prev(startInclusive);
	}

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

	public static InsnList listOf(AbstractInsnNode... insns) {
		InsnList list = new InsnList();
		for (AbstractInsnNode insn : insns) {
			list.add(insn);
		}
		return list;
	}

	public static InsnList listWithLabel(Function<LabelNode, InsnList> mappingFunction) {
		return mappingFunction.apply(new LabelNode());
	}

	public static InsnList listWithLabels(BiFunction<LabelNode, LabelNode, InsnList> mappingFunction) {
		return mappingFunction.apply(new LabelNode(), new LabelNode());
	}

	public static InsnList listWithLabels(TriFunction<LabelNode, LabelNode, LabelNode, InsnList> mappingFunction) {
		return mappingFunction.apply(new LabelNode(), new LabelNode(), new LabelNode());
	}

	public static InsnList listWithLabels(QuadFunction<LabelNode, LabelNode, LabelNode, LabelNode, InsnList> mappingFunction) {
		return mappingFunction.apply(new LabelNode(), new LabelNode(), new LabelNode(), new LabelNode());
	}

	public static InsnList removeAll(MethodNode methodNode) {
		return remove(methodNode, methodNode.instructions.getFirst(), methodNode.instructions.getLast());
	}

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

	public static void replaceAll(MethodNode methodNode, AbstractInsnNode replacement) {
		methodNode.instructions.clear();
		methodNode.instructions.insert(replacement);
	}

	public static void replaceAll(MethodNode methodNode, InsnList replacement) {
		methodNode.instructions.clear();
		methodNode.instructions.insert(replacement);
	}

	public static void replace(MethodNode methodNode, AbstractInsnNode target, AbstractInsnNode replacement) {
		AbstractInsnNode previous = target.getPrevious();
		methodNode.instructions.remove(target);
		if (previous != null) {
			methodNode.instructions.insert(previous, replacement);
		} else {
			methodNode.instructions.insert(replacement);
		}
	}

	public static void replace(MethodNode methodNode, AbstractInsnNode target, InsnList replacement) {
		AbstractInsnNode previous = target.getPrevious();
		methodNode.instructions.remove(target);
		if (previous != null) {
			methodNode.instructions.insert(previous, replacement);
		} else {
			methodNode.instructions.insert(replacement);
		}
	}

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

	public static LocalVariableNode findLocalVariable(MethodNode methodNode, String name, String desc, int ordinal) {
		int i = 0;
		for (LocalVariableNode localVariable : methodNode.localVariables) {
			if (localVariable.name.equals(name) && localVariable.desc.equals(desc) && i++ == ordinal) {
				return localVariable;
			}
		}
		throw new NoSuchElementException();
	}

	public static void addLocalVariable(MethodNode methodNode, String name, String desc, LabelNode start, LabelNode end) {
		int i = 0;
		for (LocalVariableNode localVariable : methodNode.localVariables) {
			if (localVariable.index >= i) {
				i = localVariable.index + 1;
			}
		}
		methodNode.localVariables.add(new LocalVariableNode(name, desc, null, start, end, i));
	}

	public static Stream<MethodNode> stream(ClassNode classNode) {
		return classNode.methods.stream();
	}

	public static Stream<AbstractInsnNode> stream(MethodNode methodNode) {
		return stream(methodNode.instructions);
	}

	public static Stream<AbstractInsnNode> stream(InsnList instructions) {
		return StreamSupport.stream(new InsnSpliterator(instructions), false);
	}

}
