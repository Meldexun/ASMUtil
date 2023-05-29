package meldexun.asmutil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
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

	public static final Logger LOGGER = LogManager.getLogger();

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

	public static AbstractInsnNode findFirstInsnByOpcode(MethodNode methodNode, int opCode) {
		return findInsnByOpcode(methodNode, opCode, 0, false);
	}

	public static AbstractInsnNode findFirstInsnByOpcode(MethodNode methodNode, int opCode, AbstractInsnNode startExclusive) {
		return findInsnByOpcode(methodNode, opCode, methodNode.instructions.indexOf(startExclusive) + 1, false);
	}

	public static AbstractInsnNode findLastInsnByOpcode(MethodNode methodNode, int opCode) {
		return findInsnByOpcode(methodNode, opCode, methodNode.instructions.size() - 1, true);
	}

	public static AbstractInsnNode findLastInsnByOpcode(MethodNode methodNode, int opCode, AbstractInsnNode startExclusive) {
		return findInsnByOpcode(methodNode, opCode, methodNode.instructions.indexOf(startExclusive) - 1, true);
	}

	private static AbstractInsnNode findInsnByOpcode(MethodNode methodNode, int opCode, int startInclusive, boolean reversed) {
		if (startInclusive < 0 || startInclusive >= methodNode.instructions.size()) {
			throw new NoSuchElementException();
		}
		for (int i = startInclusive; i >= 0 && i < methodNode.instructions.size(); i += reversed ? -1 : 1) {
			AbstractInsnNode ain = methodNode.instructions.get(i);
			if (ain.getOpcode() == opCode) {
				return ain;
			}
		}
		throw new NoSuchElementException();
	}

	public static AbstractInsnNode findFirstInsnByType(MethodNode methodNode, int type) {
		return findInsnByType(methodNode, type, 0, false);
	}

	public static AbstractInsnNode findFirstInsnByType(MethodNode methodNode, int type, AbstractInsnNode startExclusive) {
		return findInsnByType(methodNode, type, methodNode.instructions.indexOf(startExclusive) + 1, false);
	}

	public static AbstractInsnNode findLastInsnByType(MethodNode methodNode, int type) {
		return findInsnByType(methodNode, type, methodNode.instructions.size() - 1, true);
	}

	public static AbstractInsnNode findLastInsnByType(MethodNode methodNode, int type, AbstractInsnNode startExclusive) {
		return findInsnByType(methodNode, type, methodNode.instructions.indexOf(startExclusive) - 1, true);
	}

	private static AbstractInsnNode findInsnByType(MethodNode methodNode, int type, int startInclusive, boolean reversed) {
		if (startInclusive < 0 || startInclusive >= methodNode.instructions.size()) {
			throw new NoSuchElementException();
		}
		for (int i = startInclusive; i >= 0 && i < methodNode.instructions.size(); i += reversed ? -1 : 1) {
			AbstractInsnNode ain = methodNode.instructions.get(i);
			if (ain.getType() == type) {
				return ain;
			}
		}
		throw new NoSuchElementException();
	}

	public static MethodInsnNode findFirstMethodCall(MethodNode methodNode, int opCode, String obfOwner, String obfName, String obfDesc, String owner,
			String name, String desc) {
		return findMethodCall(methodNode, opCode, obfOwner, obfName, obfDesc, owner, name, desc, 0, false);
	}

	public static MethodInsnNode findFirstMethodCall(MethodNode methodNode, int opCode, String obfOwner, String obfName, String obfDesc, String owner,
			String name, String desc, AbstractInsnNode startExclusive) {
		return findMethodCall(methodNode, opCode, obfOwner, obfName, obfDesc, owner, name, desc, methodNode.instructions.indexOf(startExclusive) + 1, false);
	}

	public static MethodInsnNode findLastMethodCall(MethodNode methodNode, int opCode, String obfOwner, String obfName, String obfDesc, String owner,
			String name, String desc) {
		return findMethodCall(methodNode, opCode, obfOwner, obfName, obfDesc, owner, name, desc, methodNode.instructions.size() - 1, true);
	}

	public static MethodInsnNode findLastMethodCall(MethodNode methodNode, int opCode, String obfOwner, String obfName, String obfDesc, String owner,
			String name, String desc, AbstractInsnNode startExclusive) {
		return findMethodCall(methodNode, opCode, obfOwner, obfName, obfDesc, owner, name, desc, methodNode.instructions.indexOf(startExclusive) - 1, true);
	}

	private static MethodInsnNode findMethodCall(MethodNode methodNode, int opCode, String obfOwner, String obfName, String obfDesc, String owner, String name,
			String desc, int startInclusive, boolean reversed) {
		if (startInclusive < 0 || startInclusive >= methodNode.instructions.size()) {
			throw new NoSuchElementException();
		}
		for (int i = startInclusive; i >= 0 && i < methodNode.instructions.size(); i += reversed ? -1 : 1) {
			AbstractInsnNode node = methodNode.instructions.get(i);
			if (node instanceof MethodInsnNode && node.getOpcode() == opCode) {
				MethodInsnNode methodInsnNode = (MethodInsnNode) node;
				if ((methodInsnNode.owner.equals(obfOwner) && methodInsnNode.name.equals(obfName) && methodInsnNode.desc.equals(obfDesc))
						|| (methodInsnNode.owner.equals(owner) && methodInsnNode.name.equals(name) && methodInsnNode.desc.equals(desc))) {
					return methodInsnNode;
				}
			}
		}
		throw new NoSuchElementException();
	}

	public static FieldInsnNode findFirstFieldCall(MethodNode methodNode, int opCode, String obfOwner, String obfName, String obfDesc, String owner,
			String name, String desc) {
		return findFieldCall(methodNode, opCode, obfOwner, obfName, obfDesc, owner, name, desc, 0, false);
	}

	public static FieldInsnNode findFirstFieldCall(MethodNode methodNode, int opCode, String obfOwner, String obfName, String obfDesc, String owner,
			String name, String desc, AbstractInsnNode startExclusive) {
		return findFieldCall(methodNode, opCode, obfOwner, obfName, obfDesc, owner, name, desc, methodNode.instructions.indexOf(startExclusive) + 1, false);
	}

	public static FieldInsnNode findLastFieldCall(MethodNode methodNode, int opCode, String obfOwner, String obfName, String obfDesc, String owner, String name,
			String desc) {
		return findFieldCall(methodNode, opCode, obfOwner, obfName, obfDesc, owner, name, desc, methodNode.instructions.size() - 1, true);
	}

	public static FieldInsnNode findLastFieldCall(MethodNode methodNode, int opCode, String obfOwner, String obfName, String obfDesc, String owner, String name,
			String desc, AbstractInsnNode startExclusive) {
		return findFieldCall(methodNode, opCode, obfOwner, obfName, obfDesc, owner, name, desc, methodNode.instructions.indexOf(startExclusive) - 1, true);
	}

	private static FieldInsnNode findFieldCall(MethodNode methodNode, int opCode, String obfOwner, String obfName, String obfDesc, String owner, String name,
			String desc, int startInclusive, boolean reversed) {
		if (startInclusive < 0 || startInclusive >= methodNode.instructions.size()) {
			throw new NoSuchElementException();
		}
		for (int i = startInclusive; i >= 0 && i < methodNode.instructions.size(); i += reversed ? -1 : 1) {
			AbstractInsnNode node = methodNode.instructions.get(i);
			if (node instanceof FieldInsnNode && node.getOpcode() == opCode) {
				FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
				if ((fieldInsnNode.owner.equals(obfOwner) && fieldInsnNode.name.equals(obfName) && fieldInsnNode.desc.equals(obfDesc))
						|| (fieldInsnNode.owner.equals(owner) && fieldInsnNode.name.equals(name) && fieldInsnNode.desc.equals(desc))) {
					return fieldInsnNode;
				}
			}
		}
		throw new NoSuchElementException();
	}

	public static InsnList listOf(AbstractInsnNode... nodes) {
		InsnList list = new InsnList();
		for (AbstractInsnNode node : nodes) {
			list.add(node);
		}
		return list;
	}

}
