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

import java.util.Collections;
import java.util.List;
import org.hipparchus.util.FastMath;
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
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Builder for Brouwer-Lyddane propagator.
 * <p>
 * By default, Brouwer-Lyddane model considers only the perturbations due to zonal harmonics.
 * However, for low Earth orbits, the magnitude of the perturbative acceleration due to
 * atmospheric drag can be significant. Warren Phipps' 1992 thesis considered the atmospheric
 * drag by time derivatives of the <i>mean</i> mean anomaly using the catch-all coefficient M2.
 *
 * Usually, M2 is adjusted during an orbit determination process and it represents the
 * combination of all unmodeled secular along-track effects (i.e. not just the atmospheric drag).
 * The behavior of M2 is closed to the {@link TLE#getBStar()} parameter for the TLE.
 *
 * If the value of M2 is equal to {@link BrouwerLyddanePropagator#M2 0.0}, the along-track
 * secular effects are not considered in the dynamical model. Typical values for M2 are not known.
 * It depends on the orbit type. However, the value of M2 must be very small (e.g. between 1.0e-14 and 1.0e-15).
 * The unit of M2 is rad/s².
 * <p>
 * To estimate the M2 parameter, it is necessary to call the {@link #getPropagationParametersDrivers()} method
 * as follow:
 * <pre>
 *  for (ParameterDriver driver : builder.getPropagationParametersDrivers().getDrivers()) {
 *     if (BrouwerLyddanePropagator.M2_NAME.equals(driver.getName())) {
 *        driver.setSelected(true);
 *     }
 *  }
 * </pre>
 * @author Melina Vanel
 * @author Bryan Cazabonne
 * @since 11.1
 */
public class BrouwerLyddanePropagatorBuilder extends AbstractPropagatorBuilder {

    /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double SCALE = FastMath.scalb(1.0, -32);

    /** Provider for un-normalized coefficients. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /** Build a new instance.
     * <p>
     * The template orbit is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, the orbit type, and is also
     * used together with the {@code positionScale} to convert from the {@link
     * org.orekit.utils.ParameterDriver#setNormalizedValue(double) normalized} parameters
     * used by the callers of this builder to the real orbital parameters.
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
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #BrouwerLyddanePropagatorBuilder(Orbit,
     * UnnormalizedSphericalHarmonicsProvider, PositionAngleType, double, AttitudeProvider, double)
     */
    public BrouwerLyddanePropagatorBuilder(final Orbit templateOrbit,
                                           final UnnormalizedSphericalHarmonicsProvider provider,
                                           final PositionAngleType positionAngleType,
                                           final double positionScale,
                                           final double M2) {
        this(templateOrbit, provider, positionAngleType, positionScale,
             FrameAlignedProvider.of(templateOrbit.getFrame()), M2);
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
     * @param orbitType orbit type to use
     * @param positionAngleType position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     * @see #BrouwerLyddanePropagatorBuilder(Orbit,
     * UnnormalizedSphericalHarmonicsProvider, PositionAngleType, double, AttitudeProvider, double)
     */
    public BrouwerLyddanePropagatorBuilder(final Orbit templateOrbit,
                                           final double referenceRadius,
                                           final double mu,
                                           final TideSystem tideSystem,
                                           final double c20,
                                           final double c30,
                                           final double c40,
                                           final double c50,
                                           final OrbitType orbitType,
                                           final PositionAngleType positionAngleType,
                                           final double positionScale,
                                           final double M2) {
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
                                                             }
                                                         }),
                positionAngleType, positionScale, M2);
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
     * @param attitudeProvider attitude law to use
     * @param M2 value of empirical drag coefficient in rad/s².
     *        If equal to {@link BrouwerLyddanePropagator#M2} drag is not computed
     */
    public BrouwerLyddanePropagatorBuilder(final Orbit templateOrbit,
                                           final UnnormalizedSphericalHarmonicsProvider provider,
                                           final PositionAngleType positionAngleType,
                                           final double positionScale,
                                           final AttitudeProvider attitudeProvider,
                                           final double M2) {
        super(overrideMu(templateOrbit, provider, positionAngleType), positionAngleType, positionScale, true, attitudeProvider);
        this.provider = provider;
        // initialize M2 driver
        final ParameterDriver M2Driver = new ParameterDriver(BrouwerLyddanePropagator.M2_NAME, M2, SCALE,
                                                             Double.NEGATIVE_INFINITY,
                                                             Double.POSITIVE_INFINITY);
        addSupportedParameters(Collections.singletonList(M2Driver));
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
    @Override
    public BrouwerLyddanePropagatorBuilder copy() {

        // Find M2 value
        double m2 = 0.0;
        for (final ParameterDriver driver : getPropagationParametersDrivers().getDrivers()) {
            if (BrouwerLyddanePropagator.M2_NAME.equals(driver.getName())) {
                // it is OK as BL m2 parameterDriver has 1 value estimated from -INF to +INF, and
                // setPeriod method should not be called on this driver (to have several values estimated)
                m2 = driver.getValue();
            }
        }

        return new BrouwerLyddanePropagatorBuilder(createInitialOrbit(), provider, getPositionAngleType(),
                                                   getPositionScale(), getAttitudeProvider(), m2);
    }

    /** {@inheritDoc} */
    public BrouwerLyddanePropagator buildPropagator(final double[] normalizedParameters) {
        setParameters(normalizedParameters);

        // Update M2 value and selection
        double  newM2      = 0.0;
        boolean isSelected = false;
        for (final ParameterDriver driver : getPropagationParametersDrivers().getDrivers()) {
            if (BrouwerLyddanePropagator.M2_NAME.equals(driver.getName())) {
                // it is OK as BL m2 parameterDriver has 1 value estimated from -INF to +INF, and
                // setPeriod method should not be called on this driver (to have several values estimated)
                newM2      = driver.getValue();
                isSelected = driver.isSelected();
            }
        }

        // Initialize propagator
        final BrouwerLyddanePropagator propagator = new BrouwerLyddanePropagator(createInitialOrbit(), getAttitudeProvider(), provider, newM2);
        propagator.getParametersDrivers().get(0).setSelected(isSelected);

        // Return
        return propagator;

    }

    /** {@inheritDoc} */
    @Override
    public AbstractBatchLSModel buildLeastSquaresModel(final PropagatorBuilder[] builders,
                                                       final List<ObservedMeasurement<?>> measurements,
                                                       final ParameterDriversList estimatedMeasurementsParameters,
                                                       final ModelObserver observer) {
        return new BatchLSModel(builders, measurements, estimatedMeasurementsParameters, observer);
    }

}
