/* Contributed in the public domain.
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.utils;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/** Formatter used to produce strings from data that are compliant with CCSDS standards.
 * <p>
 * Formats a double number to achieve CCSDS formatting standards for: OPM, OMM, OEM, or OCM (502.0-B-3 7.5.6),
 * CDM (508.0-B-1 6.3.2.2), TDM (503.0-B-2 4.3.4), and ADM (504.0-B-2 6.8.4.1).
 * This states that the mantissa shall not exceed 16 digits.
 * </p>
 * <p>
 * This does NOT ensure round-trip safety. See {@link AccurateFormatter} for a formatter that ensures round trip safety.
 * </p>
 * @author John Ajamian
 * @since 13.0
 */
public class TruncatedCcsdsFormatter implements Formatter {

    /** Maximum digits allowed by CCSDS standards. */
    private static final int MAXIMUM_ODM_DIGITS = 16;

    /** Used to format double to be compliant with CCSDS standards. */
    private static final String CCSDS_FORMAT = "0.0##############E0##";

    /** Used to make sure seconds is only 16 digits. */
    private static final String SECOND_FORMAT = "00.0#############";

    /** Public constructor.
     */
    public TruncatedCcsdsFormatter() {
        // nothing to do
    }

    /** Format a double number. Formats to CCSDS compliant standards.
     * @param value number to format
     * @return number formatted to full accuracy or CCSDS standards
     */
    @Override
    public String toString(final double value) {
        final DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(STANDARDIZED_LOCALE);
        formatter.applyLocalizedPattern(CCSDS_FORMAT);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        formatter.setMaximumFractionDigits(MAXIMUM_ODM_DIGITS - 1);
        return formatter.format(value);
    }

    /** Formats to CCSDS 16 digit standard for the seconds variable.
     * {@inheritDoc}
     */
    @Override
    public String toString(final int year, final int month, final int day,
                           final int hour, final int minute, final double seconds) {

        final DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(STANDARDIZED_LOCALE);
        formatter.applyLocalizedPattern(SECOND_FORMAT);
        formatter.setRoundingMode(RoundingMode.DOWN);
        formatter.setMaximumFractionDigits(MAXIMUM_ODM_DIGITS - 2);
        formatter.setMinimumIntegerDigits(2);

        return String.format(STANDARDIZED_LOCALE, DATE_FORMAT,
                year, month, day,
                hour, minute, formatter.format(seconds));
    }
}
