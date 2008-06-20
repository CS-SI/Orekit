/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.errors;

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

 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */

public class OrekitException extends Exception {

    /** Serializable UID. */
    private static final long serialVersionUID = 445319503291578390L;

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

    /** Create an {@link java.lang.IllegalArgumentException} with localized message.
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     * @return an {@link java.lang.IllegalArgumentException} with localized message
     */
    public static IllegalArgumentException createIllegalArgumentException(final String specifier,
                                                                          final Object[] parts) {
        return new IllegalArgumentException(translate(specifier, parts));
    }

    /** Create an {@link java.lang.IllegalStateException} with localized message.
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     * @return an {@link java.lang.IllegalStateException} with localized message
     */
    public static IllegalStateException createIllegalStateException(final String specifier,
                                                                          final Object[] parts) {
        return new IllegalStateException(translate(specifier, parts));
    }

}
