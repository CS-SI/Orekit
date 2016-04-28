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
import java.text.ParseException;
import java.util.Locale;

import org.hipparchus.exception.Localizable;

/** Extension of {@link java.text.ParseException} with localized message.
 * @since 7.1
 */
public class OrekitParseException extends ParseException implements LocalizedException {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150611L;

    /** Format specifier (to be translated). */
    private final Localizable specifier;

    /** Parts to insert in the format (no translation). */
    private final Object[] parts;

    /** Create an exception with localized message.
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     */
    public OrekitParseException(final Localizable specifier, final Object ... parts) {
        super("", 0);
        this.specifier = specifier;
        this.parts     = (parts == null) ? new Object[0] : parts.clone();
    }

    /** {@inheritDoc} */
    @Override
    public String getMessage(final Locale locale) {
        return buildMessage(locale);
    }

    /** {@inheritDoc} */
    @Override
    public String getMessage() {
        return buildMessage(Locale.US);
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalizedMessage() {
        return buildMessage(Locale.getDefault());
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

    /**
     * Builds a message string by from a pattern and its arguments.
     * @param locale Locale in which the message should be translated
     * @return a message string
     */
    private String buildMessage(final Locale locale) {
        return (specifier == null) ?
                "" :
                new MessageFormat(specifier.getLocalizedString(locale), locale).format(parts);
    }

}
