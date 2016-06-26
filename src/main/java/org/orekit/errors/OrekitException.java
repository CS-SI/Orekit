/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import java.util.Locale;

import org.hipparchus.exception.Localizable;
import org.hipparchus.exception.MathRuntimeException;

/** This class is the base class for all specific exceptions thrown by
 * the Orekit classes.

 * <p>When the Orekit classes throw exceptions that are specific to
 * the package, these exceptions are always subclasses of
 * OrekitException. When exceptions that are already covered by the
 * standard java API should be thrown, like
 * ArrayIndexOutOfBoundsException or InvalidParameterException, these
 * standard exceptions are thrown rather than the Hipparchus specific
 * ones.</p>
 * <p>This class also provides utility methods to throw some standard
 * java exceptions with localized messages.</p>
 *
 * @author Luc Maisonobe

 */

public class OrekitException extends Exception implements LocalizedException {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150611L;

    /** Format specifier (to be translated). */
    private final Localizable specifier;

    /** Parts to insert in the format (no translation). */
    private final Object[] parts;

    /** Simple constructor.
     * Build an exception with a translated and formatted message
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     */
    public OrekitException(final Localizable specifier, final Object ... parts) {
        this.specifier = specifier;
        this.parts     = (parts == null) ? new Object[0] : parts.clone();
    }

    /** Copy constructor.
     * @param exception exception to copy from
     * @since 5.1
     */
    public OrekitException(final OrekitException exception) {
        super(exception);
        this.specifier = exception.specifier;
        this.parts     = exception.parts.clone();
    }

    /** Simple constructor.
     * Build an exception from a cause and with a specified message
     * @param message descriptive message
     * @param cause underlying cause
     */
    public OrekitException(final Localizable message, final Throwable cause) {
        super(cause);
        this.specifier = message;
        this.parts     = new Object[0];
    }

    /** Simple constructor.
     * Build an exception from a cause and with a translated and formatted message
     * @param cause underlying cause
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     */
    public OrekitException(final Throwable cause, final Localizable specifier,
                           final Object ... parts) {
        super(cause);
        this.specifier = specifier;
        this.parts     = (parts == null) ? new Object[0] : parts.clone();
    }

    /** Simple constructor.
     * Build an exception from an Hipparchus exception
     * @param exception underlying Hipparchus exception
     * @since 6.0
     */
    public OrekitException(final MathRuntimeException exception) {
        super(exception);
        this.specifier = exception.getSpecifier();
        this.parts     = exception.getParts();
    }

    /** {@inheritDoc} */
    @Override
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

    /** {@inheritDoc} */
    @Override
    public Localizable getSpecifier() {
        return specifier;
    }

    /** {@inheritDoc} */
    @Override
    public Object[] getParts() {
        return parts.clone();
    }

    /** Recover a OrekitException, possibly embedded in a {@link MathRuntimeException}.
     * <p>
     * If the {@code MathRuntimeException} does not embed a OrekitException, a
     * new one will be created.
     * </p>
     * @param exception MathRuntimeException to analyze
     * @return a (possibly embedded) OrekitException
     */
    public static OrekitException unwrap(final MathRuntimeException exception) {

        for (Throwable t = exception; t != null; t = t.getCause()) {
            if (t instanceof OrekitException) {
                return (OrekitException) t;
            }
        }

        return new OrekitException(exception);

    }

    /**
     * Builds a message string by from a pattern and its arguments.
     * @param locale Locale in which the message should be translated
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     * @return a message string
     */
    private static String buildMessage(final Locale locale, final Localizable specifier, final Object ... parts) {
        return (specifier == null) ? "" : new MessageFormat(specifier.getLocalizedString(locale), locale).format(parts);
    }

}
