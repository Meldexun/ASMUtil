/*
 * Copyright (c) Meldexun
 * SPDX-License-Identifier: MIT
 */

package meldexun.asmutil2;

/**
 * Exception thrown when a class transformation fails.
 */
public class ClassTransformException extends RuntimeException {

	private static final long serialVersionUID = -2998565344522916612L;

	/**
	 * Constructs a new class transform exception.
	 */
	public ClassTransformException() {

	}

	/**
	 * Constructs a new class transform exception with a detail message.
	 *
	 * @param message the detail message
	 */
	public ClassTransformException(String message) {
		super(message);
	}

	/**
	 * Constructs a new class transform exception with a cause.
	 *
	 * @param cause the cause
	 */
	public ClassTransformException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new class transform exception with a detail message and cause.
	 *
	 * @param message the detail message
	 * @param cause the cause
	 */
	public ClassTransformException(String message, Throwable cause) {
		super(message, cause);
	}

}
