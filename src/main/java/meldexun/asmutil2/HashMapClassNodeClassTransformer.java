/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The usual base class for a transformer: register transformers per class name in
 * {@link #registerTransformers(IClassTransformerRegistry)}, and they are looked up by exact class name when classes
 * are loaded. Transformers for the same class are applied sorted by {@link ClassNodeTransformer#priority()}, and in
 * registration order for equal priorities.
 */
public abstract class HashMapClassNodeClassTransformer extends ClassNodeClassTransformer {

	private final Map<String, List<ClassNodeTransformer>> classTransformers = new HashMap<>();

	protected HashMapClassNodeClassTransformer() {
		this.registerTransformers((className, transformer) -> {
			this.classTransformers.computeIfAbsent(className, k -> SortedArrayList.create()).add(transformer);
		});
	}

	/**
	 * Registers all transformers.
	 * <p>
	 * This is called from the constructor of this class, i.e. <b>before field initializers and the constructor body
	 * of the subclass have run</b>. Instance fields of the subclass are still {@code null}/{@code 0} at this point;
	 * use static fields or local variables for anything needed during registration.
	 *
	 * @param registry the registry to add transformers to
	 */
	protected abstract void registerTransformers(IClassTransformerRegistry registry);

	@Override
	protected List<ClassNodeTransformer> getClassNodeTransformers(String className) {
		return this.classTransformers.get(className);
	}

}
