package meldexun.asmutil.transformer.clazz;

public interface IClassTransformer {

	byte[] transform(String obfName, String name, byte[] basicClass);

}
