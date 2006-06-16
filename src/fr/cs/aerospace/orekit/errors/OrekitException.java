package fr.cs.aerospace.orekit.errors;

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
   * Build an exception with a translated and formatted message
   * @param specifier format specifier (to be translated)
   * @param parts parts to insert in the format (no translation)
   */
  public OrekitException(String specifier, String[] parts) {
    super(Translator.getInstance().translate(specifier, parts));
  }

  /** Simple constructor.
   * Build an exception from a cause and with a specified message
   * @param message descriptive message
   * @param cause underlying cause
   */
  public OrekitException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Simple constructor.
   * Build an exception from a cause and with a translated and formatted message
   * @param specifier format specifier (to be translated)
   * @param parts parts to insert in the format (no translation)
   * @param cause underlying cause
   */
  public OrekitException(String specifier, String[] parts, Throwable cause) {
    super(Translator.getInstance().translate(specifier, parts), cause);
  }

  private static final long serialVersionUID = -988928652056598915L;

}
