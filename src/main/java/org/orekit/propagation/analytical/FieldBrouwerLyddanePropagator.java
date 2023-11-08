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

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTimeSpanMap;
import org.orekit.utils.ParameterDriver;

/** This class propagates a {@link org.orekit.propagation.FieldSpacecraftState}
 *  using the analytical Brouwer-Lyddane model (from J2 to J5 zonal harmonics).
 * <p>
 * At the opposite of the {@link FieldEcksteinHechlerPropagator}, the Brouwer-Lyddane model is
 * suited for elliptical orbits, there is no problem having a rather small eccentricity or inclination
 * (Lyddane helped to solve this issue with the Brouwer model). Singularity for the critical
 * inclination i = 63.4° is avoided using the method developed in Warren Phipps' 1992 thesis.
 * <p>
 * By default, Brouwer-Lyddane model considers only the perturbations due to zonal harmonics.
 * However, for low Earth orbits, the magnitude of the perturbative acceleration due to
 * atmospheric drag can be significant. Warren Phipps' 1992 thesis considered the atmospheric
 * drag by time derivatives of the <i>mean</i> mean anomaly using the catch-all coefficient
 * {@link #M2Driver}.
 *
 * Usually, M2 is adjusted during an orbit determination process and it represents the
 * combination of all unmodeled secular along-track effects (i.e. not just the atmospheric drag).
 * The behavior of M2 is close to the {@link FieldTLE#getBStar()} parameter for the TLE.
 *
 * If the value of M2 is equal to {@link BrouwerLyddanePropagator#M2 0.0}, the along-track secular
 * effects are not considered in the dynamical model. Typical values for M2 are not known.
 * It depends on the orbit type. However, the value of M2 must be very small (e.g. between 1.0e-14 and 1.0e-15).
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
 * @param <T> type of the field elements
 */
public class FieldBrouwerLyddanePropagator<T extends CalculusFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {

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
    private static final double SCALE = FastMath.scalb(1.0, -20);

    /** Beta constant used by T2 function. */
    private static final double BETA = FastMath.scalb(100, -11);

    /** Initial Brouwer-Lyddane model. */
    private FieldBLModel<T> initialModel;

    /** All models. */
    private transient FieldTimeSpanMap<FieldBLModel<T>, T> models;

    /** Reference radius of the central body attraction model (m). */
    private double referenceRadius;

    /** Central attraction coefficient (m³/s²). */
    private T mu;

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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, UnnormalizedSphericalHarmonicsProvider, PropagationType, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final double M2) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             initialOrbit.getMu().newInstance(DEFAULT_MASS), provider,
             provider.onDate(initialOrbit.getDate().toAbsoluteDate()), M2);
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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement,
     * UnnormalizedSphericalHarmonicsProvider, UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics, PropagationType, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitude,
                                         final T mass,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final UnnormalizedSphericalHarmonics harmonics,
                                         final double M2) {
        this(initialOrbit, attitude,  mass, provider.getAe(), initialOrbit.getMu().newInstance(provider.getMu()),
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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see org.orekit.utils.Constants
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, AttitudeProvider, double, CalculusFieldElement, double, double, double, double, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final double referenceRadius, final T mu,
                                         final double c20, final double c30, final double c40,
                                         final double c50, final double M2) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             initialOrbit.getMu().newInstance(DEFAULT_MASS),
             referenceRadius, mu, c20, c30, c40, c50, M2);
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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement, UnnormalizedSphericalHarmonicsProvider, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit, final T mass,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final double M2) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             mass, provider, provider.onDate(initialOrbit.getDate().toAbsoluteDate()), M2);
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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement, double, CalculusFieldElement, double, double, double, double, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit, final T mass,
                                         final double referenceRadius, final T mu,
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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final double M2) {
        this(initialOrbit, attitudeProv, initialOrbit.getMu().newInstance(DEFAULT_MASS), provider,
             provider.onDate(initialOrbit.getDate().toAbsoluteDate()), M2);
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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final double referenceRadius, final T mu,
                                         final double c20, final double c30, final double c40,
                                         final double c50, final double M2) {
        this(initialOrbit, attitudeProv, initialOrbit.getMu().newInstance(DEFAULT_MASS),
             referenceRadius, mu, c20, c30, c40, c50, M2);
    }

    /** Build a propagator from orbit, attitude provider, mass and potential provider.
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement, UnnormalizedSphericalHarmonicsProvider, PropagationType, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final T mass,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final double M2) {
        this(initialOrbit, attitudeProv, mass, provider, provider.onDate(initialOrbit.getDate().toAbsoluteDate()), M2);
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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement, double, CalculusFieldElement, double, double, double, double, PropagationType, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final T mass,
                                         final double referenceRadius, final T mu,
                                         final double c20, final double c30, final double c40,
                                         final double c50, final double M2) {
        this(initialOrbit, attitudeProv, mass, referenceRadius, mu, c20, c30, c40, c50, PropagationType.OSCULATING, M2);
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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final PropagationType initialType,
                                         final double M2) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             initialOrbit.getMu().newInstance(DEFAULT_MASS), provider,
             provider.onDate(initialOrbit.getDate().toAbsoluteDate()), initialType, M2);
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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final T mass,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final PropagationType initialType,
                                         final double M2) {
        this(initialOrbit, attitudeProv, mass, provider, provider.onDate(initialOrbit.getDate().toAbsoluteDate()), initialType, M2);
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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitude,
                                         final T mass,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final UnnormalizedSphericalHarmonics harmonics,
                                         final PropagationType initialType,
                                         final double M2) {
        this(initialOrbit, attitude, mass, provider.getAe(), initialOrbit.getMu().newInstance(provider.getMu()),
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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final T mass,
                                         final double referenceRadius, final T mu,
                                         final double c20, final double c30, final double c40,
                                         final double c50,
                                         final PropagationType initialType,
                                         final double M2) {
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
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @since 11.2
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final T mass,
                                         final double referenceRadius, final T mu,
                                         final double c20, final double c30, final double c40,
                                         final double c50,
                                         final PropagationType initialType,
                                         final double M2, final double epsilon, final int maxIterations) {

        super(mass.getField(), attitudeProv);

        // store model coefficients
        this.referenceRadius = referenceRadius;
        this.mu  = mu;
        this.ck0 = new double[] {0.0, 0.0, c20, c30, c40, c50};

        // initialize M2 driver
        this.M2Driver = new ParameterDriver(BrouwerLyddanePropagator.M2_NAME, M2, SCALE,
                                            Double.NEGATIVE_INFINITY,
                                            Double.POSITIVE_INFINITY);

        // compute mean parameters if needed
        resetInitialState(new FieldSpacecraftState<>(initialOrbit,
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
     * @param <T> type of the filed elements
     * @param osculating osculating orbit to convert
     * @param provider for un-normalized zonal coefficients
     * @param harmonics {@code provider.onDate(osculating.getDate())}
     * @param M2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@code BrouwerLyddanePropagator.M2} drag is not considered
     * @return mean orbit in a Brouwer-Lyddane sense
     * @since 11.2
     */
    public static <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> computeMeanOrbit(final FieldOrbit<T> osculating,
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
     * @param <T> type of the filed elements
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
    public static <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> computeMeanOrbit(final FieldOrbit<T> osculating,
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
     * @param <T> type of the filed elements
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
    public static <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> computeMeanOrbit(final FieldOrbit<T> osculating,
                                                                                             final double referenceRadius, final double mu,
                                                                                             final double c20, final double c30, final double c40,
                                                                                             final double c50, final double M2Value,
                                                                                             final double epsilon, final int maxIterations) {
        final FieldBrouwerLyddanePropagator<T> propagator =
                        new FieldBrouwerLyddanePropagator<>(osculating,
                                                            FrameAlignedProvider.of(osculating.getFrame()),
                                                            osculating.getMu().newInstance(DEFAULT_MASS),
                                                            referenceRadius, osculating.getMu().newInstance(mu),
                                                            c20, c30, c40, c50,
                                                            PropagationType.OSCULATING, M2Value,
                                                            epsilon, maxIterations);
        return propagator.initialModel.mean;
    }

    /** {@inheritDoc}
     * <p>The new initial state to consider
     * must be defined with an osculating orbit.</p>
     * @see #resetInitialState(FieldSpacecraftState, PropagationType)
     */
    @Override
    public void resetInitialState(final FieldSpacecraftState<T> state) {
        resetInitialState(state, PropagationType.OSCULATING);
    }

    /** Reset the propagator initial state.
     * @param state new initial state to consider
     * @param stateType mean Brouwer-Lyddane orbit or osculating orbit
     */
    public void resetInitialState(final FieldSpacecraftState<T> state, final PropagationType stateType) {
        resetInitialState(state, stateType, EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT);
    }

    /** Reset the propagator initial state.
     * @param state new initial state to consider
     * @param stateType mean Brouwer-Lyddane orbit or osculating orbit
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @since 11.2
     */
    public void resetInitialState(final FieldSpacecraftState<T> state, final PropagationType stateType,
                                  final double epsilon, final int maxIterations) {
        super.resetInitialState(state);
        final FieldKeplerianOrbit<T> keplerian = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(state.getOrbit());
        this.initialModel = (stateType == PropagationType.MEAN) ?
                             new FieldBLModel<>(keplerian, state.getMass(), referenceRadius, mu, ck0) :
                             computeMeanParameters(keplerian, state.getMass(), epsilon, maxIterations);
        this.models = new FieldTimeSpanMap<FieldBLModel<T>, T>(initialModel, state.getA().getField());
    }

    /** {@inheritDoc} */
    @Override
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward) {
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
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward,
                                          final double epsilon, final int maxIterations) {
        final FieldBLModel<T> newModel = computeMeanParameters((FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(state.getOrbit()),
                                                               state.getMass(), epsilon, maxIterations);
        if (forward) {
            models.addValidAfter(newModel, state.getDate());
        } else {
            models.addValidBefore(newModel, state.getDate());
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
    private FieldBLModel<T> computeMeanParameters(final FieldKeplerianOrbit<T> osculating, final T mass,
                                                  final double epsilon, final int maxIterations) {

        // sanity check
        if (osculating.getA().getReal() < referenceRadius) {
            throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE,
                                           osculating.getA());
        }

        final Field<T> field = mass.getField();
        final T one = field.getOne();
        final T zero = field.getZero();
        // rough initialization of the mean parameters
        FieldBLModel<T> current = new FieldBLModel<T>(osculating, mass, referenceRadius, mu, ck0);

        // threshold for each parameter
        final T thresholdA      = current.mean.getA().abs().add(1.0).multiply(epsilon);
        final T thresholdE      = current.mean.getE().add(1.0).multiply(epsilon);
        final T thresholdAngles = one.getPi().multiply(epsilon);

        int i = 0;
        while (i++ < maxIterations) {

            // recompute the osculating parameters from the current mean parameters
            final FieldKeplerianOrbit<T> parameters = current.propagateParameters(current.mean.getDate(), getParameters(mass.getField(), current.mean.getDate()));

            // adapted parameters residuals
            final T deltaA     = osculating.getA()  .subtract(parameters.getA());
            final T deltaE     = osculating.getE()  .subtract(parameters.getE());
            final T deltaI     = osculating.getI()  .subtract(parameters.getI());
            final T deltaOmega = MathUtils.normalizeAngle(osculating.getPerigeeArgument().subtract(parameters.getPerigeeArgument()), zero);
            final T deltaRAAN  = MathUtils.normalizeAngle(osculating.getRightAscensionOfAscendingNode().subtract(parameters.getRightAscensionOfAscendingNode()), zero);
            final T deltaAnom  = MathUtils.normalizeAngle(osculating.getMeanAnomaly().subtract(parameters.getMeanAnomaly()), zero);


            // update mean parameters
            current = new FieldBLModel<T>(new FieldKeplerianOrbit<T>(current.mean.getA()            .add(deltaA),
                                                     FastMath.max(current.mean.getE().add(deltaE), zero),
                                                     current.mean.getI()                            .add(deltaI),
                                                     current.mean.getPerigeeArgument()              .add(deltaOmega),
                                                     current.mean.getRightAscensionOfAscendingNode().add(deltaRAAN),
                                                     current.mean.getMeanAnomaly()                  .add(deltaAnom),
                                                     PositionAngleType.MEAN,
                                                     current.mean.getFrame(),
                                                     current.mean.getDate(), mu),
                                  mass, referenceRadius, mu, ck0);
            // check convergence
            if (FastMath.abs(deltaA.getReal())     < thresholdA.getReal() &&
                FastMath.abs(deltaE.getReal())     < thresholdE.getReal() &&
                FastMath.abs(deltaI.getReal())     < thresholdAngles.getReal() &&
                FastMath.abs(deltaOmega.getReal()) < thresholdAngles.getReal() &&
                FastMath.abs(deltaRAAN.getReal())  < thresholdAngles.getReal() &&
                FastMath.abs(deltaAnom.getReal())  < thresholdAngles.getReal()) {
                return current;
            }
        }
        throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_BROUWER_LYDDANE_MEAN_PARAMETERS, i);
    }


    /** {@inheritDoc} */
    public FieldKeplerianOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date, final T[] parameters) {
        // compute Cartesian parameters, taking derivatives into account
        final FieldBLModel<T> current = models.get(date);
        return current.propagateParameters(date, parameters);
    }

    /**
     * Get the value of the M2 drag parameter.
     * @return the value of the M2 drag parameter
     */
    public double getM2() {
        return M2Driver.getValue();
    }

    /**
     * Get the value of the M2 drag parameter.
     * @param date date at which the model parameters want to be known
     * @return the value of the M2 drag parameter
     */
    public double getM2(final AbsoluteDate date) {
        return M2Driver.getValue(date);
    }

    /** Local class for Brouwer-Lyddane model. */
    private static class FieldBLModel<T extends CalculusFieldElement<T>> {

        /** Mean orbit. */
        private final FieldKeplerianOrbit<T> mean;

        /** Constant mass. */
        private final T mass;

        /** Central attraction coefficient. */
        private final T mu;

        // CHECKSTYLE: stop JavadocVariable check

        // preprocessed values

        // Constant for secular terms l'', g'', h''
        // l standing for true anomaly, g for perigee argument and h for raan
        private final T xnotDot;
        private final T n;
        private final T lt;
        private final T gt;
        private final T ht;


        // Long periodT
        private final T dei3sg;
        private final T de2sg;
        private final T deisg;
        private final T de;


        private final T dlgs2g;
        private final T dlgc3g;
        private final T dlgcg;


        private final T dh2sgcg;
        private final T dhsgcg;
        private final T dhcg;


        // Short perioTs
        private final T aC;
        private final T aCbis;
        private final T ac2g2f;


        private final T eC;
        private final T ecf;
        private final T e2cf;
        private final T e3cf;
        private final T ec2f2g;
        private final T ecfc2f2g;
        private final T e2cfc2f2g;
        private final T e3cfc2f2g;
        private final T ec2gf;
        private final T ec2g3f;


        private final T ide;
        private final T isfs2f2g;
        private final T icfc2f2g;
        private final T ic2f2g;


        private final T glf;
        private final T gll;
        private final T glsf;
        private final T glosf;
        private final T gls2f2g;
        private final T gls2gf;
        private final T glos2gf;
        private final T gls2g3f;
        private final T glos2g3f;


        private final T hf;
        private final T hl;
        private final T hsf;
        private final T hcfs2g2f;
        private final T hs2g2f;
        private final T hsfc2g2f;


        private final T edls2g;
        private final T edlcg;
        private final T edlc3g;
        private final T edlsf;
        private final T edls2gf;
        private final T edls2g3f;

        // Drag terms
        private final T aRate;
        private final T eRate;

        // CHECKSTYLE: resume JavadocVariable check

        /** Create a model for specified mean orbit.
         * @param mean mean Fieldorbit
         * @param mass constant mass
         * @param referenceRadius reference radius of the central body attraction model (m)
         * @param mu central attraction coefficient (m³/s²)
         * @param ck0 un-normalized zonal coefficients
         */
        FieldBLModel(final FieldKeplerianOrbit<T> mean, final T mass,
                final double referenceRadius, final T mu, final double[] ck0) {

            this.mean = mean;
            this.mass = mass;
            this.mu   = mu;
            final T one  = mass.getField().getOne();

            final T app = mean.getA();
            xnotDot = mu.divide(app).sqrt().divide(app);

            // preliminary processing
            final T q = app.divide(referenceRadius).reciprocal();
            T ql = q.multiply(q);
            final T y2 = ql.multiply(-0.5 * ck0[2]);

            n = ((mean.getE().multiply(mean.getE()).negate()).add(1.0)).sqrt();
            final T n2 = n.multiply(n);
            final T n3 = n2.multiply(n);
            final T n4 = n2.multiply(n2);
            final T n6 = n4.multiply(n2);
            final T n8 = n4.multiply(n4);
            final T n10 = n8.multiply(n2);

            final T yp2 = y2.divide(n4);
            ql = ql.multiply(q);
            final T yp3 = ql.multiply(ck0[3]).divide(n6);
            ql = ql.multiply(q);
            final T yp4 = ql.multiply(0.375 * ck0[4]).divide(n8);
            ql = ql.multiply(q);
            final T yp5 = ql.multiply(ck0[5]).divide(n10);


            final FieldSinCos<T> sc = FastMath.sinCos(mean.getI());
            final T sinI1 = sc.sin();
            final T sinI2 = sinI1.multiply(sinI1);
            final T cosI1 = sc.cos();
            final T cosI2 = cosI1.multiply(cosI1);
            final T cosI3 = cosI2.multiply(cosI1);
            final T cosI4 = cosI2.multiply(cosI2);
            final T cosI6 = cosI4.multiply(cosI2);
            final T C5c2 = T2(cosI1).reciprocal();
            final T C3c2 = cosI2.multiply(3.0).subtract(1.0);

            final T epp = mean.getE();
            final T epp2 = epp.multiply(epp);
            final T epp3 = epp2.multiply(epp);
            final T epp4 = epp2.multiply(epp2);

            if (epp.getReal() >= 1) {
                // Only for elliptical (e < 1) orbits
                throw new OrekitException(OrekitMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL,
                                          mean.getE().getReal());
            }

            // secular multiplicative
            lt = one.add(yp2.multiply(n).multiply(C3c2).multiply(1.5)).
                     add(yp2.multiply(0.09375).multiply(yp2).multiply(n).multiply(n2.multiply(25.0).add(n.multiply(16.0)).add(-15.0).
                             add(cosI2.multiply(n2.multiply(-90.0).add(n.multiply(-96.0)).add(30.0))).
                             add(cosI4.multiply(n2.multiply(25.0).add(n.multiply(144.0)).add(105.0))))).
                     add(yp4.multiply(0.9375).multiply(n).multiply(epp2).multiply(cosI4.multiply(35.0).add(cosI2.multiply(-30.0)).add(3.0)));

            gt = yp2.multiply(-1.5).multiply(C5c2).
                     add(yp2.multiply(0.09375).multiply(yp2).multiply(n2.multiply(25.0).add(n.multiply(24.0)).add(-35.0).
                            add(cosI2.multiply(n2.multiply(-126.0).add(n.multiply(-192.0)).add(90.0))).
                            add(cosI4.multiply(n2.multiply(45.0).add(n.multiply(360.0)).add(385.0))))).
                     add(yp4.multiply(0.3125).multiply(n2.multiply(-9.0).add(21.0).
                            add(cosI2.multiply(n2.multiply(126.0).add(-270.0))).
                            add(cosI4.multiply(n2.multiply(-189.0).add(385.0)))));

            ht = yp2.multiply(-3.0).multiply(cosI1).
                     add(yp2.multiply(0.375).multiply(yp2).multiply(cosI1.multiply(n2.multiply(9.0).add(n.multiply(12.0)).add(-5.0)).
                                                                    add(cosI3.multiply(n2.multiply(-5.0).add(n.multiply(-36.0)).add(-35.0))))).
                     add(yp4.multiply(1.25).multiply(cosI1).multiply(n2.multiply(-3.0).add(5.0)).multiply(cosI2.multiply(-7.0).add(3.0)));

            final T cA = one.subtract(cosI2.multiply(11.0)).subtract(cosI4.multiply(40.0).divide(C5c2));
            final T cB = one.subtract(cosI2.multiply(3.0)).subtract(cosI4.multiply(8.0).divide(C5c2));
            final T cC = one.subtract(cosI2.multiply(9.0)).subtract(cosI4.multiply(24.0).divide(C5c2));
            final T cD = one.subtract(cosI2.multiply(5.0)).subtract(cosI4.multiply(16.0).divide(C5c2));

            final T qyp2_4 = yp2.multiply(3.0).multiply(yp2).multiply(cA).
                             subtract(yp4.multiply(10.0).multiply(cB));
            final T qyp52 = cosI1.multiply(epp3).multiply(cD.multiply(0.5).divide(sinI1).
                                                          add(sinI1.multiply(cosI4.divide(C5c2).divide(C5c2).multiply(80.0).
                                                                             add(cosI2.divide(C5c2).multiply(32.0).
                                                                             add(5.0)))));
            final T qyp22 = epp2.add(2.0).subtract(epp2.multiply(3.0).add(2.0).multiply(11.0).multiply(cosI2)).
                            subtract(epp2.multiply(5.0).add(2.0).multiply(40.0).multiply(cosI4.divide(C5c2))).
                            subtract(epp2.multiply(400.0).multiply(cosI6).divide(C5c2).divide(C5c2));
            final T qyp42 = one.divide(5.0).multiply(qyp22.
                                                     add(one.multiply(4.0).multiply(epp2.
                                                                                    add(2.0).
                                                                                    subtract(cosI2.multiply(epp2.multiply(3.0).add(2.0))))));
            final T qyp52bis = cosI1.multiply(sinI1).multiply(epp).multiply(epp2.multiply(3.0).add(4.0)).
                                                                   multiply(cosI4.divide(C5c2).divide(C5c2).multiply(40.0).
                                                                            add(cosI2.divide(C5c2).multiply(16.0)).
                                                                            add(3.0));
           // long periodic multiplicative
            dei3sg =  yp5.divide(yp2).multiply(35.0 / 96.0).multiply(epp2).multiply(n2).multiply(cD).multiply(sinI1);
            de2sg = qyp2_4.divide(yp2).multiply(epp).multiply(n2).multiply(-1.0 / 12.0);
            deisg = sinI1.multiply(yp5.multiply(-35.0 / 128.0).divide(yp2).multiply(epp2).multiply(n2).multiply(cD).
                            add(n2.multiply(0.25).divide(yp2).multiply(yp3.add(yp5.multiply(5.0 / 16.0).multiply(epp2.multiply(3.0).add(4.0)).multiply(cC)))));
            de = epp2.multiply(n2).multiply(qyp2_4).divide(24.0).divide(yp2);

            final T qyp52quotient = epp.multiply(epp4.multiply(81.0).add(-32.0)).divide(n.multiply(epp2.multiply(9.0).add(4.0)).add(epp2.multiply(3.0)).add(4.0));
            dlgs2g = yp2.multiply(48.0).reciprocal().multiply(yp2.multiply(-3.0).multiply(yp2).multiply(qyp22).
                            add(yp4.multiply(10.0).multiply(qyp42))).
                            add(n3.divide(yp2).multiply(qyp2_4).divide(24.0));
            dlgc3g =  yp5.multiply(35.0 / 384.0).divide(yp2).multiply(n3).multiply(epp).multiply(cD).multiply(sinI1).
                            add(yp5.multiply(35.0 / 1152.0).divide(yp2).multiply(qyp52.multiply(2.0).multiply(cosI1).subtract(epp.multiply(cD).multiply(sinI1).multiply(epp2.multiply(2.0).add(3.0)))));
            dlgcg = yp3.negate().multiply(cosI2).multiply(epp).divide(yp2.multiply(sinI1).multiply(4.0)).
                    add(yp5.divide(yp2).multiply(0.078125).multiply(cC).multiply(cosI2.divide(sinI1).multiply(epp.negate()).multiply(epp2.multiply(3.0).add(4.0)).
                                                                    add(sinI1.multiply(epp2).multiply(epp2.multiply(9.0).add(26.0)))).
                    subtract(yp5.divide(yp2).multiply(0.46875).multiply(qyp52bis).multiply(cosI1)).
                    add(yp3.divide(yp2).multiply(0.25).multiply(sinI1).multiply(epp).divide(n3.add(1.0)).multiply(epp2.multiply(epp2.subtract(3.0)).add(3.0))).
                    add(yp5.divide(yp2).multiply(0.078125).multiply(n2).multiply(cC).multiply(qyp52quotient).multiply(sinI1)));
            final T qyp24 = yp2.multiply(yp2).multiply(3.0).multiply(cosI4.divide(sinI2).multiply(200.0).add(cosI2.divide(sinI1).multiply(80.0)).add(11.0)).
                            subtract(yp4.multiply(10.0).multiply(cosI4.divide(sinI2).multiply(40.0).add(cosI2.divide(sinI1).multiply(16.0)).add(3.0)));
            dh2sgcg = yp5.divide(yp2).multiply(qyp52).multiply(35.0 / 144.0);
            dhsgcg = qyp24.multiply(cosI1).multiply(epp2.negate()).divide(yp2.multiply(12.0));
            dhcg = yp5.divide(yp2).multiply(qyp52).multiply(-35.0 / 576.0).
                   add(cosI1.multiply(epp).divide(yp2.multiply(sinI1).multiply(4.0)).multiply(yp3.add(yp5.multiply(0.3125).multiply(cC).multiply(epp2.multiply(3.0).add(4.0))))).
                   add(yp5.multiply(qyp52bis).multiply(1.875).divide(yp2.multiply(4.0)));

            // short periodic multiplicative
            aC = yp2.negate().multiply(C3c2).multiply(app).divide(n3);
            aCbis = y2.multiply(app).multiply(C3c2);
            ac2g2f = y2.multiply(app).multiply(sinI2).multiply(3.0);

            T qe = y2.multiply(C3c2).multiply(0.5).multiply(n2).divide(n6);
            eC = qe.multiply(epp).divide(n3.add(1.0)).multiply(epp2.multiply(epp2.subtract(3.0)).add(3.0));
            ecf = qe.multiply(3.0);
            e2cf = qe.multiply(3.0).multiply(epp);
            e3cf = qe.multiply(epp2);
            qe = y2.multiply(0.5).multiply(n2).multiply(3.0).multiply(cosI2.negate().add(1.0)).divide(n6);
            ec2f2g = qe.multiply(epp);
            ecfc2f2g = qe.multiply(3.0);
            e2cfc2f2g = qe.multiply(3.0).multiply(epp);
            e3cfc2f2g = qe.multiply(epp2);
            qe = yp2.multiply(-0.5).multiply(n2).multiply(cosI2.negate().add(1.0));
            ec2gf = qe.multiply(3.0);
            ec2g3f = qe;

            T qi = yp2.multiply(epp).multiply(cosI1).multiply(sinI1);
            ide = cosI1.multiply(epp.negate()).divide(sinI1.multiply(n2));
            isfs2f2g = qi;
            icfc2f2g = qi.multiply(2.0);
            qi = yp2.multiply(cosI1).multiply(sinI1);
            ic2f2g = qi.multiply(1.5);

            T qgl1 = yp2.multiply(0.25);
            T qgl2 =  yp2.multiply(epp).multiply(n2).multiply(0.25).divide(n.add(1.0));
            glf = qgl1.multiply(C5c2).multiply(-6.0);
            gll = qgl1.multiply(C5c2).multiply(6.0);
            glsf = qgl1.multiply(C5c2).multiply(-6.0).multiply(epp).
                   add(qgl2.multiply(C3c2).multiply(2.0));
            glosf = qgl2.multiply(C3c2).multiply(2.0);
            qgl1 = qgl1.multiply(cosI2.multiply(-5.0).add(3.0));
            qgl2 = qgl2.multiply(3.0).multiply(cosI2.negate().add(1.0));
            gls2f2g = qgl1.multiply(3.0);
            gls2gf = qgl1.multiply(3.0).multiply(epp).
                     add(qgl2);
            glos2gf = qgl2.negate();
            gls2g3f = qgl1.multiply(epp).
                      add(qgl2.multiply(1.0 / 3.0));
            glos2g3f = qgl2;

            final T qh = yp2.multiply(cosI1).multiply(3.0);
            hf = qh.negate();
            hl = qh;
            hsf = qh.multiply(epp).negate();
            hcfs2g2f = yp2.multiply(cosI1).multiply(epp).multiply(2.0);
            hs2g2f = yp2.multiply(cosI1).multiply(1.5);
            hsfc2g2f = yp2.multiply(cosI1).multiply(epp).negate();

            final T qedl = yp2.multiply(n3).multiply(-0.25);
            edls2g = qyp2_4.multiply(1.0 / 24.0).multiply(epp).multiply(n3).divide(yp2);
            edlcg = yp3.divide(yp2).multiply(-0.25).multiply(n3).multiply(sinI1).
                    subtract(yp5.divide(yp2).multiply(0.078125).multiply(n3).multiply(sinI1).multiply(cC).multiply(epp2.multiply(9.0).add(4.0)));
            edlc3g = yp5.divide(yp2).multiply(n3).multiply(epp2).multiply(cD).multiply(sinI1).multiply(35.0 / 384.0);
            edlsf = qedl.multiply(C3c2).multiply(2.0);
            edls2gf = qedl.multiply(3.0).multiply(cosI2.negate().add(1.0));
            edls2g3f = qedl.multiply(1.0 / 3.0);

            // secular rates of the mean semi-major axis and eccentricity
            // Eq. 2.41 and Eq. 2.45 of Phipps' 1992 thesis
            aRate = app.multiply(-4.0).divide(xnotDot.multiply(3.0));
            eRate = epp.multiply(n).multiply(n).multiply(-4.0).divide(xnotDot.multiply(3.0));

        }

        /**
         * Accurate computation of E - e sin(E).
         *
         * @param E eccentric anomaly
         * @return E - e sin(E)
         */
        private FieldUnivariateDerivative2<T> eMeSinE(final FieldUnivariateDerivative2<T> E) {
            FieldUnivariateDerivative2<T> x = E.sin().multiply(mean.getE().negate().add(1.0));
            final FieldUnivariateDerivative2<T> mE2 = E.negate().multiply(E);
            FieldUnivariateDerivative2<T> term = E;
            FieldUnivariateDerivative2<T> d    = E.getField().getZero();
            // the inequality test below IS intentional and should NOT be replaced by a check with a small tolerance
            for (FieldUnivariateDerivative2<T> x0 = d.add(Double.NaN); !Double.valueOf(x.getValue().getReal()).equals(Double.valueOf(x0.getValue().getReal()));) {
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
        private FieldUnivariateDerivative2<T> getEccentricAnomaly(final FieldUnivariateDerivative2<T> mk) {
            final double k1 = 3 * FastMath.PI + 2;
            final double k2 = FastMath.PI - 1;
            final double k3 = 6 * FastMath.PI - 1;
            final double A  = 3.0 * k2 * k2 / k1;
            final double B  = k3 * k3 / (6.0 * k1);

            final T zero = mean.getE().getField().getZero();

            // reduce M to [-PI PI] interval
            final FieldUnivariateDerivative2<T> reducedM = new FieldUnivariateDerivative2<T>(MathUtils.normalizeAngle(mk.getValue(), zero ),
                                                                             mk.getFirstDerivative(),
                                                                             mk.getSecondDerivative());

            // compute start value according to A. W. Odell and R. H. Gooding S12 starter
            FieldUnivariateDerivative2<T> ek;
            if (reducedM.getValue().abs().getReal() < 1.0 / 6.0) {
                if (reducedM.getValue().abs().getReal() < Precision.SAFE_MIN) {
                    // this is an Orekit change to the S12 starter.
                    // If reducedM is 0.0, the derivative of cbrt is infinite which induces NaN appearing later in
                    // the computation. As in this case E and M are almost equal, we initialize ek with reducedM
                    ek = reducedM;
                } else {
                    // this is the standard S12 starter
                    ek = reducedM.add(reducedM.multiply(6).cbrt().subtract(reducedM).multiply(mean.getE()));
                }
            } else {
                if (reducedM.getValue().getReal() < 0) {
                    final FieldUnivariateDerivative2<T> w = reducedM.add(FastMath.PI);
                    ek = reducedM.add(w.multiply(-A).divide(w.subtract(B)).subtract(FastMath.PI).subtract(reducedM).multiply(mean.getE()));
                } else {
                    final FieldUnivariateDerivative2<T> minusW = reducedM.subtract(FastMath.PI);
                    ek = reducedM.add(minusW.multiply(A).divide(minusW.add(B)).add(FastMath.PI).subtract(reducedM).multiply(mean.getE()));
                }
            }

            final T e1 = mean.getE().negate().add(1.0);
            final boolean noCancellationRisk = (e1.add(ek.getValue().multiply(ek.getValue())).getReal() / 6) >= 0.1;

            // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
            for (int j = 0; j < 2; ++j) {
                final FieldUnivariateDerivative2<T> f;
                FieldUnivariateDerivative2<T> fd;
                final FieldUnivariateDerivative2<T> fdd  = ek.sin().multiply(mean.getE());
                final FieldUnivariateDerivative2<T> fddd = ek.cos().multiply(mean.getE());
                if (noCancellationRisk) {
                    f  = ek.subtract(fdd).subtract(reducedM);
                    fd = fddd.subtract(1).negate();
                } else {
                    f  = eMeSinE(ek).subtract(reducedM);
                    final FieldUnivariateDerivative2<T> s = ek.multiply(0.5).sin();
                    fd = s.multiply(s).multiply(mean.getE().multiply(2.0)).add(e1);
                }
                final FieldUnivariateDerivative2<T> dee = f.multiply(fd).divide(f.multiply(0.5).multiply(fdd).subtract(fd.multiply(fd)));

                // update eccentric anomaly, using expressions that limit underflow problems
                final FieldUnivariateDerivative2<T> w = fd.add(dee.multiply(0.5).multiply(fdd.add(dee.multiply(fdd).divide(3))));
                fd = fd.add(dee.multiply(fdd.add(dee.multiply(0.5).multiply(fdd))));
                ek = ek.subtract(f.subtract(dee.multiply(fd.subtract(w))).divide(fd));
            }

            // expand the result back to original range
            ek = ek.add(mk.getValue().subtract(reducedM.getValue()));

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
        private T T2(final T cosInc) {

            // X = (1.0 - 5.0 * cos²(inc))
            final T x  = cosInc.multiply(cosInc).multiply(-5.0).add(1.0);
            final T x2 = x.multiply(x);

            // Eq. 2.48
            T sum = x.getField().getZero();
            for (int i = 0; i <= 12; i++) {
                final double sign = i % 2 == 0 ? +1.0 : -1.0;
                sum = sum.add(FastMath.pow(x2, i).multiply(FastMath.pow(BETA, i)).multiply(sign).divide(CombinatoricsUtils.factorialDouble(i + 1)));
            }

            // Right term of equation 2.47
            T product = x.getField().getOne();
            for (int i = 0; i <= 10; i++) {
                product = product.multiply(FastMath.exp(x2.multiply(BETA).multiply(FastMath.scalb(-1.0, i))).add(1.0));
            }

            // Return (Eq. 2.47)
            return x.multiply(BETA).multiply(sum).multiply(product);

        }

        /** Extrapolate an orbit up to a specific target date.
         * @param date target date for the orbit
         * @param parameters model parameters
         * @return propagated parameters
         */
        public FieldKeplerianOrbit<T> propagateParameters(final FieldAbsoluteDate<T> date, final T[] parameters) {

            // Field
            final Field<T> field = date.getField();
            final T one  = field.getOne();
            final T zero = field.getZero();

            // Empirical drag coefficient M2
            final T m2 = parameters[0];

            // Keplerian evolution
            final FieldUnivariateDerivative2<T> dt = new FieldUnivariateDerivative2<>(date.durationFrom(mean.getDate()), one, zero);
            final FieldUnivariateDerivative2<T> xnot = dt.multiply(xnotDot);

            //____________________________________
            // secular effects

            // mean mean anomaly
            final FieldUnivariateDerivative2<T> dtM2  = dt.multiply(m2);
            final FieldUnivariateDerivative2<T> dt2M2 = dt.multiply(dtM2);
            final FieldUnivariateDerivative2<T> lpp = new FieldUnivariateDerivative2<T>(MathUtils.normalizeAngle(mean.getMeanAnomaly().add(lt.multiply(xnot.getValue())).add(dt2M2.getValue()), zero),
                                                                                        lt.multiply(xnotDot).add(dtM2.multiply(2.0).getValue()),
                                                                                        m2.multiply(2.0));
            // mean argument of perigee
            final FieldUnivariateDerivative2<T> gpp = new FieldUnivariateDerivative2<T>(MathUtils.normalizeAngle(mean.getPerigeeArgument().add(gt.multiply(xnot.getValue())), zero),
                                                                                        gt.multiply(xnotDot),
                                                                                        zero);
            // mean longitude of ascending node
            final FieldUnivariateDerivative2<T> hpp = new FieldUnivariateDerivative2<T>(MathUtils.normalizeAngle(mean.getRightAscensionOfAscendingNode().add(ht.multiply(xnot.getValue())), zero),
                                                                                        ht.multiply(xnotDot),
                                                                                        zero);

            // ________________________________________________
            // secular rates of the mean semi-major axis and eccentricity

            // semi-major axis
            final FieldUnivariateDerivative2<T> appDrag = dt.multiply(aRate.multiply(m2));

            // eccentricity
            final FieldUnivariateDerivative2<T> eppDrag = dt.multiply(eRate.multiply(m2));

            //____________________________________
            // Long periodical terms
            final FieldUnivariateDerivative2<T> cg1 = gpp.cos();
            final FieldUnivariateDerivative2<T> sg1 = gpp.sin();
            final FieldUnivariateDerivative2<T> c2g = cg1.multiply(cg1).subtract(sg1.multiply(sg1));
            final FieldUnivariateDerivative2<T> s2g = cg1.multiply(sg1).add(sg1.multiply(cg1));
            final FieldUnivariateDerivative2<T> c3g = c2g.multiply(cg1).subtract(s2g.multiply(sg1));
            final FieldUnivariateDerivative2<T> sg2 = sg1.multiply(sg1);
            final FieldUnivariateDerivative2<T> sg3 = sg1.multiply(sg2);


            // de eccentricity
            final FieldUnivariateDerivative2<T> d1e = sg3.multiply(dei3sg).
                                               add(sg1.multiply(deisg)).
                                               add(sg2.multiply(de2sg)).
                                               add(de);

            // l' + g'
            final FieldUnivariateDerivative2<T> lp_p_gp = s2g.multiply(dlgs2g).
                                               add(c3g.multiply(dlgc3g)).
                                               add(cg1.multiply(dlgcg)).
                                               add(lpp).
                                               add(gpp);

            // h'
            final FieldUnivariateDerivative2<T> hp = sg2.multiply(cg1).multiply(dh2sgcg).
                                               add(sg1.multiply(cg1).multiply(dhsgcg)).
                                               add(cg1.multiply(dhcg)).
                                               add(hpp);

            // Short periodical terms
            // eccentric anomaly
            final FieldUnivariateDerivative2<T> Ep = getEccentricAnomaly(lpp);
            final FieldUnivariateDerivative2<T> cf1 = (Ep.cos().subtract(mean.getE())).divide(Ep.cos().multiply(mean.getE().negate()).add(1.0));
            final FieldUnivariateDerivative2<T> sf1 = (Ep.sin().multiply(n)).divide(Ep.cos().multiply(mean.getE().negate()).add(1.0));
            final FieldUnivariateDerivative2<T> f = FastMath.atan2(sf1, cf1);

            final FieldUnivariateDerivative2<T> c2f = cf1.multiply(cf1).subtract(sf1.multiply(sf1));
            final FieldUnivariateDerivative2<T> s2f = cf1.multiply(sf1).add(sf1.multiply(cf1));
            final FieldUnivariateDerivative2<T> c3f = c2f.multiply(cf1).subtract(s2f.multiply(sf1));
            final FieldUnivariateDerivative2<T> s3f = c2f.multiply(sf1).add(s2f.multiply(cf1));
            final FieldUnivariateDerivative2<T> cf2 = cf1.multiply(cf1);
            final FieldUnivariateDerivative2<T> cf3 = cf1.multiply(cf2);

            final FieldUnivariateDerivative2<T> c2g1f = cf1.multiply(c2g).subtract(sf1.multiply(s2g));
            final FieldUnivariateDerivative2<T> c2g2f = c2f.multiply(c2g).subtract(s2f.multiply(s2g));
            final FieldUnivariateDerivative2<T> c2g3f = c3f.multiply(c2g).subtract(s3f.multiply(s2g));
            final FieldUnivariateDerivative2<T> s2g1f = cf1.multiply(s2g).add(c2g.multiply(sf1));
            final FieldUnivariateDerivative2<T> s2g2f = c2f.multiply(s2g).add(c2g.multiply(s2f));
            final FieldUnivariateDerivative2<T> s2g3f = c3f.multiply(s2g).add(c2g.multiply(s3f));

            final FieldUnivariateDerivative2<T> eE = (Ep.cos().multiply(mean.getE().negate()).add(1.0)).reciprocal();
            final FieldUnivariateDerivative2<T> eE3 = eE.multiply(eE).multiply(eE);
            final FieldUnivariateDerivative2<T> sigma = eE.multiply(n.multiply(n)).multiply(eE).add(eE);

            // Semi-major axis
            final FieldUnivariateDerivative2<T> a = eE3.multiply(aCbis).add(appDrag.add(mean.getA())).
                                            add(aC).
                                            add(eE3.multiply(c2g2f).multiply(ac2g2f));

            // Eccentricity
            final FieldUnivariateDerivative2<T> e = d1e.add(eppDrag.add(mean.getE())).
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
            final FieldUnivariateDerivative2<T> i = d1e.multiply(ide).
                                            add(mean.getI()).
                                            add(sf1.multiply(s2g2f.multiply(isfs2f2g))).
                                            add(cf1.multiply(c2g2f.multiply(icfc2f2g))).
                                            add(c2g2f.multiply(ic2f2g));

            // Argument of perigee + True anomaly
            final FieldUnivariateDerivative2<T> g_p_l = lp_p_gp.add(f.multiply(glf)).
                                             add(lpp.multiply(gll)).
                                             add(sf1.multiply(glsf)).
                                             add(sigma.multiply(sf1).multiply(glosf)).
                                             add(s2g2f.multiply(gls2f2g)).
                                             add(s2g1f.multiply(gls2gf)).
                                             add(sigma.multiply(s2g1f).multiply(glos2gf)).
                                             add(s2g3f.multiply(gls2g3f)).
                                             add(sigma.multiply(s2g3f).multiply(glos2g3f));


            // Longitude of ascending node
            final FieldUnivariateDerivative2<T> h = hp.add(f.multiply(hf)).
                                            add(lpp.multiply(hl)).
                                            add(sf1.multiply(hsf)).
                                            add(cf1.multiply(s2g2f).multiply(hcfs2g2f)).
                                            add(s2g2f.multiply(hs2g2f)).
                                            add(c2g2f.multiply(sf1).multiply(hsfc2g2f));

            final FieldUnivariateDerivative2<T> edl = s2g.multiply(edls2g).
                                            add(cg1.multiply(edlcg)).
                                            add(c3g.multiply(edlc3g)).
                                            add(sf1.multiply(edlsf)).
                                            add(s2g1f.multiply(edls2gf)).
                                            add(s2g3f.multiply(edls2g3f)).
                                            add(sf1.multiply(sigma).multiply(edlsf)).
                                            add(s2g1f.multiply(sigma).multiply(edls2gf.negate())).
                                            add(s2g3f.multiply(sigma).multiply(edls2g3f.multiply(3.0)));

            final FieldUnivariateDerivative2<T> A = e.multiply(lpp.cos()).subtract(edl.multiply(lpp.sin()));
            final FieldUnivariateDerivative2<T> B = e.multiply(lpp.sin()).add(edl.multiply(lpp.cos()));

            // True anomaly
            final FieldUnivariateDerivative2<T> l = FastMath.atan2(B, A);

            // Argument of perigee
            final FieldUnivariateDerivative2<T> g = g_p_l.subtract(l);

            // Return a keplerian orbit
            return new FieldKeplerianOrbit<>(a.getValue(), e.getValue(), i.getValue(),
                                             g.getValue(), h.getValue(), l.getValue(),
                                             a.getFirstDerivative(), e.getFirstDerivative(), i.getFirstDerivative(),
                                             g.getFirstDerivative(), h.getFirstDerivative(), l.getFirstDerivative(),
                                             PositionAngleType.MEAN, mean.getFrame(), date, this.mu);

        }
    }

    /** {@inheritDoc} */
    @Override
    protected T getMass(final FieldAbsoluteDate<T> date) {
        return models.get(date).mass;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(M2Driver);
    }

}
