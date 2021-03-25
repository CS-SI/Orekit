/* Copyright 2002-2021 CS GROUP
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
package org.orekit.gnss.metric.parser;

import java.util.Locale;

import org.hipparchus.util.FastMath;

/** Enum containing all intermediate level data fields that can be parsed
 * to build a RTCM message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public enum RtcmDataField implements DataField {

    /** RTCM Message number. */
    DF002 {
        /** {@inheritDoc} */
        @Override
        public String stringValue(final EncodedMessage message, final int n) {
            return String.format(Locale.US, "%4s", DataType.U_INT_12.decode(message).intValue()).trim();
        }
    },

    /** GPS Satellite ID. */
    DF009 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_6.decode(message).intValue();
        }
    },

    /** GPS IODE (Issue Of Data, Ephemeris). */
    DF071 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_8.decode(message).intValue();
        }
    },

    /** GPS Week number. */
    DF076 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_10.decode(message).intValue();
        }
    },

    /** GPS SV Accuracy. */
    DF077 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_4.decode(message).intValue();
        }
    },

    /** GPS CODE ON L2. */
    DF078 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_2.decode(message).intValue();
        }
    },

    /** GPS Rate of Inclination Angle. */
    DF079 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            // Returned value is in semi-circles/s
            return FastMath.scalb(DataType.INT_14.decode(message).intValue(), -43);
        }
    },

    /** GPS toc. */
    DF081 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.U_INT_16.decode(message).intValue() * 16.0;
        }
    },

    /** GPS a<sub>f2</sub>. */
    DF082 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_8.decode(message).intValue(), -55);
        }
    },

    /** GPS a<sub>f1</sub>. */
    DF083 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -43);
        }
    },

    /** GPS a<sub>f0</sub>. */
    DF084 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_22.decode(message).intValue(), -31);
        }
    },

    /** GPS IODC (Issue Of Data, Clock). */
    DF085 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_10.decode(message).intValue();
        }
    },

    /** GPS C<sub>rs</sub>. */
    DF086 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -5);
        }
    },

    /** GPS Δn (DELTA n). */
    DF087 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            // Returned value is in semi-circles/s
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -43);
        }
    },

    /** GPS M<sub>0</sub>. */
    DF088 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            // Returned value is in semi-circles
            return FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31);
        }
    },

    /** GPS C<sub>uc</sub>. */
    DF089 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -29);
        }
    },

    /** GPS Eccentricity (e). */
    DF090 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.U_INT_32.decode(message).longValue(), -33);
        }
    },

    /** GPS C<sub>us</sub>. */
    DF091 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -29);
        }
    },

    /** GPS A<sup>1/2</sup>. */
    DF092 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.U_INT_32.decode(message).longValue(), -19);
        }
    },

    /** GPS t<sub>oe</sub>. */
    DF093 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.U_INT_16.decode(message).intValue() * 16.0;
        }
    },

    /** GPS C<sub>ic</sub>. */
    DF094 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -29);
        }
    },

    /** GPS Ω<sub>0</sub> (OMEGA)<sub>0</sub>. */
    DF095 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            // Returned value is in semi-circles
            return FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31);
        }
    },

    /** GPS C<sub>is</sub>. */
    DF096 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -29);
        }
    },

    /** GPS i<sub>0</sub>. */
    DF097 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            // Returned value is in semi-circles
            return FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31);
        }
    },

    /** GPS C<sub>rc</sub>. */
    DF098 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -5);
        }
    },

    /** GPS ω (Argument of Perigee). */
    DF099 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            // Returned value is in semi-circles
            return FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31);
        }
    },

    /** GPS OMEGADOT (Rate of Right Ascension). */
    DF100 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            // Returned value is in semi-circles/s
            return FastMath.scalb(DataType.INT_24.decode(message).intValue(), -43);
        }
    },

    /** GPS t<sub>GD</sub>. */
    DF101 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_8.decode(message).intValue(), -31);
        }
    },

    /** GPS SV HEALTH. */
    DF102 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_6.decode(message).intValue();
        }
    },

    /** GPS L2 P data flag. */
    DF103 {
        /** {@inheritDoc} */
        @Override
        public boolean booleanValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message) == 0;
        }
    },

    /** GPS Fit Interval. */
    DF137 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
        }
    },

}
