/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.orekit.estimation.leastsquares.DSSTBatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.sequential.CovarianceMatrixProvider;
import org.orekit.estimation.sequential.DSSTKalmanModel;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Builder for DSST propagator.
 * @author Bryan Cazabonne
 * @since 10.0
 */
public class DSSTPropagatorBuilder extends AbstractPropagatorBuilder implements IntegratedPropagatorBuilder {

    /** First order integrator builder for propagation. */
    private final ODEIntegratorBuilder builder;

    /** Force models used during the extrapolation of the orbit. */
    private final List<DSSTForceModel> forceModels;

    /** Current mass for initial state (kg). */
    private double mass;

    /** Attitude provider. */
    private AttitudeProvider attProvider;

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
     * </p>
     * @param referenceOrbit reference orbit from which real orbits will be built
     * @param builder first order integrator builder
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @param propagationType type of the orbit used for the propagation (mean or osculating)
     * @param stateType type of the elements used to define the orbital state (mean or osculating)
     */
    public DSSTPropagatorBuilder(final Orbit referenceOrbit,
                                 final ODEIntegratorBuilder builder,
                                 final double positionScale,
                                 final PropagationType propagationType,
                                 final PropagationType stateType) {
        super(referenceOrbit, PositionAngle.MEAN, positionScale, true);
        this.builder           = builder;
        this.forceModels       = new ArrayList<DSSTForceModel>();
        this.mass              = Propagator.DEFAULT_MASS;
        this.attProvider       = Propagator.DEFAULT_LAW;
        this.propagationType   = propagationType;
        this.stateType         = stateType;
    }

    /** Create a copy of a DSSTPropagatorBuilder object.
     * @return Copied version of the DSSTPropagatorBuilder
     */
    public DSSTPropagatorBuilder copy() {
        final DSSTPropagatorBuilder copyBuilder =
                        new DSSTPropagatorBuilder((EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(createInitialOrbit()),
                                                  builder,
                                                  getPositionScale(),
                                                  propagationType,
                                                  stateType);
        copyBuilder.setAttitudeProvider(attProvider);
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

    /** Get the attitudeProvider.
     * @return the attitude provider
     */
    public AttitudeProvider getAttitudeProvider()
    {
        return attProvider;
    }

    /** Set the attitude provider.
     * @param attitudeProvider attitude provider
     */
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        this.attProvider = attitudeProvider;
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
        forceModels.add(model);
        for (final ParameterDriver driver : model.getParametersDrivers()) {
            addSupportedParameter(driver);
        }
    }

    /** {@inheritDoc} */
    public DSSTPropagator buildPropagator(final double[] normalizedParameters) {

        setParameters(normalizedParameters);
        final EquinoctialOrbit orbit    = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(createInitialOrbit());
        final Attitude         attitude = attProvider.getAttitude(orbit, orbit.getDate(), getFrame());
        final SpacecraftState  state    = new SpacecraftState(orbit, attitude, mass);

        final DSSTPropagator propagator = new DSSTPropagator(builder.buildIntegrator(orbit, OrbitType.EQUINOCTIAL), propagationType);
        propagator.setAttitudeProvider(attProvider);
        for (DSSTForceModel model : forceModels) {
            propagator.addForceModel(model);
        }
        propagator.setInitialState(state, stateType);

        return propagator;
    }

    /** {@inheritDoc} */
    public DSSTBatchLSModel buildLSModel(final IntegratedPropagatorBuilder[] builders,
                                final List<ObservedMeasurement<?>> measurements,
                                final ParameterDriversList estimatedMeasurementsParameters,
                                final ModelObserver observer) {
        return new DSSTBatchLSModel(builders,
                                    measurements,
                                    estimatedMeasurementsParameters,
                                    observer,
                                    propagationType, stateType);
    }

    /** {@inheritDoc} */
    public DSSTKalmanModel buildKalmanModel(final List<IntegratedPropagatorBuilder> propagatorBuilders,
                                  final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                                  final ParameterDriversList estimatedMeasurementsParameters) {
        return new DSSTKalmanModel(propagatorBuilders,
                                   covarianceMatricesProviders,
                                   estimatedMeasurementsParameters,
                                   propagationType, stateType);
    }

}
