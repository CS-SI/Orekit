/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianAnomalyUtility;
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
 * @see "Solomon, Daniel, THE NAVSPASUR Satellite Motion Model,
 *       Naval Research Laboratory, August 8, 1991."
 *
 * @author Melina Vanel
 * @author Bryan Cazabonne
 * @author Pascal Parraud
 * @since 11.1
 */
public class BrouwerLyddanePropagator extends AbstractAnalyticalPropagator implements ParameterDriversProvider {

    /** Parameter name for M2 coefficient. */
    public static final String M2_NAME = "M2";

    /** Default value for M2 coefficient. */
    public static final double M2 = 0.0;

    /** Default convergence threshold for mean parameters conversion. */
    public static final double EPSILON_DEFAULT = 1.0e-13;

    /** Default value for maxIterations. */
    public static final int MAX_ITERATIONS_DEFAULT = 200;

    /** Default value for damping. */
    public static final double DAMPING_DEFAULT = 1.0;

    /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double SCALE = FastMath.scalb(1.0, -32);

    /** Beta constant used by T2 function. */
    private static final double BETA = FastMath.scalb(100, -11);

    /** Max value for the eccentricity. */
    private static final double MAX_ECC = 0.999999;

    /** Initial Brouwer-Lyddane model. */
    private BLModel initialModel;

    /** All models. */
    private transient TimeSpanMap<BLModel> models;

    /** Reference radius of the central body attraction model (m). */
    private final double referenceRadius;

    /** Central attraction coefficient (m³/s²). */
    private final double mu;

    /** Un-normalized zonal coefficients. */
    private final double[] ck0;

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
    private BrouwerLyddanePropagator(final Orbit initialOrbit,
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
    private BrouwerLyddanePropagator(final Orbit initialOrbit,
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
    @Override
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
        this.models = new TimeSpanMap<>(initialModel);
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

    /** Compute mean parameters according to the Brouwer-Lyddane analytical model,
     * using an intermediate equinoctial orbit to avoid singularities.
     * @param osculating osculating orbit
     * @param mass constant mass
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @return Brouwer-Lyddane mean model
     */
    private BLModel computeMeanParameters(final KeplerianOrbit osculating, final double mass,
                                          final double epsilon, final int maxIterations) {

        // damping factor
        final double damping = DAMPING_DEFAULT;

        // sanity check
        if (osculating.getA() < referenceRadius) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE,
                                           osculating.getA());
        }

        // rough initialization of the mean parameters
        BLModel current = new BLModel(osculating, mass, referenceRadius, mu, ck0);

        // Get equinoctial parameters
        double sma = osculating.getA();
        double ex  = osculating.getEquinoctialEx();
        double ey  = osculating.getEquinoctialEy();
        double hx  = osculating.getHx();
        double hy  = osculating.getHy();
        double lv  = osculating.getLv();

        // threshold for each parameter
        final double thresholdA  = epsilon * (1 + FastMath.abs(osculating.getA()));
        final double thresholdE  = epsilon * (1 + FastMath.hypot(ex, ey));
        final double thresholdH  = epsilon * (1 + FastMath.hypot(hx, hy));
        final double thresholdLv = epsilon * FastMath.PI;

        int i = 0;
        while (i++ < maxIterations) {

            // recompute the osculating parameters from the current mean parameters
            final KeplerianOrbit parameters = current.propagateParameters(current.mean.getDate());

            // adapted parameters residuals
            final double deltaA  = osculating.getA() - parameters.getA();
            final double deltaEx = osculating.getEquinoctialEx() - parameters.getEquinoctialEx();
            final double deltaEy = osculating.getEquinoctialEy() - parameters.getEquinoctialEy();
            final double deltaHx = osculating.getHx() - parameters.getHx();
            final double deltaHy = osculating.getHy() - parameters.getHy();
            final double deltaLv = MathUtils.normalizeAngle(osculating.getLv() - parameters.getLv(), 0.0);

            // update state
            sma += damping * deltaA;
            ex  += damping * deltaEx;
            ey  += damping * deltaEy;
            hx  += damping * deltaHx;
            hy  += damping * deltaHy;
            lv  += damping * deltaLv;

            // Update mean orbit
            final EquinoctialOrbit mean = new EquinoctialOrbit(sma, ex, ey, hx, hy, lv,
                                                               PositionAngleType.TRUE,
                                                               osculating.getFrame(),
                                                               osculating.getDate(),
                                                               osculating.getMu());
            final KeplerianOrbit meanOrb = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(mean);

            // update mean parameters
            current = new BLModel(meanOrb, mass, referenceRadius, mu, ck0);

            // check convergence
            if (FastMath.abs(deltaA)  < thresholdA &&
                FastMath.abs(deltaEx) < thresholdE &&
                FastMath.abs(deltaEy) < thresholdE &&
                FastMath.abs(deltaHx) < thresholdH &&
                FastMath.abs(deltaHy) < thresholdH &&
                FastMath.abs(deltaLv) < thresholdLv) {
                return current;
            }
        }
        throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_BROUWER_LYDDANE_MEAN_PARAMETERS, i);
    }


    /** {@inheritDoc} */
    public KeplerianOrbit propagateOrbit(final AbsoluteDate date) {
        // compute Keplerian parameters, taking derivatives into account
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
    @Override
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

        /** Constant mass. */
        private final double mass;

        /** Brouwer-Lyddane mean orbit. */
        private final KeplerianOrbit mean;

        // Preprocessed values

        /** Mean mean motion: n0 = √(μ/a")/a". */
        private final double n0;

        /** η = √(1 - e"²). */
        private final double n;
        /** η². */
        private final double n2;
        /** η³. */
        private final double n3;
        /** η + 1 / (1 + η). */
        private final double t8;

        /** Secular correction for mean anomaly l: &delta;<sub>s</sub>l. */
        private final double dsl;
        /** Secular correction for perigee argument g: &delta;<sub>s</sub>g. */
        private final double dsg;
        /** Secular correction for raan h: &delta;<sub>s</sub>h. */
        private final double dsh;

        /** Secular rate of change of semi-major axis due to drag. */
        private final double aRate;
        /** Secular rate of change of eccentricity due to drag. */
        private final double eRate;

        // CHECKSTYLE: stop JavadocVariable check

        // Storage for speed-up
        private final double yp2;
        private final double ci;
        private final double si;
        private final double oneMci2;
        private final double ci2X3M1;

        // Long periodic corrections factors
        private final double vle1;
        private final double vle2;
        private final double vle3;
        private final double vli1;
        private final double vli2;
        private final double vli3;
        private final double vll2;
        private final double vlh1I;
        private final double vlh2I;
        private final double vlh3I;
        private final double vls1;
        private final double vls2;
        private final double vls3;

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

            this.mass = mass;

            // mean orbit
            this.mean = mean;

            // mean eccentricity e"
            final double epp = mean.getE();
            if (epp >= 1) {
                // Only for elliptical (e < 1) orbits
                throw new OrekitException(OrekitMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL,
                                          epp);
            }
            final double epp2 = epp * epp;

            // η
            n2 = 1. - epp2;
            n  = FastMath.sqrt(n2);
            n3 = n2 * n;
            t8 = n + 1. / (1. + n);

            // mean semi-major axis a"
            final double app = mean.getA();

            // mean mean motion
            n0 = FastMath.sqrt(mu / app) / app;

            // ae/a"
            final double q = referenceRadius / app;

            // γ2'
            double ql = q * q;
            double nl = n2 * n2;
            yp2 = -0.5 * ck0[2] * ql / nl;
            final double yp22 = yp2 * yp2;

            // γ3'
            ql *= q;
            nl *= n2;
            final double yp3 = ck0[3] * ql / nl;

            // γ4'
            ql *= q;
            nl *= n2;
            final double yp4 = 0.375 * ck0[4] * ql / nl;

            // γ5'
            ql *= q;
            nl *= n2;
            final double yp5 = ck0[5] * ql / nl;

            // mean inclination I" sin & cos
            final SinCos sci = FastMath.sinCos(mean.getI());
            si = sci.sin();
            ci = sci.cos();
            final double ci2 = ci * ci;
            oneMci2 = 1.0 - ci2;
            ci2X3M1 = 3.0 * ci2 - 1.0;
            final double ci2X5M1 = 5.0 * ci2 - 1.0;

            // secular corrections
            // true anomaly
            dsl = 1.5 * yp2 * n * (ci2X3M1 +
                                   0.0625 * yp2 * (-15.0 + n * (16.0 + 25.0 * n) +
                                                   ci2 * (30.0 - n * (96.0 + 90.0 * n) +
                                                          ci2 * (105.0 + n * (144.0 + 25.0 * n))))) +
                  0.9375 * yp4 * n * epp2 * (3.0 - ci2 * (30.0 - 35.0 * ci2));
            // perigee argument
            dsg = 1.5 * yp2 * ci2X5M1 +
                  0.09375 * yp22 * (-35.0 + n * (24.0 + 25.0 * n) +
                                    ci2 * (90.0 - n * (192.0 + 126.0 * n) +
                                           ci2 * (385.0 + n * (360.0 + 45.0 * n)))) +
                  0.3125 * yp4 * (21.0 - 9.0 * n2 + ci2 * (-270.0 + 126.0 * n2 +
                                                           ci2 * (385.0 - 189.0 * n2)));
            // right ascension of ascending node
            dsh = (-3.0 * yp2 +
                   0.375 * yp22 * (-5.0 + n * (12.0 + 9.0 * n) -
                                   ci2 * (35.0 + n * (36.0 + 5.0 * n))) +
                   1.25 * yp4 * (5.0 - 3.0 * n2) * (3.0 - 7.0 * ci2)) * ci;

            // secular rates of change due to drag
            // Eq. 2.41 and Eq. 2.45 of Phipps' 1992 thesis
            final double coef = -4.0 / (3.0 * n0 * (1 + dsl));
            aRate = coef * app;
            eRate = coef * epp * n2;

            // singular term 1/(1 - 5 * cos²(I")) replaced by T2 function
            final double t2 = T2(ci);

            // factors for long periodic corrections
            final double fs12 = yp3 / yp2;
            final double fs13 = 10. * yp4 / (3. * yp2);
            final double fs14 = yp5 / yp2;

            final double ci2Xt2 = ci2 * t2;
            final double cA = 1. - ci2 * (11. +  40. * ci2Xt2);
            final double cB = 1. - ci2 * ( 3. +   8. * ci2Xt2);
            final double cC = 1. - ci2 * ( 9. +  24. * ci2Xt2);
            final double cD = 1. - ci2 * ( 5. +  16. * ci2Xt2);
            final double cE = 1. - ci2 * (33. + 200. * ci2Xt2);
            final double cF = 1. - ci2 * ( 9. +  40. * ci2Xt2);

            final double p5p   = 1. + ci2Xt2 * (8. + 20 * ci2Xt2);
            final double p5p2  = 1. +  2. * p5p;
            final double p5p4  = 1. +  4. * p5p;
            final double p5p10 = 1. + 10. * p5p;

            final double e2X3P4  = 4. + 3. * epp2;
            final double ciO1Pci = ci / (1. + ci);

            final double q1 = 0.125 * (yp2 * cA - fs13 * cB);
            final double q2 = 0.125 * epp2 * ci * (yp2 * p5p10 - fs13 * p5p2);
            final double q5 = 0.25 * (fs12 + 0.3125 * e2X3P4 * fs14 * cC);
            final double p2 = 0.46875 * p5p2 * epp * ci * si * e2X3P4 * fs14;
            final double p3 = 0.15625 * epp * si * fs14 * cC;
            final double kf = 35. / 1152.;
            final double p4 = kf * epp * fs14 * cD;
            final double p5 = 2. * kf * epp * epp2 * ci * si * fs14 * p5p4;

            vle1 = epp * n2 * q1;
            vle2 = n2 * si * q5;
            vle3 = -3.0 * epp * n2 * si * p4;

            vli1 = -epp * q1 / si;
            vli2 = -epp * ci * q5;
            vli3 = -3.0 * epp2 * ci * p4;

            vll2 = vle2 + 3.0 * epp * n2 * p3;

            vlh1I = -si * q2;
            vlh2I =  epp * ci * q5 + si * p2;
            vlh3I = -epp2 * ci * p4 - si * p5;

            vls1 = (n3 - 1.0) * q1 -
                   q2 +
                   25.0 * epp2 * ci2 * ci2Xt2 * ci2Xt2 * (yp2 - 0.2 * fs13) -
                   0.0625 * epp2 * (yp2 * cE - fs13 * cF);

            vls2 = epp * si * (t8 + ciO1Pci) * q5 +
                   (11.0 + 3.0 * (epp2 - n3)) * p3 +
                   (1.0 - ci) * p2;

            vls3 = si * p4 * (3.0 * (n3 - 1.0) - epp2 * (2.0 + ciO1Pci)) -
                   (1.0 - ci) * p5;
        }

        /**
         * Get true anomaly from mean anomaly.
         * @param lM the mean anomaly (rad)
         * @param ecc the eccentricity
         * @return the true anomaly (rad)
         */
        private UnivariateDerivative1 getTrueAnomaly(final UnivariateDerivative1 lM,
                                                     final UnivariateDerivative1 ecc) {
            // reduce M to [-PI PI] interval
            final double reducedM = MathUtils.normalizeAngle(lM.getValue(), 0.);

            // compute the true anomaly
            UnivariateDerivative1 lV = FieldKeplerianAnomalyUtility.ellipticMeanToTrue(ecc, lM);

            // expand the result back to original range
            lV = lV.add(lM.getValue() - reducedM);

            // Returns the true anomaly
            return lV;
        }

        /**
         * This method is used in Brouwer-Lyddane model to avoid singularity at the
         * critical inclination (i = 63.4°).
         * <p>
         * This method, based on Warren Phipps's 1992 thesis (Eq. 2.47 and 2.48),
         * approximate the factor (1.0 - 5.0 * cos²(i))<sup>-1</sup> (causing the singularity)
         * by a function, named T2 in the thesis.
         * </p>
         * @param cosI cosine of the mean inclination
         * @return an approximation of (1.0 - 5.0 * cos²(i))<sup>-1</sup> term
         */
        private double T2(final double cosI) {

            // X = 1.0 - 5.0 * cos²(i)
            final double x  = 1.0 - 5.0 * cosI * cosI;
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
            final UnivariateDerivative1 dt  = new UnivariateDerivative1(date.durationFrom(mean.getDate()), 1.0);
            final UnivariateDerivative1 not = dt.multiply(n0);

            final UnivariateDerivative1 dtM2  = dt.multiply(m2);
            final UnivariateDerivative1 dt2M2 = dt.multiply(dtM2);

            // Secular corrections
            // -------------------

            // semi-major axis (with drag Eq. 2.41 of Phipps' 1992 thesis)
            final UnivariateDerivative1 app = dtM2.multiply(aRate).add(mean.getA());

            // eccentricity  (with drag Eq. 2.45 of Phipps' 1992 thesis) reduced to [0, 1[
            final UnivariateDerivative1 tmp = dtM2.multiply(eRate).add(mean.getE());
            final UnivariateDerivative1 epp = tmp.withValue(FastMath.max(0., FastMath.min(tmp.getValue(), MAX_ECC)));

            // argument of perigee
            final double gppVal = mean.getPerigeeArgument() + dsg * not.getValue();
            final UnivariateDerivative1 gpp = new UnivariateDerivative1(MathUtils.normalizeAngle(gppVal, 0.),
                                                                        dsg * n0);

            // longitude of ascending node
            final double hppVal = mean.getRightAscensionOfAscendingNode() + dsh * not.getValue();
            final UnivariateDerivative1 hpp = new UnivariateDerivative1(MathUtils.normalizeAngle(hppVal, 0.),
                                                                        dsh * n0);

            // mean anomaly (with drag Eq. 2.38 of Phipps' 1992 thesis)
            final double lppVal = mean.getMeanAnomaly() + (1. + dsl) * not.getValue() + dt2M2.getValue();
            final double dlppdt = (1. + dsl) * n0 + 2.0 * dtM2.getValue();
            final UnivariateDerivative1 lpp = new UnivariateDerivative1(MathUtils.normalizeAngle(lppVal, 0.),
                                                                        dlppdt);

            // Long period corrections
            //------------------------
            final FieldSinCos<UnivariateDerivative1> scgpp = gpp.sinCos();
            final UnivariateDerivative1 cgpp  = scgpp.cos();
            final UnivariateDerivative1 sgpp  = scgpp.sin();
            final FieldSinCos<UnivariateDerivative1> sc2gpp = gpp.multiply(2).sinCos();
            final UnivariateDerivative1 c2gpp  = sc2gpp.cos();
            final UnivariateDerivative1 s2gpp  = sc2gpp.sin();
            final FieldSinCos<UnivariateDerivative1> sc3gpp = gpp.multiply(3).sinCos();
            final UnivariateDerivative1 c3gpp  = sc3gpp.cos();
            final UnivariateDerivative1 s3gpp  = sc3gpp.sin();

            // δ1e
            final UnivariateDerivative1 d1e = c2gpp.multiply(vle1).
                                              add(sgpp.multiply(vle2)).
                                              add(s3gpp.multiply(vle3));

            // δ1I
            UnivariateDerivative1 d1I = sgpp.multiply(vli2).
                                        add(s3gpp.multiply(vli3));
            // Pseudo singular term, not to add if Ipp is zero
            if (Double.isFinite(vli1)) {
                d1I = d1I.add(c2gpp.multiply(vli1));
            }

            // e"δ1l
            final UnivariateDerivative1 eppd1l = s2gpp.multiply(vle1).
                                                 subtract(cgpp.multiply(vll2)).
                                                 subtract(c3gpp.multiply(vle3)).
                                                 multiply(n);

            // δ1h
            final UnivariateDerivative1 sIppd1h = s2gpp.multiply(vlh1I).
                                                  add(cgpp.multiply(vlh2I)).
                                                  add(c3gpp.multiply(vlh3I));

            // δ1z = δ1l + δ1g + δ1h
            final UnivariateDerivative1 d1z = s2gpp.multiply(vls1).
                                              add(cgpp.multiply(vls2)).
                                              add(c3gpp.multiply(vls3));

            // Short period corrections
            // ------------------------

            // true anomaly
            final UnivariateDerivative1 fpp = getTrueAnomaly(lpp, epp);
            final FieldSinCos<UnivariateDerivative1> scfpp = fpp.sinCos();
            final UnivariateDerivative1 cfpp = scfpp.cos();
            final UnivariateDerivative1 sfpp = scfpp.sin();

            // e"sin(f')
            final UnivariateDerivative1 eppsfpp = epp.multiply(sfpp);
            // e"cos(f')
            final UnivariateDerivative1 eppcfpp = epp.multiply(cfpp);
            // 1 + e"cos(f')
            final UnivariateDerivative1 eppcfppP1 = eppcfpp.add(1.);
            // 2 + e"cos(f')
            final UnivariateDerivative1 eppcfppP2 = eppcfpp.add(2.);
            // 3 + e"cos(f')
            final UnivariateDerivative1 eppcfppP3 = eppcfpp.add(3.);
            // (1 + e"cos(f'))³
            final UnivariateDerivative1 eppcfppP1_3 = eppcfppP1.square().multiply(eppcfppP1);

            // 2g"
            final UnivariateDerivative1 g2 = gpp.multiply(2);

            // 2g" + f"
            final UnivariateDerivative1 g2f = g2.add(fpp);
            final FieldSinCos<UnivariateDerivative1> sc2gf = g2f.sinCos();
            final UnivariateDerivative1 c2gf = sc2gf.cos();
            final UnivariateDerivative1 s2gf = sc2gf.sin();
            final UnivariateDerivative1 eppc2gf = epp.multiply(c2gf);
            final UnivariateDerivative1 epps2gf = epp.multiply(s2gf);

            // 2g" + 2f"
            final UnivariateDerivative1 g2f2 = g2.add(fpp.multiply(2));
            final FieldSinCos<UnivariateDerivative1> sc2g2f = g2f2.sinCos();
            final UnivariateDerivative1 c2g2f = sc2g2f.cos();
            final UnivariateDerivative1 s2g2f = sc2g2f.sin();

            // 2g" + 3f"
            final UnivariateDerivative1 g2f3 = g2.add(fpp.multiply(3));
            final FieldSinCos<UnivariateDerivative1> sc2g3f = g2f3.sinCos();
            final UnivariateDerivative1 c2g3f = sc2g3f.cos();
            final UnivariateDerivative1 s2g3f = sc2g3f.sin();

            // e"cos(2g" + 3f")
            final UnivariateDerivative1 eppc2g3f = epp.multiply(c2g3f);
            // e"sin(2g" + 3f")
            final UnivariateDerivative1 epps2g3f = epp.multiply(s2g3f);

            // f" + e"sin(f") - l"
            final UnivariateDerivative1 w17 = fpp.add(eppsfpp).subtract(lpp);

            // ((e"cos(f") + 3)e"cos(f") + 3)cos(f")
            final UnivariateDerivative1 w20 = cfpp.multiply(eppcfppP3.multiply(eppcfpp).add(3.));

            // 3sin(2g" + 2f") + 3e"sin(2g" + f") + e"sin(2g" + f")
            final UnivariateDerivative1 w21 = s2g2f.add(epps2gf).multiply(3).add(epps2g3f);

            // (1 + e"cos(f"))(2 + e"cos(f"))/η²
            final UnivariateDerivative1 w22 = eppcfppP1.multiply(eppcfppP2).divide(n2);

            // sinCos(I"/2)
            final SinCos sci = FastMath.sinCos(0.5 * mean.getI());
            final double siO2 = sci.sin();
            final double ciO2 = sci.cos();

            // δ2a
            final UnivariateDerivative1 d2a = app.multiply(yp2 / n2).
                                                  multiply(eppcfppP1_3.subtract(n3).multiply(ci2X3M1).
                                                           add(eppcfppP1_3.multiply(c2g2f).multiply(3 * oneMci2)));

            // δ2e
            final UnivariateDerivative1 d2e = (w20.add(epp.multiply(t8))).multiply(ci2X3M1).
                                               add((w20.add(epp.multiply(c2g2f))).multiply(3 * oneMci2)).
                                               subtract((eppc2gf.multiply(3).add(eppc2g3f)).multiply(n2 * oneMci2)).
                                              multiply(0.5 * yp2);

            // δ2I
            final UnivariateDerivative1 d2I = ((c2g2f.add(eppc2gf)).multiply(3).add(eppc2g3f)).
                                              multiply(0.5 * yp2 * ci * si);

            // e"δ2l
            final UnivariateDerivative1 eppd2l = (w22.add(1).multiply(sfpp).multiply(2 * oneMci2).
                                                  add((w22.subtract(1).negate().multiply(s2gf)).
                                                       add(w22.add(1. / 3.).multiply(s2g3f)).
                                                      multiply(3 * oneMci2))).
                                                 multiply(0.25 * yp2 * n3).negate();

            // sinI"δ2h
            final UnivariateDerivative1 sIppd2h = (w21.subtract(w17.multiply(6))).
                                                  multiply(0.5 * yp2 * ci * si);

            // δ2z = δ2l + δ2g + δ2h
            final UnivariateDerivative1 d2z = (epp.multiply(eppd2l).multiply(t8 - 1.).divide(n3).
                                               add(w17.multiply(6. * (1. + ci * (2 - 5. * ci)))
                                                   .subtract(w21.multiply(3. + ci * (2 - 5. * ci))).multiply(0.25 * yp2))).
                                               negate();

            // Assembling elements
            // -------------------

            // e" + δe
            final UnivariateDerivative1 de = epp.add(d1e).add(d2e);

            // e"δl
            final UnivariateDerivative1 dl = eppd1l.add(eppd2l);

            // sin(I"/2)δh = sin(I")δh / cos(I"/2) (singular for I" = π, very unlikely)
            final UnivariateDerivative1 dh = sIppd1h.add(sIppd2h).divide(2. * ciO2);

            // δI
            final UnivariateDerivative1 di = d1I.add(d2I).multiply(0.5 * ciO2).add(siO2);

            // z = l" + g" + h" + δ1z + δ2z
            final UnivariateDerivative1 z = lpp.add(gpp).add(hpp).add(d1z).add(d2z);

            // Osculating elements
            // -------------------

            // Semi-major axis
            final UnivariateDerivative1 a = app.add(d2a);

            // Eccentricity
            final UnivariateDerivative1 e = FastMath.sqrt(de.square().add(dl.square()));

            // Mean anomaly
            final FieldSinCos<UnivariateDerivative1> sclpp = lpp.sinCos();
            final UnivariateDerivative1 clpp = sclpp.cos();
            final UnivariateDerivative1 slpp = sclpp.sin();
            final UnivariateDerivative1 l = FastMath.atan2(de.multiply(slpp).add(dl.multiply(clpp)),
                                                           de.multiply(clpp).subtract(dl.multiply(slpp)));

            // Inclination
            final UnivariateDerivative1 i = FastMath.acos(di.square().add(dh.square()).multiply(2).negate().add(1.));

            // Longitude of ascending node
            final FieldSinCos<UnivariateDerivative1> schpp = hpp.sinCos();
            final UnivariateDerivative1 chpp = schpp.cos();
            final UnivariateDerivative1 shpp = schpp.sin();
            final UnivariateDerivative1 h = FastMath.atan2(di.multiply(shpp).add(dh.multiply(chpp)),
                                                           di.multiply(chpp).subtract(dh.multiply(shpp)));

            // Argument of perigee
            final UnivariateDerivative1 g = z.subtract(l).subtract(h);

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

