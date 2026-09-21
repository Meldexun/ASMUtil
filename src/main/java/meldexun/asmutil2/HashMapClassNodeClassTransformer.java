/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Convenient base class for class transformers that use a HashMap to register
 * transformers for specific classes. Transformers are registered once during
 * construction and stored in a map for fast lookup.
 */
public abstract class HashMapClassNodeClassTransformer extends ClassNodeClassTransformer {

	private final Map<String, List<ClassNodeTransformer>> classTransformers = new HashMap<>();

	protected HashMapClassNodeClassTransformer() {
		this.registerTransformers((className, transformer) -> {
			this.classTransformers.computeIfAbsent(className, k -> SortedArrayList.create()).add(transformer);
		});
	}

	/**
	 * Registers all class and method transformers. Called once during construction.
	 *
	 * @param registry the registry to add transformers to
	 */
	protected abstract void registerTransformers(IClassTransformerRegistry registry);

	@Override
	protected List<ClassNodeTransformer> getClassNodeTransformers(String className) {
		return this.classTransformers.get(className);
	}

}
