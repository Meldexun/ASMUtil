package meldexun.asmutil.transformer.clazz;

public class ClassTransformException extends RuntimeException {

	private static final long serialVersionUID = -2998565344522916612L;

	public ClassTransformException() {

	}

	public ClassTransformException(String message) {
		super(message);
	}

	public ClassTransformException(Throwable cause) {
		super(cause);
	}

	public ClassTransformException(String message, Throwable cause) {
		super(message, cause);
	}

}
