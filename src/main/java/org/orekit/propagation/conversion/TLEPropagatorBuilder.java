/* Copyright 2002-2022 CS GROUP
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

import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.InertialProvider;
import org.orekit.data.DataContext;
import org.orekit.estimation.leastsquares.AbstractBatchLSModel;
import org.orekit.estimation.leastsquares.BatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.sequential.AbstractKalmanModel;
import org.orekit.estimation.sequential.CovarianceMatrixProvider;
import org.orekit.estimation.sequential.KalmanModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.TimeScale;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Builder for TLEPropagator.
 * @author Pascal Parraud
 * @author Thomas Paulet
 * @since 6.0
 */
public class TLEPropagatorBuilder extends AbstractPropagatorBuilder implements OrbitDeterminationPropagatorBuilder {

    /** Default value for epsilon. */
    private static final double EPSILON_DEFAULT = 1.0e-10;

    /** Default value for maxIterations. */
    private static final int MAX_ITERATIONS_DEFAULT = 100;

    /** Data context used to access frames and time scales. */
    private final DataContext dataContext;

    /** Template TLE. */
    private final TLE templateTLE;

    /** Threshold for convergence used in TLE generation. */
    private final double epsilon;

    /** Maximum number of iterations for convergence used in TLE generation. */
    private final int maxIterations;

    /** Build a new instance. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * <p>
     * The template TLE is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, orbit type, satellite number,
     * classification, .... and is also used together with the {@code positionScale} to
     * convert from the {@link ParameterDriver#setNormalizedValue(double) normalized}
     * parameters used by the callers of this builder to the real orbital parameters.
     * </p><p>
     * Using this constructor, {@link #EPSILON_DEFAULT} and {@link #MAX_ITERATIONS_DEFAULT}
     * are used for spacecraft's state to TLE transformation
     * </p>
     * @param templateTLE reference TLE from which real orbits will be built
     * @param positionAngle position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @since 7.1
     * @see #TLEPropagatorBuilder(TLE, PositionAngle, double, DataContext)
     */
    @DefaultDataContext
    public TLEPropagatorBuilder(final TLE templateTLE, final PositionAngle positionAngle,
                                final double positionScale) {
        this(templateTLE, positionAngle, positionScale, DataContext.getDefault());
    }

    /** Build a new instance.
     * <p>
     * The template TLE is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, orbit type, satellite number,
     * classification, .... and is also used together with the {@code positionScale} to
     * convert from the {@link ParameterDriver#setNormalizedValue(double) normalized}
     * parameters used by the callers of this builder to the real orbital parameters.
     * </p><p>
     * Using this constructor, {@link #EPSILON_DEFAULT} and {@link #MAX_ITERATIONS_DEFAULT}
     * are used for spacecraft's state to TLE transformation
     * </p>
     * @param templateTLE reference TLE from which real orbits will be built
     * @param positionAngle position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param dataContext used to access frames and time scales.
     * @since 10.1
     * @see #TLEPropagatorBuilder(TLE, PositionAngle, double, DataContext, double, int)
     */
    public TLEPropagatorBuilder(final TLE templateTLE,
                                final PositionAngle positionAngle,
                                final double positionScale,
                                final DataContext dataContext) {
        this(templateTLE, positionAngle, positionScale, dataContext, EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT);
    }

    /** Build a new instance. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * <p>
     * The template TLE is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, orbit type, satellite number,
     * classification, .... and is also used together with the {@code positionScale} to
     * convert from the {@link ParameterDriver#setNormalizedValue(double) normalized}
     * parameters used by the callers of this builder to the real orbital parameters.
     * </p>
     * @param templateTLE reference TLE from which real orbits will be built
     * @param positionAngle position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param epsilon used to compute threshold for convergence check
     * @param maxIterations maximum number of iterations for convergence
     * @since 11.0.2
     * @see #TLEPropagatorBuilder(TLE, PositionAngle, double, DataContext, double, int)
     */
    @DefaultDataContext
    public TLEPropagatorBuilder(final TLE templateTLE, final PositionAngle positionAngle,
                                final double positionScale, final double epsilon,
                                final int maxIterations) {
        this(templateTLE, positionAngle, positionScale, DataContext.getDefault(), epsilon, maxIterations);
    }

    /** Build a new instance.
     * <p>
     * The template TLE is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, orbit type, satellite number,
     * classification, .... and is also used together with the {@code positionScale} to
     * convert from the {@link ParameterDriver#setNormalizedValue(double) normalized}
     * parameters used by the callers of this builder to the real orbital parameters.
     * </p>
     * @param templateTLE reference TLE from which real orbits will be built
     * @param positionAngle position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param dataContext used to access frames and time scales.
     * @param epsilon used to compute threshold for convergence check
     * @param maxIterations maximum number of iterations for convergence
     * @since 11.0.2
     */
    public TLEPropagatorBuilder(final TLE templateTLE,
                                final PositionAngle positionAngle,
                                final double positionScale,
                                final DataContext dataContext,
                                final double epsilon,
                                final int maxIterations) {
        super(TLEPropagator.selectExtrapolator(templateTLE, dataContext.getFrames())
                        .getInitialState().getOrbit(),
              positionAngle, positionScale, false,
              InertialProvider.of(dataContext.getFrames().getTEME()));
        for (final ParameterDriver driver : templateTLE.getParametersDrivers()) {
            addSupportedParameter(driver);
        }
        this.templateTLE   = templateTLE;
        this.dataContext   = dataContext;
        this.epsilon       = epsilon;
        this.maxIterations = maxIterations;
    }

    /** {@inheritDoc} */
    @Override
    public TLEPropagator buildPropagator(final double[] normalizedParameters) {

        // create the orbit
        setParameters(normalizedParameters);
        final Orbit           orbit = createInitialOrbit();
        final SpacecraftState state = new SpacecraftState(orbit);
        final Frame           teme  = dataContext.getFrames().getTEME();
        final TimeScale       utc   = dataContext.getTimeScales().getUTC();

        // TLE related to the orbit
        final TLE tle = TLE.stateToTLE(state, templateTLE, utc, teme, epsilon, maxIterations);
        final List<ParameterDriver> drivers = templateTLE.getParametersDrivers();
        for (int index = 0; index < drivers.size(); index++) {
            if (drivers.get(index).isSelected()) {
                tle.getParametersDrivers().get(index).setSelected(true);
            }
        }

        // propagator
        return TLEPropagator.selectExtrapolator(tle,
                                                getAttitudeProvider(),
                                                Propagator.DEFAULT_MASS,
                                                teme);

    }

    /** Getter for the template TLE.
     * @return the template TLE
     */
    public TLE getTemplateTLE() {
        return templateTLE;
    }

    /** {@inheritDoc} */
    public AbstractBatchLSModel buildLSModel(final OrbitDeterminationPropagatorBuilder[] builders,
                                final List<ObservedMeasurement<?>> measurements,
                                final ParameterDriversList estimatedMeasurementsParameters,
                                final ModelObserver observer) {
        return new BatchLSModel(builders, measurements, estimatedMeasurementsParameters, observer);
    }

    @Override
    public AbstractKalmanModel
        buildKalmanModel(final List<OrbitDeterminationPropagatorBuilder> propagatorBuilders,
                         final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                         final ParameterDriversList estimatedMeasurementsParameters,
                         final CovarianceMatrixProvider measurementProcessNoiseMatrix) {
        return new KalmanModel(propagatorBuilders, covarianceMatricesProviders, estimatedMeasurementsParameters, measurementProcessNoiseMatrix);
    }

}
