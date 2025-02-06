/* Copyright 2024-2025 The Johns Hopkins University Applied Physics Laboratory
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

import org.orekit.files.iirv.terms.base.LongValuedIIRVTerm;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.UTCScale;

/**
 * 3-character integer representing the day of the year.
 * <p>
 * Valid values: 001-366 (365 + 1 for leap year)
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class DayOfYearTerm extends LongValuedIIRVTerm {

    /** The length of the IIRV term within the message. */
    public static final int DAY_OF_YEAR_LENGTH = 3;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String DAY_OF_YEAR_PATTERN = "(00[1-9]|0[1-9][0-9]|[1-2][0-9]{2}|3[0-5][0-9]|36[0-6])";

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, String, int, boolean)}
     *
     * @param stringValue Day of the year (001-366)
     */
    public DayOfYearTerm(final String stringValue) {
        super(DAY_OF_YEAR_PATTERN, stringValue, DAY_OF_YEAR_LENGTH, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, long, int, boolean)}
     *
     * @param value Day of the year (001-366)
     */
    public DayOfYearTerm(final long value) {
        super(DAY_OF_YEAR_PATTERN, value, DAY_OF_YEAR_LENGTH, false);
    }

    /**
     * Constructs a DayOfYearTerm object from an {@link AbsoluteDate} object.
     *
     * @param absoluteDate date object from which to infer the day of year
     * @param utc          UTC time scale
     */
    public DayOfYearTerm(final AbsoluteDate absoluteDate, final UTCScale utc) {
        this(fromAbsoluteDate(absoluteDate, utc));
    }

    /**
     * Constructs an IIRV DayOfYear from an {@link AbsoluteDate} object.
     *
     * @param absoluteDate date object from which to infer the day of year
     * @param utc          UTC time scale
     * @return day of year associated with the inputted absolute date
     */
    private static String fromAbsoluteDate(final AbsoluteDate absoluteDate, final UTCScale utc) {
        final DateTimeComponents components = absoluteDate.getComponents(utc);
        final DateComponents date = components.getDate();

        return IIRVTermUtils.addPadding(String.valueOf(date.getDayOfYear()), '0', 3, true);
    }

    /**
     * Returns the {@link DateComponents} instance that corresponds this term's value.
     *
     * @param year year to associated with the created date components
     * @return the date components associated with this term
     */
    public DateComponents getDateComponents(final int year) {
        return new DateComponents(year, toInt());
    }
}
