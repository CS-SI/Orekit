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
import org.hipparchus.util.Precision;

import java.io.IOException;
import java.util.Locale;

/** Formatter for double numbers in scientific format with low overhead.
 * <p>
 * This class is intended to be used when formatting large amounts of data with
 * fixed scientific formats like, for example, large ephemeris or measurement files.
 * </p>
 * <p>
 * Building the formatter is done once, and the formatter
 * {@link #appendTo(Appendable, double)} or {@link #toString(double)} methods can
 * be called hundreds of thousands of times, without incurring the overhead that
 * would occur with {@code String.format()}. Some tests showed this formatter is
 * about 6-7 times faster than {@code String.format()} with
 * {@code %{width}.{%precision}e} format.
 * </p>
 * <p>
 * Instances of this class are immutable
 * </p>
 * @author Luc Maisonobe
 * @since 14.0
 */
public class FastScientificFormatter extends FastDoubleFormatter {

    /** Scaling formatters. */
    private static final ScalingFormatter[] SCALING = new ScalingFormatter[(0x1 << 11) - 1];

    static {
        SCALING[0] = new SubNormalNumberFormatter();
        for (int i = 1; i < SCALING.length; ++i) {
            final double d = Double.longBitsToDouble(((long) i) << 52);
            SCALING[i] = new NormalNumberFormatter((int) FastMath.floor(FastMath.log10(d)));
        }
    }

    /** Formatter for mantissa when exponent fits in 2 digits. */
    private final FastDecimalFormatter twoDigitsFormatter;

    /** Formatter for mantissa when exponent fits in 3 digits. */
    private final FastDecimalFormatter threeDigitsFormatter;

    /** Simple constructor.
     * <p>
     * This constructor is equivalent to {@link java.util.Formatter Formatter}
     * float format {@code %{width}.{width-7}e}
     * </p>
     * @param width number of characters to output
     */
    public FastScientificFormatter(final int width) {
        super(width);
        twoDigitsFormatter   = new FastDecimalFormatter(width - 4, width - 7);
        threeDigitsFormatter = new FastDecimalFormatter(width - 5, width - 8);
    }

    /** {@inheritDoc} */
    @Override
    protected void appendRegularValueTo(final Appendable appendable, final double value) throws IOException {

        // extract the binary exponent, with a special case for exact 0
        final int exponent = value == 0.0 ?
                             1023 :
                             (int) ((Double.doubleToRawLongBits(value) & 0x7ff0000000000000L) >>> 52);

        // select the scaling formatter for the correct range
        ScalingFormatter scaling = SCALING[exponent];
        if (scaling.outOfRange(value)) {
            // number is too large, we need to change the formatter
            scaling = SCALING[exponent + 1];
        }

        // format number
        scaling.appendTo(appendable, value, this);

    }

    /** Scaling formatter. */
    private interface ScalingFormatter {

        /** Check if a value exceeds formatter range.
         * @param value value to check
         * @return true if value exceeds formatter range
         */
        boolean outOfRange(double value);

        /** Append one formatted value to an {@code Appendable}.
         * @param appendable to append value to
         * @param value value to format
         * @param scFormatter calling scientific formatter
         * @exception IOException if an I/O error occurs
         */
        void appendTo(Appendable appendable, double value, FastScientificFormatter scFormatter)
            throws IOException;

    }

    /** Formatter for subnormal numbers. */
    private static final class SubNormalNumberFormatter implements ScalingFormatter {

        /** {@inheritDoc} */
        @Override
        public boolean outOfRange(final double value) {
            return value >= Precision.SAFE_MIN;
        }

        /** {@inheritDoc} */
        @Override
        public void appendTo(final Appendable appendable, final double value,
                             final FastScientificFormatter scFormatter)
            throws IOException {
            appendable.append(scFormatter.toString(value * 1.0e200).replace("e-1", "e-3"));
        }

    }

    /** Formatter for normal numbers. */
    private static final class NormalNumberFormatter implements ScalingFormatter {

        /** Exponent part. */
        private final String exponent;

        /** Indicator for two digits exponents. */
        private final boolean twoDigits;

        /** Scaling factor to retrieve a number between 1.0 (included) and 10.0 (excluded). */
        private final double factor;

        /** Simple constructor.
         * @param n decimal exponent n
         */
        NormalNumberFormatter(final int n) {
            this.twoDigits = FastMath.abs(n) < 100;
            this.exponent  = String.format(Locale.US, twoDigits ? "e%+03d" : "e%+04d", n);
            this.factor    = FastMath.pow(10.0, -n);
        }

        /** {@inheritDoc} */
        @Override
        public boolean outOfRange(final double value) {
            return FastMath.abs(value * factor) >= 10.0;
        }

        /** {@inheritDoc} */
        @Override
        public void appendTo(final Appendable appendable, final double value,
                             final FastScientificFormatter scFormatter)
            throws IOException {
            final FastDecimalFormatter formatter = twoDigits ?
                                                   scFormatter.twoDigitsFormatter :
                                                   scFormatter.threeDigitsFormatter;
            formatter.appendRegularValueTo(appendable, value * factor);
            appendable.append(exponent);
        }

    }

}
