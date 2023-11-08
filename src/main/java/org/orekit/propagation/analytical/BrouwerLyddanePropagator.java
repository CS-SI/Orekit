/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.hipparchus.util.SinCos;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeSpanMap.Span;

/**
 * This class propagates a {@link org.orekit.propagation.SpacecraftState}
 *  using the analytical Brouwer-Lyddane model (from J2 to J5 zonal harmonics).
 * <p>
 * At the opposite of the {@link EcksteinHechlerPropagator}, the Brouwer-Lyddane model is
 * suited for elliptical orbits, there is no problem having a rather small eccentricity or inclination
 * (Lyddane helped to solve this issue with the Brouwer model). Singularity for the critical
 * inclination i = 63.4° is avoided using the method developed in Warren Phipps' 1992 thesis.
 * <p>
 * By default, Brouwer-Lyddane model considers only the perturbations due to zonal harmonics.
 * However, for low Earth orbits, the magnitude of the perturbative acceleration due to
 * atmospheric drag can be significant. Warren Phipps' 1992 thesis considered the atmospheric
 * drag by time derivatives of the <i>mean</i> mean anomaly using the catch-all coefficient
 * {@link #M2Driver}. Beware that M2Driver must have only 1 span on its TimeSpanMap value.
 *
 * Usually, M2 is adjusted during an orbit determination process and it represents the
 * combination of all unmodeled secular along-track effects (i.e. not just the atmospheric drag).
 * The behavior of M2 is close to the {@link TLE#getBStar()} parameter for the TLE.
 *
 * If the value of M2 is equal to {@link #M2 0.0}, the along-track  secular effects are not
 * considered in the dynamical model. Typical values for M2 are not known. It depends on the
 * orbit type. However, the value of M2 must be very small (e.g. between 1.0e-14 and 1.0e-15).
 * The unit of M2 is rad/s².
 *
 * The along-track effects, represented by the secular rates of the mean semi-major axis
 * and eccentricity, are computed following Eq. 2.38, 2.41, and 2.45 of Warren Phipps' thesis.
 *
 * @see "Brouwer, Dirk. Solution of the problem of artificial satellite theory without drag.
 *       YALE UNIV NEW HAVEN CT NEW HAVEN United States, 1959."
 *
 * @see "Lyddane, R. H. Small eccentricities or inclinations in the Brouwer theory of the
 *       artificial satellite. The Astronomical Journal 68 (1963): 555."
 *
 * @see "Phipps Jr, Warren E. Parallelization of the Navy Space Surveillance Center
 *       (NAVSPASUR) Satellite Model. NAVAL POSTGRADUATE SCHOOL MONTEREY CA, 1992."
 *
 * @author Melina Vanel
 * @author Bryan Cazabonne
 * @since 11.1
 */
public class BrouwerLyddanePropagator extends AbstractAnalyticalPropagator implements ParameterDriversProvider {

    /** Parameter name for M2 coefficient. */
    public static final String M2_NAME = "M2";

    /** Default value for M2 coefficient. */
    public static final double M2 = 0.0;

    /** Default convergence threshold for mean parameters conversion. */
    private static final double EPSILON_DEFAULT = 1.0e-13;

    /** Default value for maxIterations. */
    private static final int MAX_ITERATIONS_DEFAULT = 200;

    /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double SCALE = FastMath.scalb(1.0, -32);

    /** Beta constant used by T2 function. */
    private static final double BETA = FastMath.scalb(100, -11);

    /** Initial Brouwer-Lyddane model. */
    private BLModel initialModel;

    /** All models. */
    private transient TimeSpanMap<BLModel> models;

    /** Reference radius of the central body attraction model (m). */
    private double referenceRadius;

    /** Central attraction coefficient (m³/s²). */
    private double mu;

    /** Un-normalized zonal coefficients. */
    private double[] ck0;

    /** Empirical coefficient used in the drag modeling. */
    private final ParameterDriver M2Driver;

    /** Build a propagator from orbit and potential provider.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial orbit
     * @param provider for un-normalized zonal coefficients
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     * @see #BrouwerLyddanePropagator(Orbit, AttitudeProvider, UnnormalizedSphericalHarmonicsProvider, double)
     * @see #BrouwerLyddanePropagator(Orbit, UnnormalizedSphericalHarmonicsProvider, PropagationType, double)
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit,
                                    final UnnormalizedSphericalHarmonicsProvider provider,
                                    final double M2) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             DEFAULT_MASS, provider, provider.onDate(initialOrbit.getDate()), M2);
    }

    /**
     * Private helper constructor.
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     * @param initialOrbit initial orbit
     * @param attitude attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param harmonics {@code provider.onDate(initialOrbit.getDate())}
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     * @see #BrouwerLyddanePropagator(Orbit, AttitudeProvider, double,
     *                                 UnnormalizedSphericalHarmonicsProvider,
     *                                 UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics,
     *                                 PropagationType, double)
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit,
                                    final AttitudeProvider attitude,
                                    final double mass,
                                    final UnnormalizedSphericalHarmonicsProvider provider,
                                    final UnnormalizedSphericalHarmonics harmonics,
                                    final double M2) {
        this(initialOrbit, attitude, mass, provider.getAe(), provider.getMu(),
             harmonics.getUnnormalizedCnm(2, 0),
             harmonics.getUnnormalizedCnm(3, 0),
             harmonics.getUnnormalizedCnm(4, 0),
             harmonics.getUnnormalizedCnm(5, 0),
             M2);
    }

    /** Build a propagator from orbit and potential.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     *
     * <p> C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *
     * <p> C<sub>n,0</sub> = -J<sub>n</sub>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial orbit
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     * @see org.orekit.utils.Constants
     * @see #BrouwerLyddanePropagator(Orbit, AttitudeProvider, double, double, double,
     * double, double, double, double, double)
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit,
                                    final double referenceRadius, final double mu,
                                    final double c20, final double c30, final double c40,
                                    final double c50, final double M2) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             DEFAULT_MASS, referenceRadius, mu, c20, c30, c40, c50, M2);
    }

    /** Build a propagator from orbit, mass and potential provider.
     * <p>Attitude law is set to an unspecified non-null arbitrary value.</p>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial orbit
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     * @see #BrouwerLyddanePropagator(Orbit, AttitudeProvider, double, UnnormalizedSphericalHarmonicsProvider, double)
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit, final double mass,
                                    final UnnormalizedSphericalHarmonicsProvider provider,
                                    final double M2) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             mass, provider, provider.onDate(initialOrbit.getDate()), M2);
    }

    /** Build a propagator from orbit, mass and potential.
     * <p>Attitude law is set to an unspecified non-null arbitrary value.</p>
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     *
     * <p> C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *
     * <p> C<sub>n,0</sub> = -J<sub>n</sub>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial orbit
     * @param mass spacecraft mass
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     * @see #BrouwerLyddanePropagator(Orbit, AttitudeProvider, double, double, double,
     * double, double, double, double, double)
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit, final double mass,
                                    final double referenceRadius, final double mu,
                                    final double c20, final double c30, final double c40,
                                    final double c50, final double M2) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             mass, referenceRadius, mu, c20, c30, c40, c50, M2);
    }

    /** Build a propagator from orbit, attitude provider and potential provider.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param provider for un-normalized zonal coefficients
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit,
                                    final AttitudeProvider attitudeProv,
                                    final UnnormalizedSphericalHarmonicsProvider provider,
                                    final double M2) {
        this(initialOrbit, attitudeProv, DEFAULT_MASS, provider, provider.onDate(initialOrbit.getDate()), M2);
    }

    /** Build a propagator from orbit, attitude provider and potential.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     *
     * <p> C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *
     * <p> C<sub>n,0</sub> = -J<sub>n</sub>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit,
                                    final AttitudeProvider attitudeProv,
                                    final double referenceRadius, final double mu,
                                    final double c20, final double c30, final double c40,
                                    final double c50, final double M2) {
        this(initialOrbit, attitudeProv, DEFAULT_MASS, referenceRadius, mu, c20, c30, c40, c50, M2);
    }

    /** Build a propagator from orbit, attitude provider, mass and potential provider.
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     * @see #BrouwerLyddanePropagator(Orbit, AttitudeProvider, double,
     *                                UnnormalizedSphericalHarmonicsProvider, PropagationType, double)
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit,
                                    final AttitudeProvider attitudeProv,
                                    final double mass,
                                    final UnnormalizedSphericalHarmonicsProvider provider,
                                    final double M2) {
        this(initialOrbit, attitudeProv, mass, provider, provider.onDate(initialOrbit.getDate()), M2);
    }

    /** Build a propagator from orbit, attitude provider, mass and potential.
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     *
     * <p> C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *
     * <p> C<sub>n,0</sub> = -J<sub>n</sub>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     * @see #BrouwerLyddanePropagator(Orbit, AttitudeProvider, double, double, double,
     *                                 double, double, double, double, PropagationType, double)
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit,
                                    final AttitudeProvider attitudeProv,
                                    final double mass,
                                    final double referenceRadius, final double mu,
                                    final double c20, final double c30, final double c40,
                                    final double c50, final double M2) {
        this(initialOrbit, attitudeProv, mass, referenceRadius, mu, c20, c30, c40, c50,
             PropagationType.OSCULATING, M2);
    }


    /** Build a propagator from orbit and potential provider.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     *
     * <p>Using this constructor, it is possible to define the initial orbit as
     * a mean Brouwer-Lyddane orbit or an osculating one.</p>
     *
     * @param initialOrbit initial orbit
     * @param provider for un-normalized zonal coefficients
     * @param initialType initial orbit type (mean Brouwer-Lyddane orbit or osculating orbit)
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit,
                                    final UnnormalizedSphericalHarmonicsProvider provider,
                                    final PropagationType initialType, final double M2) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             DEFAULT_MASS, provider, provider.onDate(initialOrbit.getDate()), initialType, M2);
    }

    /** Build a propagator from orbit, attitude provider, mass and potential provider.
     * <p>Using this constructor, it is possible to define the initial orbit as
     * a mean Brouwer-Lyddane orbit or an osculating one.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param initialType initial orbit type (mean Brouwer-Lyddane orbit or osculating orbit)
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit,
                                    final AttitudeProvider attitudeProv,
                                    final double mass,
                                    final UnnormalizedSphericalHarmonicsProvider provider,
                                    final PropagationType initialType, final double M2) {
        this(initialOrbit, attitudeProv, mass, provider, provider.onDate(initialOrbit.getDate()), initialType, M2);
    }

    /**
     * Private helper constructor.
     * <p>Using this constructor, it is possible to define the initial orbit as
     * a mean Brouwer-Lyddane orbit or an osculating one.</p>
     * @param initialOrbit initial orbit
     * @param attitude attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param harmonics {@code provider.onDate(initialOrbit.getDate())}
     * @param initialType initial orbit type (mean Brouwer-Lyddane orbit or osculating orbit)
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit,
                                    final AttitudeProvider attitude,
                                    final double mass,
                                    final UnnormalizedSphericalHarmonicsProvider provider,
                                    final UnnormalizedSphericalHarmonics harmonics,
                                    final PropagationType initialType, final double M2) {
        this(initialOrbit, attitude, mass, provider.getAe(), provider.getMu(),
             harmonics.getUnnormalizedCnm(2, 0),
             harmonics.getUnnormalizedCnm(3, 0),
             harmonics.getUnnormalizedCnm(4, 0),
             harmonics.getUnnormalizedCnm(5, 0),
             initialType, M2);
    }

    /** Build a propagator from orbit, attitude provider, mass and potential.
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     *
     * <p> C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *
     * <p> C<sub>n,0</sub> = -J<sub>n</sub>
     *
     * <p>Using this constructor, it is possible to define the initial orbit as
     * a mean Brouwer-Lyddane orbit or an osculating one.</p>
     *
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param initialType initial orbit type (mean Brouwer-Lyddane orbit or osculating orbit)
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit,
                                    final AttitudeProvider attitudeProv,
                                    final double mass,
                                    final double referenceRadius, final double mu,
                                    final double c20, final double c30, final double c40,
                                    final double c50,
                                    final PropagationType initialType, final double M2) {
        this(initialOrbit, attitudeProv, mass, referenceRadius, mu,
             c20, c30, c40, c50, initialType, M2, EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT);
    }

    /** Build a propagator from orbit, attitude provider, mass and potential.
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     *
     * <p> C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *
     * <p> C<sub>n,0</sub> = -J<sub>n</sub>
     *
     * <p>Using this constructor, it is possible to define the initial orbit as
     * a mean Brouwer-Lyddane orbit or an osculating one.</p>
     *
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param initialType initial orbit type (mean Brouwer-Lyddane orbit or osculating orbit)
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link #M2} drag is not computed
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @since 11.2
     */
    public BrouwerLyddanePropagator(final Orbit initialOrbit,
                                    final AttitudeProvider attitudeProv,
                                    final double mass,
                                    final double referenceRadius, final double mu,
                                    final double c20, final double c30, final double c40,
                                    final double c50,
                                    final PropagationType initialType, final double M2,
                                    final double epsilon, final int maxIterations) {

        super(attitudeProv);

        // store model coefficients
        this.referenceRadius = referenceRadius;
        this.mu  = mu;
        this.ck0 = new double[] {0.0, 0.0, c20, c30, c40, c50};

        // initialize M2 driver
        this.M2Driver = new ParameterDriver(M2_NAME, M2, SCALE,
                                            Double.NEGATIVE_INFINITY,
                                            Double.POSITIVE_INFINITY);

        // compute mean parameters if needed
        resetInitialState(new SpacecraftState(initialOrbit,
                                              attitudeProv.getAttitude(initialOrbit,
                                                                       initialOrbit.getDate(),
                                                                       initialOrbit.getFrame()),
                                              mass),
                          initialType, epsilon, maxIterations);

    }

    /** Conversion from osculating to mean orbit.
     * <p>
     * Compute mean orbit <b>in a Brouwer-Lyddane sense</b>, corresponding to the
     * osculating SpacecraftState in input.
     * </p>
     * <p>
     * Since the osculating orbit is obtained with the computation of
     * short-periodic variation, the resulting output will depend on
     * both the gravity field parameterized in input and the
     * atmospheric drag represented by the {@code m2} parameter.
     * </p>
     * <p>
     * The computation is done through a fixed-point iteration process.
     * </p>
     * @param osculating osculating orbit to convert
     * @param provider for un-normalized zonal coefficients
     * @param harmonics {@code provider.onDate(osculating.getDate())}
     * @param M2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@code BrouwerLyddanePropagator.M2} drag is not considered
     * @return mean orbit in a Brouwer-Lyddane sense
     * @since 11.2
     */
    public static KeplerianOrbit computeMeanOrbit(final Orbit osculating,
                                                  final UnnormalizedSphericalHarmonicsProvider provider,
                                                  final UnnormalizedSphericalHarmonics harmonics,
                                                  final double M2Value) {
        return computeMeanOrbit(osculating, provider, harmonics, M2Value, EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT);
    }

    /** Conversion from osculating to mean orbit.
     * <p>
     * Compute mean orbit <b>in a Brouwer-Lyddane sense</b>, corresponding to the
     * osculating SpacecraftState in input.
     * </p>
     * <p>
     * Since the osculating orbit is obtained with the computation of
     * short-periodic variation, the resulting output will depend on
     * both the gravity field parameterized in input and the
     * atmospheric drag represented by the {@code m2} parameter.
     * </p>
     * <p>
     * The computation is done through a fixed-point iteration process.
     * </p>
     * @param osculating osculating orbit to convert
     * @param provider for un-normalized zonal coefficients
     * @param harmonics {@code provider.onDate(osculating.getDate())}
     * @param M2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@code BrouwerLyddanePropagator.M2} drag is not considered
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @return mean orbit in a Brouwer-Lyddane sense
     * @since 11.2
     */
    public static KeplerianOrbit computeMeanOrbit(final Orbit osculating,
                                                  final UnnormalizedSphericalHarmonicsProvider provider,
                                                  final UnnormalizedSphericalHarmonics harmonics,
                                                  final double M2Value,
                                                  final double epsilon, final int maxIterations) {
        return computeMeanOrbit(osculating,
                                provider.getAe(), provider.getMu(),
                                harmonics.getUnnormalizedCnm(2, 0),
                                harmonics.getUnnormalizedCnm(3, 0),
                                harmonics.getUnnormalizedCnm(4, 0),
                                harmonics.getUnnormalizedCnm(5, 0),
                                M2Value, epsilon, maxIterations);
    }

    /** Conversion from osculating to mean orbit.
     * <p>
     * Compute mean orbit <b>in a Brouwer-Lyddane sense</b>, corresponding to the
     * osculating SpacecraftState in input.
     * </p>
     * <p>
     * Since the osculating orbit is obtained with the computation of
     * short-periodic variation, the resulting output will depend on
     * both the gravity field parameterized in input and the
     * atmospheric drag represented by the {@code m2} parameter.
     * </p>
     * <p>
     * The computation is done through a fixed-point iteration process.
     * </p>
     * @param osculating osculating orbit to convert
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param M2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@code BrouwerLyddanePropagator.M2} drag is not considered
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @return mean orbit in a Brouwer-Lyddane sense
     * @since 11.2
     */
    public static KeplerianOrbit computeMeanOrbit(final Orbit osculating,
                                                  final double referenceRadius, final double mu,
                                                  final double c20, final double c30, final double c40,
                                                  final double c50, final double M2Value,
                                                  final double epsilon, final int maxIterations) {
        final BrouwerLyddanePropagator propagator =
                        new BrouwerLyddanePropagator(osculating,
                                                     FrameAlignedProvider.of(osculating.getFrame()),
                                                     DEFAULT_MASS,
                                                     referenceRadius, mu, c20, c30, c40, c50,
                                                     PropagationType.OSCULATING, M2Value,
                                                     epsilon, maxIterations);
        return propagator.initialModel.mean;
    }

    /** {@inheritDoc}
     * <p>The new initial state to consider
     * must be defined with an osculating orbit.</p>
     * @see #resetInitialState(SpacecraftState, PropagationType)
     */
    public void resetInitialState(final SpacecraftState state) {
        resetInitialState(state, PropagationType.OSCULATING);
    }

    /** Reset the propagator initial state.
     * @param state new initial state to consider
     * @param stateType mean Brouwer-Lyddane orbit or osculating orbit
     */
    public void resetInitialState(final SpacecraftState state, final PropagationType stateType) {
        resetInitialState(state, stateType, EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT);
    }

    /** Reset the propagator initial state.
     * @param state new initial state to consider
     * @param stateType mean Brouwer-Lyddane orbit or osculating orbit
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @since 11.2
     */
    public void resetInitialState(final SpacecraftState state, final PropagationType stateType,
                                  final double epsilon, final int maxIterations) {
        super.resetInitialState(state);
        final KeplerianOrbit keplerian = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(state.getOrbit());
        this.initialModel = (stateType == PropagationType.MEAN) ?
                             new BLModel(keplerian, state.getMass(), referenceRadius, mu, ck0) :
                             computeMeanParameters(keplerian, state.getMass(), epsilon, maxIterations);
        this.models = new TimeSpanMap<BLModel>(initialModel);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        resetIntermediateState(state, forward, EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT);
    }

    /** Reset an intermediate state.
     * @param state new intermediate state to consider
     * @param forward if true, the intermediate state is valid for
     * propagations after itself
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @since 11.2
     */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward,
                                          final double epsilon, final int maxIterations) {
        final BLModel newModel = computeMeanParameters((KeplerianOrbit) OrbitType.KEPLERIAN.convertType(state.getOrbit()),
                                                       state.getMass(), epsilon, maxIterations);
        if (forward) {
            models.addValidAfter(newModel, state.getDate(), false);
        } else {
            models.addValidBefore(newModel, state.getDate(), false);
        }
        stateChanged(state);
    }

    /** Compute mean parameters according to the Brouwer-Lyddane analytical model computation
     * in order to do the propagation.
     * @param osculating osculating orbit
     * @param mass constant mass
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @return Brouwer-Lyddane mean model
     */
    private BLModel computeMeanParameters(final KeplerianOrbit osculating, final double mass,
                                          final double epsilon, final int maxIterations) {

        // sanity check
        if (osculating.getA() < referenceRadius) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE,
                                           osculating.getA());
        }

        // rough initialization of the mean parameters
        BLModel current = new BLModel(osculating, mass, referenceRadius, mu, ck0);

        // threshold for each parameter
        final double thresholdA      = epsilon * (1 + FastMath.abs(current.mean.getA()));
        final double thresholdE      = epsilon * (1 + current.mean.getE());
        final double thresholdAngles = epsilon * FastMath.PI;

        int i = 0;
        while (i++ < maxIterations) {

            // recompute the osculating parameters from the current mean parameters
            final KeplerianOrbit parameters = current.propagateParameters(current.mean.getDate());

            // adapted parameters residuals
            final double deltaA     = osculating.getA() - parameters.getA();
            final double deltaE     = osculating.getE() - parameters.getE();
            final double deltaI     = osculating.getI() - parameters.getI();
            final double deltaOmega = MathUtils.normalizeAngle(osculating.getPerigeeArgument() -
                                                               parameters.getPerigeeArgument(),
                                                               0.0);
            final double deltaRAAN  = MathUtils.normalizeAngle(osculating.getRightAscensionOfAscendingNode() -
                                                               parameters.getRightAscensionOfAscendingNode(),
                                                               0.0);
            final double deltaAnom = MathUtils.normalizeAngle(osculating.getMeanAnomaly() -
                                                              parameters.getMeanAnomaly(),
                                                              0.0);


            // update mean parameters
            current = new BLModel(new KeplerianOrbit(current.mean.getA() + deltaA,
                                                     FastMath.max(current.mean.getE() + deltaE, 0.0),
                                                     current.mean.getI() + deltaI,
                                                     current.mean.getPerigeeArgument() + deltaOmega,
                                                     current.mean.getRightAscensionOfAscendingNode() + deltaRAAN,
                                                     current.mean.getMeanAnomaly() + deltaAnom,
                                                     PositionAngleType.MEAN,
                                                     current.mean.getFrame(),
                                                     current.mean.getDate(), mu),
                                  mass, referenceRadius, mu, ck0);
            // check convergence
            if (FastMath.abs(deltaA)     < thresholdA &&
                FastMath.abs(deltaE)     < thresholdE &&
                FastMath.abs(deltaI)     < thresholdAngles &&
                FastMath.abs(deltaOmega) < thresholdAngles &&
                FastMath.abs(deltaRAAN)  < thresholdAngles &&
                FastMath.abs(deltaAnom)  < thresholdAngles) {
                return current;
            }
        }
        throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_BROUWER_LYDDANE_MEAN_PARAMETERS, i);
    }


    /** {@inheritDoc} */
    public KeplerianOrbit propagateOrbit(final AbsoluteDate date) {
        // compute keplerian parameters, taking derivatives into account
        final BLModel current = models.get(date);
        return current.propagateParameters(date);
    }

    /**
     * Get the value of the M2 drag parameter. Beware that M2Driver
     * must have only 1 span on its TimeSpanMap value (that is
     * to say setPeriod method should not be called)
     * @return the value of the M2 drag parameter
     */
    public double getM2() {
        // As Brouwer Lyddane is an analytical propagator, for now it is not possible for
        // M2Driver to have several values estimated
        return M2Driver.getValue();
    }

    /**
     * Get the central attraction coefficient μ.
     * @return mu central attraction coefficient (m³/s²)
     */
    public double getMu() {
        return mu;
    }

    /**
     * Get the un-normalized zonal coefficients.
     * @return the un-normalized zonal coefficients
     */
    public double[] getCk0() {
        return ck0.clone();
    }

    /**
     * Get the reference radius of the central body attraction model.
     * @return the reference radius in meters
     */
    public double getReferenceRadius() {
        return referenceRadius;
    }

    /**
     * Get the parameters driver for propagation model.
     * @return drivers for propagation model
     */
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(M2Driver);
    }

    /** {@inheritDoc} */
    @Override
    protected AbstractMatricesHarvester createHarvester(final String stmName, final RealMatrix initialStm,
                                                        final DoubleArrayDictionary initialJacobianColumns) {
        // Create the harvester
        final BrouwerLyddaneHarvester harvester = new BrouwerLyddaneHarvester(this, stmName, initialStm, initialJacobianColumns);
        // Update the list of additional state provider
        addAdditionalStateProvider(harvester);
        // Return the configured harvester
        return harvester;
    }

    /**
     * Get the names of the parameters in the matrix returned by {@link MatricesHarvester#getParametersJacobian}.
     * @return names of the parameters (i.e. columns) of the Jacobian matrix
     */
    protected List<String> getJacobiansColumnsNames() {
        final List<String> columnsNames = new ArrayList<>();
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected() && !columnsNames.contains(driver.getNamesSpanMap().getFirstSpan().getData())) {
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    columnsNames.add(span.getData());
                }
            }
        }
        Collections.sort(columnsNames);
        return columnsNames;
    }

    /** Local class for Brouwer-Lyddane model. */
    private class BLModel {

        /** Mean orbit. */
        private final KeplerianOrbit mean;

        /** Constant mass. */
        private final double mass;

        // CHECKSTYLE: stop JavadocVariable check

        // preprocessed values

        // Constant for secular terms l'', g'', h''
        // l standing for true anomaly, g for perigee argument and h for raan
        private final double xnotDot;
        private final double n;
        private final double lt;
        private final double gt;
        private final double ht;


        // Long period terms
        private final double dei3sg;
        private final double de2sg;
        private final double deisg;
        private final double de;


        private final double dlgs2g;
        private final double dlgc3g;
        private final double dlgcg;


        private final double dh2sgcg;
        private final double dhsgcg;
        private final double dhcg;


        // Short period terms
        private final double aC;
        private final double aCbis;
        private final double ac2g2f;


        private final double eC;
        private final double ecf;
        private final double e2cf;
        private final double e3cf;
        private final double ec2f2g;
        private final double ecfc2f2g;
        private final double e2cfc2f2g;
        private final double e3cfc2f2g;
        private final double ec2gf;
        private final double ec2g3f;


        private final double ide;
        private final double isfs2f2g;
        private final double icfc2f2g;
        private final double ic2f2g;


        private final double glf;
        private final double gll;
        private final double glsf;
        private final double glosf;
        private final double gls2f2g;
        private final double gls2gf;
        private final double glos2gf;
        private final double gls2g3f;
        private final double glos2g3f;


        private final double hf;
        private final double hl;
        private final double hsf;
        private final double hcfs2g2f;
        private final double hs2g2f;
        private final double hsfc2g2f;


        private final double edls2g;
        private final double edlcg;
        private final double edlc3g;
        private final double edlsf;
        private final double edls2gf;
        private final double edls2g3f;

        // Drag terms
        private final double aRate;
        private final double eRate;

        // CHECKSTYLE: resume JavadocVariable check

        /** Create a model for specified mean orbit.
         * @param mean mean orbit
         * @param mass constant mass
         * @param referenceRadius reference radius of the central body attraction model (m)
         * @param mu central attraction coefficient (m³/s²)
         * @param ck0 un-normalized zonal coefficients
         */
        BLModel(final KeplerianOrbit mean, final double mass,
                final double referenceRadius, final double mu, final double[] ck0) {

            this.mean = mean;
            this.mass = mass;

            final double app = mean.getA();
            xnotDot = FastMath.sqrt(mu / app) / app;

            // preliminary processing
            final double q = referenceRadius / app;
            double ql = q * q;
            final double y2 = -0.5 * ck0[2] * ql;

            n = FastMath.sqrt(1 - mean.getE() * mean.getE());
            final double n2 = n * n;
            final double n3 = n2 * n;
            final double n4 = n2 * n2;
            final double n6 = n4 * n2;
            final double n8 = n4 * n4;
            final double n10 = n8 * n2;

            final double yp2 = y2 / n4;
            ql *= q;
            final double yp3 = ck0[3] * ql / n6;
            ql *= q;
            final double yp4 = 0.375 * ck0[4] * ql / n8;
            ql *= q;
            final double yp5 = ck0[5] * ql / n10;


            final SinCos sc    = FastMath.sinCos(mean.getI());
            final double sinI1 = sc.sin();
            final double sinI2 = sinI1 * sinI1;
            final double cosI1 = sc.cos();
            final double cosI2 = cosI1 * cosI1;
            final double cosI3 = cosI2 * cosI1;
            final double cosI4 = cosI2 * cosI2;
            final double cosI6 = cosI4 * cosI2;
            final double C5c2 = 1.0 / T2(cosI1);
            final double C3c2 = 3.0 * cosI2 - 1.0;

            final double epp = mean.getE();
            final double epp2 = epp * epp;
            final double epp3 = epp2 * epp;
            final double epp4 = epp2 * epp2;

            if (epp >= 1) {
                // Only for elliptical (e < 1) orbits
                throw new OrekitException(OrekitMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL,
                                          mean.getE());
            }

            // secular multiplicative
            lt = 1 +
                    1.5 * yp2 * n * C3c2 +
                    0.09375 * yp2 * yp2 * n * (-15.0 + 16.0 * n + 25.0 * n2 + (30.0 - 96.0 * n - 90.0 * n2) * cosI2 + (105.0 + 144.0 * n + 25.0 * n2) * cosI4) +
                    0.9375 * yp4 * n * epp2 * (3.0 - 30.0 * cosI2 + 35.0 * cosI4);

            gt = -1.5 * yp2 * C5c2 +
                    0.09375 * yp2 * yp2 * (-35.0 + 24.0 * n + 25.0 * n2 + (90.0 - 192.0 * n - 126.0 * n2) * cosI2 + (385.0 + 360.0 * n + 45.0 * n2) * cosI4) +
                    0.3125 * yp4 * (21.0 - 9.0 * n2 + (-270.0 + 126.0 * n2) * cosI2 + (385.0 - 189.0 * n2) * cosI4 );

            ht = -3.0 * yp2 * cosI1 +
                    0.375 * yp2 * yp2 * ((-5.0 + 12.0 * n + 9.0 * n2) * cosI1 + (-35.0 - 36.0 * n - 5.0 * n2) * cosI3) +
                    1.25 * yp4 * (5.0 - 3.0 * n2) * cosI1 * (3.0 - 7.0 * cosI2);

            final double cA = 1.0 - 11.0 * cosI2 - 40.0 * cosI4 / C5c2;
            final double cB = 1.0 - 3.0 * cosI2 - 8.0 * cosI4 / C5c2;
            final double cC = 1.0 - 9.0 * cosI2 - 24.0 * cosI4 / C5c2;
            final double cD = 1.0 - 5.0 * cosI2 - 16.0 * cosI4 / C5c2;

            final double qyp2_4 = 3.0 * yp2 * yp2 * cA -
                                  10.0 * yp4 * cB;
            final double qyp52 = epp3 * cosI1 * (0.5 * cD / sinI1 +
                                 sinI1 * (5.0 + 32.0 * cosI2 / C5c2 + 80.0 * cosI4 / C5c2 / C5c2));
            final double qyp22 = 2.0 + epp2 - 11.0 * (2.0 + 3.0 * epp2) * cosI2 -
                                 40.0 * (2.0 + 5.0 * epp2) * cosI4 / C5c2 -
                                 400.0 * epp2 * cosI6 / C5c2 / C5c2;
            final double qyp42 = ( qyp22 + 4.0 * (2.0 + epp2 - (2.0 + 3.0 * epp2) * cosI2) ) / 5.0;
            final double qyp52bis = epp * cosI1 * sinI1 *
                                    (4.0 + 3.0 * epp2) *
                                    (3.0 + 16.0 * cosI2 / C5c2 + 40.0 * cosI4 / C5c2 / C5c2);

            // long periodic multiplicative
            dei3sg =  35.0 / 96.0 * yp5 / yp2 * epp2 * n2 * cD * sinI1;
            de2sg = -1.0 / 12.0 * epp * n2 / yp2 * qyp2_4;
            deisg = ( -35.0 / 128.0 * yp5 / yp2 * epp2 * n2 * cD +
                    1.0 / 4.0 * n2 / yp2 * (yp3 + 5.0 / 16.0 * yp5 * (4.0 + 3.0 * epp2) * cC)) * sinI1;
            de = epp2 * n2 / 24.0 / yp2 * qyp2_4;

            final double qyp52quotient = epp * (-32.0 + 81.0 * epp4) / (4.0 + 3.0 * epp2 + n * (4.0 + 9.0 * epp2));
            dlgs2g = 1.0 / 48.0 / yp2 * (-3.0 * yp2 * yp2 * qyp22 +
                     10.0 * yp4 * qyp42 ) +
                     n3 / yp2 * qyp2_4 / 24.0;
            dlgc3g = 35.0 / 384.0 * yp5 / yp2 * n3 * epp * cD * sinI1 +
                     35.0 / 1152.0 * yp5 / yp2 * (2.0 * qyp52 * cosI1 - epp * cD * sinI1 * (3.0 + 2.0 * epp2));
            dlgcg = -yp3 * epp * cosI2 / ( 4.0 * yp2 * sinI1) +
                    0.078125 * yp5 / yp2 * (-epp * cosI2 / sinI1 * (4.0 + 3.0 * epp2) + epp2 * sinI1 * (26.0 + 9.0 * epp2)) * cC -
                    0.46875 * yp5 / yp2 * qyp52bis * cosI1 +
                    0.25 * yp3 / yp2 * sinI1 * epp / (1.0 + n3) * (3.0 - epp2 * (3.0 - epp2)) +
                    0.078125 * yp5 / yp2 * n2 * cC * qyp52quotient * sinI1;


            final double qyp24 = 3.0 * yp2 * yp2 * (11.0 + 80.0 * cosI2 / sinI1 + 200.0 * cosI4 / sinI2) -
                                 10.0 * yp4 * (3.0 + 16.0 * cosI2 / sinI1 + 40.0 * cosI4 / sinI2);
            dh2sgcg = 35.0 / 144.0 * yp5 / yp2 * qyp52;
            dhsgcg = -epp2 * cosI1 / (12.0 * yp2) * qyp24;
            dhcg = -35.0 / 576.0 * yp5 / yp2 * qyp52 +
                   epp * cosI1 / (4.0 * yp2 * sinI1) * (yp3 + 0.3125 * yp5 * (4.0 + 3.0 * epp2) * cC) +
                   1.875 / (4.0 * yp2) * yp5 * qyp52bis;

            // short periodic multiplicative
            aC = -yp2 * C3c2 * app / n3;
            aCbis = y2 * app * C3c2;
            ac2g2f = y2 * app * 3.0 * sinI2;

            double qe = 0.5 * n2 * y2 * C3c2 / n6;
            eC = qe * epp / (1.0 + n3) * (3.0 - epp2 * (3.0 - epp2));
            ecf = 3.0 * qe;
            e2cf = 3.0 * epp * qe;
            e3cf = epp2 * qe;
            qe = 0.5 * n2 * y2 * 3.0 * (1.0 - cosI2) / n6;
            ec2f2g = qe * epp;
            ecfc2f2g = 3.0 * qe;
            e2cfc2f2g = 3.0 * epp * qe;
            e3cfc2f2g = epp2 * qe;
            qe = -0.5 * yp2 * n2 * (1.0 - cosI2);
            ec2gf = 3.0 * qe;
            ec2g3f = qe;

            double qi = epp * yp2 * cosI1 * sinI1;
            ide = -epp * cosI1 / (n2 * sinI1);
            isfs2f2g = qi;
            icfc2f2g = 2.0 * qi;
            qi = yp2 * cosI1 * sinI1;
            ic2f2g = 1.5 * qi;

            double qgl1 = 0.25 * yp2;
            double qgl2 = 0.25 * yp2 * epp * n2 / (1.0 + n);
            glf = qgl1 * -6.0 * C5c2;
            gll = qgl1 * 6.0 * C5c2;
            glsf = qgl1 * -6.0 * C5c2 * epp +
                   qgl2 * 2.0 * C3c2;
            glosf = qgl2 * 2.0 * C3c2;
            qgl1 = qgl1 * (3.0 - 5.0 * cosI2);
            qgl2 = qgl2 * 3.0 * (1.0 - cosI2);
            gls2f2g = 3.0 * qgl1;
            gls2gf = 3.0 * epp * qgl1 +
                     qgl2;
            glos2gf = -1.0 * qgl2;
            gls2g3f = qgl1 * epp +
                      1.0 / 3.0 * qgl2;
            glos2g3f = qgl2;

            final double qh = 3.0 * yp2 * cosI1;
            hf = -qh;
            hl = qh;
            hsf = -epp * qh;
            hcfs2g2f = 2.0 * epp * yp2 * cosI1;
            hs2g2f = 1.5 * yp2 * cosI1;
            hsfc2g2f = -epp * yp2 * cosI1;

            final double qedl = -0.25 * yp2 * n3;
            edls2g = 1.0 / 24.0 * epp * n3 / yp2 * qyp2_4;
            edlcg = -0.25 * yp3 / yp2 * n3 * sinI1 -
                    0.078125 * yp5 / yp2 * n3 * sinI1 * (4.0 + 9.0 * epp2) * cC;
            edlc3g = 35.0 / 384.0 * yp5 / yp2 * n3 * epp2 * cD * sinI1;
            edlsf = 2.0 * qedl * C3c2;
            edls2gf = 3.0 * qedl * (1.0 - cosI2);
            edls2g3f = 1.0 / 3.0 * qedl;

            // secular rates of the mean semi-major axis and eccentricity
            // Eq. 2.41 and Eq. 2.45 of Phipps' 1992 thesis
            aRate = -4.0 * app / (3.0 * xnotDot);
            eRate = -4.0 * epp * n * n / (3.0 * xnotDot);

        }

        /**
         * Accurate computation of E - e sin(E).
         *
         * @param E eccentric anomaly
         * @return E - e sin(E)
         */
        private UnivariateDerivative2 eMeSinE(final UnivariateDerivative2 E) {
            UnivariateDerivative2 x = E.sin().multiply(1 - mean.getE());
            final UnivariateDerivative2 mE2 = E.negate().multiply(E);
            UnivariateDerivative2 term = E;
            UnivariateDerivative2 d    = E.getField().getZero();
            // the inequality test below IS intentional and should NOT be replaced by a check with a small tolerance
            for (UnivariateDerivative2 x0 = d.add(Double.NaN); !Double.valueOf(x.getValue()).equals(Double.valueOf(x0.getValue()));) {
                d = d.add(2);
                term = term.multiply(mE2.divide(d.multiply(d.add(1))));
                x0 = x;
                x = x.subtract(term);
            }
            return x;
        }

        /**
         * Gets eccentric anomaly from mean anomaly.
         * <p>The algorithm used to solve the Kepler equation has been published in:
         * "Procedures for  solving Kepler's Equation", A. W. Odell and R. H. Gooding,
         * Celestial Mechanics 38 (1986) 307-334</p>
         * <p>It has been copied from the OREKIT library (KeplerianOrbit class).</p>
         *
         * @param mk the mean anomaly (rad)
         * @return the eccentric anomaly (rad)
         */
        private UnivariateDerivative2 getEccentricAnomaly(final UnivariateDerivative2 mk) {
            final double k1 = 3 * FastMath.PI + 2;
            final double k2 = FastMath.PI - 1;
            final double k3 = 6 * FastMath.PI - 1;
            final double A  = 3.0 * k2 * k2 / k1;
            final double B  = k3 * k3 / (6.0 * k1);
            // reduce M to [-PI PI] interval
            final UnivariateDerivative2 reducedM = new UnivariateDerivative2(MathUtils.normalizeAngle(mk.getValue(), 0.0),
                                                                             mk.getFirstDerivative(),
                                                                             mk.getSecondDerivative());

            // compute start value according to A. W. Odell and R. H. Gooding S12 starter
            UnivariateDerivative2 ek;
            if (FastMath.abs(reducedM.getValue()) < 1.0 / 6.0) {
                if (FastMath.abs(reducedM.getValue()) < Precision.SAFE_MIN) {
                    // this is an Orekit change to the S12 starter.
                    // If reducedM is 0.0, the derivative of cbrt is infinite which induces NaN appearing later in
                    // the computation. As in this case E and M are almost equal, we initialize ek with reducedM
                    ek = reducedM;
                } else {
                    // this is the standard S12 starter
                    ek = reducedM.add(reducedM.multiply(6).cbrt().subtract(reducedM).multiply(mean.getE()));
                }
            } else {
                if (reducedM.getValue() < 0) {
                    final UnivariateDerivative2 w = reducedM.add(FastMath.PI);
                    ek = reducedM.add(w.multiply(-A).divide(w.subtract(B)).subtract(FastMath.PI).subtract(reducedM).multiply(mean.getE()));
                } else {
                    final UnivariateDerivative2 minusW = reducedM.subtract(FastMath.PI);
                    ek = reducedM.add(minusW.multiply(A).divide(minusW.add(B)).add(FastMath.PI).subtract(reducedM).multiply(mean.getE()));
                }
            }

            final double e1 = 1 - mean.getE();
            final boolean noCancellationRisk = (e1 + ek.getValue() * ek.getValue() / 6) >= 0.1;

            // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
            for (int j = 0; j < 2; ++j) {
                final UnivariateDerivative2 f;
                UnivariateDerivative2 fd;
                final UnivariateDerivative2 fdd  = ek.sin().multiply(mean.getE());
                final UnivariateDerivative2 fddd = ek.cos().multiply(mean.getE());
                if (noCancellationRisk) {
                    f  = ek.subtract(fdd).subtract(reducedM);
                    fd = fddd.subtract(1).negate();
                } else {
                    f  = eMeSinE(ek).subtract(reducedM);
                    final UnivariateDerivative2 s = ek.multiply(0.5).sin();
                    fd = s.multiply(s).multiply(2 * mean.getE()).add(e1);
                }
                final UnivariateDerivative2 dee = f.multiply(fd).divide(f.multiply(0.5).multiply(fdd).subtract(fd.multiply(fd)));

                // update eccentric anomaly, using expressions that limit underflow problems
                final UnivariateDerivative2 w = fd.add(dee.multiply(0.5).multiply(fdd.add(dee.multiply(fdd).divide(3))));
                fd = fd.add(dee.multiply(fdd.add(dee.multiply(0.5).multiply(fdd))));
                ek = ek.subtract(f.subtract(dee.multiply(fd.subtract(w))).divide(fd));
            }

            // expand the result back to original range
            ek = ek.add(mk.getValue() - reducedM.getValue());

            // Returns the eccentric anomaly
            return ek;
        }

        /**
         * This method is used in Brouwer-Lyddane model to avoid singularity at the
         * critical inclination (i = 63.4°).
         * <p>
         * This method, based on Warren Phipps's 1992 thesis (Eq. 2.47 and 2.48),
         * approximate the factor (1.0 - 5.0 * cos²(inc))^-1 (causing the singularity)
         * by a function, named T2 in the thesis.
         * </p>
         * @param cosInc cosine of the mean inclination
         * @return an approximation of (1.0 - 5.0 * cos²(inc))^-1 term
         */
        private double T2(final double cosInc) {

            // X = (1.0 - 5.0 * cos²(inc))
            final double x  = 1.0 - 5.0 * cosInc * cosInc;
            final double x2 = x * x;

            // Eq. 2.48
            double sum = 0.0;
            for (int i = 0; i <= 12; i++) {
                final double sign = i % 2 == 0 ? +1.0 : -1.0;
                sum += sign * FastMath.pow(BETA, i) * FastMath.pow(x2, i) / CombinatoricsUtils.factorialDouble(i + 1);
            }

            // Right term of equation 2.47
            double product = 1.0;
            for (int i = 0; i <= 10; i++) {
                product *= 1 + FastMath.exp(FastMath.scalb(-1.0, i) * BETA * x2);
            }

            // Return (Eq. 2.47)
            return BETA * x * sum * product;

        }

        /** Extrapolate an orbit up to a specific target date.
         * @param date target date for the orbit
         * @return propagated parameters
         */
        public KeplerianOrbit propagateParameters(final AbsoluteDate date) {

            // Empirical drag coefficient M2
            final double m2 = M2Driver.getValue();

            // Keplerian evolution
            final UnivariateDerivative2 dt = new UnivariateDerivative2(date.durationFrom(mean.getDate()), 1.0, 0.0);
            final UnivariateDerivative2 xnot = dt.multiply(xnotDot);

            //____________________________________
            // secular effects

            // mean mean anomaly (with drag Eq. 2.38 of Phipps' 1992 thesis)
            final UnivariateDerivative2 dtM2  = dt.multiply(m2);
            final UnivariateDerivative2 dt2M2 = dt.multiply(dtM2);
            final UnivariateDerivative2 lpp = new UnivariateDerivative2(MathUtils.normalizeAngle(mean.getMeanAnomaly() + lt * xnot.getValue() + dt2M2.getValue(), 0),
                                                                      lt * xnotDot + 2.0 * dtM2.getValue(),
                                                                      2.0 * m2);
            // mean argument of perigee
            final UnivariateDerivative2 gpp = new UnivariateDerivative2(MathUtils.normalizeAngle(mean.getPerigeeArgument() + gt * xnot.getValue(), 0),
                                                                      gt * xnotDot,
                                                                      0.0);
            // mean longitude of ascending node
            final UnivariateDerivative2 hpp = new UnivariateDerivative2(MathUtils.normalizeAngle(mean.getRightAscensionOfAscendingNode() + ht * xnot.getValue(), 0),
                                                                      ht * xnotDot,
                                                                      0.0);

            // ________________________________________________
            // secular rates of the mean semi-major axis and eccentricity

            // semi-major axis
            final UnivariateDerivative2 appDrag = dt.multiply(aRate * m2);

            // eccentricity
            final UnivariateDerivative2 eppDrag = dt.multiply(eRate * m2);

            //____________________________________
            // Long periodical terms
            final UnivariateDerivative2 cg1 = gpp.cos();
            final UnivariateDerivative2 sg1 = gpp.sin();
            final UnivariateDerivative2 c2g = cg1.multiply(cg1).subtract(sg1.multiply(sg1));
            final UnivariateDerivative2 s2g = cg1.multiply(sg1).add(sg1.multiply(cg1));
            final UnivariateDerivative2 c3g = c2g.multiply(cg1).subtract(s2g.multiply(sg1));
            final UnivariateDerivative2 sg2 = sg1.multiply(sg1);
            final UnivariateDerivative2 sg3 = sg1.multiply(sg2);


            // de eccentricity
            final UnivariateDerivative2 d1e = sg3.multiply(dei3sg).
                                              add(sg1.multiply(deisg)).
                                              add(sg2.multiply(de2sg)).
                                              add(de);

            // l' + g'
            final UnivariateDerivative2 lp_p_gp = s2g.multiply(dlgs2g).
                                               add(c3g.multiply(dlgc3g)).
                                               add(cg1.multiply(dlgcg)).
                                               add(lpp).
                                               add(gpp);

            // h'
            final UnivariateDerivative2 hp = sg2.multiply(cg1).multiply(dh2sgcg).
                                               add(sg1.multiply(cg1).multiply(dhsgcg)).
                                               add(cg1.multiply(dhcg)).
                                               add(hpp);

            // Short periodical terms
            // eccentric anomaly
            final UnivariateDerivative2 Ep = getEccentricAnomaly(lpp);
            final UnivariateDerivative2 cf1 = (Ep.cos().subtract(mean.getE())).divide(Ep.cos().multiply(-mean.getE()).add(1.0));
            final UnivariateDerivative2 sf1 = (Ep.sin().multiply(n)).divide(Ep.cos().multiply(-mean.getE()).add(1.0));
            final UnivariateDerivative2 f = FastMath.atan2(sf1, cf1);

            final UnivariateDerivative2 c2f = cf1.multiply(cf1).subtract(sf1.multiply(sf1));
            final UnivariateDerivative2 s2f = cf1.multiply(sf1).add(sf1.multiply(cf1));
            final UnivariateDerivative2 c3f = c2f.multiply(cf1).subtract(s2f.multiply(sf1));
            final UnivariateDerivative2 s3f = c2f.multiply(sf1).add(s2f.multiply(cf1));
            final UnivariateDerivative2 cf2 = cf1.multiply(cf1);
            final UnivariateDerivative2 cf3 = cf1.multiply(cf2);

            final UnivariateDerivative2 c2g1f = cf1.multiply(c2g).subtract(sf1.multiply(s2g));
            final UnivariateDerivative2 c2g2f = c2f.multiply(c2g).subtract(s2f.multiply(s2g));
            final UnivariateDerivative2 c2g3f = c3f.multiply(c2g).subtract(s3f.multiply(s2g));
            final UnivariateDerivative2 s2g1f = cf1.multiply(s2g).add(c2g.multiply(sf1));
            final UnivariateDerivative2 s2g2f = c2f.multiply(s2g).add(c2g.multiply(s2f));
            final UnivariateDerivative2 s2g3f = c3f.multiply(s2g).add(c2g.multiply(s3f));

            final UnivariateDerivative2 eE = (Ep.cos().multiply(-mean.getE()).add(1.0)).reciprocal();
            final UnivariateDerivative2 eE3 = eE.multiply(eE).multiply(eE);
            final UnivariateDerivative2 sigma = eE.multiply(n * n).multiply(eE).add(eE);

            // Semi-major axis (with drag Eq. 2.41 of Phipps' 1992 thesis)
            final UnivariateDerivative2 a = eE3.multiply(aCbis).add(appDrag.add(mean.getA())).
                                            add(aC).
                                            add(eE3.multiply(c2g2f).multiply(ac2g2f));

            // Eccentricity (with drag Eq. 2.45 of Phipps' 1992 thesis)
            final UnivariateDerivative2 e = d1e.add(eppDrag.add(mean.getE())).
                                            add(eC).
                                            add(cf1.multiply(ecf)).
                                            add(cf2.multiply(e2cf)).
                                            add(cf3.multiply(e3cf)).
                                            add(c2g2f.multiply(ec2f2g)).
                                            add(c2g2f.multiply(cf1).multiply(ecfc2f2g)).
                                            add(c2g2f.multiply(cf2).multiply(e2cfc2f2g)).
                                            add(c2g2f.multiply(cf3).multiply(e3cfc2f2g)).
                                            add(c2g1f.multiply(ec2gf)).
                                            add(c2g3f.multiply(ec2g3f));

            // Inclination
            final UnivariateDerivative2 i = d1e.multiply(ide).
                                            add(mean.getI()).
                                            add(sf1.multiply(s2g2f.multiply(isfs2f2g))).
                                            add(cf1.multiply(c2g2f.multiply(icfc2f2g))).
                                            add(c2g2f.multiply(ic2f2g));

            // Argument of perigee + True anomaly
            final UnivariateDerivative2 g_p_l = lp_p_gp.add(f.multiply(glf)).
                                                add(lpp.multiply(gll)).
                                                add(sf1.multiply(glsf)).
                                                add(sigma.multiply(sf1).multiply(glosf)).
                                                add(s2g2f.multiply(gls2f2g)).
                                                add(s2g1f.multiply(gls2gf)).
                                                add(sigma.multiply(s2g1f).multiply(glos2gf)).
                                                add(s2g3f.multiply(gls2g3f)).
                                                add(sigma.multiply(s2g3f).multiply(glos2g3f));


            // Longitude of ascending node
            final UnivariateDerivative2 h = hp.add(f.multiply(hf)).
                                            add(lpp.multiply(hl)).
                                            add(sf1.multiply(hsf)).
                                            add(cf1.multiply(s2g2f).multiply(hcfs2g2f)).
                                            add(s2g2f.multiply(hs2g2f)).
                                            add(c2g2f.multiply(sf1).multiply(hsfc2g2f));

            final UnivariateDerivative2 edl = s2g.multiply(edls2g).
                                            add(cg1.multiply(edlcg)).
                                            add(c3g.multiply(edlc3g)).
                                            add(sf1.multiply(edlsf)).
                                            add(s2g1f.multiply(edls2gf)).
                                            add(s2g3f.multiply(edls2g3f)).
                                            add(sf1.multiply(sigma).multiply(edlsf)).
                                            add(s2g1f.multiply(sigma).multiply(-edls2gf)).
                                            add(s2g3f.multiply(sigma).multiply(3.0 * edls2g3f));

            final UnivariateDerivative2 A = e.multiply(lpp.cos()).subtract(edl.multiply(lpp.sin()));
            final UnivariateDerivative2 B = e.multiply(lpp.sin()).add(edl.multiply(lpp.cos()));

            // True anomaly
            final UnivariateDerivative2 l = FastMath.atan2(B, A);

            // Argument of perigee
            final UnivariateDerivative2 g = g_p_l.subtract(l);

            // Return a Keplerian orbit
            return new KeplerianOrbit(a.getValue(), e.getValue(), i.getValue(),
                                      g.getValue(), h.getValue(), l.getValue(),
                                      a.getFirstDerivative(), e.getFirstDerivative(), i.getFirstDerivative(),
                                      g.getFirstDerivative(), h.getFirstDerivative(), l.getFirstDerivative(),
                                      PositionAngleType.MEAN, mean.getFrame(), date, mu);

        }

    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) {
        return models.get(date).mass;
    }

}

