package util.exception;

/**
 * A set of utils to handle exception-related procedures.
 */
public class ExceptionUtil {
    /**
     * Throws an IllegalStateException with the specified message if the specified condition is not satisfied.
     * @param condition the specified condition
     * @param message the specified message
     */
    public static void require(boolean condition, String message){
        if (!condition) { throw new IllegalStateException(message); }
    }
}