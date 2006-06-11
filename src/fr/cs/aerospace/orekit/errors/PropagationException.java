package fr.cs.aerospace.orekit.errors;

/**
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

  private static final long serialVersionUID = -1769023640956695918L;

}
