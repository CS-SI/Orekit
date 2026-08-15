/* Copyright 2022-2026 Romain Serra
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
package org.orekit.orbits;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.PVCoordinates;

/**
 * Class for converting between equinoctial elements and Cartesian coordinates.
 * @author Romain Serra
 * @see EquinoctialParameters
 * @since 14.0
 */
public class EquinoctialParametersConverter {

    /** Central body gravitational parameter. */
    private final double mu;

    /**
     * Constructor.
     * @param mu central body gravitational parameter
     */
    public EquinoctialParametersConverter(final double mu) {
        this.mu = mu;
    }

    /**
     * Convert Cartesian coordinates to equinoctial elements.
     * @param cartesian position and velocity in inertial frame
     * @param positionAngleType type of position angle to use
     * @return equinoctial elements
     */
    public EquinoctialParameters toParameters(final PVCoordinates cartesian, final PositionAngleType positionAngleType) {
        //  compute semi-major axis
        final Vector3D pvP   = cartesian.getPosition();
        final Vector3D pvV   = cartesian.getVelocity();
        final double r2      = pvP.getNorm2Sq();
        final double r       = FastMath.sqrt(r2);
        final double V2      = pvV.getNorm2Sq();
        final double rV2OnMu = r * V2 / mu;

        // compute semi-major axis
        final double a = r / (2 - rV2OnMu);

        if (a <= 0.) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                    getClass().getName());
        }

        // compute inclination vector
        final Vector3D w = cartesian.getMomentum().normalize();
        final double d = 1.0 / (1 + w.getZ());
        final double hx = -d * w.getY();
        final double hy =  d * w.getX();

        // compute true longitude argument
        final double cLv = (pvP.getX() - d * pvP.getZ() * w.getX()) / r;
        final double sLv = (pvP.getY() - d * pvP.getZ() * w.getY()) / r;
        final double lV = FastMath.atan2(sLv, cLv);

        // compute eccentricity vector
        final double eSE = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(mu * a);
        final double eCE = rV2OnMu - 1;
        final double e2  = eCE * eCE + eSE * eSE;
        final double f   = eCE - e2;
        final double g   = FastMath.sqrt(1 - e2) * eSE;
        final double ex = a * (f * cLv + g * sLv) / r;
        final double ey = a * (f * sLv - g * cLv) / r;

        final EquinoctialParameters elements = new EquinoctialParameters(a, ex, ey, hx, hy, lV, PositionAngleType.TRUE);
        return positionAngleType == PositionAngleType.TRUE ? elements : elements.withPositionAngleType(positionAngleType);
    }

    /**
     * Convert equinoctial elements to Cartesian coordinates.
     * @param elements equinoctial elements
     * @return position and velocity in inertial frame
     */
    public PVCoordinates toCartesian(final EquinoctialParameters elements) {
        // get equinoctial parameters
        final double a = elements.a();
        final double ex = elements.ex();
        final double ey = elements.ey();
        final double hx = elements.hx();
        final double hy = elements.hy();
        final double lE = elements.positionAngleType() == PositionAngleType.ECCENTRIC ? elements.longitudeArgument() :
                elements.withPositionAngleType(PositionAngleType.ECCENTRIC).longitudeArgument();

        // inclination-related intermediate parameters
        final double hx2   = hx * hx;
        final double hy2   = hy * hy;
        final double factH = 1. / (1 + hx2 + hy2);

        // reference axes defining the orbital plane
        final double ux = (1 + hx2 - hy2) * factH;
        final double uy =  2 * hx * hy * factH;
        final double uz = -2 * hy * factH;

        final double vx = uy;
        final double vy = (1 - hx2 + hy2) * factH;
        final double vz =  2 * hx * factH;

        // eccentricity-related intermediate parameters
        final double exey = ex * ey;
        final double ex2  = ex * ex;
        final double ey2  = ey * ey;
        final double e2   = ex2 + ey2;
        final double eta  = 1 + FastMath.sqrt(1 - e2);
        final double beta = 1. / eta;

        // eccentric longitude argument
        final SinCos scLe   = FastMath.sinCos(lE);
        final double cLe    = scLe.cos();
        final double sLe    = scLe.sin();
        final double exCeyS = ex * cLe + ey * sLe;

        // coordinates of position and velocity in the orbital plane
        final double x      = a * ((1 - beta * ey2) * cLe + beta * exey * sLe - ex);
        final double y      = a * ((1 - beta * ex2) * sLe + beta * exey * cLe - ey);

        final double factor = FastMath.sqrt(mu / a) / (1 - exCeyS);
        final double xdot   = factor * (-sLe + beta * ey * exCeyS);
        final double ydot   = factor * ( cLe - beta * ex * exCeyS);

        final Vector3D position =
                new Vector3D(x * ux + y * vx, x * uy + y * vy, x * uz + y * vz);
        final Vector3D velocity =
                new Vector3D(xdot * ux + ydot * vx, xdot * uy + ydot * vy, xdot * uz + ydot * vz);
        return new PVCoordinates(position, velocity);
    }
}
