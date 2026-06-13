/* Copyright 2002-2026 CS GROUP
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

import org.hipparchus.ode.ODEIntegrator;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.leastsquares.BatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.orbits.AbstractOrbitFactory;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.ParameterDriversList;

/** Builder for numerical propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class NumericalPropagatorBuilder
    extends AbstractIntegratedPropagatorBuilder<NumericalPropagator, Orbit, AbstractOrbitFactory<Orbit>> {

    /** Force models used during the extrapolation of the orbit. */
    private final List<ForceModel> forceModels;

    /** Impulse maneuvers. */
    private final List<ImpulseManeuver> impulseManeuvers;

    /** Build a new instance.
     * @param factory factory for initial orbit
     * @param builder first order integrator builder
     * @since 14.0
     * @see #NumericalPropagatorBuilder(AbstractOrbitFactory, ODEIntegratorBuilder, AttitudeProvider)
     */
    public NumericalPropagatorBuilder(final AbstractOrbitFactory<? extends Orbit> factory,
                                      final ODEIntegratorBuilder builder) {
        this(factory, builder, FrameAlignedProvider.of(factory.getFrame()));
    }

    /** Build a new instance.
     * @param factory factory for initial orbit
     * @param builder first order integrator builder
     * @param attitudeProvider attitude law.
     * @since 14.0
     */
    public NumericalPropagatorBuilder(final AbstractOrbitFactory<? extends Orbit> factory,
                                      final ODEIntegratorBuilder builder,
                                      final AttitudeProvider attitudeProvider) {
        super((AbstractOrbitFactory<Orbit>) factory, builder,
              PropagationType.OSCULATING, attitudeProvider, Propagator.DEFAULT_MASS);
        this.forceModels = new ArrayList<>();
        this.impulseManeuvers = new ArrayList<>();
    }

    /** Copy constructor.
     *
     * @param builder builder to copy
     */
    private NumericalPropagatorBuilder(final NumericalPropagatorBuilder builder) {
        this(builder.getOrbitalParameterFactory(),
             builder.getIntegratorBuilder(),
             builder.getAttitudeProvider());
    }

    /** {@inheritDoc}. */
    @Override
    public NumericalPropagatorBuilder clone() {
        // Call to super clone() method to avoid warning
        final NumericalPropagatorBuilder clonedBuilder = (NumericalPropagatorBuilder) super.clone();

        // Use copy constructor to unlink orbital drivers
        final NumericalPropagatorBuilder builder =  new NumericalPropagatorBuilder(clonedBuilder);

        // Set mass and force models
        builder.setMass(getMass());
        for (ForceModel model : forceModels) {
            builder.addForceModel(model);
        }

        // Add impulse maneuvers
        impulseManeuvers.forEach(builder::addImpulseManeuver);

        return builder;
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

        addPropagationParameters(model.getParametersDrivers());
    }

    /** {@inheritDoc} */
    public NumericalPropagator buildPropagator(final double[] normalizedParameters) {

        final AbstractOrbitFactory<Orbit> factory = getOrbitalParameterFactory();
        setParameters(normalizedParameters);
        final Orbit           orbit    = factory.createFromDrivers();
        final Attitude        attitude = getAttitudeProvider().
                                         getAttitude(orbit, orbit.getDate(), factory.getFrame());
        final SpacecraftState state    = new SpacecraftState(orbit, attitude).withMass(getMass());

        final ODEIntegrator integrator = getIntegratorBuilder().
                                         buildIntegrator(orbit,
                                                         factory.getOrbitType(),
                                                         factory.getPositionAngleType());
        final NumericalPropagator propagator = new NumericalPropagator(integrator, getAttitudeProvider());
        propagator.setOrbitType(factory.getOrbitType());
        propagator.setPositionAngleType(factory.getPositionAngleType());

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
