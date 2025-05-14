/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.utils.formatting;

import org.hipparchus.util.FastMath;

/** Formatter for real numbers with low overhead.
 * <p>
 * This class is intended to be used when formatting large amounts of data with
 * fixed formats like, for example, large ephemeris or measurement files. Building
 * the formatter is done once, and the formatter {@link #toString(double)} method
 * can be called hundreds of thousands of times, without incurring the overhead
 * that would occur with {@code String.format()}.
 * </p>
 * <p>
 * Instances of this class are immutable
 * </p>
 * @author Luc Maisonobe
 * @since 13.0.3
 */
public class FastRealFormatter {

    /** Scaling array for fractional part. */
    private static final long[] SCALING = new long[19];

    static {
        SCALING[0] = 1L;
        for (int i = 1; i < SCALING.length; ++i) {
            SCALING[i] = 10L * SCALING[i - 1];
        }
    }

    /** Number of characters to output. */
    private final int width;

    /** Scaling. */
    private final long scaling;

    /** Simple constructor.
     * <p>
     * This constructor is equivalent to {@link java.util.Formatter} float
     * format specification {@code %width.placesf}
     * </p>
     * @param width number of characters to output
     * @param places number of decimal places
     */
    public FastRealFormatter(final int width, final int places) {
        this.width   = width;
        this.scaling = SCALING[places];
    }

    /** Format one value.
     * @param value value to format
     * @return formatted string
     */
    public String toString(final double value) {

        // prepare formatted string
        final StringBuilder formatted = new StringBuilder();

        if (Double.isNaN(value)) {
            // special case for NaN
            formatted.append("NaN");
        } else {

            // manage sign
            if (FastMath.copySign(1.0, value) < 0) {
                formatted.append('-');
            }

            if (Double.isInfinite(value)) {
                // special case for infinities
                formatted.append("Infinity");
            } else {

                // regular number
                final double abs    = FastMath.abs(value);
                double       before = FastMath.floor(abs);
                long         after  = FastMath.round((abs - before) * scaling);

                if (after >= scaling) {
                    // we have to round up to the next integer
                    before += 1;
                    after   = 0L;
                }

                // convert to string
                formatted.append(FastMath.round(before));
                if (scaling > 1) {

                    // added decimal point
                    formatted.append('.');

                    // adding scaling is done to put a leading '1' that is stripped right away
                    // this allows getting the proper number of leading zeros in the fractional part
                    formatted.append(Long.toString(after + scaling).substring(1));

                }

            }
        }

        // left padding
        while (formatted.length() < width) {
            formatted.insert(0, ' ');
        }

        // finalize string
        return formatted.toString();

    }

}
