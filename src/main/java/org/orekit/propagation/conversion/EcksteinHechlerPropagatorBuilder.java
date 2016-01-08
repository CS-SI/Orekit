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
package org.orekit.propagation.conversion;

import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;

/** Builder for Eckstein-Hechler propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class EcksteinHechlerPropagatorBuilder extends AbstractPropagatorBuilder {

    /** Provider for un-normalized coefficients. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /** Build a new instance.
     * @param frame the frame in which the orbit is propagated
     *        (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param provider for un-normalized zonal coefficients
     * @deprecated as of 7.1, replaced with {@link #EcksteinHechlerPropagatorBuilder(Frame,
     * UnnormalizedSphericalHarmonicsProvider, OrbitType, PositionAngle)}
     */
    @Deprecated
    public EcksteinHechlerPropagatorBuilder(final Frame frame,
                                            final UnnormalizedSphericalHarmonicsProvider provider) {
        this(frame, provider, OrbitType.CARTESIAN, PositionAngle.TRUE);
    }

    /** Build a new instance.
     * @param frame the frame in which the orbit is propagated
     *        (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param provider for un-normalized zonal coefficients
     * @param orbitType orbit type to use
     * @param positionAngle position angle type to use
     * @since 7.1
     */
    public EcksteinHechlerPropagatorBuilder(final Frame frame,
                                            final UnnormalizedSphericalHarmonicsProvider provider,
                                            final OrbitType orbitType, final PositionAngle positionAngle) {
        super(frame, provider.getMu(), orbitType, positionAngle);
        this.provider = provider;
    }

    /** Build a new instance.
     * @param frame the frame in which the orbit is propagated
     *        (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param tideSystem tide system
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @deprecated as of 7.1, replaced with {@link #EcksteinHechlerPropagatorBuilder(Frame,
     * double, double, TideSystem, double, double, double, double, double, OrbitType, PositionAngle)}
     */
    @Deprecated
    public EcksteinHechlerPropagatorBuilder(final Frame frame,
                                            final double referenceRadius,
                                            final double mu,
                                            final TideSystem tideSystem,
                                            final double c20,
                                            final double c30,
                                            final double c40,
                                            final double c50,
                                            final double c60) {
        this(frame, referenceRadius, mu, tideSystem, c20, c30, c40, c50, c60,
             OrbitType.CARTESIAN, PositionAngle.TRUE);
    }

    /** Build a new instance.
     * @param frame the frame in which the orbit is propagated
     *        (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param tideSystem tide system
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @param orbitType orbit type to use
     * @param positionAngle position angle type to use
     * @since 7.1
     */
    public EcksteinHechlerPropagatorBuilder(final Frame frame,
                                            final double referenceRadius,
                                            final double mu,
                                            final TideSystem tideSystem,
                                            final double c20,
                                            final double c30,
                                            final double c40,
                                            final double c50,
                                            final double c60,
                                            final OrbitType orbitType,
                                            final PositionAngle positionAngle) {
        this(frame,
             GravityFieldFactory.getUnnormalizedProvider(referenceRadius, mu, tideSystem,
                                                         new double[][] {
                                                             {
                                                                 0
                                                             }, {
                                                                 0
                                                             }, {
                                                                 c20
                                                             }, {
                                                                 c30
                                                             }, {
                                                                 c40
                                                             }, {
                                                                 c50
                                                             }, {
                                                                 c60
                                                             }
                                                         }, new double[][] {
                                                             {
                                                                 0
                                                             }, {
                                                                 0
                                                             }, {
                                                                 0
                                                             }, {
                                                                 0
                                                             }, {
                                                                 0
                                                             }, {
                                                                 0
                                                             }, {
                                                                 0
                                                             }
                                                         }),
             orbitType, positionAngle);
    }

    /** {@inheritDoc} */
    public Propagator buildPropagator(final AbsoluteDate date, final double[] parameters)
        throws OrekitException {
        checkParameters(parameters);
        return new EcksteinHechlerPropagator(createInitialOrbit(date, parameters), provider);
    }

}
