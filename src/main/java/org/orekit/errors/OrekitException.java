/* Copyright 2002-2010 CS Communication & Systèmes
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
import java.text.ParseException;
import java.util.Locale;
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
    private static final long serialVersionUID = -6565493623428869182L;

    /** Format specifier (to be translated). */
    private final String specifier;

    /** Parts to insert in the format (no translation). */
    private final Object[] parts;

    /** Simple constructor.
     * Build an exception with a translated and formatted message
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     */
    public OrekitException(final String specifier, final Object ... parts) {
        this.specifier   = specifier;
        this.parts = (parts == null) ? new Object[0] : parts.clone();
    }

    /** Simple constructor.
     * Build an exception from a cause and with a specified message
     * @param message descriptive message
     * @param cause underlying cause
     */
    public OrekitException(final String message, final Throwable cause) {
        super(cause);
        this.specifier = message;
        this.parts = new Object[0];
    }

    /** Simple constructor.
     * Build an exception from a cause and with a translated and formatted message
     * @param cause underlying cause
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     */
    public OrekitException(final Throwable cause, final String specifier,
                           final Object ... parts) {
        super(cause);
        this.specifier = specifier;
        this.parts = new Object[0];
    }

    /** Gets the message in a specified locale.
     * @param locale Locale in which the message should be translated
     * @return localized message
     * @since 5.0
     */
    public String getMessage(final Locale locale) {
        return buildMessage(locale, specifier, parts);
    }

    /** {@inheritDoc} */
    @Override
    public String getMessage() {
        return getMessage(Locale.US);
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalizedMessage() {
        return getMessage(Locale.getDefault());
    }

    /**
     * Translate a string to a given locale.
     * @param s string to translate
     * @param locale locale into which to translate the string
     * @return translated string or original string
     * for unsupported locales or unknown strings
     */
    private static String translate(final String s, final Locale locale) {
        try {
            final ResourceBundle bundle =
                ResourceBundle.getBundle("META-INF/localization/ExceptionsMessages", locale);
            if (bundle.getLocale().getLanguage().equals(locale.getLanguage())) {
                // the value of the resource is the translated string
                return bundle.getString(s);
            }

        } catch (MissingResourceException mre) {
            // do nothing here
        }

        // the locale is not supported or the resource is unknown
        // don't translate and fall back to using the string as is
        return s;

    }

    /**
     * Builds a message string by from a pattern and its arguments.
     * @param locale Locale in which the message should be translated
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     * @return a message string
     */
    private static String buildMessage(final Locale locale, final String specifier,
                                       final Object ... parts) {
        return (specifier == null) ? "" : new MessageFormat(translate(specifier, locale), locale).format(parts);
    }

    /** Create an {@link java.lang.IllegalArgumentException} with localized message.
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     * @return an {@link java.lang.IllegalArgumentException} with localized message
     */
    public static IllegalArgumentException createIllegalArgumentException(final String specifier,
                                                                          final Object ... parts) {
        return new IllegalArgumentException() {

            /** Serializable UID. */
            private static final long serialVersionUID = -2363165884898091700L;

            /** {@inheritDoc} */
            @Override
            public String getMessage() {
                return buildMessage(Locale.US, specifier, parts);
            }

            /** {@inheritDoc} */
            @Override
            public String getLocalizedMessage() {
                return buildMessage(Locale.getDefault(), specifier, parts);
            }

        };

    }

    /** Create an {@link java.lang.IllegalStateException} with localized message.
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     * @return an {@link java.lang.IllegalStateException} with localized message
     */
    public static IllegalStateException createIllegalStateException(final String specifier,
                                                                    final Object ... parts) {

        return new IllegalStateException() {

            /** Serializable UID. */
            private static final long serialVersionUID = 6291773904397909896L;

            /** {@inheritDoc} */
            @Override
            public String getMessage() {
                return buildMessage(Locale.US, specifier, parts);
            }

            /** {@inheritDoc} */
            @Override
            public String getLocalizedMessage() {
                return buildMessage(Locale.getDefault(), specifier, parts);
            }

        };

    }

    /** Create an {@link java.text.ParseException} with localized message.
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     * @return an {@link java.text.ParseException} with localized message
     */
    public static ParseException createParseException(final String specifier,
                                                      final Object ... parts) {

        return new ParseException("", 0) {

            /** Serializable UID. */
            private static final long serialVersionUID = 6711407414521775021L;

            /** {@inheritDoc} */
            @Override
            public String getMessage() {
                return buildMessage(Locale.US, specifier, parts);
            }

            /** {@inheritDoc} */
            @Override
            public String getLocalizedMessage() {
                return buildMessage(Locale.getDefault(), specifier, parts);
            }

        };

    }

    /** Create an {@link java.lang.RuntimeException} for an internal error.
     * @param cause underlying cause
     * @return an {@link java.lang.RuntimeException} for an internal error
     */
    public static RuntimeException createInternalError(final Throwable cause) {

        /** Format specifier (to be translated). */
        final String specifier = "internal error, contact maintenance at {0}";

        /** Parts to insert in the format (no translation). */
        final String parts     = "orekit@c-s.fr";

        return new RuntimeException() {

            /** Serializable UID. */
            private static final long serialVersionUID = -8775626362761076727L;

            /** {@inheritDoc} */
            @Override
            public String getMessage() {
                return buildMessage(Locale.US, specifier, parts);
            }

            /** {@inheritDoc} */
            @Override
            public String getLocalizedMessage() {
                return buildMessage(Locale.getDefault(), specifier, parts);
            }

        };

    }

}
