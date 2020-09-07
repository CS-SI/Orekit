/* Copyright 2002-2020 CS GROUP
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
import org.orekit.data.DataContext;
import org.orekit.estimation.leastsquares.BatchLSODModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.leastsquares.TLEBatchLSModel;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.sequential.CovarianceMatrixProvider;
import org.orekit.estimation.sequential.KalmanODModel;
import org.orekit.estimation.sequential.TLEKalmanModel;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Builder for TLEPropagator.
 * @author Pascal Parraud
 * @author Thomas Paulet
 * @since 6.0
 */
public class TLEPropagatorBuilder extends AbstractPropagatorBuilder implements ODPropagatorBuilder {

    /** Data context used to access frames and time scales. */
    private final DataContext dataContext;

    /** Template TLE. */
    private final TLE templateTLE;

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
     * </p>
     * @param templateTLE reference TLE from which real orbits will be built
     * @param positionAngle position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param dataContext used to access frames and time scales.
     * @since 10.1
     */
    public TLEPropagatorBuilder(final TLE templateTLE,
                                final PositionAngle positionAngle,
                                final double positionScale,
                                final DataContext dataContext) {
        super(OrbitType.KEPLERIAN.convertType(TLEPropagator.selectExtrapolator(templateTLE, dataContext.getFrames())
                        .getInitialState().getOrbit()),
              positionAngle, positionScale, false,
              Propagator.getDefaultLaw(dataContext.getFrames()));
        for (final ParameterDriver driver : templateTLE.getParametersDrivers()) {
            addSupportedParameter(driver);
        }
        this.templateTLE = templateTLE;
        this.dataContext = dataContext;

    }

    /** {@inheritDoc} */
    @DefaultDataContext
    public TLEPropagator buildPropagator(final double[] normalizedParameters) {

        // create the orbit
        setParameters(normalizedParameters);

        final Orbit orbit = createInitialOrbit();

        // we really need a Keplerian orbit type
        final KeplerianOrbit kep = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);
        final SpacecraftState state = new SpacecraftState(kep);

        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(templateTLE, getAttitudeProvider(),
                                                                    Propagator.DEFAULT_MASS, dataContext.getFrames().getTEME());
        propagator.resetInitialState(state);
        return propagator;
    }

    /** Getter for the template TLE.
     * @return the template TLE
     */
    public TLE getTemplateTLE() {
        return templateTLE;
    }

    /** {@inheritDoc} */
    public BatchLSODModel buildLSModel(final ODPropagatorBuilder[] builders,
                                final List<ObservedMeasurement<?>> measurements,
                                final ParameterDriversList estimatedMeasurementsParameters,
                                final ModelObserver observer) {
        return new TLEBatchLSModel(builders, measurements, estimatedMeasurementsParameters, observer);
    }

    /** {@inheritDoc} */
    public KalmanODModel buildKalmanModel(final List<ODPropagatorBuilder> propagatorBuilders,
                                   final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                                   final ParameterDriversList estimatedMeasurementsParameters) {
        return new TLEKalmanModel(propagatorBuilders, covarianceMatricesProviders, estimatedMeasurementsParameters);
    }

}
