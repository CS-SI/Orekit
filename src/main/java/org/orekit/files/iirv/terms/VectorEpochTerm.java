/* Copyright 2024 The Johns Hopkins University Applied Physics Laboratory
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
package org.orekit.files.iirv.terms;

import org.orekit.files.iirv.terms.base.DoubleValuedIIRVTerm;
import org.orekit.files.iirv.terms.base.IIRVVectorTerm;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.UTCScale;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Vector epoch in UTC with resolution to nearest millisecond.
 * <p>
 * Valid values:
 * <p>
 * hhmmsssss where:
 * <ul>
 * <li> hh = 00 to 23
 * <li> mm = 00 to 59
 * <li> sssss = 00000 to 59999 (milliseconds, implied decimal point three places from right)
 * </ul>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class VectorEpochTerm extends IIRVVectorTerm<TimeComponents> {

    /** The length of the IIRV term within the message. */
    public static final int VECTOR_EPOCH_TERM_LENGTH = 9;

    /**
     * Regular expression that ensures the validity of string values for this term.
     * <p>
     * String in the form "hhmmsssss":
     * <ul>
     * <li> hh is 00 to 23: (0[0-9]|1[0-9]|2[0-3])
     * <li> mm is 00 to 59: ([0-5][0-9])
     * <li> sssss is 00000 to 599999: ([0-5][0-9]{4})
     * </ul>
     */
    public static final String VECTOR_EPOCH_TERM_PATTERN = "(0[0-9]|1[0-9]|2[0-3])([0-5][0-9])([0-5][0-9]{4})";

    /**
     * Constructs from a String value.
     *
     * @param stringValue Day of the year (001-366)
     */
    public VectorEpochTerm(final String stringValue) {
        super(VECTOR_EPOCH_TERM_PATTERN, VectorEpochTerm.fromString(stringValue), VECTOR_EPOCH_TERM_LENGTH);
    }

    /**
     * Constructs from a {@link TimeComponents} value.
     *
     * @param timeComponents TimeComponents value to extract vector epoch information from
     */
    public VectorEpochTerm(final TimeComponents timeComponents) {
        super(VECTOR_EPOCH_TERM_PATTERN, timeComponents, VECTOR_EPOCH_TERM_LENGTH);
    }

    /**
     * Constructs from a {@link AbsoluteDate} value.
     *
     * @param absoluteDate AbsoluteDate value to extract vector epoch information from (in UTC)
     * @param utc          UTC time scale
     */
    public VectorEpochTerm(final AbsoluteDate absoluteDate, final UTCScale utc) {
        super(VECTOR_EPOCH_TERM_PATTERN,
            absoluteDate.getComponents(utc).getTime(),
            VECTOR_EPOCH_TERM_LENGTH);
    }

    /**
     * Parses an IIRV string in  format to a {@link TimeComponents} instance.
     * <p>
     * Format is "hhmmsssss" where the implied decimal place is three from the left (hh mm ss.sss)
     *
     * @param iirvString IIRV-formatted string to parse
     * @return time components contained in the input string
     */
    private static TimeComponents fromString(final String iirvString) {
        final int hour = Integer.parseInt(iirvString.substring(0, 2));
        final int minute = Integer.parseInt(iirvString.substring(2, 4));

        // Convert {fullSeconds}.{fractionalSeconds} to seconds
        final double second = DoubleValuedIIRVTerm.computeValueFromString(iirvString.substring(4, 9), 3);

        return new TimeComponents(hour, minute, second);
    }

    /**
     * Gets the two-character hour of the vector epoch.
     *
     * @return hh: hour of the vector epoch
     */
    public String hh() {
        return toEncodedString().substring(0, 2);
    }

    /**
     * Gets the two-character minute of the vector epoch.
     *
     * @return mm: minute of the vector epoch
     */
    public String mm() {
        return toEncodedString().substring(2, 4);
    }

    /**
     * Gets the two-character second of the vector epoch.
     *
     * @return ss: second of the vector epoch
     */
    public String ss() {
        return toEncodedString().substring(4, 6);
    }


    /** {@inheritDoc} */
    @Override
    public String toEncodedString(final TimeComponents value) {

        final DecimalFormat secondsFormat = new DecimalFormat("00.000", new DecimalFormatSymbols(Locale.US));
        final String ss_sss = secondsFormat.format(value.getSecond());

        // Edge case: 60th second doesn't make sense... Round up the hour instead
        if (ss_sss.charAt(0) == '6') {
            final int nextSecond = 0;
            int nextMinute = value.getMinute() + 1;
            int nextHour = value.getHour();
            if (nextMinute == 60) {
                nextMinute = 0;
                nextHour++;
                if (nextHour == 24) {
                    nextHour = 0;
                }
            }
            return toEncodedString(new TimeComponents(nextHour, nextMinute, nextSecond));
        }

        return String.format("%02d%02d%s",
            value.getHour(),
            value.getMinute(),
            ss_sss.replace(".", "")
        );
    }
}
