/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2.reader;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.objectweb.asm.Opcodes;

import meldexun.asmutil2.ClassTransformException;

/**
 * Looks up class hierarchies by reading only the header (name, super class, interfaces) of class files found through
 * a class loader, without loading the classes. Results are cached per instance. Used by
 * {@link meldexun.asmutil2.NonLoadingClassWriter}, and also useful on its own to check whether a class being
 * transformed extends or implements something:
 *
 * <pre>
 * boolean isList = ClassUtil.DEFAULT.findInClassHierarchy("com/example/Foo", "java/util/List"::equals) != null;
 * </pre>
 *
 * All class names are internal names ({@code com/example/Foo}).
 */
public final class ClassUtil {

	/**
	 * Where class files are read from, and optionally how class names are mapped. Mappings are needed when the code
	 * being written refers to classes by different names than the class files on the class path have, e.g. when
	 * transformers see deobfuscated names but the class files are obfuscated.
	 */
	public static final class Configuration {

		/** Reads class files through the class loader that loaded this library, without mappings. */
		public static final Configuration DEFAULT = new Configuration(Configuration.class.getClassLoader());
		private final ClassLoader classLoader;
		private final Map<String, String> obfuscationMap;
		private final Map<String, String> deobfuscationMap;

		/**
		 * Creates a configuration without mappings.
		 *
		 * @param classLoader the ClassLoader used to find class files via {@link ClassLoader#getResource(String)}
		 */
		public Configuration(ClassLoader classLoader) {
			this(classLoader, null, null);
		}

		/**
		 * Creates a configuration with mappings. Maps use internal names; inner classes without an own entry are
		 * mapped by their outer class.
		 *
		 * @param classLoader the ClassLoader used to find class files via {@link ClassLoader#getResource(String)}
		 * @param obfuscationMap deobfuscated to obfuscated names, used to find the class file for a requested name,
		 *        or {@code null}
		 * @param deobfuscationMap obfuscated to deobfuscated names, applied to the names read from class files, or
		 *        {@code null}
		 */
		public Configuration(ClassLoader classLoader, Map<String, String> obfuscationMap,
				Map<String, String> deobfuscationMap) {
			this.classLoader = Objects.requireNonNull(classLoader);
			this.obfuscationMap = obfuscationMap;
			this.deobfuscationMap = deobfuscationMap;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof Configuration)) {
				return false;
			}
			Configuration other = (Configuration) obj;
			return this.classLoader.equals(other.classLoader)
					&& Objects.equals(this.obfuscationMap, other.obfuscationMap)
					&& Objects.equals(this.deobfuscationMap, other.deobfuscationMap);
		}

		@Override
		public int hashCode() {
			int h = 1;
			h = h * 31 + this.classLoader.hashCode();
			h = h * 31 + Objects.hashCode(this.obfuscationMap);
			h = h * 31 + Objects.hashCode(this.deobfuscationMap);
			return h;
		}

		/**
		 * Maps a deobfuscated internal class name to its obfuscated form. For inner classes without an own mapping
		 * the outer class is mapped and the {@code $...} suffix kept.
		 *
		 * @param className the deobfuscated class name
		 * @return the obfuscated class name, or the input if no mapping exists
		 */
		public String obfuscate(String className) {
			if (this.obfuscationMap == null || this.obfuscationMap.isEmpty()) {
				return className;
			}
			String s = this.obfuscationMap.get(className);
			if (s != null) {
				return s;
			}
			int i = className.lastIndexOf('$');
			if (i != -1) {
				return this.obfuscate(className.substring(0, i)) + className.substring(i);
			}
			return className;
		}

		/**
		 * Maps an obfuscated internal class name to its deobfuscated form. For inner classes without an own mapping
		 * the outer class is mapped and the {@code $...} suffix kept.
		 *
		 * @param className the obfuscated class name
		 * @return the deobfuscated class name, or the input if no mapping exists
		 */
		public String deobfuscate(String className) {
			if (this.deobfuscationMap == null || this.deobfuscationMap.isEmpty()) {
				return className;
			}
			String s = this.deobfuscationMap.get(className);
			if (s != null) {
				return s;
			}
			int i = className.lastIndexOf('$');
			if (i != -1) {
				return this.deobfuscate(className.substring(0, i)) + className.substring(i);
			}
			return className;
		}

	}

	private static final Map<Configuration, ClassUtil> INSTANCES = new ConcurrentHashMap<>();
	/** Instance for {@link Configuration#DEFAULT}. */
	public static final ClassUtil DEFAULT = ClassUtil.getInstance(Configuration.DEFAULT);
	private static final String OBJECT_CLASS_NAME = Object.class.getName().replace('.', '/');
	private final Configuration configuration;
	private final Map<String, ClassInfo> classInfoCache = new ConcurrentHashMap<>();

	private ClassUtil(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Returns the instance for the given configuration, creating it on first use. Instances (and their caches) are
	 * kept forever, so reuse configurations instead of creating new ones per call.
	 *
	 * @param configuration the configuration to use
	 * @return the shared instance for this configuration
	 */
	public static ClassUtil getInstance(Configuration configuration) {
		return INSTANCES.computeIfAbsent(configuration, ClassUtil::new);
	}

	/**
	 * Searches the class itself and all its super types for one accepted by {@code filter}. The order is: the class
	 * and its super classes (nearest first), then the interfaces of each of them (depth first), then
	 * {@code java/lang/Object}. Array types are treated as direct subclasses of {@code java/lang/Object}.
	 *
	 * @param className the internal name of the class to start at
	 * @param filter predicate to test internal class names
	 * @return the first accepted class name, or {@code null} if none is accepted
	 * @throws java.util.MissingResourceException if the class file of a class in the hierarchy can't be found
	 * @throws meldexun.asmutil2.ClassTransformException if a class file can't be read
	 */
	public String findInClassHierarchy(String className, Predicate<String> filter) {
		String r1 = this.findClass(className, filter);
		if (r1 != null) {
			return r1;
		}
		String r2 = this.findInterface(className, filter);
		if (r2 != null) {
			return r2;
		}
		if (filter.test(OBJECT_CLASS_NAME)) {
			return OBJECT_CLASS_NAME;
		}
		return null;
	}

	private String findClass(String className, Predicate<String> filter) {
		String s = className;
		while (s != null && !s.equals(OBJECT_CLASS_NAME)) {
			if (filter.test(s)) {
				return s;
			}
			s = this.getClassInfoCached(s).superClass;
		}
		return null;
	}

	private String findInterface(String className, Predicate<String> filter) {
		String s = className;
		while (s != null && !s.equals(OBJECT_CLASS_NAME)) {
			ClassInfo c = this.getClassInfoCached(s);
			for (String i : c.interfaces) {
				if (filter.test(i)) {
					return i;
				}
				String r = this.findInterface(i, filter);
				if (r != null) {
					return r;
				}
			}
			s = c.superClass;
		}
		return null;
	}

	private ClassInfo getClassInfoCached(String className) {
		return this.classInfoCache.computeIfAbsent(className, k -> {
			return ClassUtil.getClassInfo(this.configuration.classLoader, this.configuration.obfuscate(k),
					this.configuration::deobfuscate);
		});
	}

	private static ClassInfo getClassInfo(ClassLoader classLoader, String className,
			Function<String, String> deobfuscationFunction) {
		if (className.startsWith("[")) {
			return new ClassInfo(Opcodes.ACC_PUBLIC, className, OBJECT_CLASS_NAME, new String[0]);
		}
		URL url = classLoader.getResource(className + ".class");
		if (url == null) {
			throw new MissingResourceException("Can't find class resource", className, "");
		}
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(IOUtil.openStream(url)))) {
			IOUtil.skip(in, 4); // magic
			IOUtil.skip(in, 2); // minor class version
			IOUtil.skip(in, 2); // major class version

			// read constant pool
			FilteredConstantPool constantPool = FilteredConstantPool.read(in, (in1, type) -> {
				switch (type) {
				case 7: // Class
					return in1.readUnsignedShort();
				case 1: // Utf8
					return IOUtil.read(in1, in1.readUnsignedShort());
				case 8: // String
				case 16: // MethodType
					IOUtil.skip(in1, 2);
					return null;
				case 15: // MethodHandle
					IOUtil.skip(in1, 3);
					return null;
				case 3: // Int
				case 4: // Float
				case 9: // Fieldref
				case 10: // Methodref
				case 11: // InterfaceMethodref
				case 12: // NameAndType
				case 18: // InvokeDynamic
					IOUtil.skip(in1, 4);
					return null;
				case 5: // Long
				case 6: // Double
					IOUtil.skip(in1, 8);
					return null;
				default:
					throw new IllegalStateException();
				}
			});

			return ClassInfo.read(in, constantPool, deobfuscationFunction);
		} catch (IOException | URISyntaxException e) {
			throw new ClassTransformException(e);
		}
	}

	static class ClassInfo {

		final int access;
		final String name;
		final String superClass;
		final String[] interfaces;

		private ClassInfo(int access, String name, String superClass, String[] interfaces) {
			this.access = access;
			this.name = name;
			this.superClass = superClass;
			this.interfaces = interfaces;
		}

		static ClassInfo read(DataInput in, FilteredConstantPool constantPool,
				Function<String, String> deobfuscationFunction) throws IOException {
			int access = in.readUnsignedShort();
			String name = deobfuscationFunction.apply(constantPool.getClass(in.readUnsignedShort()));
			String superName = deobfuscationFunction.apply(constantPool.getClass(in.readUnsignedShort()));
			String[] interfaces = new String[in.readUnsignedShort()];
			for (int i = 0; i < interfaces.length; i++) {
				interfaces[i] = deobfuscationFunction.apply(constantPool.getClass(in.readUnsignedShort()));
			}
			return new ClassInfo(access, name, superName, interfaces);
		}

	}

}
