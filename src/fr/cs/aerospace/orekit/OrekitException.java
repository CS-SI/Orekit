package fr.cs.aerospace.orekit;

/** This class is the base class for all specific exceptions thrown by
 * the orekit classes.

 * <p>When the orekit classes throw exceptions that are specific to
 * the package, these exceptions are always subclasses of
 * OrekitException. When exceptions that are already covered by the
 * standard java API should be thrown, like
 * ArrayIndexOutOfBoundsException or InvalidParameterException, these
 * standard exceptions are thrown rather than the mantissa specific
 * ones.</p>

 * @version $Id$
 * @author L. Maisonobe

 */

public class OrekitException
  extends Exception {

  /** Simple constructor.
   * Build an exception with a default message
   */
  public OrekitException() {
    super("orekit exception");
  }

  /** Simple constructor.
   * @param message message of the exception
   * Build an exception with the specified message
   */
  public OrekitException(String message) {
    super(message);
  }
  
  /** Simple constructor.
   * Build an exception from a cause
   * @param cause cause of the exception
   */
  public OrekitException(Throwable cause) {
      super(cause);
  }

  /** Simple constructor.
   * Build an exception from a message and a cause
   * @param message message of the exception
   * @param cause cause of the exception
   */
  public OrekitException(String message, Throwable cause) {
      super(message, cause);
  }

}
