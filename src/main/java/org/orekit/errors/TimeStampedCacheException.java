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

import org.hipparchus.exception.Localizable;
import org.hipparchus.exception.MathRuntimeException;

/** This class is the base class for all specific exceptions thrown by
 * during the {@link org.orekit.utils.GenericTimeStampedCache}.
 *
 * @author Luc Maisonobe
 */
public class TimeStampedCacheException extends OrekitException {

    /** Serializable UID. */
    private static final long serialVersionUID = 9015424948577907926L;

    /** Simple constructor.
     * Build an exception with a translated and formatted message
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     */
    public TimeStampedCacheException(final Localizable specifier, final Object ... parts) {
        super(specifier, parts);
    }

    /** Simple constructor.
     * Build an exception from a cause and with a specified message
     * @param cause underlying cause
     * @param specifier format specifier (to be translated)
     * @param parts parts to insert in the format (no translation)
     */
    public TimeStampedCacheException(final Throwable cause, final Localizable specifier,
                                     final Object ... parts) {
        super(cause, specifier, parts);
    }

    /** Simple constructor.
     * Build an exception wrapping an {@link OrekitException} instance
     * @param exception underlying cause
     */
    public TimeStampedCacheException(final OrekitException exception) {
        super(exception);
    }

    /** Simple constructor.
     * Build an exception from an Hipparchus exception
     * @param exception underlying Hipparchus exception
     */
    public TimeStampedCacheException(final MathRuntimeException exception) {
        super(exception);
    }

    /** Recover a TimeStampedCacheException, possibly embedded in a {@link OrekitException}.
     * <p>
     * If the {@code OrekitException} does not embed a TimeStampedCacheException, a
     * new one will be created.
     * </p>
     * @param oe OrekitException to analyze
     * @return a (possibly embedded) TimeStampedCacheException
     */
    public static TimeStampedCacheException unwrap(final OrekitException oe) {

        for (Throwable t = oe; t != null; t = t.getCause()) {
            if (t instanceof TimeStampedCacheException) {
                return (TimeStampedCacheException) t;
            }
        }

        return new TimeStampedCacheException(oe);

    }

    /** Recover a TimeStampedCacheException, possibly embedded in a {@link MathRuntimeException}.
     * <p>
     * If the {@code MathRuntimeException} does not embed a TimeStampedCacheException, a
     * new one will be created.
     * </p>
     * @param exception MathRuntimeException to analyze
     * @return a (possibly embedded) TimeStampedCacheException
     */
    public static TimeStampedCacheException unwrap(final MathRuntimeException exception) {

        for (Throwable t = exception; t != null; t = t.getCause()) {
            if (t instanceof OrekitException) {
                if (t instanceof TimeStampedCacheException) {
                    return (TimeStampedCacheException) t;
                } else {
                    return new TimeStampedCacheException((OrekitException) t);
                }
            }
        }

        return new TimeStampedCacheException(exception);

    }

}
