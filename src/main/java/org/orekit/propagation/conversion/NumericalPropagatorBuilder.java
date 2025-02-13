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
package org.orekit.propagation.conversion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.leastsquares.BatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Builder for numerical propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class NumericalPropagatorBuilder extends AbstractIntegratedPropagatorBuilder<NumericalPropagator> {

    /** Force models used during the extrapolation of the orbit. */
    private final List<ForceModel> forceModels;

    /** Impulse maneuvers. */
    private final List<ImpulseManeuver> impulseManeuvers;

    /** Build a new instance.
     * <p>
     * The reference orbit is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, and is also used together
     * with the {@code positionScale} to convert from the {@link
     * ParameterDriver#setNormalizedValue(double) normalized} parameters used by the
     * callers of this builder to the real orbital parameters.
     * The default attitude provider is aligned with the orbit's inertial frame.
     * </p>
     *
     * @param referenceOrbit reference orbit from which real orbits will be built
     * @param builder first order integrator builder
     * @param positionAngleType position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @since 8.0
     * @see #NumericalPropagatorBuilder(Orbit, ODEIntegratorBuilder, PositionAngleType,
     * double, AttitudeProvider)
     */
    public NumericalPropagatorBuilder(final Orbit referenceOrbit,
                                      final ODEIntegratorBuilder builder,
                                      final PositionAngleType positionAngleType,
                                      final double positionScale) {
        this(referenceOrbit, builder, positionAngleType, positionScale,
             FrameAlignedProvider.of(referenceOrbit.getFrame()));
    }

    /** Build a new instance.
     * <p>
     * The reference orbit is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, and is also used together
     * with the {@code positionScale} to convert from the {@link
     * ParameterDriver#setNormalizedValue(double) normalized} parameters used by the
     * callers of this builder to the real orbital parameters.
     * </p>
     * @param referenceOrbit reference orbit from which real orbits will be built
     * @param builder first order integrator builder
     * @param positionAngleType position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param attitudeProvider attitude law.
     * @since 10.1
     */
    public NumericalPropagatorBuilder(final Orbit referenceOrbit,
                                      final ODEIntegratorBuilder builder,
                                      final PositionAngleType positionAngleType,
                                      final double positionScale,
                                      final AttitudeProvider attitudeProvider) {
        super(referenceOrbit, builder, positionAngleType, positionScale, PropagationType.OSCULATING, attitudeProvider, Propagator.DEFAULT_MASS);
        this.forceModels = new ArrayList<>();
        this.impulseManeuvers = new ArrayList<>();
    }

    /**
     * Add impulse maneuver.
     * @param impulseManeuver impulse maneuver
     * @since 12.2
     */
    public void addImpulseManeuver(final ImpulseManeuver impulseManeuver) {
        impulseManeuvers.add(impulseManeuver);
    }

    /**
     * Remove all impulse maneuvers.
     * @since 12.2
     */
    public void clearImpulseManeuvers() {
        impulseManeuvers.clear();
    }

    /** Get the list of all force models.
     * @return the list of all force models
     * @since 9.2
     */
    public List<ForceModel> getAllForceModels()
    {
        return Collections.unmodifiableList(forceModels);
    }

    /** Add a force model to the global perturbation model.
     * <p>If this method is not called at all, the integrated orbit will follow
     * a Keplerian evolution only.</p>
     * @param model perturbing {@link ForceModel} to add
     */
    public void addForceModel(final ForceModel model) {
        if (model instanceof NewtonianAttraction) {
            // we want to add the central attraction force model
            if (hasNewtonianAttraction()) {
                // there is already a central attraction model, replace it
                forceModels.set(forceModels.size() - 1, model);
            } else {
                // there are no central attraction model yet, add it at the end of the list
                forceModels.add(model);
            }
        } else {
            // we want to add a perturbing force model
            if (hasNewtonianAttraction()) {
                // insert the new force model before Newtonian attraction,
                // which should always be the last one in the list
                forceModels.add(forceModels.size() - 1, model);
            } else {
                // we only have perturbing force models up to now, just append at the end of the list
                forceModels.add(model);
            }
        }

        addSupportedParameters(model.getParametersDrivers());
    }

    /** {@inheritDoc} */
    public NumericalPropagator buildPropagator(final double[] normalizedParameters) {

        setParameters(normalizedParameters);
        final Orbit           orbit    = createInitialOrbit();
        final Attitude        attitude =
                getAttitudeProvider().getAttitude(orbit, orbit.getDate(), getFrame());
        final SpacecraftState state    = new SpacecraftState(orbit, attitude).withMass(getMass());

        final NumericalPropagator propagator = new NumericalPropagator(
                getIntegratorBuilder().buildIntegrator(orbit, getOrbitType(), getPositionAngleType()),
                getAttitudeProvider());
        propagator.setOrbitType(getOrbitType());
        propagator.setPositionAngleType(getPositionAngleType());

        // Configure force models
        if (!hasNewtonianAttraction()) {
            // There are no central attraction model yet, add it at the end of the list
            addForceModel(new NewtonianAttraction(orbit.getMu()));
        }
        for (ForceModel model : forceModels) {
            propagator.addForceModel(model);
        }
        impulseManeuvers.forEach(propagator::addEventDetector);

        propagator.resetInitialState(state);

        // Add additional derivatives providers to the propagator
        for (AdditionalDerivativesProvider provider: getAdditionalDerivativesProviders()) {
            propagator.addAdditionalDerivativesProvider(provider);
        }

        return propagator;

    }

    /** {@inheritDoc} */
    @Override
    public BatchLSModel buildLeastSquaresModel(final PropagatorBuilder[] builders,
                                               final List<ObservedMeasurement<?>> measurements,
                                               final ParameterDriversList estimatedMeasurementsParameters,
                                               final ModelObserver observer) {
        return new BatchLSModel(builders, measurements, estimatedMeasurementsParameters, observer);
    }

    /** Check if Newtonian attraction force model is available.
     * <p>
     * Newtonian attraction is always the last force model in the list.
     * </p>
     * @return true if Newtonian attraction force model is available
     */
    private boolean hasNewtonianAttraction() {
        final int last = forceModels.size() - 1;
        return last >= 0 && forceModels.get(last) instanceof NewtonianAttraction;
    }

}
