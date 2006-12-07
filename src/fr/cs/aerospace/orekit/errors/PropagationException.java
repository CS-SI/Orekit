package fr.cs.aerospace.orekit.errors;

/** This class is the base class for all specific exceptions thrown by
 * during the propagation calculus.
 * 
 * @author  L. Maisonobe
 */
public class PropagationException
  extends OrekitException {
    
  /** Simple constructor.
   * Build an exception with a translated and formatted message
   * @param specifier format specifier (to be translated)
   * @param parts parts to insert in the format (no translation)
   */
  public PropagationException(String specifier, String[] parts) {
    super(specifier, parts);
  }
  
  /** Simple constructor.
   * Build an exception from a cause and with a specified message
   * @param message descriptive message
   * @param cause underlying cause
   */
  public PropagationException(String message, Throwable cause) {
    super(message, cause);
  }

  private static final long serialVersionUID = -1769023640956695918L;

}
