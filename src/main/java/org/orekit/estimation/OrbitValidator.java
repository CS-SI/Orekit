/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.estimation;

import org.hipparchus.linear.RealVector;
import org.hipparchus.optim.leastsquares.ParameterValidator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitInternalError;
import org.orekit.orbits.OrbitType;


/** Enumerate for validating orbital parameters.
 * <p>
 * This class prevents the orbit determination engine to use
 * inconsistent orbital parameters by trimming them back
 * to fit domain boundary. As an example, if an optimizer
 * attempts to evaluate a point containing an orbit with
 * negative inclination, the inclination will be reset to
 * 0.0 by the validator before it is really used in the
 * space flight dynamics code.
 * </p>
 * @author Luc Maisonobe
 * @since 8.0
 */
public enum OrbitValidator implements ParameterValidator {

    /** Validator for {@link OrbitType#CARTESIAN Cartesian} orbits. */
    CARTESIAN() {

        /** {@inheritDoc} */
        @Override
        public RealVector validate(final RealVector params) {
            // nothing to validate for Cartesian parameters
            return params;
        }

    },

    /** Validator for {@link OrbitType#CIRCULAR circular} orbits. */
    CIRCULAR() {

        /** {@inheritDoc} */
        @Override
        public RealVector validate(final RealVector params) {

            // ensure semi-major axis is positive
            if (params.getEntry(0) <= 0) {
                params.setEntry(0, Precision.SAFE_MIN);
            }

            // ensure eccentricity is less than 1
            final double e = FastMath.hypot(params.getEntry(1), params.getEntry(2));
            if (e >= 1.0) {
                params.setEntry(1, params.getEntry(1) / FastMath.nextUp(e));
                params.setEntry(2, params.getEntry(2) / FastMath.nextUp(e));
            }

            // ensure inclination is non-negative
            if (params.getEntry(3) < 0.0) {
                params.setEntry(3, 0.0);
            }

            return params;

        }

    },

    /** Validator for {@link OrbitType#KEPLERIAN Keplerian} orbits. */
    KEPLERIAN() {

        /** {@inheritDoc} */
        @Override
        public RealVector validate(final RealVector params) {

            // ensure inclination is non-negative
            if (params.getEntry(2) < 0.0) {
                params.setEntry(2, 0.0);
            }

            final double a = params.getEntry(0);
            final double e = params.getEntry(1);
            if (a > 0 && e > 0 && e < 1) {
                // regular elliptic orbit, nothing to do
                return params;
            } else if (a < 0 && e > 1) {
                // regular hyperbolic orbit, nothing to do
                return params;
            } else {
                // inconsistent parameters, force orbit to elliptic
                params.setEntry(0, FastMath.max(a, Precision.SAFE_MIN));
                params.setEntry(1, FastMath.max(0.0, FastMath.min(e, FastMath.nextDown(1.0))));
                return params;
            }
        }

    },

    /** Validator for {@link OrbitType#EQUINOCTIAL equinoctial} orbits. */
    EQUINOCTIAL() {

        /** {@inheritDoc} */
        @Override
        public RealVector validate(final RealVector params) {

            // ensure semi-major axis is positive
            if (params.getEntry(0) <= 0) {
                params.setEntry(0, Precision.SAFE_MIN);
            }

            // ensure eccentricity is less than 1
            final double e = FastMath.hypot(params.getEntry(1), params.getEntry(2));
            if (e >= 1.0) {
                params.setEntry(1, params.getEntry(1) / FastMath.nextUp(e));
                params.setEntry(2, params.getEntry(2) / FastMath.nextUp(e));
            }

            return params;

        }

    };

    /** Get the validator corresponding to an orbit type.
     * @param type orbite type
     * @return validator for the orbit type
     */
    public static OrbitValidator getValidator(final OrbitType type) {
        switch (type) {
            case CARTESIAN:
                return CARTESIAN;
            case CIRCULAR:
                return CIRCULAR;
            case KEPLERIAN:
                return KEPLERIAN;
            case EQUINOCTIAL:
                return EQUINOCTIAL;
            default :
                // this should never happen
                throw new OrekitInternalError(null);
        }
    }

}
