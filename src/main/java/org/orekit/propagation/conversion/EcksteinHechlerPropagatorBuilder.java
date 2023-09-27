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
package org.orekit.propagation.conversion;

import java.util.List;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.leastsquares.AbstractBatchLSModel;
import org.orekit.estimation.leastsquares.BatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.utils.ParameterDriversList;

/** Builder for Eckstein-Hechler propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class EcksteinHechlerPropagatorBuilder extends AbstractPropagatorBuilder {

    /** Provider for un-normalized coefficients. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /** Build a new instance.
     * <p>
     * The template orbit is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, the orbit type, and is also
     * used together with the {@code positionScale} to convert from the {@link
     * org.orekit.utils.ParameterDriver#setNormalizedValue(double) normalized} parameters used by the
     * callers of this builder to the real orbital parameters.
     * The default attitude provider is aligned with the orbit's inertial frame.
     * </p>
     *
     * @param templateOrbit reference orbit from which real orbits will be built
     * (note that the mu from this orbit will be overridden with the mu from the
     * {@code provider})
     * @param provider for un-normalized zonal coefficients
     * @param positionAngleType position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @since 8.0
     * @see #EcksteinHechlerPropagatorBuilder(Orbit,
     * UnnormalizedSphericalHarmonicsProvider, PositionAngleType, double, AttitudeProvider)
     */
    public EcksteinHechlerPropagatorBuilder(final Orbit templateOrbit,
                                            final UnnormalizedSphericalHarmonicsProvider provider,
                                            final PositionAngleType positionAngleType,
                                            final double positionScale) {
        this(templateOrbit, provider, positionAngleType, positionScale,
             FrameAlignedProvider.of(templateOrbit.getFrame()));
    }

    /** Build a new instance.
     * <p>
     * The template orbit is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, the orbit type, and is also
     * used together with the {@code positionScale} to convert from the {@link
     * org.orekit.utils.ParameterDriver#setNormalizedValue(double) normalized} parameters used by the
     * callers of this builder to the real orbital parameters.
     * </p>
     * @param templateOrbit reference orbit from which real orbits will be built
     * (note that the mu from this orbit will be overridden with the mu from the
     * {@code provider})
     * @param provider for un-normalized zonal coefficients
     * @param positionAngleType position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param attitudeProvider attitude law to use.
     * @since 10.1
     */
    public EcksteinHechlerPropagatorBuilder(final Orbit templateOrbit,
                                            final UnnormalizedSphericalHarmonicsProvider provider,
                                            final PositionAngleType positionAngleType,
                                            final double positionScale,
                                            final AttitudeProvider attitudeProvider) {
        super(overrideMu(templateOrbit, provider, positionAngleType), positionAngleType,
              positionScale, true, attitudeProvider);
        this.provider = provider;
    }

    /** Build a new instance.
     * <p>
     * The template orbit is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, the orbit type, and is also
     * used together with the {@code positionScale} to convert from the {@link
     * org.orekit.utils.ParameterDriver#setNormalizedValue(double) normalized} parameters used by the
     * callers of this builder to the real orbital parameters.
     * </p>
     *
     * @param templateOrbit reference orbit from which real orbits will be built
     * (note that the mu from this orbit will be overridden with the mu from the
     * {@code provider})
     * @param referenceRadius reference radius of the Earth for the potential model (m)
     * @param mu central attraction coefficient (m³/s²)
     * @param tideSystem tide system
     * @param c20 un-normalized zonal coefficient (about -1.08e-3 for Earth)
     * @param c30 un-normalized zonal coefficient (about +2.53e-6 for Earth)
     * @param c40 un-normalized zonal coefficient (about +1.62e-6 for Earth)
     * @param c50 un-normalized zonal coefficient (about +2.28e-7 for Earth)
     * @param c60 un-normalized zonal coefficient (about -5.41e-7 for Earth)
     * @param orbitType orbit type to use
     * @param positionAngleType position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @since 8.0
     * @see #EcksteinHechlerPropagatorBuilder(Orbit,
     * UnnormalizedSphericalHarmonicsProvider, PositionAngleType, double, AttitudeProvider)
     */
    public EcksteinHechlerPropagatorBuilder(final Orbit templateOrbit,
                                            final double referenceRadius,
                                            final double mu,
                                            final TideSystem tideSystem,
                                            final double c20,
                                            final double c30,
                                            final double c40,
                                            final double c50,
                                            final double c60,
                                            final OrbitType orbitType,
                                            final PositionAngleType positionAngleType,
                                            final double positionScale) {
        this(templateOrbit,
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
                positionAngleType, positionScale);
    }

    /** Override central attraction coefficient.
     * @param templateOrbit template orbit
     * @param provider gravity field provider
     * @param positionAngleType position angle type to use
     * @return orbit with overridden central attraction coefficient
     */
    private static Orbit overrideMu(final Orbit templateOrbit,
                                    final UnnormalizedSphericalHarmonicsProvider provider,
                                    final PositionAngleType positionAngleType) {
        final double[] parameters    = new double[6];
        final double[] parametersDot = templateOrbit.hasDerivatives() ? new double[6] : null;
        templateOrbit.getType().mapOrbitToArray(templateOrbit, positionAngleType, parameters, parametersDot);
        return templateOrbit.getType().mapArrayToOrbit(parameters, parametersDot, positionAngleType,
                                                       templateOrbit.getDate(),
                                                       provider.getMu(),
                                                       templateOrbit.getFrame());
    }

    /** {@inheritDoc} */
    public Propagator buildPropagator(final double[] normalizedParameters) {
        setParameters(normalizedParameters);
        return new EcksteinHechlerPropagator(createInitialOrbit(), getAttitudeProvider(),
                provider);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractBatchLSModel buildLeastSquaresModel(final PropagatorBuilder[] builders,
                                                       final List<ObservedMeasurement<?>> measurements,
                                                       final ParameterDriversList estimatedMeasurementsParameters,
                                                       final ModelObserver observer) {
        return new BatchLSModel(builders, measurements, estimatedMeasurementsParameters, observer);
    }

    /** {@inheritDoc} */
    @Override
    public EcksteinHechlerPropagatorBuilder copy() {
        return new EcksteinHechlerPropagatorBuilder(createInitialOrbit(), provider, getPositionAngleType(),
                                                    getPositionScale(), getAttitudeProvider());
    }
}
