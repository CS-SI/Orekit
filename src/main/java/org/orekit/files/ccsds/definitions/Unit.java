/* Copyright 2002-2021 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.files.ccsds.definitions;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** CCSDS/SANA units (km, km/s, degrees...).
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum Unit {

    /** Kilometers. */
    KM() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value * 1000.0;
        }
    },

    /** Kilometers per seconds. */
    KM_S() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value * 1000.0;
        }
    },

    /** Kilometers per squared seconds. */
    KM_S2() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value * 1000.0;
        }
    },

    /** Square kilometers. */
    KM2() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value * 1000000.0;
        }
    },

    /** Square kilometers per second. */
    KM2_S() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value * 1000000.0;
        }
    },

    /** Kilometers square roots. */
    SQKM() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value * FastMath.sqrt(1000.0);
        }
    },

    /** Kilometers per seconds square roots. */
    KM_SQS() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value * 1000.0;
        }
    },

    /** Meters. */
    M() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value;
        }
    },

    /** Meters per seconds. */
    M_S() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value;
        }
    },

    /** Meters per squared seconds. */
    M_S2() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value;
        }
    },

    /** Square meters. */
    M2() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value;
        }
    },

    /** Square meters per second. */
    M2_S() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value;
        }
    },

    /** Meters square roots. */
    SQM() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value;
        }
    },

    /** Meters per seconds square roots. */
    M_SQS() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value;
        }
    },

    /** Degrees. */
    DEG() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return FastMath.toRadians(value);
        }
    },

    /** Radians. */
    RAD() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value;
        }
    },

    /** Dimensionless values. */
    ND() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return value;
        }
    };

    /** Convert a value from CCSDS/SANA units to SI units.
     * @param value value in CCSDS/SANA units
     * @return value in SI units
     */
    public abstract double toSI(double value);

    /** Parse a unit.
     * @param unit unit to parse
     * @return parsed unit
     */
    public static Unit parse(final String unit) {
        try {
            return Unit.valueOf(unit);
        } catch (IllegalArgumentException iae) {
            throw new OrekitException(OrekitMessages.UNKNOWN_UNIT, unit);
        }
    }

}
