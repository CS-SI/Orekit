package fr.cs.aerospace.orekit.extrapolation;

import fr.cs.aerospace.orekit.OrekitException;
/**
 *
 * @author  L. Maisonobe
 */
public class ExtrapolationException
  extends OrekitException {
    
    /**
     * Creates a new instance of <code>ExtrapolationException</code> without
     * detail message.
     */
    public ExtrapolationException() {
    }
    
    /**
     * Constructs an instance of <code>ExtrapolationException</code> with the
     * specified detail message.
     * @param msg the detail message.
     */
    public ExtrapolationException(String msg) {
        super(msg);
    }
    
    /** Simple constructor.
     * Build an exception from a cause
     * @param cause cause of the exception
     */
    public ExtrapolationException(Throwable cause) {
        super(cause);
    }

    /** Simple constructor.
     * Build an exception from a message and a cause
     * @param message message of the exception
     * @param cause cause of the exception
     */
    public ExtrapolationException(String message, Throwable cause) {
        super(message, cause);
    }

}
