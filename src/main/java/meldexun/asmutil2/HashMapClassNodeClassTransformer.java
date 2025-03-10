/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class HashMapClassNodeClassTransformer extends ClassNodeClassTransformer {

	private final Map<String, List<ClassNodeTransformer>> classTransformers = new HashMap<>();

	protected HashMapClassNodeClassTransformer() {
		this.registerTransformers((className, transformer) -> {
			this.classTransformers.computeIfAbsent(className, k -> SortedArrayList.create()).add(transformer);
		});
	}

	protected abstract void registerTransformers(IClassTransformerRegistry registry);

	@Override
	protected List<ClassNodeTransformer> getClassNodeTransformers(String className) {
		return this.classTransformers.get(className);
	}

}
