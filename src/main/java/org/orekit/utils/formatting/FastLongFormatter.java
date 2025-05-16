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
import org.orekit.errors.OrekitInternalError;

import java.io.IOException;
import java.util.Arrays;

/** Formatter for long integers with low overhead.
 * <p>
 * This class is intended to be used when formatting large amounts of data with
 * fixed formats like, for example, large ephemeris or measurement files.
 * </p>
 * <p>
 * Building the formatter is done once, and the formatter
 * {@link #appendTo(Appendable, long)} or {@link #toString(long)} methods can be
 * called hundreds of thousands of times, without incurring the overhead that
 * would occur with {@code String.format()}. Some tests showed this formatter is
 * about 10 times faster than {@code String.format()} with {@code %{width}d} format.
 * </p>
 * <p>
 * Instances of this class are immutable
 * </p>
 * @author Luc Maisonobe
 * @since 13.0.3
 */
public class FastLongFormatter {

    /** Digits. */
    private static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    };

    /** Number of characters to output. */
    private final int width;

    /** Zero padding indicator. */
    private final boolean zeroPadding;

    /** Size of the conversion array. */
    private final int size;

    /** Simple constructor.
     * <p>
     * This constructor is equivalent to either {@link java.util.Formatter Formatter}
     * integer format {@code %{width}d} or {@code %0{width}d}
     * </p>
     * @param width       number of characters to output
     * @param zeroPadding if true, the result is left padded with '0' until it matches width
     */
    public FastLongFormatter(final int width, final boolean zeroPadding) {
        this.width       = width;
        this.zeroPadding = zeroPadding;
        this.size        = FastMath.max(width, 20);
    }

    /** Get the width.
     * @return width
     */
    public int getWidth() {
        return width;
    }

    /** Check if left padding uses '0' characters.
     * @return true if left padding uses '0' characters
     */
    public boolean hasZeroPadding() {
        return zeroPadding;
    }

    /** Append one formatted value to an {@code Appendable}.
     * @param appendable to append value to
     * @param value value to format
     * @exception IOException if an I/O error occurs
     */
    public void appendTo(final Appendable appendable, final long value) throws IOException {

        // initialize conversion loop
        final char[] digits = new char[size];
        int index = 0;
        long remaining;
        if (value == Long.MIN_VALUE) {
            // special case for value -9223372036854775808L that has no representable opposite
            digits[0] = '8';
            index     = 1;
            remaining = 922337203685477580L;
        } else {
            remaining = FastMath.abs(value);
        }

        // convert to decimal string
        do {
            digits[index++] = DIGITS[(int) (remaining % 10L)];
            remaining /= 10L;
        } while (remaining > 0L);

        // manage sign and padding
        if (zeroPadding) {
            if (value < 0L) {
                // zero padding a negative value occurs between the minus sign and the most significant digit
                if (index < width - 1) {
                    Arrays.fill(digits, index, width - 1, '0');
                    index = width - 1;
                }
                digits[index++] = '-';
            }
            else {
                if (index < width) {
                    Arrays.fill(digits, index, width, '0');
                    index = width;
                }
            }
        } else {
            if (value < 0L) {
                // space padding a negative value is before minus sign
                digits[index++] = '-';
            }
            if (index < width) {
                Arrays.fill(digits, index, width, ' ');
                index = width;
            }
        }

        // fill up string
        while (index > 0) {
            appendable.append(digits[--index]);
        }

    }

    /** Format one value.
     * @param value value to format
     * @return formatted string
     */
    public String toString(final long value) {
        try {
            final StringBuilder builder = new StringBuilder();
            appendTo(builder, value);
            return builder.toString();
        } catch (IOException ioe) {
            // this should never happen
            throw new OrekitInternalError(ioe);
        }
    }

}
