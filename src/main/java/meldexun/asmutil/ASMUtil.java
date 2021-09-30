package meldexun.asmutil;

import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ASMUtil {

	public static final Logger LOGGER = LogManager.getLogger();

	public static void printMethodInstructions(MethodNode methodNode) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (AbstractInsnNode instruction : methodNode.instructions.toArray()) {
			sb.append("\n" + insnToString(i++, instruction));
		}
		LOGGER.info(sb);
	}

	public static String insnToString(int index, AbstractInsnNode insn) {
		StringBuilder sb = new StringBuilder();
		sb.append(index);
		while (sb.length() < 5) {
			sb.append(' ');
		}
		sb.append(insn.getOpcode());
		while (sb.length() < 9) {
			sb.append(' ');
		}
		sb.append(insn.getClass().getSimpleName());
		while (sb.length() < 24) {
			sb.append(' ');
		}
		if (insn instanceof MethodInsnNode) {
			sb.append(" " + ((MethodInsnNode) insn).owner);
			sb.append(" " + ((MethodInsnNode) insn).name);
			sb.append(" " + ((MethodInsnNode) insn).desc);
		} else if (insn instanceof VarInsnNode) {
			sb.append(" " + ((VarInsnNode) insn).var);
		} else if (insn instanceof FieldInsnNode) {
			sb.append(" " + ((FieldInsnNode) insn).owner);
			sb.append(" " + ((FieldInsnNode) insn).name);
			sb.append(" " + ((FieldInsnNode) insn).desc);
		} else if (insn instanceof JumpInsnNode) {
			sb.append(" " + ((JumpInsnNode) insn).label.getLabel());
		} else if (insn instanceof LabelNode) {
			sb.append(" " + ((LabelNode) insn).getLabel());
		} else if (insn instanceof FrameNode) {
			sb.append(" " + ((FrameNode) insn).local);
			sb.append(" " + ((FrameNode) insn).stack);
		} else if (insn instanceof LineNumberNode) {
			sb.append(" " + ((LineNumberNode) insn).line);
		}
		return sb.toString();
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
