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
package org.orekit.propagation.analytical;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Analytical model for J2 effect.
 * <p>
 * This class computes the differential effect of J2 due to an initial orbit
 * offset. A typical case is when an inclination maneuver changes an orbit
 * inclination at time t₀. As ascending node drift rate depends on
 * inclination, the change induces a time-dependent change in ascending node
 * for later dates.
 * </p>
 * @see org.orekit.forces.maneuvers.SmallManeuverAnalyticalModel
 * @author Luc Maisonobe
 */
public class J2DifferentialEffect
    implements AdapterPropagator.DifferentialEffect {

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Differential drift on perigee argument. */
    private final double dPaDot;

    /** Differential drift on ascending node. */
    private final double dRaanDot;

    /** Indicator for applying effect before reference date. */
    private final boolean applyBefore;

    /** Simple constructor.
     * <p>
     * The {@code applyBefore} parameter is mainly used when the differential
     * effect is associated with a maneuver. In this case, the parameter must be
     * set to {@code false}.
     * </p>
     * @param original original state at reference date
     * @param directEffect direct effect changing the orbit
     * @param applyBefore if true, effect is applied both before and after
     * reference date, if false it is only applied after reference date
     * @param gravityField gravity field to use
     * @exception OrekitException if gravity field does not contain J2 coefficient
     */
    public J2DifferentialEffect(final SpacecraftState original,
                                final AdapterPropagator.DifferentialEffect directEffect,
                                final boolean applyBefore,
                                final UnnormalizedSphericalHarmonicsProvider gravityField)
        throws OrekitException {
        this(original, directEffect, applyBefore,
             gravityField.getAe(), gravityField.getMu(),
             -gravityField.onDate(original.getDate()).getUnnormalizedCnm(2, 0));
    }

    /** Simple constructor.
         * <p>
         * The {@code applyBefore} parameter is mainly used when the differential
         * effect is associated with a maneuver. In this case, the parameter must be
         * set to {@code false}.
         * </p>
         * @param orbit0 original orbit at reference date
         * @param orbit1 shifted orbit at reference date
         * @param applyBefore if true, effect is applied both before and after
         * reference date, if false it is only applied after reference date
         * @param gravityField gravity field to use
         * @exception OrekitException if gravity field does not contain J2 coefficient
         */
    public J2DifferentialEffect(final Orbit orbit0, final Orbit orbit1, final boolean applyBefore,
                                final UnnormalizedSphericalHarmonicsProvider gravityField)
        throws OrekitException {
        this(orbit0, orbit1, applyBefore,
             gravityField.getAe(), gravityField.getMu(),
             -gravityField.onDate(orbit0.getDate()).getUnnormalizedCnm(2, 0));
    }

    /** Simple constructor.
     * <p>
     * The {@code applyBefore} parameter is mainly used when the differential
     * effect is associated with a maneuver. In this case, the parameter must be
     * set to {@code false}.
     * </p>
     * @param original original state at reference date
     * @param directEffect direct effect changing the orbit
     * @param applyBefore if true, effect is applied both before and after
     * reference date, if false it is only applied after reference date
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param j2 un-normalized zonal coefficient (about +1.08e-3 for Earth)
     * @exception OrekitException if direct effect cannot be applied
     */
    public J2DifferentialEffect(final SpacecraftState original,
                                final AdapterPropagator.DifferentialEffect directEffect,
                                final boolean applyBefore,
                                final double referenceRadius, final double mu, final double j2)
        throws OrekitException {
        this(original.getOrbit(),
             directEffect.apply(original.shiftedBy(0.001)).getOrbit().shiftedBy(-0.001),
             applyBefore, referenceRadius, mu, j2);
    }

    /** Simple constructor.
     * <p>
     * The {@code applyBefore} parameter is mainly used when the differential
     * effect is associated with a maneuver. In this case, the parameter must be
     * set to {@code false}.
     * </p>
     * @param orbit0 original orbit at reference date
     * @param orbit1 shifted orbit at reference date
     * @param applyBefore if true, effect is applied both before and after
     * reference date, if false it is only applied after reference date
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param j2 un-normalized zonal coefficient (about +1.08e-3 for Earth)
     */
    public J2DifferentialEffect(final Orbit orbit0, final Orbit orbit1, final boolean applyBefore,
                                final double referenceRadius, final double mu, final double j2) {

        this.referenceDate = orbit0.getDate();
        this.applyBefore   = applyBefore;

        // extract useful parameters
        final double a0 = orbit0.getA();
        final double e0 = orbit0.getE();
        final double i0 = orbit0.getI();
        final double a1 = orbit1.getA();
        final double e1 = orbit1.getE();
        final double i1 = orbit1.getI();

        // compute reference drifts
        final double oMe2       = 1 - e0 * e0;
        final double ratio      = referenceRadius / (a0 * oMe2);
        final double cosI       = FastMath.cos(i0);
        final double sinI       = FastMath.sin(i0);
        final double n          = FastMath.sqrt(mu / a0) / a0;
        final double c          = ratio * ratio * n * j2;
        final double refPaDot   =  0.75 * c * (4 - 5 * sinI * sinI);
        final double refRaanDot = -1.5  * c * cosI;

        // differential model on perigee argument drift
        final double dPaDotDa = -3.5 * refPaDot / a0;
        final double dPaDotDe = 4 * refPaDot * e0 / oMe2;
        final double dPaDotDi = -7.5 * c * sinI * cosI;
        dPaDot = dPaDotDa * (a1 - a0) + dPaDotDe * (e1 - e0) + dPaDotDi * (i1 - i0);

        // differential model on ascending node drift
        final double dRaanDotDa = -3.5 * refRaanDot / a0;
        final double dRaanDotDe = 4 * refRaanDot * e0 / oMe2;
        final double dRaanDotDi = -refRaanDot * FastMath.tan(i0);
        dRaanDot = dRaanDotDa * (a1 - a0) + dRaanDotDe * (e1 - e0) + dRaanDotDi * (i1 - i0);

    }

    /** Compute the effect of the maneuver on an orbit.
     * @param orbit1 original orbit at t₁, without maneuver
     * @return orbit at t₁, taking the maneuver
     * into account if t₁ &gt; t₀
     * @see #apply(SpacecraftState)
     */
    public Orbit apply(final Orbit orbit1) {

        if (orbit1.getDate().compareTo(referenceDate) <= 0 && !applyBefore) {
            // the orbit change has not occurred yet, don't change anything
            return orbit1;
        }

        return updateOrbit(orbit1);

    }

    /** {@inheritDoc} */
    public SpacecraftState apply(final SpacecraftState state1) {

        if (state1.getDate().compareTo(referenceDate) <= 0 && !applyBefore) {
            // the orbit change has not occurred yet, don't change anything
            return state1;
        }

        return new SpacecraftState(updateOrbit(state1.getOrbit()),
                                   state1.getAttitude(), state1.getMass());

    }

    /** Compute the differential effect of J2 on an orbit.
     * @param orbit1 original orbit at t₁, without differential J2
     * @return orbit at t₁, always taking the effect into account
     */
    private Orbit updateOrbit(final Orbit orbit1) {

        // convert current orbital state to equinoctial elements
        final EquinoctialOrbit original =
                (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(orbit1);

        // compute differential effect
        final AbsoluteDate date = original.getDate();
        final double dt         = date.durationFrom(referenceDate);
        final double dPaRaan    = (dPaDot + dRaanDot) * dt;
        final double cPaRaan    = FastMath.cos(dPaRaan);
        final double sPaRaan    = FastMath.sin(dPaRaan);
        final double dRaan      = dRaanDot * dt;
        final double cRaan      = FastMath.cos(dRaan);
        final double sRaan      = FastMath.sin(dRaan);

        final double ex         = original.getEquinoctialEx() * cPaRaan -
                                  original.getEquinoctialEy() * sPaRaan;
        final double ey         = original.getEquinoctialEx() * sPaRaan +
                                  original.getEquinoctialEy() * cPaRaan;
        final double hx         = original.getHx() * cRaan - original.getHy() * sRaan;
        final double hy         = original.getHx() * sRaan + original.getHy() * cRaan;
        final double lambda     = original.getLv() + dPaRaan;

        // build updated orbit
        final EquinoctialOrbit updated =
                new EquinoctialOrbit(original.getA(), ex, ey, hx, hy, lambda, PositionAngle.TRUE,
                                     original.getFrame(), date, original.getMu());

        // convert to required type
        return orbit1.getType().convertType(updated);

    }

}
