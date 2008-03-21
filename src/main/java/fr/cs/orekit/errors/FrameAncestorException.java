package fr.cs.orekit.errors;

/** This class is the base class for exception thrown by
 * the {@link fr.cs.aerospace.orekit.frames.Frame#updateTransform(Frame,
 * Frame,Transform,AbsoluteDate) Frame.updateTransform} method.
 */
public class FrameAncestorException extends OrekitException {

  public FrameAncestorException(String specifier, String[] parts) {
    super(specifier, parts);
  }

  private static final long serialVersionUID = -4100579364338940418L;

}
