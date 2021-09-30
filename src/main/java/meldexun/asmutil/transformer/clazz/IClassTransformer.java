package meldexun.asmutil.transformer.clazz;

@FunctionalInterface
public interface IClassTransformer {

	byte[] transform(String name, String transformedName, byte[] basicClass);

}
