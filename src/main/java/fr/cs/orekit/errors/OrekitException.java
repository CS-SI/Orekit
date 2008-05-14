package fr.cs.orekit.errors;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** This class is the base class for all specific exceptions thrown by
 * the orekit classes.

 * <p>When the orekit classes throw exceptions that are specific to
 * the package, these exceptions are always subclasses of
 * OrekitException. When exceptions that are already covered by the
 * standard java API should be thrown, like
 * ArrayIndexOutOfBoundsException or InvalidParameterException, these
 * standard exceptions are thrown rather than the commons-math specific
 * ones.</p>
 * <p>This class also provides utility methods to throw some standard
 * java exceptions with localized messages.</p>
 *
 * @author Luc Maisonobe

 */

public class OrekitException extends Exception {

    /** Serializable UID. */
    private static final long serialVersionUID = 8837701027854807120L;

    /** Resources bundle. */
    private static final ResourceBundle RESOURCES;

    static {
        RESOURCES = ResourceBundle.getBundle("META-INF/localization/ExceptionsMessages");
    }

    /** Simple constructor.
     * Build an exception with a translated and formatted message
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     */
    public OrekitException(final String specifier, final Object[] parts) {
        super(translate(specifier, parts));
    }

    /** Simple constructor.
     * Build an exception from a cause and with a specified message
     * @param message descriptive message
     * @param cause underlying cause
     */
    public OrekitException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /** Simple constructor.
     * Build an exception from a cause and with a translated and formatted message
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     * @param cause underlying cause
     */
    public OrekitException(final String specifier, final Object[] parts,
                           final Throwable cause) {
        super(translate(specifier, parts), cause);
    }

    /** Translate a string.
     * @param s string to translate
     * @return translated string, or original string if no translation
     * can be found)
     */
    public static String translate(final String s) {
        return translate(s, new Object[0]);
    }

    /** Translate and format a message.
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     * @return translated and formatted message
     */
    public static String translate(final String specifier, final Object[] parts) {
        String translated;
        try {
            translated = RESOURCES.getString(specifier);
        } catch (MissingResourceException mre) {
            translated = specifier;
        }
        return new MessageFormat(translated).format(parts);
    }

    /** Throw an {@link java.lang.IllegalArgumentException} with localized message.
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     * @exception IllegalArgumentException always throws an exception
     */
    public static void throwIllegalArgumentException(final String specifier,
                                                     final Object[] parts)
        throws IllegalArgumentException {
        throw new IllegalArgumentException(translate(specifier, parts));
    }

}
