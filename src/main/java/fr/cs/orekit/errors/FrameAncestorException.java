package fr.cs.orekit.errors;

/** This class is the base class for exception thrown by
 * the {@link fr.cs.orekit.frames.Frame#updateTransform(Frame,
 * Frame,Transform,AbsoluteDate) Frame.updateTransform} method.
 */
public class FrameAncestorException extends OrekitException {

    /** Serializable UID. */
    private static final long serialVersionUID = -4364398908632938172L;

    /** Simple constructor.
     * Build an exception with a translated and formatted message
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     */
    public FrameAncestorException(final String specifier, final Object[] parts) {
        super(specifier, parts);
    }

}
