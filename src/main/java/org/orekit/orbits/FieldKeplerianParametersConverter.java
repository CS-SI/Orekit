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

import java.lang.reflect.Array;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.orekit.utils.FieldPVCoordinates;

/**
 * Class for converting between Keplerian elements and Cartesian coordinates (Field version).
 * It works for all types of orbits (elliptical or not).
 * @author Romain Serra
 * @see FieldKeplerianParameters
 * @see KeplerianParametersConverter
 * @since 14.0
 */
public class FieldKeplerianParametersConverter<T extends CalculusFieldElement<T>> {

    /** Central body gravitational parameter. */
    private final T mu;

    /**
     * Constructor.
     * @param mu central body gravitational parameter
     */
    public FieldKeplerianParametersConverter(final T mu) {
        this.mu = mu;
    }

    /**
     * Convert Cartesian coordinates to Keplerian elements.
     * @param cartesian position and velocity in inertial frame
     * @param positionAngleType type of position angle to use
     * @return Keplerian elements
     */
    public FieldKeplerianParameters<T> toParameters(final FieldPVCoordinates<T> cartesian,
                                                    final PositionAngleType positionAngleType) {
        // third canonical vector
        final Field<T> field = cartesian.getPosition().getX().getField();
        final FieldVector3D<T> plusK = FieldVector3D.getPlusK(field);

        // compute inclination
        final FieldVector3D<T> momentum = cartesian.getMomentum();
        final T m2 = momentum.getNorm2Sq();

        final T i = FieldVector3D.angle(momentum, plusK);
        // compute right ascension of ascending node
        final T raan = FieldVector3D.crossProduct(plusK, momentum).getAlpha();
        // preliminary computations for parameters depending on orbit shape (elliptic or hyperbolic)
        final FieldVector3D<T> pvP     = cartesian.getPosition();
        final FieldVector3D<T> pvV     = cartesian.getVelocity();

        final T   r2      = pvP.getNorm2Sq();
        final T   r       = r2.sqrt();
        final T   v2      = pvV.getNorm2Sq();
        final T   rV2OnMu = r.multiply(v2).divide(mu);

        // compute semi-major axis (will be negative for hyperbolic orbits)
        final T a = r.divide(rV2OnMu.negate().add(2.0));
        final T muA = a.multiply(mu);

        // compute true anomaly
        final T e;
        final T eccentricAnomaly;
        final PositionAngleType intermediateType = PositionAngleType.ECCENTRIC;
        if (a.getReal() > 0.) {
            // elliptic or circular orbit
            final T eSE = FieldVector3D.dotProduct(pvP, pvV).divide(muA.sqrt());
            final T eCE = rV2OnMu.subtract(1);
            e = (eSE.multiply(eSE).add(eCE.multiply(eCE))).sqrt();
            eccentricAnomaly = eSE.atan2(eCE);
        } else {
            // hyperbolic orbit
            final T eSH = FieldVector3D.dotProduct(pvP, pvV).divide(muA.negate().sqrt());
            final T eCH = rV2OnMu.subtract(1);
            e = (m2.negate().divide(muA).add(1)).sqrt();
            eccentricAnomaly = (eCH.add(eSH)).divide(eCH.subtract(eSH)).log().divide(2);
        }

        // compute perigee argument
        final FieldVector3D<T> node = new FieldVector3D<>(raan, field.getZero());
        final T px = FieldVector3D.dotProduct(pvP, node);
        final T py = FieldVector3D.dotProduct(pvP, FieldVector3D.crossProduct(momentum, node)).divide(m2.sqrt());
        final T trueAnomaly = FieldKeplerianAnomalyUtility.convertAnomaly(intermediateType, eccentricAnomaly, e, PositionAngleType.TRUE);
        final T pa = py.atan2(px).subtract(trueAnomaly);

        return switch (positionAngleType) {
            case PositionAngleType.MEAN -> {
                final T meanAnomaly = FieldKeplerianAnomalyUtility.convertAnomaly(intermediateType, eccentricAnomaly, e, positionAngleType);
                yield new FieldKeplerianParameters<>(a, e, i, pa, raan, meanAnomaly, positionAngleType);
            }
            case PositionAngleType.TRUE -> new FieldKeplerianParameters<>(a, e, i, pa, raan, trueAnomaly, positionAngleType);
            case PositionAngleType.ECCENTRIC -> new FieldKeplerianParameters<>(a, e, i, pa, raan, eccentricAnomaly, positionAngleType);
        };
    }

    /**
     * Convert Keplerian elements to Cartesian coordinates.
     * @param elements Keplerian elements
     * @return position and velocity in inertial frame
     */
    public FieldPVCoordinates<T> toCartesian(final FieldKeplerianParameters<T> elements) {
        final FieldVector3D<T> position;
        final FieldVector3D<T> velocity;
        final FieldVector3D<T>[] axes = referenceAxes(elements.i(), elements.pa(), elements.raan());

        final T e = elements.e();
        final T a = elements.a();
        if (elements.a().getReal() > 0.) {
            // elliptic eccentric anomaly
            final T uME2             = e.negate().add(1).multiply(e.add(1));
            final T s1Me2            = uME2.sqrt();
            final FieldKeplerianParameters<T> elementsWithEccentricAnomaly = elements.withPositionAngleType(PositionAngleType.ECCENTRIC);
            final T eccentricAnomaly = elementsWithEccentricAnomaly.anomaly();
            final FieldSinCos<T> scE = FastMath.sinCos(eccentricAnomaly);
            final T cosE             = scE.cos();
            final T sinE             = scE.sin();

            // coordinates of position and velocity in the orbital plane
            final T x      = a.multiply(cosE.subtract(e));
            final T y      = a.multiply(sinE).multiply(s1Me2);
            final T factor = FastMath.sqrt(mu.divide(a)).divide(e.negate().multiply(cosE).add(1));
            final T xDot   = sinE.negate().multiply(factor);
            final T yDot   = cosE.multiply(s1Me2).multiply(factor);

            position = new FieldVector3D<>(x, axes[0], y, axes[1]);
            velocity = new FieldVector3D<>(xDot, axes[0], yDot, axes[1]);

        } else {
            // hyperbolic case

            // compute position and velocity factors
            final FieldKeplerianParameters<T> elementsWithTrueAnomaly = elements.withPositionAngleType(PositionAngleType.TRUE);
            final FieldSinCos<T> scV = FastMath.sinCos(elementsWithTrueAnomaly.anomaly());
            final T sinV             = scV.sin();
            final T cosV             = scV.cos();
            final T f                = a.multiply(e.square().negate().add(1));
            final T posFactor        = f.divide(e.multiply(cosV).add(1));
            final T velFactor        = FastMath.sqrt(mu.divide(f));

            position     = new FieldVector3D<>(posFactor.multiply(cosV), axes[0], posFactor.multiply(sinV), axes[1]);
            velocity     = new FieldVector3D<>(velFactor.multiply(sinV).negate(), axes[0], velFactor.multiply(e.add(cosV)), axes[1]);

        }
        return new FieldPVCoordinates<>(position, velocity);
    }

    /** Compute reference axes.
     * @param <W> type of the field elements
     * @param i inclination
     * @param pa perigee argument
     * @param raan right ascension of ascending node
     * @return reference axes
     */
    static <W extends CalculusFieldElement<W>> FieldVector3D<W>[] referenceAxes(final W i, final W pa, final W raan) {
        // preliminary variables
        final FieldSinCos<W> scRaan = FastMath.sinCos(raan);
        final FieldSinCos<W> scPa   = FastMath.sinCos(pa);
        final FieldSinCos<W> scI    = FastMath.sinCos(i);
        final W cosRaan = scRaan.cos();
        final W sinRaan = scRaan.sin();
        final W cosPa   = scPa.cos();
        final W sinPa   = scPa.sin();
        final W cosI    = scI.cos();
        final W sinI    = scI.sin();
        final W crcp    = cosRaan.multiply(cosPa);
        final W crsp    = cosRaan.multiply(sinPa);
        final W srcp    = sinRaan.multiply(cosPa);
        final W srsp    = sinRaan.multiply(sinPa);

        // reference axes defining the orbital plane
        @SuppressWarnings("unchecked")
        final FieldVector3D<W>[] axes = (FieldVector3D<W>[]) Array.newInstance(FieldVector3D.class, 2);
        axes[0] = new FieldVector3D<>(crcp.subtract(cosI.multiply(srsp)),  srcp.add(cosI.multiply(crsp)), sinI.multiply(sinPa));
        axes[1] = new FieldVector3D<>(crsp.add(cosI.multiply(srcp)).negate(), cosI.multiply(crcp).subtract(srsp), sinI.multiply(cosPa));

        return axes;
    }
}
