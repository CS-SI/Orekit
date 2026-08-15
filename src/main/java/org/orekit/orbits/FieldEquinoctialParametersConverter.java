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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.FieldPVCoordinates;

/**
 * Class for converting between equinoctial elements and Cartesian coordinates (Field version).
 * @param <T> type of the field element
 * @author Romain Serra
 * @see FieldEquinoctialParameters
 * @see EquinoctialParametersConverter
 * @since 14.0
 */
public class FieldEquinoctialParametersConverter<T extends CalculusFieldElement<T>> {

    /** Central body gravitational parameter. */
    private final T mu;

    /**
     * Constructor.
     * @param mu central body gravitational parameter
     */
    public FieldEquinoctialParametersConverter(final T mu) {
        this.mu = mu;
    }

    /**
     * Convert Cartesian coordinates to equinoctial elements.
     * @param cartesian position and velocity in inertial frame
     * @param positionAngleType type of position angle to use
     * @return equinoctial elements
     */
    public FieldEquinoctialParameters<T> toParameters(final FieldPVCoordinates<T> cartesian,
                                                      final PositionAngleType positionAngleType) {
        // compute semi-major axis
        final FieldVector3D<T> pvP = cartesian.getPosition();
        final T r = pvP.getNorm2();
        final T v2 = cartesian.getVelocity().getNorm2Sq();
        final T rV2OnMu = r.multiply(v2).divide(mu);
        final T a = r.divide(rV2OnMu.negate().add(2));

        if (a.getReal() < 0.) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                    getClass().getName());
        }

        // compute inclination vector
        final FieldVector3D<T> w = cartesian.getMomentum().normalize();
        final T one = a.getField().getOne();
        final T d = one.divide(one.add(w.getZ()));
        final T hx =  d.negate().multiply(w.getY());
        final T hy =  d.multiply(w.getX());

        // compute true longitude argument
        final T cLv = (pvP.getX().subtract(d.multiply(pvP.getZ()).multiply(w.getX()))).divide(r);
        final T sLv = (pvP.getY().subtract(d.multiply(pvP.getZ()).multiply(w.getY()))).divide(r);
        final T trueLongitude = sLv.atan2(cLv);

        // compute eccentricity vector
        final T eSE = FieldVector3D.dotProduct(pvP, cartesian.getVelocity()).divide(a.multiply(mu).sqrt());
        final T eCE = rV2OnMu.subtract(1);
        final T e2  = eCE.square().add(eSE.square());
        final T f   = eCE.subtract(e2);
        final T g   = e2.negate().add(1).sqrt().multiply(eSE);
        final T ex = a.multiply(f.multiply(cLv).add( g.multiply(sLv))).divide(r);
        final T ey = a.multiply(f.multiply(sLv).subtract(g.multiply(cLv))).divide(r);

        final FieldEquinoctialParameters<T> equinoctialParameters = new FieldEquinoctialParameters<>(a, ex, ey, hx, hy,
                trueLongitude, PositionAngleType.TRUE);
        return positionAngleType == PositionAngleType.TRUE ? equinoctialParameters :
                equinoctialParameters.withPositionAngleType(positionAngleType);
    }

    /**
     * Convert equinoctial elements to Cartesian coordinates.
     * @param elements equinoctial elements
     * @return position and velocity in inertial frame
     */
    public FieldPVCoordinates<T> toCartesian(final FieldEquinoctialParameters<T> elements) {
        // get equinoctial parameters
        final T a = elements.a();
        final T ex = elements.ex();
        final T ey = elements.ey();
        final T hx = elements.hx();
        final T hy = elements.hy();
        final T lE = elements.positionAngleType() == PositionAngleType.ECCENTRIC ? elements.longitudeArgument() :
                elements.withPositionAngleType(PositionAngleType.ECCENTRIC).longitudeArgument();

        // inclination-related intermediate parameters
        final T hx2   = hx.square();
        final T hy2   = hy.square();
        final T one = a.getField().getOne();
        final T factH = one.divide(hx2.add(1.0).add(hy2));

        // reference axes defining the orbital plane
        final T ux = hx2.add(1.0).subtract(hy2).multiply(factH);
        final T uy = hx.multiply(hy).multiply(factH).multiply(2);
        final T uz = hy.multiply(-2).multiply(factH);

        final T vx = uy;
        final T vy = (hy2.subtract(hx2).add(1)).multiply(factH);
        final T vz =  hx.multiply(factH).multiply(2);

        // eccentricity-related intermediate parameters
        final T ex2  = ex.square();
        final T exey = ex.multiply(ey);
        final T ey2  = ey.square();
        final T e2   = ex2.add(ey2);
        final T eta  = one.subtract(e2).sqrt().add(1);
        final T beta = one.divide(eta);

        // eccentric longitude argument
        final FieldSinCos<T> scLe = FastMath.sinCos(lE);
        final T cLe    = scLe.cos();
        final T sLe    = scLe.sin();
        final T exCeyS = ex.multiply(cLe).add(ey.multiply(sLe));

        // coordinates of position and velocity in the orbital plane
        final T x      = a.multiply(one.subtract(beta.multiply(ey2)).multiply(cLe).add(beta.multiply(exey).multiply(sLe)).subtract(ex));
        final T y      = a.multiply(one.subtract(beta.multiply(ex2)).multiply(sLe).add(beta .multiply(exey).multiply(cLe)).subtract(ey));

        final T factor = mu.divide(a).sqrt().divide(one.subtract(exCeyS));
        final T xdot   = factor.multiply(sLe.negate().add(beta.multiply(ey).multiply(exCeyS)));
        final T ydot   = factor.multiply(cLe.subtract(beta.multiply(ex).multiply(exCeyS)));

        final FieldVector3D<T> position =
                new FieldVector3D<>(x.multiply(ux).add(y.multiply(vx)),
                        x.multiply(uy).add(y.multiply(vy)),
                        x.multiply(uz).add(y.multiply(vz)));
        final FieldVector3D<T> velocity =
                new FieldVector3D<>(xdot.multiply(ux).add(ydot.multiply(vx)), xdot.multiply(uy).add(ydot.multiply(vy)), xdot.multiply(uz).add(ydot.multiply(vz)));

        return new FieldPVCoordinates<>(position, velocity);
    }
}
