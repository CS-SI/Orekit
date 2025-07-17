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

/** Formatter for double numbers with low overhead.
 * <p>
 * This class is intended to be used when formatting large amounts of data with
 * fixed formats like, for example, large ephemeris or measurement files.
 * </p>
 * <p>
 * Building the formatter is done once, and the formatter
 * {@link #appendTo(Appendable, double)} or {@link #toString(double)} methods can
 * be called hundreds of thousands of times, without incurring the overhead that
 * would occur with {@code String.format()}. Some tests showed this formatter is
 * about 5 times faster than {@code String.format()} with
 * {@code %{width}.{%precision}f} format.
 * </p>
 * <p>
 * Instances of this class are immutable
 * </p>
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class FastDoubleFormatter {

    /** Number of characters to output. */
    private final int width;

    /** Simple constructor.
     * <p>
     * This constructor is equivalent to {@link java.util.Formatter Formatter}
     * float format {@code %{width}.{precision}f}
     * </p>
     * @param width number of characters to output
     */
    public FastDoubleFormatter(final int width) {
        this.width = width;
    }

    /** Get the width.
     * @return width
     */
    public int getWidth() {
        return width;
    }

    /** Append one formatted value to an {@code Appendable}.
     * @param appendable to append value to
     * @param value value to format
     * @exception IOException if an I/O error occurs
     */
    public void appendTo(final Appendable appendable, final double value) throws IOException {

        if (Double.isNaN(value)) {
            // special case for NaN
            for (int i = 0; i < width - 3; ++i) {
                appendable.append(' ');
            }
            appendable.append("NaN");
        } else {

            if (Double.isInfinite(value)) {
                // special case for infinities
                if (FastMath.copySign(1.0, value) < 0) {
                    for (int i = 0; i < width - 9; ++i) {
                        appendable.append(' ');
                    }
                    appendable.append("-Infinity");
                } else {
                    for (int i = 0; i < width - 8; ++i) {
                        appendable.append(' ');
                    }
                    appendable.append("Infinity");
                }
            } else {
                // regular number
                appendRegularValueTo(appendable, value);
            }
        }

    }

    /** Append one formatted value to an {@code Appendable}.
     * @param appendable to append value to
     * @param value value to format
     * @exception IOException if an I/O error occurs
     */
    protected abstract void appendRegularValueTo(final Appendable appendable, final double value) throws IOException;

    /** Format one value.
     * @param value value to format
     * @return formatted string
     */
    public String toString(final double value) {
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
