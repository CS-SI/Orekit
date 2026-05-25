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
import org.orekit.utils.PVCoordinates;

/**
 * Class for converting between Keplerian elements and Cartesian coordinates.
 * It works for all types of orbits (elliptical or not).
 * @author Romain Serra
 * @see KeplerianParameters
 * @since 14.0
 */
public class KeplerianParametersConverter {

    /** Central body gravitational parameter. */
    private final double mu;

    /**
     * Constructor.
     * @param mu central body gravitational parameter
     */
    public KeplerianParametersConverter(final double mu) {
        this.mu = mu;
    }

    /**
     * Convert Cartesian coordinates to Keplerian elements.
     * @param cartesian position and velocity in inertial frame
     * @param positionAngleType type of position angle to use
     * @return Keplerian elements
     */
    public KeplerianParameters toParameters(final PVCoordinates cartesian, final PositionAngleType positionAngleType) {
        // compute inclination
        final Vector3D momentum = cartesian.getMomentum();
        final double m2 = momentum.getNorm2Sq();
        final double i = Vector3D.angle(momentum, Vector3D.PLUS_K);

        // compute right ascension of ascending node
        final double raan = Vector3D.crossProduct(Vector3D.PLUS_K, momentum).getAlpha();

        // preliminary computations for parameters depending on orbit shape (elliptic or hyperbolic)
        final Vector3D pvP     = cartesian.getPosition();
        final Vector3D pvV     = cartesian.getVelocity();
        final double   r2      = pvP.getNorm2Sq();
        final double   r       = FastMath.sqrt(r2);
        final double   V2      = pvV.getNorm2Sq();
        final double   rV2OnMu = r * V2 / mu;

        // compute semi-major axis (will be negative for hyperbolic orbits)
        final double a = r / (2 - rV2OnMu);
        final double muA = mu * a;

        // compute cached anomaly
        final double e;
        final double anomaly;
        final PositionAngleType intermediateType = PositionAngleType.ECCENTRIC;
        if (a > 0.) {
            // elliptic or circular orbit
            final double eSE = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(muA);
            final double eCE = rV2OnMu - 1;
            e = FastMath.sqrt(eSE * eSE + eCE * eCE);
            anomaly = FastMath.atan2(eSE, eCE);
        } else {
            // hyperbolic orbit
            final double eSH = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(-muA);
            final double eCH = rV2OnMu - 1;
            e = FastMath.sqrt(1 - m2 / muA);
            anomaly = FastMath.log((eCH + eSH) / (eCH - eSH)) / 2;
        }

        // compute perigee argument
        final Vector3D node = new Vector3D(raan, 0.0);
        final double px = Vector3D.dotProduct(pvP, node);
        final double py = Vector3D.dotProduct(pvP, Vector3D.crossProduct(momentum, node)) / FastMath.sqrt(m2);
        final double trueAnomaly = KeplerianAnomalyUtility.convertAnomaly(intermediateType, anomaly, e, PositionAngleType.TRUE);
        final double pa = FastMath.atan2(py, px) - trueAnomaly;

        return switch (positionAngleType) {
            case PositionAngleType.MEAN -> {
                final double meanAnomaly = KeplerianAnomalyUtility.convertAnomaly(intermediateType, anomaly, e, positionAngleType);
                yield new KeplerianParameters(a, e, i, pa, raan, meanAnomaly, positionAngleType);
            }
            case PositionAngleType.TRUE -> new KeplerianParameters(a, e, i, pa, raan, trueAnomaly, positionAngleType);
            case PositionAngleType.ECCENTRIC -> new KeplerianParameters(a, e, i, pa, raan, anomaly, positionAngleType);
        };
    }

    /**
     * Convert Keplerian elements to Cartesian coordinates.
     * @param elements Keplerian elements
     * @return position and velocity in inertial frame
     */
    public PVCoordinates toCartesian(final KeplerianParameters elements) {
        final Vector3D position;
        final Vector3D velocity;
        final Vector3D[] axes = referenceAxes(elements.i(), elements.pa(), elements.raan());

        final double e = elements.e();
        final double a = elements.a();
        if (elements.a() > 0.) {
            // elliptical case

            // elliptic eccentric anomaly
            final double uME2   = (1 - e) * (1 + e);
            final double s1Me2  = FastMath.sqrt(uME2);
            final KeplerianParameters elementsWithEccentricAnomaly = elements.withPositionAngleType(PositionAngleType.ECCENTRIC);
            final double eccentricAnomaly = elementsWithEccentricAnomaly.anomaly();
            final SinCos scE    = FastMath.sinCos(eccentricAnomaly);
            final double cosE   = scE.cos();
            final double sinE   = scE.sin();

            // coordinates of position and velocity in the orbital plane
            final double x      = a * (cosE - e);
            final double y      = a * sinE * s1Me2;
            final double factor = FastMath.sqrt(mu / a) / (1 - e * cosE);
            final double xDot   = -sinE * factor;
            final double yDot   =  cosE * s1Me2 * factor;

            position = new Vector3D(x, axes[0], y, axes[1]);
            velocity = new Vector3D(xDot, axes[0], yDot, axes[1]);

        } else {
            // hyperbolic case

            // compute position and velocity factors
            final KeplerianParameters elementsWithTrueAnomaly = elements.withPositionAngleType(PositionAngleType.TRUE);
            final double trueAnomaly = elementsWithTrueAnomaly.anomaly();
            final SinCos scV       = FastMath.sinCos(trueAnomaly);
            final double sinV      = scV.sin();
            final double cosV      = scV.cos();
            final double f         = a * (1 - e * e);
            final double posFactor = f / (1 + e * cosV);
            final double velFactor = FastMath.sqrt(mu / f);

            final double   x            =  posFactor * cosV;
            final double   y            =  posFactor * sinV;
            final double   xDot         = -velFactor * sinV;
            final double   yDot         =  velFactor * (e + cosV);

            position = new Vector3D(x, axes[0], y, axes[1]);
            velocity = new Vector3D(xDot, axes[0], yDot, axes[1]);

        }
        return new PVCoordinates(position, velocity);
    }

    /** Compute reference axes.
     * @param i inclination
     * @param pa perigee argument
     * @param raan right ascension of ascending node
     * @return reference axes
     */
    static Vector3D[] referenceAxes(final double i, final double pa, final double raan) {
        // preliminary variables
        final SinCos scRaan  = FastMath.sinCos(raan);
        final SinCos scPa    = FastMath.sinCos(pa);
        final SinCos scI     = FastMath.sinCos(i);
        final double cosRaan = scRaan.cos();
        final double sinRaan = scRaan.sin();
        final double cosPa   = scPa.cos();
        final double sinPa   = scPa.sin();
        final double cosI    = scI.cos();
        final double sinI    = scI.sin();

        final double crcp    = cosRaan * cosPa;
        final double crsp    = cosRaan * sinPa;
        final double srcp    = sinRaan * cosPa;
        final double srsp    = sinRaan * sinPa;

        // reference axes defining the orbital plane
        return new Vector3D[] { new Vector3D( crcp - cosI * srsp,  srcp + cosI * crsp, sinI * sinPa),
                                new Vector3D(-crsp - cosI * srcp, -srsp + cosI * crcp, sinI * cosPa)
        };

    }
}
