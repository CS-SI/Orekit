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

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.orbits.FieldKeplerianAnomalyUtility;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.propagation.conversion.osc2mean.BrouwerLyddaneTheory;
import org.orekit.propagation.conversion.osc2mean.FixedPointConverter;
import org.orekit.propagation.conversion.osc2mean.MeanTheory;
import org.orekit.propagation.conversion.osc2mean.OsculatingToMeanConverter;
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
 * @see "Solomon, Daniel, THE NAVSPASUR Satellite Motion Model,
 *       Naval Research Laboratory, August 8, 1991."
 *
 * @author Melina Vanel
 * @author Bryan Cazabonne
 * @author Pascal Parraud
 * @since 11.1
 * @param <T> type of the field elements
 */
public class FieldBrouwerLyddanePropagator<T extends CalculusFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {

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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, UnnormalizedSphericalHarmonicsProvider, PropagationType, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final double m2Value) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             initialOrbit.getMu().newInstance(DEFAULT_MASS), provider,
             provider.onDate(initialOrbit.getDate().toAbsoluteDate()), m2Value);
    }

    /**
     * Private helper constructor.
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     * @param initialOrbit initial orbit
     * @param attitude attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param harmonics {@code provider.onDate(initialOrbit.getDate())}
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement,
     * UnnormalizedSphericalHarmonicsProvider, UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics, PropagationType, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitude,
                                         final T mass,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final UnnormalizedSphericalHarmonics harmonics,
                                         final double m2Value) {
        this(initialOrbit, attitude,  mass, provider.getAe(), initialOrbit.getMu().newInstance(provider.getMu()),
             harmonics.getUnnormalizedCnm(2, 0),
             harmonics.getUnnormalizedCnm(3, 0),
             harmonics.getUnnormalizedCnm(4, 0),
             harmonics.getUnnormalizedCnm(5, 0),
             m2Value);
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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see org.orekit.utils.Constants
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, AttitudeProvider, double, CalculusFieldElement, double, double, double, double, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final double referenceRadius,
                                         final T mu,
                                         final double c20,
                                         final double c30,
                                         final double c40,
                                         final double c50,
                                         final double m2Value) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             initialOrbit.getMu().newInstance(DEFAULT_MASS),
             referenceRadius, mu, c20, c30, c40, c50, m2Value);
    }

    /** Build a propagator from orbit, mass and potential provider.
     * <p>Attitude law is set to an unspecified non-null arbitrary value.</p>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial orbit
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement, UnnormalizedSphericalHarmonicsProvider, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final T mass,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final double m2Value) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             mass, provider, provider.onDate(initialOrbit.getDate().toAbsoluteDate()), m2Value);
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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement, double, CalculusFieldElement, double, double, double, double, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit, final T mass,
                                         final double referenceRadius, final T mu,
                                         final double c20, final double c30, final double c40,
                                         final double c50, final double m2Value) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             mass, referenceRadius, mu, c20, c30, c40, c50, m2Value);
    }

    /** Build a propagator from orbit, attitude provider and potential provider.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param provider for un-normalized zonal coefficients
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final double m2Value) {
        this(initialOrbit, attitudeProv, initialOrbit.getMu().newInstance(DEFAULT_MASS), provider,
             provider.onDate(initialOrbit.getDate().toAbsoluteDate()), m2Value);
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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final double referenceRadius, final T mu,
                                         final double c20, final double c30, final double c40,
                                         final double c50, final double m2Value) {
        this(initialOrbit, attitudeProv, initialOrbit.getMu().newInstance(DEFAULT_MASS),
             referenceRadius, mu, c20, c30, c40, c50, m2Value);
    }

    /** Build a propagator from orbit, attitude provider, mass and potential provider.
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement, UnnormalizedSphericalHarmonicsProvider, PropagationType, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final T mass,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final double m2Value) {
        this(initialOrbit, attitudeProv, mass, provider, provider.onDate(initialOrbit.getDate().toAbsoluteDate()), m2Value);
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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #FieldBrouwerLyddanePropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement, double, CalculusFieldElement, double, double, double, double, PropagationType, double)
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final T mass,
                                         final double referenceRadius, final T mu,
                                         final double c20, final double c30, final double c40,
                                         final double c50, final double m2Value) {
        this(initialOrbit, attitudeProv, mass, referenceRadius, mu, c20, c30, c40, c50, PropagationType.OSCULATING, m2Value);
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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final PropagationType initialType,
                                         final double m2Value) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             initialOrbit.getMu().newInstance(DEFAULT_MASS), provider,
             provider.onDate(initialOrbit.getDate().toAbsoluteDate()), initialType, m2Value);
    }

    /** Build a propagator from orbit, attitude provider, mass and potential provider.
     * <p>Using this constructor, it is possible to define the initial orbit as
     * a mean Brouwer-Lyddane orbit or an osculating one.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param initialType initial orbit type (mean Brouwer-Lyddane orbit or osculating orbit)
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final T mass,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final PropagationType initialType,
                                         final double m2Value) {
        this(initialOrbit, attitudeProv, mass, provider,
             provider.onDate(initialOrbit.getDate().toAbsoluteDate()), initialType, m2Value);
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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitude,
                                         final T mass,
                                         final UnnormalizedSphericalHarmonicsProvider provider,
                                         final UnnormalizedSphericalHarmonics harmonics,
                                         final PropagationType initialType,
                                         final double m2Value) {
        this(initialOrbit, attitude, mass, provider.getAe(), initialOrbit.getMu().newInstance(provider.getMu()),
             harmonics.getUnnormalizedCnm(2, 0),
             harmonics.getUnnormalizedCnm(3, 0),
             harmonics.getUnnormalizedCnm(4, 0),
             harmonics.getUnnormalizedCnm(5, 0),
             initialType, m2Value);
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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final T mass,
                                         final double referenceRadius, final T mu,
                                         final double c20, final double c30, final double c40,
                                         final double c50,
                                         final PropagationType initialType,
                                         final double m2Value) {
        this(initialOrbit, attitudeProv, mass, referenceRadius, mu,
             c20, c30, c40, c50, initialType, m2Value,
             new FixedPointConverter(BrouwerLyddanePropagator.EPSILON_DEFAULT,
                                     BrouwerLyddanePropagator.MAX_ITERATIONS_DEFAULT,
                                     FixedPointConverter.DEFAULT_DAMPING));
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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @since 11.2
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final T mass,
                                         final double referenceRadius,
                                         final T mu,
                                         final double c20,
                                         final double c30,
                                         final double c40,
                                         final double c50,
                                         final PropagationType initialType,
                                         final double m2Value,
                                         final double epsilon,
                                         final int maxIterations) {
        this(initialOrbit, attitudeProv, mass, referenceRadius, mu, c20, c30, c40, c50,
             initialType, m2Value, new FixedPointConverter(epsilon, maxIterations,
                                                           FixedPointConverter.DEFAULT_DAMPING));
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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @param converter osculating to mean orbit converter
     * @since 13.0
     */
    public FieldBrouwerLyddanePropagator(final FieldOrbit<T> initialOrbit,
                                         final AttitudeProvider attitudeProv,
                                         final T mass,
                                         final double referenceRadius,
                                         final T mu,
                                         final double c20,
                                         final double c30,
                                         final double c40,
                                         final double c50,
                                         final PropagationType initialType,
                                         final double m2Value,
                                         final OsculatingToMeanConverter converter) {

        super(mass.getField(), attitudeProv);

        // store model coefficients
        this.referenceRadius = referenceRadius;
        this.mu  = mu;
        this.ck0 = new double[] {0.0, 0.0, c20, c30, c40, c50};

        // initialize M2 driver
        this.M2Driver = new ParameterDriver(BrouwerLyddanePropagator.M2_NAME, m2Value, SCALE,
                                            Double.NEGATIVE_INFINITY,
                                            Double.POSITIVE_INFINITY);

        // compute mean parameters if needed
        resetInitialState(new FieldSpacecraftState<>(initialOrbit,
                                                     attitudeProv.getAttitude(initialOrbit,
                                                                              initialOrbit.getDate(),
                                                                              initialOrbit.getFrame())).withMass(mass),
                          initialType, converter);

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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@code BrouwerLyddanePropagator.M2} drag is not considered
     * @return mean orbit in a Brouwer-Lyddane sense
     * @since 11.2
     */
    public static <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> computeMeanOrbit(final FieldOrbit<T> osculating,
                                                                                              final UnnormalizedSphericalHarmonicsProvider provider,
                                                                                              final UnnormalizedSphericalHarmonics harmonics,
                                                                                              final double m2Value) {
        return computeMeanOrbit(osculating, provider, harmonics, m2Value,
                                BrouwerLyddanePropagator.EPSILON_DEFAULT,
                                BrouwerLyddanePropagator.MAX_ITERATIONS_DEFAULT);
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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@code BrouwerLyddanePropagator.M2} drag is not considered
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @return mean orbit in a Brouwer-Lyddane sense
     * @since 11.2
     */
    public static <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> computeMeanOrbit(final FieldOrbit<T> osculating,
                                                                                              final UnnormalizedSphericalHarmonicsProvider provider,
                                                                                              final UnnormalizedSphericalHarmonics harmonics,
                                                                                              final double m2Value,
                                                                                              final double epsilon,
                                                                                              final int maxIterations) {
        return computeMeanOrbit(osculating,
                                provider.getAe(), provider.getMu(),
                                harmonics.getUnnormalizedCnm(2, 0),
                                harmonics.getUnnormalizedCnm(3, 0),
                                harmonics.getUnnormalizedCnm(4, 0),
                                harmonics.getUnnormalizedCnm(5, 0),
                                m2Value, epsilon, maxIterations);
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
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@code BrouwerLyddanePropagator.M2} drag is not considered
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @return mean orbit in a Brouwer-Lyddane sense
     * @since 11.2
     */
    public static <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> computeMeanOrbit(final FieldOrbit<T> osculating,
                                                                                              final double referenceRadius,
                                                                                              final double mu,
                                                                                              final double c20,
                                                                                              final double c30,
                                                                                              final double c40,
                                                                                              final double c50,
                                                                                              final double m2Value,
                                                                                              final double epsilon,
                                                                                              final int maxIterations) {
        // Build a fixed-point converter
        final OsculatingToMeanConverter converter = new FixedPointConverter(epsilon, maxIterations,
                                                                            FixedPointConverter.DEFAULT_DAMPING);
        return computeMeanOrbit(osculating, referenceRadius, mu, c20, c30, c40, c50, m2Value, converter);
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
     * The computation is done through the given osculating to mean orbit converter.
     * </p>
     * @param <T> type of the filed elements
     * @param osculating osculating orbit to convert
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param m2Value value of empirical drag coefficient in rad/s².
     *        If equal to {@code BrouwerLyddanePropagator.M2} drag is not considered
     * @param converter osculating to mean orbit converter
     * @return mean orbit in a Brouwer-Lyddane sense
     * @since 13.0
     */
    public static <T extends CalculusFieldElement<T>> FieldKeplerianOrbit<T> computeMeanOrbit(final FieldOrbit<T> osculating,
                                                                                              final double referenceRadius,
                                                                                              final double mu,
                                                                                              final double c20,
                                                                                              final double c30,
                                                                                              final double c40,
                                                                                              final double c50,
                                                                                              final double m2Value,
                                                                                              final OsculatingToMeanConverter converter) {
        // Set BL as the mean theory for converting
        final MeanTheory theory = new BrouwerLyddaneTheory(referenceRadius, mu, c20, c30, c40, c50, m2Value);
        converter.setMeanTheory(theory);
        return (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(converter.convertToMean(osculating));
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
    public void resetInitialState(final FieldSpacecraftState<T> state,
                                  final PropagationType stateType) {
        final OsculatingToMeanConverter converter = new FixedPointConverter(BrouwerLyddanePropagator.EPSILON_DEFAULT,
                                                                            BrouwerLyddanePropagator.MAX_ITERATIONS_DEFAULT,
                                                                            FixedPointConverter.DEFAULT_DAMPING);
        resetInitialState(state, stateType, converter);
    }

    /** Reset the propagator initial state.
     * @param state new initial state to consider
     * @param stateType mean Brouwer-Lyddane orbit or osculating orbit
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @since 11.2
     */
    public void resetInitialState(final FieldSpacecraftState<T> state,
                                  final PropagationType stateType,
                                  final double epsilon,
                                  final int maxIterations) {
        final OsculatingToMeanConverter converter = new FixedPointConverter(epsilon, maxIterations,
                                                                            FixedPointConverter.DEFAULT_DAMPING);
        resetInitialState(state, stateType, converter);
    }

    /** Reset the propagator initial state.
     * @param state     new initial state to consider
     * @param stateType mean Brouwer-Lyddane orbit or osculating orbit
     * @param converter osculating to mean orbit converter
     * @since 13.0
     */
    public void resetInitialState(final FieldSpacecraftState<T> state,
                                  final PropagationType stateType,
                                  final OsculatingToMeanConverter converter) {
        super.resetInitialState(state);
        FieldKeplerianOrbit<T> keplerian = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(state.getOrbit());
        if (stateType == PropagationType.OSCULATING) {
            final MeanTheory theory = new BrouwerLyddaneTheory(referenceRadius, mu.getReal(),
                                                               ck0[2], ck0[3], ck0[4], ck0[5],
                                                               getM2());
            converter.setMeanTheory(theory);
            keplerian = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(converter.convertToMean(keplerian));
        }
        this.initialModel = new FieldBLModel<>(keplerian, state.getMass(), referenceRadius, mu, ck0);
        this.models = new FieldTimeSpanMap<>(initialModel, state.getMass().getField());
    }

    /** {@inheritDoc} */
    @Override
    protected void resetIntermediateState(final FieldSpacecraftState<T> state,
                                          final boolean forward) {
        final OsculatingToMeanConverter converter = new FixedPointConverter(BrouwerLyddanePropagator.EPSILON_DEFAULT,
                                                                            BrouwerLyddanePropagator.MAX_ITERATIONS_DEFAULT,
                                                                            FixedPointConverter.DEFAULT_DAMPING);
        resetIntermediateState(state, forward, converter);
    }

    /** Reset an intermediate state.
     * @param state new intermediate state to consider
     * @param forward if true, the intermediate state is valid for
     *                propagations after itself
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @since 11.2
     */
    protected void resetIntermediateState(final FieldSpacecraftState<T> state,
                                          final boolean forward,
                                          final double epsilon,
                                          final int maxIterations) {
        final OsculatingToMeanConverter converter = new FixedPointConverter(epsilon, maxIterations,
                                                                            FixedPointConverter.DEFAULT_DAMPING);
        resetIntermediateState(state, forward, converter);
    }

    /** Reset an intermediate state.
     * @param state     new intermediate state to consider
     * @param forward   if true, the intermediate state is valid for
     *                  propagations after itself
     * @param converter osculating to mean orbit converter
     * @since 13.0
     */
    protected void resetIntermediateState(final FieldSpacecraftState<T> state,
                                          final boolean forward,
                                          final OsculatingToMeanConverter converter) {
        final MeanTheory theory = new BrouwerLyddaneTheory(referenceRadius, mu.getReal(),
                                                           ck0[2], ck0[3], ck0[4], ck0[5],
                                                           getM2());
        converter.setMeanTheory(theory);
        final FieldKeplerianOrbit<T> mean = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(converter.convertToMean(state.getOrbit()));
        final FieldBLModel<T> newModel = new FieldBLModel<>(mean, state.getMass(), referenceRadius, mu, ck0);
        if (forward) {
            models.addValidAfter(newModel, state.getDate(), false);
        } else {
            models.addValidBefore(newModel, state.getDate(), false);
        }
        stateChanged(state);
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

        /** Constant mass. */
        private final T mass;

        /** Central attraction coefficient. */
        private final T mu;

        /** Brouwer-Lyddane mean orbit. */
        private final FieldKeplerianOrbit<T> mean;

        // Preprocessed values

        /** Mean mean motion: n0 = √(μ/a")/a". */
        private final T n0;

        /** η = √(1 - e"²). */
        private final T n;
        /** η². */
        private final T n2;
        /** η³. */
        private final T n3;
        /** η + 1 / (1 + η). */
        private final T t8;

        /** Secular correction for mean anomaly l: &delta;<sub>s</sub>l. */
        private final T dsl;
        /** Secular correction for perigee argument g: &delta;<sub>s</sub>g. */
        private final T dsg;
        /** Secular correction for raan h: &delta;<sub>s</sub>h. */
        private final T dsh;

        /** Secular rate of change of semi-major axis due to drag. */
        private final T aRate;
        /** Secular rate of change of eccentricity due to drag. */
        private final T eRate;

        // CHECKSTYLE: stop JavadocVariable check

        // Storage for speed-up
        private final T yp2;
        private final T ci;
        private final T si;
        private final T oneMci2;
        private final T ci2X3M1;

        // Long periodic corrections factors
        private final T vle1;
        private final T vle2;
        private final T vle3;
        private final T vli1;
        private final T vli2;
        private final T vli3;
        private final T vll2;
        private final T vlh1I;
        private final T vlh2I;
        private final T vlh3I;
        private final T vls1;
        private final T vls2;
        private final T vls3;

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

            this.mass = mass;
            this.mu   = mu;

            // mean orbit
            this.mean = mean;

            final T one = mass.getField().getOne();

            // mean eccentricity e"
            final T epp = mean.getE();
            if (epp.getReal() >= 1) {
                // Only for elliptical (e < 1) orbits
                throw new OrekitException(OrekitMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL,
                                          epp.getReal());
            }
            final T epp2 = epp.square();

            // η
            n2 = one.subtract(epp2);
            n  = n2.sqrt();
            n3 = n2.multiply(n);
            t8 = n.add(one.add(n).reciprocal());

            // mean semi-major axis a"
            final T app = mean.getA();

            // mean mean motion
            n0 = mu.divide(app).sqrt().divide(app);

            // ae/a"
            final T q = app.divide(referenceRadius).reciprocal();

            // γ2'
            T ql = q.square();
            T nl = n2.square();
            yp2 = ql.multiply(-0.5 * ck0[2]).divide(nl);
            final T yp22 = yp2.square();

            // γ3'
            ql = ql.multiply(q);
            nl = nl.multiply(n2);
            final T yp3 = ql.multiply(ck0[3]).divide(nl);

            // γ4'
            ql = ql.multiply(q);
            nl = nl.multiply(n2);
            final T yp4 = ql.multiply(0.375 * ck0[4]).divide(nl);

            // γ5'
            ql = ql.multiply(q);
            nl = nl.multiply(n2);
            final T yp5 = ql.multiply(ck0[5]).divide(nl);

            // mean inclination I" sin & cos
            final FieldSinCos<T> sc = FastMath.sinCos(mean.getI());
            si = sc.sin();
            ci = sc.cos();
            final T ci2 = ci.square();
            oneMci2 = one.subtract(ci2);
            ci2X3M1 = ci2.multiply(3.).subtract(one);
            final T ci2X5M1 = ci2.multiply(5.).subtract(one);

            // secular corrections
            // true anomaly
            final T dsl1  = yp2.multiply(n).multiply(1.5);
            final T dsl2a = n.multiply(n.multiply(25.).add(16.)).subtract(15.);
            final T dsl2b = n.multiply(n.multiply(90.).add(96.)).negate().add(30.);
            final T dsl2c = n.multiply(n.multiply(25.).add(144.)).add(105.);
            final T dsl21 = dsl2a.add(ci2.multiply(dsl2b.add(ci2.multiply(dsl2c))));
            final T dsl2  = ci2X3M1.add(yp2.multiply(0.0625).multiply(dsl21));
            final T dsl3  = yp4.multiply(n).multiply(epp2).multiply(0.9375).
                                multiply(ci2.multiply(35.0).subtract(30.0).multiply(ci2).add(3.));
            dsl = dsl1.multiply(dsl2).add(dsl3);

            // perigee argument
            final T dsg1  = yp2.multiply(1.5).multiply(ci2X5M1);
            final T dsg2a = n.multiply(25.).add(24.).multiply(n).add(-35.);
            final T dsg2b = n.multiply(126.).add(192.).multiply(n).negate().add(90.);
            final T dsg2c = n.multiply(45.).add(360.).multiply(n).add(385.);
            final T dsg21 = dsg2a.add(ci2.multiply(dsg2b.add(ci2.multiply(dsg2c))));
            final T dsg2  = yp22.multiply(0.09375).multiply(dsg21);
            final T dsg3a = n2.multiply(-9.).add(21.);
            final T dsg3b = n2.multiply(126.).add(-270.);
            final T dsg3c = n2.multiply(-189.).add(385.);
            final T dsg31 = dsg3a.add(ci2.multiply(dsg3b.add(ci2.multiply(dsg3c))));
            final T dsg3  = yp4.multiply(0.3125).multiply(dsg31);
            dsg = dsg1.add(dsg2).add(dsg3);

            // right ascension of ascending node
            final T dsh1  = yp2.multiply(-3.);
            final T dsh2a = n.multiply(9.).add(12.).multiply(n).add(-5.);
            final T dsh2b = n.multiply(5.).add(36.).multiply(n).add(35.);
            final T dsh21 = dsh2a.subtract(ci2.multiply(dsh2b));
            final T dsh2  = yp22.multiply(0.375).multiply(dsh21);
            final T dsh31 = n2.multiply(3.).subtract(5.);
            final T dsh32 = ci2.multiply(7.).subtract(3.);
            final T dsh3  = yp4.multiply(1.25).multiply(dsh31).multiply(dsh32);
            dsh = ci.multiply(dsh1.add(dsh2).add(dsh3));

            // secular rates of change due to drag
            // Eq. 2.41 and Eq. 2.45 of Phipps' 1992 thesis
            final T coef = n0.multiply(one.add(dsl)).multiply(3.).reciprocal().multiply(-4);
            aRate = coef.multiply(app);
            eRate = coef.multiply(epp).multiply(n2);

            // singular term 1/(1 - 5 * cos²(I")) replaced by T2 function
            final T t2 = T2(ci);

            // factors for long periodic corrections
            final T fs12 = yp3.divide(yp2);
            final T fs13 = yp4.multiply(10).divide(yp2.multiply(3));
            final T fs14 = yp5.divide(yp2);

            final T ci2Xt2 = ci2.multiply(t2);
            final T cA = one.subtract(ci2.multiply(ci2Xt2.multiply(40.) .add(11.)));
            final T cB = one.subtract(ci2.multiply(ci2Xt2.multiply(8.)  .add(3.)));
            final T cC = one.subtract(ci2.multiply(ci2Xt2.multiply(24.) .add(9.)));
            final T cD = one.subtract(ci2.multiply(ci2Xt2.multiply(16.) .add(5.)));
            final T cE = one.subtract(ci2.multiply(ci2Xt2.multiply(200.).add(33.)));
            final T cF = one.subtract(ci2.multiply(ci2Xt2.multiply(40.) .add(9.)));

            final T p5p   = one.add(ci2Xt2.multiply(ci2Xt2.multiply(20.).add(8.)));
            final T p5p2  = one.add(p5p.multiply(2.));
            final T p5p4  = one.add(p5p.multiply(4.));
            final T p5p10 = one.add(p5p.multiply(10.));

            final T e2X3P4  = epp2.multiply(3.).add(4.);
            final T ciO1Pci = ci.divide(one.add(ci));
            final T oneMci  = one.subtract(ci);

            final T q1 = (yp2.multiply(cA).subtract(fs13.multiply(cB))).
                            multiply(0.125);
            final T q2 = (yp2.multiply(p5p10).subtract(fs13.multiply(p5p2))).
                            multiply(epp2).multiply(ci).multiply(0.125);
            final T q5 = (fs12.add(e2X3P4.multiply(fs14).multiply(cC).multiply(0.3125))).
                            multiply(0.25);
            final T p2 = p5p2.multiply(epp).multiply(ci).multiply(si).multiply(e2X3P4).multiply(fs14).
                            multiply(0.46875);
            final T p3 = epp.multiply(si).multiply(fs14).multiply(cC).
                            multiply(0.15625);
            final double kf = 35. / 1152.;
            final T p4 = epp.multiply(fs14).multiply(cD).
                            multiply(kf);
            final T p5 = epp.multiply(epp2).multiply(ci).multiply(si).multiply(fs14).multiply(p5p4).
                            multiply(2. * kf);

            vle1 = epp.multiply(n2).multiply(q1);
            vle2 = n2.multiply(si).multiply(q5);
            vle3 = epp.multiply(n2).multiply(si).multiply(p4).multiply(-3.0);

            vli1 = epp.multiply(q1).divide(si).negate();
            vli2 = epp.multiply(ci).multiply(q5).negate();
            vli3 = epp2.multiply(ci).multiply(p4).multiply(-3.0);

            vll2 = vle2.add(epp.multiply(n2).multiply(p3).multiply(3.0));

            vlh1I = si.multiply(q2).negate();
            vlh2I = epp.multiply(ci).multiply(q5).add(si.multiply(p2));
            vlh3I = (epp2.multiply(ci).multiply(p4).add(si.multiply(p5))).negate();

            vls1 = q1.multiply(n3.subtract(one)).
                   subtract(q2).
                   add(epp2.multiply(ci2).multiply(ci2Xt2).multiply(ci2Xt2).
                       multiply(yp2.subtract(fs13.multiply(0.2))).multiply(25.0)).
                   subtract(epp2.multiply(yp2.multiply(cE).subtract(fs13.multiply(cF))).multiply(0.0625));

            vls2 = epp.multiply(si).multiply(t8.add(ciO1Pci)).multiply(q5).
                   add((epp2.subtract(n3).multiply(3.).add(11.)).multiply(p3)).
                   add(oneMci.multiply(p2));

            vls3 = si.multiply(p4).multiply(n3.subtract(one).multiply(3.).
                                            subtract(epp2.multiply(ciO1Pci.add(2.)))).
                   subtract(oneMci.multiply(p5));
        }

        /**
         * Get true anomaly from mean anomaly.
         * @param lM  the mean anomaly (rad)
         * @param ecc the eccentricity
         * @return the true anomaly (rad)
         */
        private FieldUnivariateDerivative1<T> getTrueAnomaly(final FieldUnivariateDerivative1<T> lM,
                                                             final FieldUnivariateDerivative1<T> ecc) {

            final T zero = mean.getE().getField().getZero();

            // reduce M to [-PI PI] interval
            final FieldUnivariateDerivative1<T> reducedM = new FieldUnivariateDerivative1<>(MathUtils.normalizeAngle(lM.getValue(), zero),
                                                                                            lM.getFirstDerivative());

            // compute the true anomaly
            FieldUnivariateDerivative1<T> lV = FieldKeplerianAnomalyUtility.ellipticMeanToTrue(ecc, lM);

            // expand the result back to original range
            lV = lV.add(lM.getValue().subtract(reducedM.getValue()));

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
        private T T2(final T cosI) {

            // X = (1.0 - 5.0 * cos²(i))
            final T x  = cosI.square().multiply(-5.0).add(1.0);
            final T x2 = x.square();
            final T xb = x2.multiply(BETA);

            // Eq. 2.48
            T sum = x.getField().getZero();
            for (int i = 0; i <= 12; i++) {
                final double sign = i % 2 == 0 ? +1.0 : -1.0;
                sum = sum.add(FastMath.pow(x2, i).
                              multiply(FastMath.pow(BETA, i)).
                              multiply(sign).
                              divide(CombinatoricsUtils.factorialDouble(i + 1)));
            }

            // Right term of equation 2.47
            final T one = x.getField().getOne();
            T product = one;
            for (int i = 0; i <= 10; i++) {
                product = product.multiply(one.add(FastMath.exp(xb.multiply(FastMath.scalb(-1.0, i)))));
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
            final FieldUnivariateDerivative1<T> dt  = new FieldUnivariateDerivative1<>(date.durationFrom(mean.getDate()), one);
            final FieldUnivariateDerivative1<T> not = dt.multiply(n0);

            final FieldUnivariateDerivative1<T> dtM2  = dt.multiply(m2);
            final FieldUnivariateDerivative1<T> dt2M2 = dt.multiply(dtM2);

            // Secular corrections
            // -------------------

            // semi-major axis (with drag Eq. 2.41 of Phipps' 1992 thesis)
            final FieldUnivariateDerivative1<T> app = dtM2.multiply(aRate).add(mean.getA());

            // eccentricity  (with drag Eq. 2.45 of Phipps' 1992 thesis) reduced to [0, 1[
            final FieldUnivariateDerivative1<T> tmp = dtM2.multiply(eRate).add(mean.getE());
            final FieldUnivariateDerivative1<T> epp = FastMath.max(FastMath.min(tmp, MAX_ECC), 0.);

            // mean argument of perigee
            final T gp0 = MathUtils.normalizeAngle(mean.getPerigeeArgument().add(dsg.multiply(not.getValue())), zero);
            final T gp1 = dsg.multiply(n0);
            final FieldUnivariateDerivative1<T> gpp = new FieldUnivariateDerivative1<>(gp0, gp1);

            // mean longitude of ascending node
            final T hp0 = MathUtils.normalizeAngle(mean.getRightAscensionOfAscendingNode().add(dsh.multiply(not.getValue())), zero);
            final T hp1 = dsh.multiply(n0);
            final FieldUnivariateDerivative1<T> hpp = new FieldUnivariateDerivative1<>(hp0, hp1);

            // mean anomaly (with drag Eq. 2.38 of Phipps' 1992 thesis)
            final T lp0 = MathUtils.normalizeAngle(mean.getMeanAnomaly().add(dsl.add(one).multiply(not.getValue())).add(dt2M2.getValue()), zero);
            final T lp1 = dsl.add(one).multiply(n0).add(dtM2.multiply(2.0).getValue());
            final FieldUnivariateDerivative1<T> lpp = new FieldUnivariateDerivative1<>(lp0, lp1);

            // Long period corrections
            //------------------------
            final FieldSinCos<FieldUnivariateDerivative1<T>> scgpp = gpp.sinCos();
            final FieldUnivariateDerivative1<T> cgpp = scgpp.cos();
            final FieldUnivariateDerivative1<T> sgpp = scgpp.sin();
            final FieldSinCos<FieldUnivariateDerivative1<T>> sc2gpp = gpp.multiply(2).sinCos();
            final FieldUnivariateDerivative1<T> c2gpp  = sc2gpp.cos();
            final FieldUnivariateDerivative1<T> s2gpp  = sc2gpp.sin();
            final FieldSinCos<FieldUnivariateDerivative1<T>> sc3gpp = gpp.multiply(3).sinCos();
            final FieldUnivariateDerivative1<T> c3gpp  = sc3gpp.cos();
            final FieldUnivariateDerivative1<T> s3gpp  = sc3gpp.sin();

            // δ1e
            final FieldUnivariateDerivative1<T> d1e = c2gpp.multiply(vle1).
                                                      add(sgpp.multiply(vle2)).
                                                      add(s3gpp.multiply(vle3));

            // δ1I
            FieldUnivariateDerivative1<T> d1I = sgpp.multiply(vli2).
                                                add(s3gpp.multiply(vli3));
            // Pseudo singular term, not to add if I" is zero
            if (Double.isFinite(vli1.getReal())) {
                d1I = d1I.add(c2gpp.multiply(vli1));
            }

            // e"δ1l
            final FieldUnivariateDerivative1<T> eppd1l = s2gpp.multiply(vle1).
                                                         subtract(cgpp.multiply(vll2)).
                                                         subtract(c3gpp.multiply(vle3)).
                                                         multiply(n);

            // sinI"δ1h
            final FieldUnivariateDerivative1<T> sIppd1h = s2gpp.multiply(vlh1I).
                                                          add(cgpp.multiply(vlh2I)).
                                                          add(c3gpp.multiply(vlh3I));

            // δ1z = δ1l + δ1g + δ1h
            final FieldUnivariateDerivative1<T> d1z = s2gpp.multiply(vls1).
                                                      add(cgpp.multiply(vls2)).
                                                      add(c3gpp.multiply(vls3));

            // Short period corrections
            // ------------------------

            // true anomaly
            final FieldUnivariateDerivative1<T> fpp = getTrueAnomaly(lpp, epp);
            final FieldSinCos<FieldUnivariateDerivative1<T>> scfpp = fpp.sinCos();
            final FieldUnivariateDerivative1<T> cfpp = scfpp.cos();
            final FieldUnivariateDerivative1<T> sfpp = scfpp.sin();

            // e"sin(f')
            final FieldUnivariateDerivative1<T> eppsfpp = epp.multiply(sfpp);
            // e"cos(f')
            final FieldUnivariateDerivative1<T> eppcfpp = epp.multiply(cfpp);
            // 1 + e"cos(f')
            final FieldUnivariateDerivative1<T> eppcfppP1 = eppcfpp.add(1);
            // 2 + e"cos(f')
            final FieldUnivariateDerivative1<T> eppcfppP2 = eppcfpp.add(2);
            // 3 + e"cos(f')
            final FieldUnivariateDerivative1<T> eppcfppP3 = eppcfpp.add(3);
            // (1 + e"cos(f'))³
            final FieldUnivariateDerivative1<T> eppcfppP1_3 = eppcfppP1.square().multiply(eppcfppP1);

            // 2g"
            final FieldUnivariateDerivative1<T> g2 = gpp.multiply(2);

            // 2g" + f"
            final FieldUnivariateDerivative1<T> g2f = g2.add(fpp);
            final FieldSinCos<FieldUnivariateDerivative1<T>> sc2gf = g2f.sinCos();
            final FieldUnivariateDerivative1<T> c2gf = sc2gf.cos();
            final FieldUnivariateDerivative1<T> s2gf = sc2gf.sin();
            final FieldUnivariateDerivative1<T> eppc2gf = epp.multiply(c2gf);
            final FieldUnivariateDerivative1<T> epps2gf = epp.multiply(s2gf);

            // 2g" + 2f"
            final FieldUnivariateDerivative1<T> g2f2 = g2.add(fpp.multiply(2));
            final FieldSinCos<FieldUnivariateDerivative1<T>> sc2g2f = g2f2.sinCos();
            final FieldUnivariateDerivative1<T> c2g2f = sc2g2f.cos();
            final FieldUnivariateDerivative1<T> s2g2f = sc2g2f.sin();

            // 2g" + 3f"
            final FieldUnivariateDerivative1<T> g2f3 = g2.add(fpp.multiply(3));
            final FieldSinCos<FieldUnivariateDerivative1<T>> sc2g3f = g2f3.sinCos();
            final FieldUnivariateDerivative1<T> c2g3f = sc2g3f.cos();
            final FieldUnivariateDerivative1<T> s2g3f = sc2g3f.sin();

            // e"cos(2g" + 3f")
            final FieldUnivariateDerivative1<T> eppc2g3f = epp.multiply(c2g3f);
            // e"sin(2g" + 3f")
            final FieldUnivariateDerivative1<T> epps2g3f = epp.multiply(s2g3f);

            // f" + e"sin(f") - l"
            final FieldUnivariateDerivative1<T> w17 = fpp.add(eppsfpp).subtract(lpp);

            // ((e"cos(f") + 3)e"cos(f") + 3)cos(f")
            final FieldUnivariateDerivative1<T> w20 = cfpp.multiply(eppcfppP3.multiply(eppcfpp).add(3.));

            // 3sin(2g" + 2f") + 3e"sin(2g" + f") + e"sin(2g" + f")
            final FieldUnivariateDerivative1<T> w21 = s2g2f.add(epps2gf).multiply(3).add(epps2g3f);

            // (1 + e"cos(f"))(2 + e"cos(f"))/η²
            final FieldUnivariateDerivative1<T> w22 = eppcfppP1.multiply(eppcfppP2).divide(n2);

            // sinCos(I"/2)
            final FieldSinCos<T> sci = FastMath.sinCos(mean.getI().divide(2.));
            final T siO2 = sci.sin();
            final T ciO2 = sci.cos();

            // δ2a
            final FieldUnivariateDerivative1<T> d2a = app.multiply(yp2).divide(n2).
                                                      multiply(eppcfppP1_3.subtract(n3).multiply(ci2X3M1).
                                                               add(c2g2f.multiply(eppcfppP1_3).multiply(oneMci2).multiply(3.)));

            // δ2e
            final FieldUnivariateDerivative1<T> d2e = (w20.add(epp.multiply(t8))).multiply(ci2X3M1).
                                                       add((w20.add(epp.multiply(c2g2f))).multiply(oneMci2.multiply(3))).
                                                       subtract((eppc2gf.multiply(3).add(eppc2g3f)).multiply(oneMci2.multiply(n2))).
                                                      multiply(yp2.multiply(0.5));

            // δ2I
            final FieldUnivariateDerivative1<T> d2I = ((c2g2f.add(eppc2gf)).multiply(3).add(eppc2g3f)).
                                                       multiply(yp2.divide(2.).multiply(ci).multiply(si));

            // e"δ2l
            final FieldUnivariateDerivative1<T> eppd2l = (w22.add(1).multiply(sfpp).multiply(oneMci2).multiply(2.).
                                                         add((w22.subtract(1).negate().multiply(s2gf)).
                                                              add(w22.add(1. / 3.).multiply(s2g3f)).
                                                             multiply(oneMci2.multiply(3.)))).
                                                        multiply(yp2.divide(4.).multiply(n3)).negate();

            // sinI"δ2h
            final FieldUnivariateDerivative1<T> sIppd2h = (w21.subtract(w17.multiply(6))).
                                                           multiply(yp2).multiply(ci).multiply(si).divide(2.);

            // δ2z = δ2l + δ2g + δ2h
            final T ttt = one.add(ci.multiply(ci.multiply(-5).add(2.)));
            final FieldUnivariateDerivative1<T> d2z = (epp.multiply(eppd2l).multiply(t8.subtract(one)).divide(n3).
                                                       add(w17.multiply(ttt).multiply(6).subtract(w21.multiply(ttt.add(2.))).
                                                           multiply(yp2.divide(4.)))).
                                                       negate();

            // Assembling elements
            // -------------------

            // e" + δe
            final FieldUnivariateDerivative1<T> de = epp.add(d1e).add(d2e);

            // e"δl
            final FieldUnivariateDerivative1<T> dl = eppd1l.add(eppd2l);

            // sin(I"/2)δh = sin(I")δh / cos(I"/2) (singular for I" = π, very unlikely)
            final FieldUnivariateDerivative1<T> dh = sIppd1h.add(sIppd2h).divide(ciO2.multiply(2.));

            // δI
            final FieldUnivariateDerivative1<T> di = d1I.add(d2I).multiply(ciO2).divide(2.).add(siO2);

            // z = l" + g" + h" + δ1z + δ2z
            final FieldUnivariateDerivative1<T> z = lpp.add(gpp).add(hpp).add(d1z).add(d2z);

            // Osculating elements
            // -------------------

            // Semi-major axis
            final FieldUnivariateDerivative1<T> a = app.add(d2a);

            // Eccentricity
            final FieldUnivariateDerivative1<T> e = FastMath.sqrt(de.square().add(dl.square()));

            // Mean anomaly
            final FieldSinCos<FieldUnivariateDerivative1<T>> sclpp = lpp.sinCos();
            final FieldUnivariateDerivative1<T> clpp = sclpp.cos();
            final FieldUnivariateDerivative1<T> slpp = sclpp.sin();
            final FieldUnivariateDerivative1<T> l = FastMath.atan2(de.multiply(slpp).add(dl.multiply(clpp)),
                                                                   de.multiply(clpp).subtract(dl.multiply(slpp)));

            // Inclination
            final FieldUnivariateDerivative1<T> i = FastMath.acos(di.square().add(dh.square()).multiply(2).negate().add(1.));

            // Longitude of ascending node
            final FieldSinCos<FieldUnivariateDerivative1<T>> schpp = hpp.sinCos();
            final FieldUnivariateDerivative1<T> chpp = schpp.cos();
            final FieldUnivariateDerivative1<T> shpp = schpp.sin();
            final FieldUnivariateDerivative1<T> h = FastMath.atan2(di.multiply(shpp).add(dh.multiply(chpp)),
                                                                   di.multiply(chpp).subtract(dh.multiply(shpp)));

            // Argument of perigee
            final FieldUnivariateDerivative1<T> g = z.subtract(l).subtract(h);

            // Return a Keplerian orbit
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
