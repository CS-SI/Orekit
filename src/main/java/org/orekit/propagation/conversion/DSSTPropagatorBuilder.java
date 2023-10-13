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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.leastsquares.DSSTBatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Builder for DSST propagator.
 * @author Bryan Cazabonne
 * @since 10.0
 */
public class DSSTPropagatorBuilder extends AbstractPropagatorBuilder {

    /** First order integrator builder for propagation. */
    private final ODEIntegratorBuilder builder;

    /** Force models used during the extrapolation of the orbit. */
    private final List<DSSTForceModel> forceModels;

    /** Current mass for initial state (kg). */
    private double mass;

    /** Type of the orbit used for the propagation.*/
    private PropagationType propagationType;

    /** Type of the elements used to define the orbital state.*/
    private PropagationType stateType;

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
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param propagationType type of the orbit used for the propagation (mean or osculating)
     * @param stateType type of the elements used to define the orbital state (mean or osculating)
     * @see #DSSTPropagatorBuilder(Orbit, ODEIntegratorBuilder, double, PropagationType,
     * PropagationType, AttitudeProvider)
     */
    public DSSTPropagatorBuilder(final Orbit referenceOrbit,
                                 final ODEIntegratorBuilder builder,
                                 final double positionScale,
                                 final PropagationType propagationType,
                                 final PropagationType stateType) {
        this(referenceOrbit, builder, positionScale, propagationType, stateType,
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
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param propagationType type of the orbit used for the propagation (mean or osculating)
     * @param stateType type of the elements used to define the orbital state (mean or osculating)
     * @param attitudeProvider attitude law.
     * @since 10.1
     */
    public DSSTPropagatorBuilder(final Orbit referenceOrbit,
                                 final ODEIntegratorBuilder builder,
                                 final double positionScale,
                                 final PropagationType propagationType,
                                 final PropagationType stateType,
                                 final AttitudeProvider attitudeProvider) {
        super(referenceOrbit, PositionAngleType.MEAN, positionScale, true, attitudeProvider);
        this.builder           = builder;
        this.forceModels       = new ArrayList<DSSTForceModel>();
        this.mass              = Propagator.DEFAULT_MASS;
        this.propagationType   = propagationType;
        this.stateType         = stateType;
    }

    /** Get the type of the orbit used for the propagation (mean or osculating).
     * @return the type of the orbit used for the propagation
     */
    public PropagationType getPropagationType() {
        return propagationType;
    }

    /** Get the type of the elements used to define the orbital state (mean or osculating).
     * @return the type of the elements used to define the orbital state
     */
    public PropagationType getStateType() {
        return stateType;
    }

    /** Create a copy of a DSSTPropagatorBuilder object.
     * @return Copied version of the DSSTPropagatorBuilder
     */
    public DSSTPropagatorBuilder copy() {
        final DSSTPropagatorBuilder copyBuilder =
                        new DSSTPropagatorBuilder(createInitialOrbit(),
                                                  builder,
                                                  getPositionScale(),
                                                  propagationType,
                                                  stateType,
                                                  getAttitudeProvider());
        copyBuilder.setMass(mass);
        for (DSSTForceModel model : forceModels) {
            copyBuilder.addForceModel(model);
        }
        return copyBuilder;
    }

    /** Get the integrator builder.
     * @return the integrator builder
     */
    public ODEIntegratorBuilder getIntegratorBuilder()
    {
        return builder;
    }

    /** Get the list of all force models.
     * @return the list of all force models
     */
    public List<DSSTForceModel> getAllForceModels()
    {
        return Collections.unmodifiableList(forceModels);
    }

    /** Get the mass.
     * @return the mass
     */
    public double getMass()
    {
        return mass;
    }

    /** Set the initial mass.
     * @param mass the mass (kg)
     */
    public void setMass(final double mass) {
        this.mass = mass;
    }

    /** Add a force model to the global perturbation model.
     * <p>If this method is not called at all, the integrated orbit will follow
     * a Keplerian evolution only.</p>
     * @param model perturbing {@link DSSTForceModel} to add
     */
    public void addForceModel(final DSSTForceModel model) {
        if (model instanceof DSSTNewtonianAttraction) {
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

    /** Reset the orbit in the propagator builder.
     * @param newOrbit newOrbit New orbit to set in the propagator builder
     * @param orbitType orbit type (MEAN or OSCULATING)
     */
    public void resetOrbit(final Orbit newOrbit, final PropagationType orbitType) {
        this.stateType = orbitType;
        super.resetOrbit(newOrbit);
    }

    /** {@inheritDoc} */
    public DSSTPropagator buildPropagator(final double[] normalizedParameters) {

        setParameters(normalizedParameters);
        final EquinoctialOrbit orbit    = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(createInitialOrbit());
        final Attitude         attitude = getAttitudeProvider().getAttitude(orbit, orbit.getDate(), getFrame());
        final SpacecraftState  state    = new SpacecraftState(orbit, attitude, mass);

        final DSSTPropagator propagator = new DSSTPropagator(
                builder.buildIntegrator(orbit, OrbitType.EQUINOCTIAL),
                propagationType,
                getAttitudeProvider());

        // Configure force models
        if (!hasNewtonianAttraction()) {
            // There are no central attraction model yet, add it at the end of the list
            addForceModel(new DSSTNewtonianAttraction(orbit.getMu()));
        }
        for (DSSTForceModel model : forceModels) {
            propagator.addForceModel(model);
        }

        propagator.setInitialState(state, stateType);

        // Add additional derivatives providers to the propagator
        for (AdditionalDerivativesProvider provider: getAdditionalDerivativesProviders()) {
            propagator.addAdditionalDerivativesProvider(provider);
        }

        return propagator;

    }

    /** {@inheritDoc} */
    @Override
    public DSSTBatchLSModel buildLeastSquaresModel(final PropagatorBuilder[] builders,
                                                   final List<ObservedMeasurement<?>> measurements,
                                                   final ParameterDriversList estimatedMeasurementsParameters,
                                                   final ModelObserver observer) {
        return new DSSTBatchLSModel(builders,
                                    measurements,
                                    estimatedMeasurementsParameters,
                                    observer,
                                    propagationType);
    }

    /** Check if Newtonian attraction force model is available.
     * <p>
     * Newtonian attraction is always the last force model in the list.
     * </p>
     * @return true if Newtonian attraction force model is available
     */
    private boolean hasNewtonianAttraction() {
        final int last = forceModels.size() - 1;
        return last >= 0 && forceModels.get(last) instanceof DSSTNewtonianAttraction;
    }

}
