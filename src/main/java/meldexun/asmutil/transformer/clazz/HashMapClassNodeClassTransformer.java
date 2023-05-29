package meldexun.asmutil.transformer.clazz;

import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meldexun.asmutil.transformer.node.ClassNodeTransformer;

public abstract class HashMapClassNodeClassTransformer extends ClassNodeClassTransformer {

	private final Map<String, List<ClassNodeTransformer>> classTransformers = new Object2ObjectOpenHashMap<>();

	protected HashMapClassNodeClassTransformer() {
		this.registerTransformers((className, transformer) -> this.classTransformers
				.computeIfAbsent(className, k -> new ObjectArrayList<>()).add(transformer));
	}

	protected abstract void registerTransformers(IClassTransformerRegistry registry);

	@Override
	protected List<ClassNodeTransformer> getClassNodeTransformers(String className) {
		return this.classTransformers.get(className);
	}

}
