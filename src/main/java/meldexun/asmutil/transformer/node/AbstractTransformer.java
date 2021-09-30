package meldexun.asmutil.transformer.node;

import java.util.function.Predicate;

import org.objectweb.asm.tree.ClassNode;

public abstract class AbstractTransformer {

	private final String obfClassName;
	private final String className;
	private final Predicate<ClassNode> transformer;

	protected AbstractTransformer(String obfClassName, String className, Predicate<ClassNode> transformer) {
		this.obfClassName = obfClassName;
		this.className = className;
		this.transformer = transformer;
	}

	public boolean apply(ClassNode classNode) {
		if (!classNode.name.equals(this.obfClassName) && !classNode.name.equals(this.className)) {
			return false;
		}
		return this.transformer.test(classNode);
	}

}
