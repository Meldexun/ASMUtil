package meldexun.asmutil.transformer.node;

import java.util.function.Consumer;

import org.objectweb.asm.tree.MethodNode;

public class MethodNodeTransformer extends AbstractTransformer {

	public MethodNodeTransformer(String obfClassName, String obfName, String obfDesc, String className, String name, String desc,
			Consumer<MethodNode> transformer) {
		super(obfClassName, className, classNode -> {
			for (MethodNode methodNode : classNode.methods) {
				if ((methodNode.name.equals(obfName) && methodNode.desc.equals(obfDesc)) || (methodNode.name.equals(name) && methodNode.desc.equals(desc))) {
					transformer.accept(methodNode);
					return true;
				}
			}
			return false;
		});
	}

}
