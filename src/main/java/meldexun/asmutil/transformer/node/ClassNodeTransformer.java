package meldexun.asmutil.transformer.node;

import java.util.function.Consumer;

import org.objectweb.asm.tree.ClassNode;

public class ClassNodeTransformer extends AbstractTransformer {

	public ClassNodeTransformer(String obfClassName, String className, Consumer<ClassNode> transformer) {
		super(obfClassName, className, classNode -> {
			transformer.accept(classNode);
			return true;
		});
	}

}
