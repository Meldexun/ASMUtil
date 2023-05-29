package meldexun.asmutil.transformer.clazz;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

class ReductionUtil {

	public static <T, U> boolean booleanOr(Iterable<T> iterable, U u, BiPredicate<T, U> predicate) {
		boolean result = false;
		for (T t : iterable) {
			result |= predicate.test(t, u);
		}
		return result;
	}

	public static <T> int intOr(Iterable<T> iterable, ToIntFunction<T> predicate) {
		int result = 0;
		for (T t : iterable) {
			result |= predicate.applyAsInt(t);
		}
		return result;
	}

}
