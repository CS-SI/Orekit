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
package org.orekit.files.ccsds.utils;

import org.hipparchus.util.FastMath;

/** CCSDS/SANA units (km, km/s, degrees...).
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum CcsdsUnit {

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

    /** Degrees. */
    DEG() {
        /** {@inheritDoc} */
        @Override
        public double toSI(final double value) {
            return FastMath.toRadians(value);
        }
    },

    /** Dimensionless values. */
    DIMENSIONLESS() {
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

}
