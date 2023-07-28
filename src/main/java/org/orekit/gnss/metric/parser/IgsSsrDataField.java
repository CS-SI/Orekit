/* Copyright 2002-2023 CS GROUP
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

import org.orekit.utils.units.Unit;

/** Enum containing all intermediate level data fields that can be parsed
 * to build an IGS SSR message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public enum IgsSsrDataField implements DataField {

    /** IGS SSR Version. */
    IDF001 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_3.decode(message).intValue();
        }
    },

    /** IGS Message number. */
    IDF002 {
        /** {@inheritDoc} */
        @Override
        public String stringValue(final EncodedMessage message, final int n) {
            return String.format(Locale.US, "%3s", DataType.U_INT_8.decode(message).intValue()).trim();
        }
    },

    /** SSR Epoch time 1s. */
    IDF003 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_20.decode(message).intValue();
        }
    },

    /** SSR Update interval. */
    IDF004 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            switch (DataType.BIT_4.decode(message).byteValue()) {
                case 0  : return 1;
                case 1  : return 2;
                case 2  : return 5;
                case 3  : return 10;
                case 4  : return 15;
                case 5  : return 30;
                case 6  : return 60;
                case 7  : return 120;
                case 8  : return 240;
                case 9  : return 300;
                case 10 : return 600;
                case 11 : return 900;
                case 12 : return 1800;
                case 13 : return 3600;
                case 14 : return 7200;
                default : return 10800;
            }
        }
    },

    /** Multiple Message Indicator. */
    IDF005 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
        }
    },

    /** Global/Regional CRS Indicator. */
    IDF006 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
        }
    },

    /** IOD SSR. */
    IDF007 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_4.decode(message).intValue();
        }
    },

    /** SSR Provider ID. */
    IDF008 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_16.decode(message).intValue();
        }
    },

    /** SSR Solution ID. */
    IDF009 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_4.decode(message).intValue();
        }
    },

    /** Number of satellites. */
    IDF010 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_6.decode(message).intValue();
        }
    },

    /** GNSS satellite ID. */
    IDF011 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_6.decode(message).intValue();
        }
    },

    /** GNSS IOD. */
    IDF012 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_8.decode(message).intValue();
        }
    },

    /** Delta Orbit Radial (m). */
    IDF013 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.MM.toSI(DataType.INT_22.decode(message).intValue() * 0.1);
        }
    },

    /** Delta Along-Track (m). */
    IDF014 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.MM.toSI(DataType.INT_20.decode(message).intValue() * 0.4);
        }
    },

    /** Delta Cross-Track (m). */
    IDF015 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.MM.toSI(DataType.INT_20.decode(message).intValue() * 0.4);
        }
    },

    /** Dot Delta Radial (m/s). */
    IDF016 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.MM_PER_S.toSI(DataType.INT_21.decode(message).intValue() * 0.001);
        }
    },

    /** Dot Delta Along-Track (m/s). */
    IDF017 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.MM_PER_S.toSI(DataType.INT_19.decode(message).intValue() * 0.004);
        }
    },

    /** Dot Delta Cross-Track (m/s). */
    IDF018 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.MM_PER_S.toSI(DataType.INT_19.decode(message).intValue() * 0.004);
        }
    },

    /** Delta Clock C0. */
    IDF019 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.MM.toSI(DataType.INT_22.decode(message).intValue() * 0.1);
        }
    },

    /** Delta Clock C1. */
    IDF020 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.MM_PER_S.toSI(DataType.INT_21.decode(message).intValue() * 0.001);
        }
    },

    /** Delta Clock C2. */
    IDF021 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.MM_PER_S2.toSI(DataType.INT_27.decode(message).intValue() * 0.00002);
        }
    },

    /** High Rate Clock Correction. */
    IDF022 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.MM.toSI(DataType.INT_22.decode(message).intValue() * 0.1);
        }
    },

    /** No. of Code Biases Processed. */
    IDF023 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_5.decode(message).intValue();
        }
    },

    /** GNSS Signal and Tracking Mode Identifier. */
    IDF024 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_5.decode(message).intValue();
        }
    },

    /** Code Bias for specified GNSS signal. */
    IDF025 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.INT_14.decode(message).intValue() * 0.01;
        }
    },

    /** Yaw angle used for computation of phase wind-up correction. */
    IDF026 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(DataType.U_INT_9.decode(message).intValue() / 256.0);
        }
    },

    /** Yaw rate. */
    IDF027 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(DataType.INT_8.decode(message).intValue() / 8192.0);
        }
    },

    /** Phase Bias for specified GNSS signal (m). */
    IDF028 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.INT_20.decode(message).intValue() * 0.0001;
        }
    },

    /** Signal Integer Indicator. */
    IDF029 {
        /** {@inheritDoc} */
        @Override
        public boolean booleanValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message) > 0;
        }
    },

    /** Signal Wide-Lane Integer Indicator. */
    IDF030 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_2.decode(message).intValue();
        }
    },

    /** Signal Discontinuity Counter. */
    IDF031 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_4.decode(message).intValue();
        }
    },

    /** Dispersive Bias Consistency Indicator. */
    IDF032 {
        /** {@inheritDoc} */
        @Override
        public boolean booleanValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message) > 0;
        }
    },

    /** Melbourne-Wübbena Consistency Indicator. */
    IDF033 {
        /** {@inheritDoc} */
        @Override
        public boolean booleanValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message) > 0;
        }
    },

    /** SSR URA. */
    IDF034 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_6.decode(message).intValue();
        }
    },

    /** Number of Ionospheric Layers. */
    IDF035 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            // The parsed number is between 0 and 3. So, we need to add 1.
            return DataType.U_INT_2.decode(message).intValue() + 1;
        }
    },

    /** Height of the Ionospheric layer (m). */
    IDF036 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            // 10 km resolution
            return Unit.KILOMETRE.toSI(DataType.U_INT_8.decode(message).intValue() * 10.0);
        }
    },

    /** Spherical Harmonic Degree. */
    IDF037 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            // The parsed number is between 0 and 15. So, we need to add 1.
            return DataType.U_INT_4.decode(message).intValue() + 1;
        }
    },

    /** Spherical Harmonic Order. */
    IDF038 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            // The parsed number is between 0 and 15. So, we need to add 1.
            return DataType.U_INT_4.decode(message).intValue() + 1;
        }
    },

    /** Spherical Harmonic Coefficient C (TECU). */
    IDF039 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.INT_16.decode(message).intValue() * 0.005;
        }
    },

    /** Spherical Harmonic Coefficient S (TECU). */
    IDF040 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.INT_16.decode(message).intValue() * 0.005;
        }
    },

    /** VTEC Quality Indicator (TECU). */
    IDF041 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.U_INT_9.decode(message).intValue() * 0.05;
        }
    };

}
