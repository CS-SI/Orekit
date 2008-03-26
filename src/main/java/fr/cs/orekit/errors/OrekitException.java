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
 * standard exceptions are thrown rather than the mantissa specific
 * ones.</p>
 * <p>This class also provides utility methods to throw some standard
 * java exceptions with localized messages.</p>
 *
 * @author Luc Maisonobe

 */

public class OrekitException
extends Exception {

    /** Serializable UID. */
    private static final long serialVersionUID = 8837701027854807120L;

    /** Resources bundle. */
    private static final ResourceBundle resources;

    static {
        resources = ResourceBundle.getBundle("META-INF/localization/ExceptionsMessages");
    }

    /** Translate a string.
     * @param s string to translate
     * @return translated string, or original string if no translation
     * can be found)
     */
    public static String translate(String s) {
        return translate(s, new String[0]);
    }

    /** Translate and format a message.
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     */
    public static String translate(String specifier, Object[] parts) {
        String translated;
        try {
            translated = resources.getString(specifier);
        } catch (MissingResourceException mre) {
            translated = specifier;
        }
        return new MessageFormat(translated).format(parts);
    }

    /** Throw an {@link java.lang.IllegalArgumentException} with
     * localized message.
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     * @exception IllegalArgumentException always throws an exception
     */
    public static void throwIllegalArgumentException(String specifier,
                                                     Object[] parts)
        throws IllegalArgumentException {
        throw new IllegalArgumentException(translate(specifier, parts));
    }

    /** Simple constructor.
     * Build an exception with a translated and formatted message
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     */
    public OrekitException(String specifier, Object[] parts) {
        super(translate(specifier, parts));
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
    public OrekitException(String specifier, Object[] parts, Throwable cause) {
        super(translate(specifier, parts), cause);
    }

}
