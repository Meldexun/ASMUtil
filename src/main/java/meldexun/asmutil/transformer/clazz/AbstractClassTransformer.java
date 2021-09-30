package meldexun.asmutil.transformer.clazz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import meldexun.asmutil.transformer.node.AbstractTransformer;
import meldexun.asmutil.transformer.node.ClassNodeTransformer;
import meldexun.asmutil.transformer.node.FieldNodeTransformer;
import meldexun.asmutil.transformer.node.MethodNodeTransformer;

public abstract class AbstractClassTransformer implements IClassTransformer {

	private final Map<String, List<AbstractTransformer>> classTransformers = new HashMap<>();

	protected AbstractClassTransformer() {
		this.registerTransformers();
	}

	protected abstract void registerTransformers();

	protected void registerClassTransformer(String obfClassName, String className, Consumer<ClassNode> transformer) {
		this.classTransformers.computeIfAbsent(className.replace('/', '.'), key -> new ArrayList<>())
				.add(new ClassNodeTransformer(obfClassName, className, transformer));
	}

	protected void registerFieldTransformer(String obfClassName, String obfName, String obfDesc, String className, String name, String desc,
			Consumer<FieldNode> transformer) {
		this.classTransformers.computeIfAbsent(className.replace('/', '.'), key -> new ArrayList<>())
				.add(new FieldNodeTransformer(obfClassName, obfName, obfDesc, className, name, desc, transformer));
	}

	protected void registerMethodTransformer(String obfClassName, String obfName, String obfDesc, String className, String name, String desc,
			Consumer<MethodNode> transformer) {
		this.classTransformers.computeIfAbsent(className.replace('/', '.'), key -> new ArrayList<>())
				.add(new MethodNodeTransformer(obfClassName, obfName, obfDesc, className, name, desc, transformer));
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		List<AbstractTransformer> classTransformerList = this.classTransformers.get(transformedName);

		if (classTransformerList == null || classTransformerList.isEmpty()) {
			return basicClass;
		}

		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);
		boolean isDirty = false;

		for (AbstractTransformer classTransformer : classTransformerList) {
			isDirty |= classTransformer.apply(classNode);
		}

		if (!isDirty) {
			return basicClass;
		}

		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(classWriter);
		return classWriter.toByteArray();
	}

}
