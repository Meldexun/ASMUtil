package meldexun.asmutil.transformer.clazz;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public abstract class ClassVisitorClassTransformer<T extends ClassVisitor> implements IClassTransformer {

	@Override
	public byte[] transform(String obfName, String name, byte[] basicClass) {
		ITransformInfo<T> transformInfo = this.getTransformInfo(name);
		if (transformInfo == null) {
			return basicClass;
		}
		ClassReader classReader = new ClassReader(basicClass);
		Lazy<ClassWriter> classWriter = new Lazy<>(() -> new ClassWriter(transformInfo.writeFlags()));
		T classVisitor = transformInfo.visitor(classWriter);
		classReader.accept(classVisitor, transformInfo.readFlags());
		if (!transformInfo.transform(classVisitor)) {
			return basicClass;
		}
		return classWriter.get().toByteArray();
	}

	protected abstract ITransformInfo<T> getTransformInfo(String name);

}
