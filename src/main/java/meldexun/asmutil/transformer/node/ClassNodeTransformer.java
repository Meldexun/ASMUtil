package meldexun.asmutil.transformer.node;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.objectweb.asm.tree.ClassNode;

public interface ClassNodeTransformer {

	boolean transform(ClassNode classNode);

	int writeFlags();

	static ClassNodeTransformer create(int writeFlags, Consumer<ClassNode> transformer) {
		return create(writeFlags, classNode -> {
			transformer.accept(classNode);
			return true;
		});
	}

	static ClassNodeTransformer create(int writeFlags, Predicate<ClassNode> transformer) {
		return new ClassNodeTransformer() {

			@Override
			public boolean transform(ClassNode classNode) {
				return transformer.test(classNode);
			}

			@Override
			public int writeFlags() {
				return writeFlags;
			}

		};
	}

}
