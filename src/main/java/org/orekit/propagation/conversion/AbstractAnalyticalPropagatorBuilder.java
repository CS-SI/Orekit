/* Copyright 2022-2025 Romain Serra
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


import org.orekit.attitudes.AttitudeProvider;
import org.orekit.estimation.leastsquares.AbstractBatchLSModel;
import org.orekit.estimation.leastsquares.BatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.utils.ParameterDriversList;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for propagator builders of analytical models (except for ephemeris i.e. interpolated ones).
 *
 * @author Romain Serra
 * @since 12.2
 */
public abstract class AbstractAnalyticalPropagatorBuilder<T extends AbstractAnalyticalPropagator>
        extends AbstractPropagatorBuilder<T> {

    /** Impulse maneuvers. */
    private final List<ImpulseManeuver> impulseManeuvers;

    /** Build a new instance.
     * <p>
     * The template orbit is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, the orbit type, and is also
     * used together with the {@code positionScale} to convert from the {@link
     * org.orekit.utils.ParameterDriver#setNormalizedValue(double) normalized} parameters used by the
     * callers of this builder to the real orbital parameters. The default attitude
     * provider is aligned with the orbit's inertial frame.
     * </p>
     * <p>
     * By default, all the {@link #getOrbitalParametersDrivers() orbital parameters drivers}
     * are selected, which means that if the builder is used for orbit determination or
     * propagator conversion, all orbital parameters will be estimated. If only a subset
     * of the orbital parameters must be estimated, caller must retrieve the orbital
     * parameters by calling {@link #getOrbitalParametersDrivers()} and then call
     * {@link org.orekit.utils.ParameterDriver#setSelected(boolean) setSelected(false)}.
     * </p>
     * @param templateOrbit reference orbit from which real orbits will be built
     * @param positionAngleType position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param addDriverForCentralAttraction if true, a {@link org.orekit.utils.ParameterDriver} should
     * be set up for central attraction coefficient
     * @param attitudeProvider for the propagator
     * @param initialMass mass
     */
    protected AbstractAnalyticalPropagatorBuilder(final Orbit templateOrbit, final PositionAngleType positionAngleType,
                                                  final double positionScale, final boolean addDriverForCentralAttraction,
                                                  final AttitudeProvider attitudeProvider, final double initialMass) {
        super(templateOrbit, positionAngleType, positionScale, addDriverForCentralAttraction, attitudeProvider, initialMass);
        this.impulseManeuvers = new ArrayList<>();
    }

    /**
     * Protected getter for the impulse maneuvers.
     * @return impulse maneuvers
     */
    protected List<ImpulseManeuver> getImpulseManeuvers() {
        return new ArrayList<>(impulseManeuvers);
    }

    /**
     * Add impulse maneuver.
     * @param impulseManeuver impulse maneuver
     */
    public void addImpulseManeuver(final ImpulseManeuver impulseManeuver) {
        impulseManeuvers.add(impulseManeuver);
    }

    /**
     * Remove all impulse maneuvers.
     */
    public void clearImpulseManeuvers() {
        impulseManeuvers.clear();
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
