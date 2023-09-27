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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTimeSpanMap;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** This class propagates a {@link org.orekit.propagation.FieldSpacecraftState}
 *  using the analytical Eckstein-Hechler model.
 * <p>The Eckstein-Hechler model is suited for near circular orbits
 * (e &lt; 0.1, with poor accuracy between 0.005 and 0.1) and inclination
 * neither equatorial (direct or retrograde) nor critical (direct or
 * retrograde).</p>
 * @see FieldOrbit
 * @author Guylaine Prat
 * @param <T> type of the field elements
 */
public class FieldEcksteinHechlerPropagator<T extends CalculusFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {

    /** Default convergence threshold for mean parameters conversion. */
    private static final double EPSILON_DEFAULT = 1.0e-13;

    /** Default value for maxIterations. */
    private static final int MAX_ITERATIONS_DEFAULT = 100;

    /** Initial Eckstein-Hechler model. */
    private FieldEHModel<T> initialModel;

    /** All models. */
    private transient FieldTimeSpanMap<FieldEHModel<T>, T> models;

    /** Reference radius of the central body attraction model (m). */
    private double referenceRadius;

    /** Central attraction coefficient (m³/s²). */
    private T mu;

    /** Un-normalized zonal coefficients. */
    private double[] ck0;

    /** Build a propagator from FieldOrbit and potential provider.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial FieldOrbit
     * @param provider for un-normalized zonal coefficients
     * @see #FieldEcksteinHechlerPropagator(FieldOrbit, AttitudeProvider,
     * UnnormalizedSphericalHarmonicsProvider)
     * @see #FieldEcksteinHechlerPropagator(FieldOrbit, UnnormalizedSphericalHarmonicsProvider,
     * PropagationType)
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                          final UnnormalizedSphericalHarmonicsProvider provider) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             initialOrbit.getMu().newInstance(DEFAULT_MASS), provider,
             provider.onDate(initialOrbit.getDate().toAbsoluteDate()));
    }

    /**
     * Private helper constructor.
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     * @param initialOrbit initial FieldOrbit
     * @param attitude attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param harmonics {@code provider.onDate(initialOrbit.getDate())}
     * @see #FieldEcksteinHechlerPropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement,
     * UnnormalizedSphericalHarmonicsProvider, UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics, PropagationType)
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                          final AttitudeProvider attitude,
                                          final T mass,
                                          final UnnormalizedSphericalHarmonicsProvider provider,
                                          final UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics harmonics) {
        this(initialOrbit, attitude,  mass, provider.getAe(), initialOrbit.getMu().newInstance(provider.getMu()),
             harmonics.getUnnormalizedCnm(2, 0),
             harmonics.getUnnormalizedCnm(3, 0),
             harmonics.getUnnormalizedCnm(4, 0),
             harmonics.getUnnormalizedCnm(5, 0),
             harmonics.getUnnormalizedCnm(6, 0));
    }

    /** Build a propagator from FieldOrbit and potential.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:
     * <p>
     *   C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     *                      <span style="text-decoration: overline">C</span><sub>n,0</sub>
     * <p>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial FieldOrbit
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @see org.orekit.utils.Constants
     * @see #FieldEcksteinHechlerPropagator(FieldOrbit, AttitudeProvider, double,
     * CalculusFieldElement, double, double, double, double, double)
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                          final double referenceRadius, final T mu,
                                          final double c20, final double c30, final double c40,
                                          final double c50, final double c60) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             initialOrbit.getMu().newInstance(DEFAULT_MASS),
             referenceRadius, mu, c20, c30, c40, c50, c60);
    }

    /** Build a propagator from FieldOrbit, mass and potential provider.
     * <p>Attitude law is set to an unspecified non-null arbitrary value.</p>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial FieldOrbit
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @see #FieldEcksteinHechlerPropagator(FieldOrbit, AttitudeProvider,
     * CalculusFieldElement, UnnormalizedSphericalHarmonicsProvider)
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit, final T mass,
                                          final UnnormalizedSphericalHarmonicsProvider provider) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             mass, provider, provider.onDate(initialOrbit.getDate().toAbsoluteDate()));
    }

    /** Build a propagator from FieldOrbit, mass and potential.
     * <p>Attitude law is set to an unspecified non-null arbitrary value.</p>
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     * <p>
     *   C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     *                      <span style="text-decoration: overline">C</span><sub>n,0</sub>
     * <p>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial FieldOrbit
     * @param mass spacecraft mass
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @see #FieldEcksteinHechlerPropagator(FieldOrbit, AttitudeProvider,
     * CalculusFieldElement, double, CalculusFieldElement, double, double, double, double, double)
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit, final T mass,
                                          final double referenceRadius, final T mu,
                                          final double c20, final double c30, final double c40,
                                          final double c50, final double c60) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             mass, referenceRadius, mu, c20, c30, c40, c50, c60);
    }

    /** Build a propagator from FieldOrbit, attitude provider and potential provider.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     * @param initialOrbit initial FieldOrbit
     * @param attitudeProv attitude provider
     * @param provider for un-normalized zonal coefficients
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                          final AttitudeProvider attitudeProv,
                                          final UnnormalizedSphericalHarmonicsProvider provider) {
        this(initialOrbit, attitudeProv, initialOrbit.getMu().newInstance(DEFAULT_MASS), provider, provider.onDate(initialOrbit.getDate().toAbsoluteDate()));
    }

    /** Build a propagator from FieldOrbit, attitude provider and potential.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     * <p>
     *   C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     *                     <span style="text-decoration: overline">C</span><sub>n,0</sub>
     * <p>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial FieldOrbit
     * @param attitudeProv attitude provider
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                          final AttitudeProvider attitudeProv,
                                          final double referenceRadius, final T mu,
                                          final double c20, final double c30, final double c40,
                                          final double c50, final double c60) {
        this(initialOrbit, attitudeProv, initialOrbit.getMu().newInstance(DEFAULT_MASS),
             referenceRadius, mu, c20, c30, c40, c50, c60);
    }

    /** Build a propagator from FieldOrbit, attitude provider, mass and potential provider.
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     * @param initialOrbit initial FieldOrbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @see #FieldEcksteinHechlerPropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement,
     * UnnormalizedSphericalHarmonicsProvider, PropagationType)
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                          final AttitudeProvider attitudeProv,
                                          final T mass,
                                          final UnnormalizedSphericalHarmonicsProvider provider) {
        this(initialOrbit, attitudeProv, mass, provider, provider.onDate(initialOrbit.getDate().toAbsoluteDate()));
    }

    /** Build a propagator from FieldOrbit, attitude provider, mass and potential.
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     * <p>
     *   C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     *                      <span style="text-decoration: overline">C</span><sub>n,0</sub>
     * <p>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     *
     * <p>Using this constructor, an initial osculating orbit is considered.</p>
     *
     * @param initialOrbit initial FieldOrbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @see #FieldEcksteinHechlerPropagator(FieldOrbit, AttitudeProvider, CalculusFieldElement, double,
     *                                      CalculusFieldElement, double, double, double, double, double, PropagationType)
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                          final AttitudeProvider attitudeProv,
                                          final T mass,
                                          final double referenceRadius, final T mu,
                                          final double c20, final double c30, final double c40,
                                          final double c50, final double c60) {
        this(initialOrbit, attitudeProv, mass, referenceRadius, mu, c20, c30, c40, c50, c60, PropagationType.OSCULATING);
    }

    /** Build a propagator from orbit and potential provider.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     *
     * <p>Using this constructor, it is possible to define the initial orbit as
     * a mean Eckstein-Hechler orbit or an osculating one.</p>
     *
     * @param initialOrbit initial orbit
     * @param provider for un-normalized zonal coefficients
     * @param initialType initial orbit type (mean Eckstein-Hechler orbit or osculating orbit)
     * @since 10.2
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                          final UnnormalizedSphericalHarmonicsProvider provider,
                                          final PropagationType initialType) {
        this(initialOrbit, FrameAlignedProvider.of(initialOrbit.getFrame()),
             initialOrbit.getMu().newInstance(DEFAULT_MASS), provider,
             provider.onDate(initialOrbit.getDate().toAbsoluteDate()), initialType);
    }

    /** Build a propagator from orbit, attitude provider, mass and potential provider.
     * <p>Using this constructor, it is possible to define the initial orbit as
     * a mean Eckstein-Hechler orbit or an osculating one.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param initialType initial orbit type (mean Eckstein-Hechler orbit or osculating orbit)
     * @since 10.2
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                          final AttitudeProvider attitudeProv,
                                          final T mass,
                                          final UnnormalizedSphericalHarmonicsProvider provider,
                                          final PropagationType initialType) {
        this(initialOrbit, attitudeProv, mass, provider, provider.onDate(initialOrbit.getDate().toAbsoluteDate()), initialType);
    }

    /**
     * Private helper constructor.
     * <p>Using this constructor, it is possible to define the initial orbit as
     * a mean Eckstein-Hechler orbit or an osculating one.</p>
     * @param initialOrbit initial orbit
     * @param attitude attitude provider
     * @param mass spacecraft mass
     * @param provider for un-normalized zonal coefficients
     * @param harmonics {@code provider.onDate(initialOrbit.getDate())}
     * @param initialType initial orbit type (mean Eckstein-Hechler orbit or osculating orbit)
     * @since 10.2
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                          final AttitudeProvider attitude,
                                          final T mass,
                                          final UnnormalizedSphericalHarmonicsProvider provider,
                                          final UnnormalizedSphericalHarmonics harmonics,
                                          final PropagationType initialType) {
        this(initialOrbit, attitude, mass, provider.getAe(), initialOrbit.getMu().newInstance(provider.getMu()),
             harmonics.getUnnormalizedCnm(2, 0),
             harmonics.getUnnormalizedCnm(3, 0),
             harmonics.getUnnormalizedCnm(4, 0),
             harmonics.getUnnormalizedCnm(5, 0),
             harmonics.getUnnormalizedCnm(6, 0),
             initialType);
    }

    /** Build a propagator from FieldOrbit, attitude provider, mass and potential.
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     * <p>
     *   C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     *                      <span style="text-decoration: overline">C</span><sub>n,0</sub>
     * <p>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     *
     * <p>Using this constructor, it is possible to define the initial orbit as
     * a mean Eckstein-Hechler orbit or an osculating one.</p>
     *
     * @param initialOrbit initial FieldOrbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @param initialType initial orbit type (mean Eckstein-Hechler orbit or osculating orbit)
     * @since 10.2
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                          final AttitudeProvider attitudeProv,
                                          final T mass,
                                          final double referenceRadius, final T mu,
                                          final double c20, final double c30, final double c40,
                                          final double c50, final double c60,
                                          final PropagationType initialType) {
        this(initialOrbit, attitudeProv, mass, referenceRadius, mu,
             c20, c30, c40, c50, c60, initialType, EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT);
    }

    /** Build a propagator from FieldOrbit, attitude provider, mass and potential.
     * <p>The C<sub>n,0</sub> coefficients are the denormalized zonal coefficients, they
     * are related to both the normalized coefficients
     * <span style="text-decoration: overline">C</span><sub>n,0</sub>
     *  and the J<sub>n</sub> one as follows:</p>
     * <p>
     *   C<sub>n,0</sub> = [(2-δ<sub>0,m</sub>)(2n+1)(n-m)!/(n+m)!]<sup>½</sup>
     *                      <span style="text-decoration: overline">C</span><sub>n,0</sub>
     * <p>
     *   C<sub>n,0</sub> = -J<sub>n</sub>
     *
     * <p>Using this constructor, it is possible to define the initial orbit as
     * a mean Eckstein-Hechler orbit or an osculating one.</p>
     *
     * @param initialOrbit initial FieldOrbit
     * @param attitudeProv attitude provider
     * @param mass spacecraft mass
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @param initialType initial orbit type (mean Eckstein-Hechler orbit or osculating orbit)
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @since 11.2
     */
    public FieldEcksteinHechlerPropagator(final FieldOrbit<T> initialOrbit,
                                          final AttitudeProvider attitudeProv,
                                          final T mass,
                                          final double referenceRadius, final T mu,
                                          final double c20, final double c30, final double c40,
                                          final double c50, final double c60,
                                          final PropagationType initialType,
                                          final double epsilon, final int maxIterations) {
        super(mass.getField(), attitudeProv);
        try {

            // store model coefficients
            this.referenceRadius = referenceRadius;
            this.mu  = mu;
            this.ck0 = new double[] {
                0.0, 0.0, c20, c30, c40, c50, c60
            };

            // compute mean parameters if needed
            // transform into circular adapted parameters used by the Eckstein-Hechler model
            resetInitialState(new FieldSpacecraftState<>(initialOrbit,
                                                         attitudeProv.getAttitude(initialOrbit,
                                                                                  initialOrbit.getDate(),
                                                                                  initialOrbit.getFrame()),
                                                         mass),
                              initialType, epsilon, maxIterations);

        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

    /** Conversion from osculating to mean orbit.
     * <p>
     * Compute mean orbit <b>in a Eckstein-Hechler sense</b>, corresponding to the
     * osculating SpacecraftState in input.
     * </p>
     * <p>
     * Since the osculating orbit is obtained with the computation of
     * short-periodic variation, the resulting output will depend on
     * the gravity field parameterized in input.
     * </p>
     * <p>
     * The computation is done through a fixed-point iteration process.
     * </p>
     * @param <T> type of the filed elements
     * @param osculating osculating orbit to convert
     * @param provider for un-normalized zonal coefficients
     * @param harmonics {@code provider.onDate(osculating.getDate())}
     * @return mean orbit in a Eckstein-Hechler sense
     * @since 11.2
     */
    public static <T extends CalculusFieldElement<T>> FieldCircularOrbit<T> computeMeanOrbit(final FieldOrbit<T> osculating,
                                                                                             final UnnormalizedSphericalHarmonicsProvider provider,
                                                                                             final UnnormalizedSphericalHarmonics harmonics) {
        return computeMeanOrbit(osculating, provider, harmonics, EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT);
    }

    /** Conversion from osculating to mean orbit.
     * <p>
     * Compute mean orbit <b>in a Eckstein-Hechler sense</b>, corresponding to the
     * osculating SpacecraftState in input.
     * </p>
     * <p>
     * Since the osculating orbit is obtained with the computation of
     * short-periodic variation, the resulting output will depend on
     * the gravity field parameterized in input.
     * </p>
     * <p>
     * The computation is done through a fixed-point iteration process.
     * </p>
     * @param <T> type of the filed elements
     * @param osculating osculating orbit to convert
     * @param provider for un-normalized zonal coefficients
     * @param harmonics {@code provider.onDate(osculating.getDate())}
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @return mean orbit in a Eckstein-Hechler sense
     * @since 11.2
     */
    public static <T extends CalculusFieldElement<T>> FieldCircularOrbit<T> computeMeanOrbit(final FieldOrbit<T> osculating,
                                                                                             final UnnormalizedSphericalHarmonicsProvider provider,
                                                                                             final UnnormalizedSphericalHarmonics harmonics,
                                                                                             final double epsilon, final int maxIterations) {
        return computeMeanOrbit(osculating,
                                provider.getAe(), provider.getMu(),
                                harmonics.getUnnormalizedCnm(2, 0),
                                harmonics.getUnnormalizedCnm(3, 0),
                                harmonics.getUnnormalizedCnm(4, 0),
                                harmonics.getUnnormalizedCnm(5, 0),
                                harmonics.getUnnormalizedCnm(6, 0),
                                epsilon, maxIterations);
    }

    /** Conversion from osculating to mean orbit.
     * <p>
     * Compute mean orbit <b>in a Eckstein-Hechler sense</b>, corresponding to the
     * osculating SpacecraftState in input.
     * </p>
     * <p>
     * Since the osculating orbit is obtained with the computation of
     * short-periodic variation, the resulting output will depend on
     * the gravity field parameterized in input.
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
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @return mean orbit in a Eckstein-Hechler sense
     * @since 11.2
     */
    public static <T extends CalculusFieldElement<T>> FieldCircularOrbit<T> computeMeanOrbit(final FieldOrbit<T> osculating,
                                                                                             final double referenceRadius, final double mu,
                                                                                             final double c20, final double c30, final double c40,
                                                                                             final double c50, final double c60,
                                                                                             final double epsilon, final int maxIterations) {
        final FieldEcksteinHechlerPropagator<T> propagator =
                        new FieldEcksteinHechlerPropagator<>(osculating,
                                                             FrameAlignedProvider.of(osculating.getFrame()),
                                                             osculating.getMu().newInstance(DEFAULT_MASS),
                                                             referenceRadius, osculating.getMu().newInstance(mu),
                                                             c20, c30, c40, c50, c60,
                                                             PropagationType.OSCULATING, epsilon, maxIterations);
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
     * @param stateType mean Eckstein-Hechler orbit or osculating orbit
     * @since 10.2
     */
    public void resetInitialState(final FieldSpacecraftState<T> state, final PropagationType stateType) {
        resetInitialState(state, stateType, EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT);
    }

    /** Reset the propagator initial state.
     * @param state new initial state to consider
     * @param stateType mean Eckstein-Hechler orbit or osculating orbit
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @since 11.2
     */
    public void resetInitialState(final FieldSpacecraftState<T> state, final PropagationType stateType,
                                  final double epsilon, final int maxIterations) {
        super.resetInitialState(state);
        final FieldCircularOrbit<T> circular = (FieldCircularOrbit<T>) OrbitType.CIRCULAR.convertType(state.getOrbit());
        this.initialModel = (stateType == PropagationType.MEAN) ?
                             new FieldEHModel<>(circular, state.getMass(), referenceRadius, mu, ck0) :
                             computeMeanParameters(circular, state.getMass(), epsilon, maxIterations);
        this.models = new FieldTimeSpanMap<FieldEHModel<T>, T>(initialModel, state.getA().getField());
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
        final FieldEHModel<T> newModel = computeMeanParameters((FieldCircularOrbit<T>) OrbitType.CIRCULAR.convertType(state.getOrbit()),
                                                               state.getMass(), epsilon, maxIterations);
        if (forward) {
            models.addValidAfter(newModel, state.getDate());
        } else {
            models.addValidBefore(newModel, state.getDate());
        }
        stateChanged(state);
    }

    /** Compute mean parameters according to the Eckstein-Hechler analytical model.
     * @param osculating osculating FieldOrbit
     * @param mass constant mass
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @return Eckstein-Hechler mean model
     */
    private FieldEHModel<T> computeMeanParameters(final FieldCircularOrbit<T> osculating, final T mass,
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
        FieldEHModel<T> current = new FieldEHModel<>(osculating, mass, referenceRadius, mu, ck0);
        // threshold for each parameter
        final T thresholdA      = current.mean.getA().abs().add(1.0).multiply(epsilon);
        final T thresholdE      = current.mean.getE().add(1.0).multiply(epsilon);
        final T thresholdAngles = one.getPi().multiply(2).multiply(epsilon);


        int i = 0;
        while (i++ < maxIterations) {

            // recompute the osculating parameters from the current mean parameters
            final FieldUnivariateDerivative2<T>[] parameters = current.propagateParameters(current.mean.getDate());
            // adapted parameters residuals
            final T deltaA      = osculating.getA()         .subtract(parameters[0].getValue());
            final T deltaEx     = osculating.getCircularEx().subtract(parameters[1].getValue());
            final T deltaEy     = osculating.getCircularEy().subtract(parameters[2].getValue());
            final T deltaI      = osculating.getI()         .subtract(parameters[3].getValue());
            final T deltaRAAN   = MathUtils.normalizeAngle(osculating.getRightAscensionOfAscendingNode().subtract(
                                                                parameters[4].getValue()),
                                                                zero);
            final T deltaAlphaM = MathUtils.normalizeAngle(osculating.getAlphaM().subtract(parameters[5].getValue()), zero);
            // update mean parameters
            current = new FieldEHModel<>(new FieldCircularOrbit<>(current.mean.getA().add(deltaA),
                                                                  current.mean.getCircularEx().add( deltaEx),
                                                                  current.mean.getCircularEy().add( deltaEy),
                                                                  current.mean.getI()         .add( deltaI ),
                                                                  current.mean.getRightAscensionOfAscendingNode().add(deltaRAAN),
                                                                  current.mean.getAlphaM().add(deltaAlphaM),
                                                                  PositionAngleType.MEAN,
                                                                  current.mean.getFrame(),
                                                                  current.mean.getDate(), mu),
                                  mass, referenceRadius, mu, ck0);
            // check convergence
            if (FastMath.abs(deltaA.getReal())      < thresholdA.getReal() &&
                FastMath.abs(deltaEx.getReal())     < thresholdE.getReal() &&
                FastMath.abs(deltaEy.getReal())     < thresholdE.getReal() &&
                FastMath.abs(deltaI.getReal())      < thresholdAngles.getReal() &&
                FastMath.abs(deltaRAAN.getReal())   < thresholdAngles.getReal() &&
                FastMath.abs(deltaAlphaM.getReal()) < thresholdAngles.getReal()) {
                return current;
            }

        }

        throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_ECKSTEIN_HECHLER_MEAN_PARAMETERS, i);

    }

    /** {@inheritDoc} */
    @Override
    public FieldCartesianOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date, final T[] parameters) {
        // compute Cartesian parameters, taking derivatives into account
        // to make sure velocity and acceleration are consistent
        final FieldEHModel<T> current = models.get(date);
        return new FieldCartesianOrbit<>(toCartesian(date, current.propagateParameters(date)),
                                         current.mean.getFrame(), mu);
    }

    /** Local class for Eckstein-Hechler model, with fixed mean parameters. */
    private static class FieldEHModel<T extends CalculusFieldElement<T>> {

        /** Mean FieldOrbit. */
        private final FieldCircularOrbit<T> mean;

        /** Constant mass. */
        private final T mass;
        // CHECKSTYLE: stop JavadocVariable check

        // preprocessed values
        private final T xnotDot;
        private final T rdpom;
        private final T rdpomp;
        private final T eps1;
        private final T eps2;
        private final T xim;
        private final T ommD;
        private final T rdl;
        private final T aMD;

        private final T kh;
        private final T kl;

        private final T ax1;
        private final T ay1;
        private final T as1;
        private final T ac2;
        private final T axy3;
        private final T as3;
        private final T ac4;
        private final T as5;
        private final T ac6;

        private final T ex1;
        private final T exx2;
        private final T exy2;
        private final T ex3;
        private final T ex4;

        private final T ey1;
        private final T eyx2;
        private final T eyy2;
        private final T ey3;
        private final T ey4;

        private final T rx1;
        private final T ry1;
        private final T r2;
        private final T r3;
        private final T rl;

        private final T iy1;
        private final T ix1;
        private final T i2;
        private final T i3;
        private final T ih;

        private final T lx1;
        private final T ly1;
        private final T l2;
        private final T l3;
        private final T ll;

        // CHECKSTYLE: resume JavadocVariable check

        /** Create a model for specified mean FieldOrbit.
         * @param mean mean FieldOrbit
         * @param mass constant mass
         * @param referenceRadius reference radius of the central body attraction model (m)
         * @param mu central attraction coefficient (m³/s²)
         * @param ck0 un-normalized zonal coefficients
         */
        FieldEHModel(final FieldCircularOrbit<T> mean, final T mass,
                     final double referenceRadius, final T mu, final double[] ck0) {

            this.mean            = mean;
            this.mass            = mass;
            final T zero = mass.getField().getZero();
            final T one  = mass.getField().getOne();
            // preliminary processing
            T q =  zero.add(referenceRadius).divide(mean.getA());
            T ql = q.multiply(q);
            final T g2 = ql.multiply(ck0[2]);
            ql = ql.multiply(q);
            final T g3 = ql.multiply(ck0[3]);
            ql = ql.multiply(q);
            final T g4 = ql.multiply(ck0[4]);
            ql = ql.multiply(q);
            final T g5 = ql.multiply(ck0[5]);
            ql = ql.multiply(q);
            final T g6 = ql.multiply(ck0[6]);

            final FieldSinCos<T> sc = FastMath.sinCos(mean.getI());
            final T cosI1 = sc.cos();
            final T sinI1 = sc.sin();
            final T sinI2 = sinI1.multiply(sinI1);
            final T sinI4 = sinI2.multiply(sinI2);
            final T sinI6 = sinI2.multiply(sinI4);

            if (sinI2.getReal() < 1.0e-10) {
                throw new OrekitException(OrekitMessages.ALMOST_EQUATORIAL_ORBIT,
                                               FastMath.toDegrees(mean.getI().getReal()));
            }

            if (FastMath.abs(sinI2.getReal() - 4.0 / 5.0) < 1.0e-3) {
                throw new OrekitException(OrekitMessages.ALMOST_CRITICALLY_INCLINED_ORBIT,
                                               FastMath.toDegrees(mean.getI().getReal()));
            }

            if (mean.getE().getReal() > 0.1) {
                // if 0.005 < e < 0.1 no error is triggered, but accuracy is poor
                throw new OrekitException(OrekitMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL,
                                               mean.getE());
            }

            xnotDot = mu.divide(mean.getA()).sqrt().divide(mean.getA());

            rdpom = g2.multiply(-0.75).multiply(sinI2.multiply(-5.0).add(4.0));
            rdpomp = g4.multiply(7.5).multiply(sinI2.multiply(-31.0 / 8.0).add(1.0).add( sinI4.multiply(49.0 / 16.0))).subtract(
                    g6.multiply(13.125).multiply(one.subtract(sinI2.multiply(8.0)).add(sinI4.multiply(129.0 / 8.0)).subtract(sinI6.multiply(297.0 / 32.0)) ));


            q = zero.add(3.0).divide(rdpom.multiply(32.0));
            eps1 = q.multiply(g4).multiply(sinI2).multiply(sinI2.multiply(-35.0).add(30.0)).subtract(
                   q.multiply(175.0).multiply(g6).multiply(sinI2).multiply(sinI2.multiply(-3.0).add(sinI4.multiply(2.0625)).add(1.0)));
            q = sinI1.multiply(3.0).divide(rdpom.multiply(8.0));
            eps2 = q.multiply(g3).multiply(sinI2.multiply(-5.0).add(4.0)).subtract(q.multiply(g5).multiply(sinI2.multiply(-35.0).add(sinI4.multiply(26.25)).add(10.0)));

            xim = mean.getI();
            ommD = cosI1.multiply(g2.multiply(1.50).subtract(g2.multiply(2.25).multiply(g2).multiply(sinI2.multiply(-19.0 / 6.0).add(2.5))).add(
                            g4.multiply(0.9375).multiply(sinI2.multiply(7.0).subtract(4.0))).add(
                            g6.multiply(3.28125).multiply(sinI2.multiply(-9.0).add(2.0).add(sinI4.multiply(8.25)))));

            rdl = g2.multiply(-1.50).multiply(sinI2.multiply(-4.0).add(3.0)).add(1.0);
            aMD = rdl.add(
                    g2.multiply(2.25).multiply(g2.multiply(sinI2.multiply(-263.0 / 12.0 ).add(9.0).add(sinI4.multiply(341.0 / 24.0))))).add(
                    g4.multiply(15.0 / 16.0).multiply(sinI2.multiply(-31.0).add(8.0).add(sinI4.multiply(24.5)))).add(
                    g6.multiply(105.0 / 32.0).multiply(sinI2.multiply(25.0).add(-10.0 / 3.0).subtract(sinI4.multiply(48.75)).add(sinI6.multiply(27.5))));

            final T qq   = g2.divide(rdl).multiply(-1.5);
            final T qA   = g2.multiply(0.75).multiply(g2).multiply(sinI2);
            final T qB   = g4.multiply(0.25).multiply(sinI2);
            final T qC   = g6.multiply(105.0 / 16.0).multiply(sinI2);
            final T qD   = g3.multiply(-0.75).multiply(sinI1);
            final T qE   = g5.multiply(3.75).multiply(sinI1);
            kh = zero.add(0.375).divide(rdpom);
            kl = kh.divide(sinI1);

            ax1 = qq.multiply(sinI2.multiply(-3.5).add(2.0));
            ay1 = qq.multiply(sinI2.multiply(-2.5).add(2.0));
            as1 = qD.multiply(sinI2.multiply(-5.0).add(4.0)).add(
                  qE.multiply(sinI4.multiply(2.625).add(sinI2.multiply(-3.5)).add(1.0)));
            ac2 = qq.multiply(sinI2).add(
                  qA.multiply(7.0).multiply(sinI2.multiply(-3.0).add(2.0))).add(
                  qB.multiply(sinI2.multiply(-17.5).add(15.0))).add(
                  qC.multiply(sinI2.multiply(3.0).subtract(1.0).subtract(sinI4.multiply(33.0 / 16.0))));
            axy3 = qq.multiply(3.5).multiply(sinI2);
            as3 = qD.multiply(5.0 / 3.0).multiply(sinI2).add(
                  qE.multiply(7.0 / 6.0).multiply(sinI2).multiply(sinI2.multiply(-1.125).add(1)));
            ac4 = qA.multiply(sinI2).add(
                  qB.multiply(4.375).multiply(sinI2)).add(
                  qC.multiply(0.75).multiply(sinI4.multiply(1.1).subtract(sinI2)));

            as5 = qE.multiply(21.0 / 80.0).multiply(sinI4);

            ac6 = qC.multiply(-11.0 / 80.0).multiply(sinI4);

            ex1 = qq.multiply(sinI2.multiply(-1.25).add(1.0));
            exx2 = qq.multiply(0.5).multiply(sinI2.multiply(-5.0).add(3.0));
            exy2 = qq.multiply(sinI2.multiply(-1.5).add(2.0));
            ex3 = qq.multiply(7.0 / 12.0).multiply(sinI2);
            ex4 = qq.multiply(17.0 / 8.0).multiply(sinI2);

            ey1 = qq.multiply(sinI2.multiply(-1.75).add(1.0));
            eyx2 = qq.multiply(sinI2.multiply(-3.0).add(1.0));
            eyy2 = qq.multiply(sinI2.multiply(2.0).subtract(1.5));
            ey3 = qq.multiply(7.0 / 12.0).multiply(sinI2);
            ey4 = qq.multiply(17.0 / 8.0).multiply(sinI2);

            q  = cosI1.multiply(qq).negate();
            rx1 = q.multiply(3.5);
            ry1 = q.multiply(-2.5);
            r2 = q.multiply(-0.5);
            r3 =  q.multiply(7.0 / 6.0);
            rl = g3 .multiply( cosI1).multiply(sinI2.multiply(-15.0).add(4.0)).subtract(
                 g5.multiply(2.5).multiply(cosI1).multiply(sinI2.multiply(-42.0).add(4.0).add(sinI4.multiply(52.5))));

            q = qq.multiply(0.5).multiply(sinI1).multiply(cosI1);
            iy1 =  q;
            ix1 = q.negate();
            i2 =  q;
            i3 =  q.multiply(7.0 / 3.0);
            ih = g3.negate().multiply(cosI1).multiply(sinI2.multiply(-5.0).add(4)).add(
                 g5.multiply(2.5).multiply(cosI1).multiply(sinI2.multiply(-14.0).add(4.0).add(sinI4.multiply(10.5))));
            lx1 = qq.multiply(sinI2.multiply(-77.0 / 8.0).add(7.0));
            ly1 = qq.multiply(sinI2.multiply(55.0 / 8.0).subtract(7.50));
            l2 = qq.multiply(sinI2.multiply(1.25).subtract(0.5));
            l3 = qq.multiply(sinI2.multiply(77.0 / 24.0).subtract(7.0 / 6.0));
            ll = g3.multiply(sinI2.multiply(53.0).subtract(4.0).add(sinI4.multiply(-57.5))).add(
                 g5.multiply(2.5).multiply(sinI2.multiply(-96.0).add(4.0).add(sinI4.multiply(269.5).subtract(sinI6.multiply(183.75)))));

        }

        /** Extrapolate a FieldOrbit up to a specific target date.
         * @param date target date for the FieldOrbit
         * @return propagated parameters
         */
        public FieldUnivariateDerivative2<T>[] propagateParameters(final FieldAbsoluteDate<T> date) {
            final Field<T> field = date.durationFrom(mean.getDate()).getField();
            final T one = field.getOne();
            final T zero = field.getZero();
            // Keplerian evolution
            final FieldUnivariateDerivative2<T> dt = new FieldUnivariateDerivative2<>(date.durationFrom(mean.getDate()), one, zero);
            final FieldUnivariateDerivative2<T> xnot = dt.multiply(xnotDot);

            // secular effects

            // eccentricity
            final FieldUnivariateDerivative2<T> x   = xnot.multiply(rdpom.add(rdpomp));
            final FieldUnivariateDerivative2<T> cx  = x.cos();
            final FieldUnivariateDerivative2<T> sx  = x.sin();
            final FieldUnivariateDerivative2<T> exm = cx.multiply(mean.getCircularEx()).
                                            add(sx.multiply(eps2.subtract(one.subtract(eps1).multiply(mean.getCircularEy()))));
            final FieldUnivariateDerivative2<T> eym = sx.multiply(eps1.add(1.0).multiply(mean.getCircularEx())).
                                            add(cx.multiply(mean.getCircularEy().subtract(eps2))).
                                            add(eps2);
            // no secular effect on inclination

            // right ascension of ascending node
            final FieldUnivariateDerivative2<T> omm =
                           new FieldUnivariateDerivative2<>(MathUtils.normalizeAngle(mean.getRightAscensionOfAscendingNode().add(ommD.multiply(xnot.getValue())),
                                                                                     one.getPi()),
                                                            ommD.multiply(xnotDot),
                                                            zero);
            // latitude argument
            final FieldUnivariateDerivative2<T> xlm =
                            new FieldUnivariateDerivative2<>(MathUtils.normalizeAngle(mean.getAlphaM().add(aMD.multiply(xnot.getValue())),
                                                                                      one.getPi()),
                                                           aMD.multiply(xnotDot),
                                                           zero);

            // periodical terms
            final FieldUnivariateDerivative2<T> cl1 = xlm.cos();
            final FieldUnivariateDerivative2<T> sl1 = xlm.sin();
            final FieldUnivariateDerivative2<T> cl2 = cl1.multiply(cl1).subtract(sl1.multiply(sl1));
            final FieldUnivariateDerivative2<T> sl2 = cl1.multiply(sl1).add(sl1.multiply(cl1));
            final FieldUnivariateDerivative2<T> cl3 = cl2.multiply(cl1).subtract(sl2.multiply(sl1));
            final FieldUnivariateDerivative2<T> sl3 = cl2.multiply(sl1).add(sl2.multiply(cl1));
            final FieldUnivariateDerivative2<T> cl4 = cl3.multiply(cl1).subtract(sl3.multiply(sl1));
            final FieldUnivariateDerivative2<T> sl4 = cl3.multiply(sl1).add(sl3.multiply(cl1));
            final FieldUnivariateDerivative2<T> cl5 = cl4.multiply(cl1).subtract(sl4.multiply(sl1));
            final FieldUnivariateDerivative2<T> sl5 = cl4.multiply(sl1).add(sl4.multiply(cl1));
            final FieldUnivariateDerivative2<T> cl6 = cl5.multiply(cl1).subtract(sl5.multiply(sl1));

            final FieldUnivariateDerivative2<T> qh  = eym.subtract(eps2).multiply(kh);
            final FieldUnivariateDerivative2<T> ql  = exm.multiply(kl);

            final FieldUnivariateDerivative2<T> exmCl1 = exm.multiply(cl1);
            final FieldUnivariateDerivative2<T> exmSl1 = exm.multiply(sl1);
            final FieldUnivariateDerivative2<T> eymCl1 = eym.multiply(cl1);
            final FieldUnivariateDerivative2<T> eymSl1 = eym.multiply(sl1);
            final FieldUnivariateDerivative2<T> exmCl2 = exm.multiply(cl2);
            final FieldUnivariateDerivative2<T> exmSl2 = exm.multiply(sl2);
            final FieldUnivariateDerivative2<T> eymCl2 = eym.multiply(cl2);
            final FieldUnivariateDerivative2<T> eymSl2 = eym.multiply(sl2);
            final FieldUnivariateDerivative2<T> exmCl3 = exm.multiply(cl3);
            final FieldUnivariateDerivative2<T> exmSl3 = exm.multiply(sl3);
            final FieldUnivariateDerivative2<T> eymCl3 = eym.multiply(cl3);
            final FieldUnivariateDerivative2<T> eymSl3 = eym.multiply(sl3);
            final FieldUnivariateDerivative2<T> exmCl4 = exm.multiply(cl4);
            final FieldUnivariateDerivative2<T> exmSl4 = exm.multiply(sl4);
            final FieldUnivariateDerivative2<T> eymCl4 = eym.multiply(cl4);
            final FieldUnivariateDerivative2<T> eymSl4 = eym.multiply(sl4);

            // semi major axis
            final FieldUnivariateDerivative2<T> rda = exmCl1.multiply(ax1).
                                            add(eymSl1.multiply(ay1)).
                                            add(sl1.multiply(as1)).
                                            add(cl2.multiply(ac2)).
                                            add(exmCl3.add(eymSl3).multiply(axy3)).
                                            add(sl3.multiply(as3)).
                                            add(cl4.multiply(ac4)).
                                            add(sl5.multiply(as5)).
                                            add(cl6.multiply(ac6));

            // eccentricity
            final FieldUnivariateDerivative2<T> rdex = cl1.multiply(ex1).
                                             add(exmCl2.multiply(exx2)).
                                             add(eymSl2.multiply(exy2)).
                                             add(cl3.multiply(ex3)).
                                             add(exmCl4.add(eymSl4).multiply(ex4));
            final FieldUnivariateDerivative2<T> rdey = sl1.multiply(ey1).
                                             add(exmSl2.multiply(eyx2)).
                                             add(eymCl2.multiply(eyy2)).
                                             add(sl3.multiply(ey3)).
                                             add(exmSl4.subtract(eymCl4).multiply(ey4));

            // ascending node
            final FieldUnivariateDerivative2<T> rdom = exmSl1.multiply(rx1).
                                             add(eymCl1.multiply(ry1)).
                                             add(sl2.multiply(r2)).
                                             add(eymCl3.subtract(exmSl3).multiply(r3)).
                                             add(ql.multiply(rl));

            // inclination
            final FieldUnivariateDerivative2<T> rdxi = eymSl1.multiply(iy1).
                                             add(exmCl1.multiply(ix1)).
                                             add(cl2.multiply(i2)).
                                             add(exmCl3.add(eymSl3).multiply(i3)).
                                             add(qh.multiply(ih));

            // latitude argument
            final FieldUnivariateDerivative2<T> rdxl = exmSl1.multiply(lx1).
                                             add(eymCl1.multiply(ly1)).
                                             add(sl2.multiply(l2)).
                                             add(exmSl3.subtract(eymCl3).multiply(l3)).
                                             add(ql.multiply(ll));
            // osculating parameters
            final FieldUnivariateDerivative2<T>[] FTD = MathArrays.buildArray(rdxl.getField(), 6);

            FTD[0] = rda.add(1.0).multiply(mean.getA());
            FTD[1] = rdex.add(exm);
            FTD[2] = rdey.add(eym);
            FTD[3] = rdxi.add(xim);
            FTD[4] = rdom.add(omm);
            FTD[5] = rdxl.add(xlm);
            return FTD;

        }

    }

    /** Convert circular parameters <em>with derivatives</em> to Cartesian coordinates.
     * @param date date of the parameters
     * @param parameters circular parameters (a, ex, ey, i, raan, alphaM)
     * @return Cartesian coordinates consistent with values and derivatives
     */
    private TimeStampedFieldPVCoordinates<T> toCartesian(final FieldAbsoluteDate<T> date, final FieldUnivariateDerivative2<T>[] parameters) {

        // evaluate coordinates in the FieldOrbit canonical reference frame
        final FieldUnivariateDerivative2<T> cosOmega = parameters[4].cos();
        final FieldUnivariateDerivative2<T> sinOmega = parameters[4].sin();
        final FieldUnivariateDerivative2<T> cosI     = parameters[3].cos();
        final FieldUnivariateDerivative2<T> sinI     = parameters[3].sin();
        final FieldUnivariateDerivative2<T> alphaE   = meanToEccentric(parameters[5], parameters[1], parameters[2]);
        final FieldUnivariateDerivative2<T> cosAE    = alphaE.cos();
        final FieldUnivariateDerivative2<T> sinAE    = alphaE.sin();
        final FieldUnivariateDerivative2<T> ex2      = parameters[1].multiply(parameters[1]);
        final FieldUnivariateDerivative2<T> ey2      = parameters[2].multiply(parameters[2]);
        final FieldUnivariateDerivative2<T> exy      = parameters[1].multiply(parameters[2]);
        final FieldUnivariateDerivative2<T> q        = ex2.add(ey2).subtract(1).negate().sqrt();
        final FieldUnivariateDerivative2<T> beta     = q.add(1).reciprocal();
        final FieldUnivariateDerivative2<T> bx2      = beta.multiply(ex2);
        final FieldUnivariateDerivative2<T> by2      = beta.multiply(ey2);
        final FieldUnivariateDerivative2<T> bxy      = beta.multiply(exy);
        final FieldUnivariateDerivative2<T> u        = bxy.multiply(sinAE).subtract(parameters[1].add(by2.subtract(1).multiply(cosAE)));
        final FieldUnivariateDerivative2<T> v        = bxy.multiply(cosAE).subtract(parameters[2].add(bx2.subtract(1).multiply(sinAE)));
        final FieldUnivariateDerivative2<T> x        = parameters[0].multiply(u);
        final FieldUnivariateDerivative2<T> y        = parameters[0].multiply(v);

        // canonical FieldOrbit reference frame
        final FieldVector3D<FieldUnivariateDerivative2<T>> p =
                new FieldVector3D<>(x.multiply(cosOmega).subtract(y.multiply(cosI.multiply(sinOmega))),
                                    x.multiply(sinOmega).add(y.multiply(cosI.multiply(cosOmega))),
                                    y.multiply(sinI));

        // dispatch derivatives
        final FieldVector3D<T> p0 = new FieldVector3D<>(p.getX().getValue(),
                                                        p.getY().getValue(),
                                                        p.getZ().getValue());
        final FieldVector3D<T> p1 = new FieldVector3D<>(p.getX().getFirstDerivative(),
                                                        p.getY().getFirstDerivative(),
                                                        p.getZ().getFirstDerivative());
        final FieldVector3D<T> p2 = new FieldVector3D<>(p.getX().getSecondDerivative(),
                                                        p.getY().getSecondDerivative(),
                                                        p.getZ().getSecondDerivative());
        return new TimeStampedFieldPVCoordinates<>(date, p0, p1, p2);

    }

    /** Computes the eccentric latitude argument from the mean latitude argument.
     * @param alphaM = M + Ω mean latitude argument (rad)
     * @param ex e cos(Ω), first component of circular eccentricity vector
     * @param ey e sin(Ω), second component of circular eccentricity vector
     * @return the eccentric latitude argument.
     */
    private FieldUnivariateDerivative2<T> meanToEccentric(final FieldUnivariateDerivative2<T> alphaM,
                                                final FieldUnivariateDerivative2<T> ex,
                                                final FieldUnivariateDerivative2<T> ey) {
        // Generalization of Kepler equation to circular parameters
        // with alphaE = PA + E and
        //      alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)
        FieldUnivariateDerivative2<T> alphaE        = alphaM;
        FieldUnivariateDerivative2<T> shift         = alphaM.getField().getZero();
        FieldUnivariateDerivative2<T> alphaEMalphaM = alphaM.getField().getZero();
        FieldUnivariateDerivative2<T> cosAlphaE     = alphaE.cos();
        FieldUnivariateDerivative2<T> sinAlphaE     = alphaE.sin();
        int                 iter          = 0;
        do {
            final FieldUnivariateDerivative2<T> f2 = ex.multiply(sinAlphaE).subtract(ey.multiply(cosAlphaE));
            final FieldUnivariateDerivative2<T> f1 = alphaM.getField().getOne().subtract(ex.multiply(cosAlphaE)).subtract(ey.multiply(sinAlphaE));
            final FieldUnivariateDerivative2<T> f0 = alphaEMalphaM.subtract(f2);

            final FieldUnivariateDerivative2<T> f12 = f1.multiply(2);
            shift = f0.multiply(f12).divide(f1.multiply(f12).subtract(f0.multiply(f2)));

            alphaEMalphaM  = alphaEMalphaM.subtract(shift);
            alphaE         = alphaM.add(alphaEMalphaM);
            cosAlphaE      = alphaE.cos();
            sinAlphaE      = alphaE.sin();

        } while (++iter < 50 && FastMath.abs(shift.getValue().getReal()) > 1.0e-12);

        return alphaE;

    }

    /** {@inheritDoc} */
    @Override
    protected T getMass(final FieldAbsoluteDate<T> date) {
        return models.get(date).mass;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        // Eckstein Hechler propagation model does not have parameter drivers.
        return Collections.emptyList();
    }

}
