/* Copyright 2002-2022 CS GROUP
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
import org.orekit.utils.units.Unit;

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

    /** GLONASS Satellite ID. */
    DF038 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_6.decode(message).intValue();
        }
    },

    /** GLONASS Satellite Frequency Channel Number. */
    DF040 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            final int id = DataType.U_INT_5.decode(message).intValue();
            return id - 7;
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
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_14.decode(message).intValue(), -43));
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
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_16.decode(message).intValue(), -43));
        }
    },

    /** GPS M<sub>0</sub>. */
    DF088 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
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
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
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
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
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
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** GPS OMEGADOT (Rate of Right Ascension). */
    DF100 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_24.decode(message).intValue(), -43));
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

    /** GLONASS almanac health (C<sub>n</sub> word). */
    DF104 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
        }
    },

    /** GLONASS almanac health availability indicator. */
    DF105 {
        /** {@inheritDoc} */
        @Override
        public boolean booleanValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message) > 0;
        }
    },

    /** GLONASS P1. */
    DF106 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            // Word P1 indicates a time interval (in sec) between two adjacent values of tb parameter
            switch (DataType.BIT_2.decode(message).intValue()) {
                case 0  : return 0;
                case 1  : return 1800;
                case 2  : return 2700;
                default : return 3600;
            }
        }
    },

    /** GLONASS t<sub>k</sub> (s). */
    DF107 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            final int    hours    = DataType.U_INT_5.decode(message).intValue();
            final int    minutes  = DataType.U_INT_6.decode(message).intValue();
            final double secondes = DataType.BIT_1.decode(message).intValue() * 30.0;
            return hours * 3600.0 + minutes * 60.0 + secondes;
        }
    },

    /** GLONASS MSB of B<sub>n</sub> word. */
    DF108 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
        }
    },

    /** GLONASS P2 Flag. */
    DF109 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
        }
    },

    /** GLONASS t<sub>b</sub>. */
    DF110 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Unit.MINUTE.toSI(DataType.U_INT_7.decode(message).intValue() * 15.0);
        }
    },

    /** GLONASS x<sub>n</sub> (t<sub>b</sub>), first derivative. */
    DF111 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.KM_PER_S.toSI(FastMath.scalb(DataType.INT_S_24.decode(message).intValue(), -20));
        }
    },

    /** GLONASS x<sub>n</sub> (t<sub>b</sub>). */
    DF112 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Unit.KILOMETRE.toSI(FastMath.scalb(DataType.INT_S_27.decode(message).intValue(), -11));
        }
    },

    /** GLONASS x<sub>n</sub> (t<sub>b</sub>), second derivative. */
    DF113 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.KM_PER_S2.toSI(FastMath.scalb(DataType.INT_S_5.decode(message).intValue(), -30));
        }
    },

    /** GLONASS y<sub>n</sub> (t<sub>b</sub>), first derivative. */
    DF114 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.KM_PER_S.toSI(FastMath.scalb(DataType.INT_S_24.decode(message).intValue(), -20));
        }
    },

    /** GLONASS y<sub>n</sub> (t<sub>b</sub>). */
    DF115 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Unit.KILOMETRE.toSI(FastMath.scalb(DataType.INT_S_27.decode(message).intValue(), -11));
        }
    },

    /** GLONASS y<sub>n</sub> (t<sub>b</sub>), second derivative. */
    DF116 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.KM_PER_S2.toSI(FastMath.scalb(DataType.INT_S_5.decode(message).intValue(), -30));
        }
    },

    /** GLONASS z<sub>n</sub> (t<sub>b</sub>), first derivative. */
    DF117 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.KM_PER_S.toSI(FastMath.scalb(DataType.INT_S_24.decode(message).intValue(), -20));
        }
    },

    /** GLONASS z<sub>n</sub> (t<sub>b</sub>). */
    DF118 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Unit.KILOMETRE.toSI(FastMath.scalb(DataType.INT_S_27.decode(message).intValue(), -11));
        }
    },

    /** GLONASS z<sub>n</sub> (t<sub>b</sub>), second derivative. */
    DF119 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.KM_PER_S2.toSI(FastMath.scalb(DataType.INT_S_5.decode(message).intValue(), -30));
        }
    },

    /** GLONASS P3 Flag. */
    DF120 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            // Flag indicating a number of satellites for which almanac is transmitted within given frame
            if (DataType.BIT_1.decode(message) == 0) {
                return 4;
            } else {
                return 5;
            }
        }
    },

    /** GLONASS γ<sub>n</sub> (t<sub>b</sub>). */
    DF121 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_S_11.decode(message).intValue(), -40);
        }
    },

    /** GLONASS-M P. */
    DF122 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_2.decode(message).intValue();
        }
    },

    /** GLONASS-M l<sub>n</sub> (third string). */
    DF123 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
        }
    },

    /** GLONASS τ<sub>n</sub> (t<sub>b</sub>). */
    DF124 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_S_22.decode(message).intValue(), -30);
        }
    },

    /** GLONASS Δτ<sub>n</sub>. */
    DF125 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_S_5.decode(message).intValue(), -30);
        }
    },

    /** GLONASS E<sub>n</sub>. */
    DF126 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_5.decode(message).intValue();
        }
    },

    /** GLONASS-M P4. */
    DF127 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
        }
    },

    /** GLONASS-M F<sub>T</sub>. */
    DF128 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_4.decode(message).intValue();
        }
    },

    /** GLONASS-M N<sub>T</sub>. */
    DF129 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_11.decode(message).intValue();
        }
    },

    /** GLONASS-M M. */
    DF130 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_2.decode(message).intValue();
        }
    },

    /** GLONASS The Availability of Additional Data. */
    DF131 {
        /** {@inheritDoc} */
        @Override
        public boolean booleanValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message) > 0;
        }
    },

    /** GLONASS N<sup>A</sup>. */
    DF132 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_11.decode(message).intValue();
        }
    },

    /** GLONASS τ<sub>c</sub>. */
    DF133 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_S_32.decode(message).intValue(), -31);
        }
    },

    /** GLONASS-M N<sub>4</sub>. */
    DF134 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_5.decode(message).intValue();
        }
    },

    /** GLONASS τ<sub>GPS</sub>. */
    DF135 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_S_22.decode(message).intValue(), -31);
        }
    },

    /** GLONASS-M l<sub>n</sub> (fifth string). */
    DF136 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
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

    /** Galileo Satellite ID. */
    DF252 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_6.decode(message).intValue();
        }
    },

    /** Galileo Week Number (WN). */
    DF289 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_12.decode(message).intValue();
        }
    },

    /** Galileo IODnav. */
    DF290 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_10.decode(message).intValue();
        }
    },

    /** Galileo SV SISA. */
    DF291 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_8.decode(message).intValue();
        }
    },

    /** Galileo IDOT. */
    DF292 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_14.decode(message).intValue(), -43));
        }
    },

    /** Galileo t<sub>oc</sub>. */
    DF293 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.U_INT_14.decode(message).intValue() * 60.0;
        }
    },

    /** Galileo a<sub>f2</sub>. */
    DF294 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_6.decode(message).intValue(), -59);
        }
    },

    /** Galileo a<sub>f1</sub>. */
    DF295 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_21.decode(message).intValue(), -46);
        }
    },

    /** Galileo a<sub>f0</sub>. */
    DF296 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_31.decode(message).intValue(), -34);
        }
    },

    /** Galileo C<sub>rs</sub>. */
    DF297 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -5);
        }
    },

    /** Galileo Δn. */
    DF298 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_16.decode(message).intValue(), -43));
        }
    },

    /** Galileo M<sub>0</sub>. */
    DF299 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** Galileo C<sub>uc</sub>. */
    DF300 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -29);
        }
    },

    /** Galileo e. */
    DF301 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.U_INT_32.decode(message).doubleValue(), -33);
        }
    },

    /** Galileo C<sub>us</sub>. */
    DF302 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -29);
        }
    },

    /** Galileo A<sup>1/2</sup>. */
    DF303 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.U_INT_32.decode(message).doubleValue(), -19);
        }
    },

    /** Galileo t<sub>oe</sub>. */
    DF304 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.U_INT_14.decode(message).intValue() * 60.0;
        }
    },

    /** Galileo C<sub>ic</sub>. */
    DF305 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -29);
        }
    },

    /** Galileo Ω<sub>0</sub>. */
    DF306 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** Galileo C<sub>is</sub>. */
    DF307 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -29);
        }
    },

    /** Galileo i<sub>0</sub>. */
    DF308 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** Galileo C<sub>rc</sub>. */
    DF309 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -5);
        }
    },

    /** Galileo ω. */
    DF310 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** Galileo OMEGADOT (Rate of Right Ascension). */
    DF311 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_24.decode(message).intValue(), -43));
        }
    },

    /** Galileo BGD (E1/E5a). */
    DF312 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_10.decode(message).intValue(), -32);
        }
    },

    /** Galileo BGD (E5b/E1). */
    DF313 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_10.decode(message).intValue(), -32);
        }
    },

    /** E5a SIGNAL Health Status. */
    DF314 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_2.decode(message).intValue();
        }
    },

    /** E5a Data Validity Status. */
    DF315 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
        }
    },

    /** Galileo SOL NAV Signal Health Status (SOLHS). */
    DF316 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_2.decode(message).intValue();
        }
    },

    /** Galileo SOL NAV Data Validity Status (SOLDVS). */
    DF317 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
        }
    },

    /** QZSS Satellite ID. */
    DF429 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            // 1 refers to satellite 193, 2 refers to satellite 194, etc.
            return DataType.U_INT_4.decode(message).intValue() + 192;
        }
    },

    /** QZSS t<sub>oc</sub>. */
    DF430 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.U_INT_16.decode(message).intValue() * 16.0;
        }
    },

    /** QZSS a<sub>f2</sub>. */
    DF431 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_8.decode(message).intValue(), -55);
        }
    },

    /** QZSS a<sub>f1</sub>. */
    DF432 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -43);
        }
    },

    /** QZSS a<sub>f0</sub>. */
    DF433 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_22.decode(message).intValue(), -31);
        }
    },

    /** QZSS IODE. */
    DF434 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_8.decode(message).intValue();
        }
    },

    /** QZSS C<sub>rs</sub>. */
    DF435 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -5);
        }
    },

    /** QZSS Δn. */
    DF436 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_16.decode(message).intValue(), -43));
        }
    },

    /** QZSS M<sub>0</sub>. */
    DF437 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** QZSS C<sub>uc</sub>. */
    DF438 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -29);
        }
    },

    /** QZSS e. */
    DF439 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.U_INT_32.decode(message).doubleValue(), -33);
        }
    },

    /** QZSS C<sub>us</sub>. */
    DF440 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -29);
        }
    },

    /** QZSS A<sup>1/2</sup>. */
    DF441 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.U_INT_32.decode(message).doubleValue(), -19);
        }
    },

    /** QZSS t<sub>oe</sub>. */
    DF442 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.U_INT_16.decode(message).intValue() * 16.0;
        }
    },

    /** QZSS C<sub>ic</sub>. */
    DF443 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -29);
        }
    },

    /** QZSS Ω<sub>0</sub>. */
    DF444 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** QZSS C<sub>is</sub>. */
    DF445 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -29);
        }
    },

    /** QZSS i<sub>0</sub>. */
    DF446 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** QZSS C<sub>rc</sub>. */
    DF447 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_16.decode(message).intValue(), -5);
        }
    },

    /** QZSS ω. */
    DF448 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** QZSS OMEGADOT (Rate of Right Ascension). */
    DF449 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_24.decode(message).intValue(), -43));
        }
    },

    /** QZSS iDOT. */
    DF450 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_14.decode(message).intValue(), -43));
        }
    },

    /** QZSS Codes on L2 Channel. */
    DF451 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_2.decode(message).intValue();
        }
    },

    /** QZSS Week Number. */
    DF452 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_10.decode(message).intValue();
        }
    },

    /** QZSS URA Number. */
    DF453 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_4.decode(message).intValue();
        }
    },

    /** QZSS SV health. */
    DF454 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_6.decode(message).intValue();
        }
    },

    /** QZSS t<sub>GD</sub>. */
    DF455 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_8.decode(message).intValue(), -31);
        }
    },

    /** QZSS IODC. */
    DF456 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_10.decode(message).intValue();
        }
    },

    /** QZSS Fit Interval. */
    DF457 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
        }
    },

    /** BDS Satellite ID. */
    DF488 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_6.decode(message).intValue();
        }
    },

    /** BDS Week Number. */
    DF489 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_13.decode(message).intValue();
        }
    },

    /** BDS SV URA Index. */
    DF490 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_4.decode(message).intValue();
        }
    },

    /** BDS IDOT. */
    DF491 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_14.decode(message).intValue(), -43));
        }
    },

    /** BDS AODE. */
    DF492 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_5.decode(message).intValue();
        }
    },

    /** BDS t<sub>oc</sub>. */
    DF493 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.U_INT_17.decode(message).intValue() * 8.0;
        }
    },

    /** BDS a<sub>f2</sub>. */
    DF494 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_11.decode(message).intValue(), -66);
        }
    },

    /** BDS a<sub>f1</sub>. */
    DF495 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_22.decode(message).intValue(), -50);
        }
    },

    /** BDS a<sub>f0</sub>. */
    DF496 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_24.decode(message).intValue(), -33);
        }
    },

    /** BDS AODC. */
    DF497 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.U_INT_5.decode(message).intValue();
        }
    },

    /** BDS C<sub>rs</sub>. */
    DF498 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_18.decode(message).intValue(), -6);
        }
    },

    /** BDS Δn. */
    DF499 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_16.decode(message).intValue(), -43));
        }
    },

    /** BDS M<sub>0</sub>. */
    DF500 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** BDS C<sub>uc</sub>. */
    DF501 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_18.decode(message).intValue(), -31);
        }
    },

    /** BDS e. */
    DF502 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.U_INT_32.decode(message).doubleValue(), -33);
        }
    },

    /** BDS C<sub>us</sub>. */
    DF503 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_18.decode(message).intValue(), -31);
        }
    },

    /** BDS a<sup>1/2</sup>. */
    DF504 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.U_INT_32.decode(message).doubleValue(), -19);
        }
    },

    /** BDS t<sub>oe</sub>. */
    DF505 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return DataType.U_INT_17.decode(message).intValue() * 8.0;
        }
    },

    /** BDS C<sub>ic</sub>. */
    DF506 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_18.decode(message).intValue(), -31);
        }
    },

    /** BDS Ω<sub>0</sub>. */
    DF507 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** BDS C<sub>is</sub>. */
    DF508 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_18.decode(message).intValue(), -31);
        }
    },

    /** BDS i<sub>0</sub>. */
    DF509 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** BDS C<sub>rc</sub>. */
    DF510 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return FastMath.scalb(DataType.INT_18.decode(message).intValue(), -6);
        }
    },

    /** BDS ω. */
    DF511 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_32.decode(message).intValue(), -31));
        }
    },

    /** BDS OMEGADOT (Rate of Right Ascension). */
    DF512 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.SEMI_CIRCLE.toSI(FastMath.scalb(DataType.INT_24.decode(message).intValue(), -43));
        }
    },

    /** BDS t<sub>GD1</sub>. */
    DF513 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.NS.toSI(DataType.INT_10.decode(message).intValue() * 0.1);
        }
    },

    /** BDS t<sub>GD2</sub>. */
    DF514 {
        /** {@inheritDoc} */
        @Override
        public double doubleValue(final EncodedMessage message) {
            return Units.NS.toSI(DataType.INT_10.decode(message).intValue() * 0.1);
        }
    },

    /** BDS SV Health. */
    DF515 {
        /** {@inheritDoc} */
        @Override
        public int intValue(final EncodedMessage message) {
            return DataType.BIT_1.decode(message).intValue();
        }
    };

}
