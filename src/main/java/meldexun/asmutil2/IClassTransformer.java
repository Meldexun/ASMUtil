package meldexun.asmutil2;

public interface IClassTransformer {

	byte[] transform(String obfName, String name, byte[] basicClass);

}
