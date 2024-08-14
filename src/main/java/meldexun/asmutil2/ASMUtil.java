/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ASMUtil {

	public static final Logger LOGGER = LogManager.getLogger("ASMUtil");

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

}
