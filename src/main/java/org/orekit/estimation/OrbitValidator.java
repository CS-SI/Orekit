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
import org.hipparchus.optim.nonlinear.vector.leastsquares.ParameterValidator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitInternalError;
import org.orekit.orbits.OrbitType;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;


/** Validator for orbital parameters.
 * <p>
 * This class prevents the orbit determination engine to use
 * inconsistent orbital parameters by trimming them back
 * to fit domain boundary. As an example, if an optimizer
 * attempts to evaluate a point containing an orbit with
 * negative inclination, the inclination will be reset to
 * 0.0 by the validator before it is really used in the
 * space flight dynamics code.
 * </p>
 * <p>
 * The parameters are expected to be normalized according
 * to the {@link ParameterDriver parameters drivers} set
 * at construction.
 * </p>
 * @author Luc Maisonobe
 * @since 8.0
 */
public class OrbitValidator implements ParameterValidator {

    /** Orbit type. */
    private final OrbitType type;

    /** Drivers for the orbital parameters. */
    private final ParameterDriversList drivers;

    /** Simple constructor.
     * @param type orbit type
     * @param drivers drivers for the orbital parameters
     */
    public OrbitValidator(final OrbitType type, final ParameterDriversList drivers) {
        this.type    = type;
        this.drivers = drivers;
    }

    /** {@inheritDoc} */
    @Override
    public RealVector validate(final RealVector params) {
        switch (type) {
            case CARTESIAN:
                // nothing to validate for Cartesian parameters
                return params;
            case CIRCULAR:
                return normalize(circularValidate(unnormalize(params)));
            case KEPLERIAN:
                return normalize(keplerValidate(unnormalize(params)));
            case EQUINOCTIAL:
                return normalize(equinoctialValidate(unnormalize(params)));
            default :
                // this should never happen
                throw new OrekitInternalError(null);
        }
    }

    /** Unnormalize parameters.
     * @param params normalized parameters
     * @return unnormalized parameters
     */
    private RealVector unnormalize(final RealVector params) {
        for (int i = 0; i < drivers.getNbParams(); ++i) {
            final ParameterDriver driver = drivers.getDrivers().get(i);
            final double normalized      = params.getEntry(i);
            final double unnormalized    = driver.getInitialValue() + driver.getScale() * normalized;
            params.setEntry(i, unnormalized);
        }
        return params;
    }

    /** Normalize parameters.
     * @param params unnormalized parameters
     * @return normalized parameters
     */
    private RealVector normalize(final RealVector params) {
        for (int i = 0; i < drivers.getNbParams(); ++i) {
            final ParameterDriver driver = drivers.getDrivers().get(i);
            final double unnormalized    = params.getEntry(i);
            final double normalized      = (unnormalized - driver.getInitialValue()) / driver.getScale();
            params.setEntry(i, normalized);
        }
        return params;
    }

    /** Validate circular orbit parameters.
     * @param params unnormalized circular orbit parameters
     * @return validated parameters
     */
    private RealVector circularValidate(final RealVector params) {

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

    /** Validate Keplerian orbit parameters.
     * @param params unnormalized Keplerian orbit parameters
     * @return validated parameters
     */
    private RealVector keplerValidate(final RealVector params) {

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

    /** Validate equinoctial orbit parameters.
     * @param params unnormalized equinoctial orbit parameters
     * @return validated parameters
     */
    private RealVector equinoctialValidate(final RealVector params) {

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

}
