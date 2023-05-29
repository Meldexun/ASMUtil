package meldexun.asmutil2;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.objectweb.asm.tree.MethodNode;

public class MethodNodeTransformer {

	private static Predicate<MethodNode> matching(String name, String desc) {
		return method -> name.equals(method.name) && desc.equals(method.desc);
	}

	private static Predicate<MethodNode> matching(String name, String obfName, String desc) {
		return method -> (obfName.equals(method.name) || name.equals(method.name)) && desc.equals(method.desc);
	}

	private static Predicate<MethodNode> matching(String name, String desc, String obfName, String obfDesc) {
		return method -> obfName.equals(method.name) && obfDesc.equals(method.desc)
				|| name.equals(method.name) && desc.equals(method.desc);
	}

	public static ClassNodeTransformer create(String name, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(name, desc, 1, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, String obfName, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(name, obfName, desc, 1, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, String desc, String obfName, String obfDesc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(name, desc, obfName, obfDesc, 1, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(Predicate<MethodNode> predicate, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(predicate, 1, writeFlags, transformer);
	}

	public static ClassNodeTransformer createOptional(String name, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(name, desc, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer createOptional(String name, String obfName, String desc, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(name, obfName, desc, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer createOptional(String name, String desc, String obfName, String obfDesc,
			int writeFlags, Consumer<MethodNode> transformer) {
		return create(name, desc, obfName, obfDesc, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer createOptional(Predicate<MethodNode> predicate, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(predicate, 0, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, String desc, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(matching(name, desc), required, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, String obfName, String desc, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return create(matching(name, obfName, desc), required, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(String name, String desc, String obfName, String obfDesc, int required,
			int writeFlags, Consumer<MethodNode> transformer) {
		return create(matching(name, desc, obfName, obfDesc), required, writeFlags, transformer);
	}

	public static ClassNodeTransformer create(Predicate<MethodNode> predicate, int required, int writeFlags,
			Consumer<MethodNode> transformer) {
		return ClassNodeTransformer.create(writeFlags, classNode -> {
			int transformedMethodCount = 0;
			for (MethodNode method : classNode.methods) {
				if (predicate.test(method)) {
					if (required > 0 && transformedMethodCount >= required) {
						throw new ClassTransformException(
								String.format("Found more method transform targets than expected (%s)", required));
					}
					transformer.accept(method);
					transformedMethodCount++;
				}
			}
			if (required > 0 && transformedMethodCount < required) {
				throw new ClassTransformException(
						String.format("Found less method transform targets (%s) than expected (%s)",
								transformedMethodCount, required));
			}
			return transformedMethodCount > 0;
		});
	}

}
