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
import org.orekit.estimation.leastsquares.BatchLSModel;
import org.orekit.estimation.leastsquares.ModelObserver;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.sequential.CovarianceMatrixProvider;
import org.orekit.estimation.sequential.KalmanModel;
import org.orekit.forces.ForceModel;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Builder for numerical propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class NumericalPropagatorBuilder extends AbstractPropagatorBuilder implements IntegratedPropagatorBuilder {

    /** First order integrator builder for propagation. */
    private final ODEIntegratorBuilder builder;

    /** Force models used during the extrapolation of the orbit. */
    private final List<ForceModel> forceModels;

    /** Current mass for initial state (kg). */
    private double mass;

    /** Attitude provider. */
    private AttitudeProvider attProvider;

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
     * @param positionAngle position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
          * @since 8.0
     */
    public NumericalPropagatorBuilder(final Orbit referenceOrbit,
                                      final ODEIntegratorBuilder builder,
                                      final PositionAngle positionAngle,
                                      final double positionScale) {
        super(referenceOrbit, positionAngle, positionScale, true);
        this.builder     = builder;
        this.forceModels = new ArrayList<ForceModel>();
        this.mass        = Propagator.DEFAULT_MASS;
        this.attProvider = Propagator.DEFAULT_LAW;
    }

    /** Create a copy of a NumericalPropagatorBuilder object.
     * @return Copied version of the NumericalPropagatorBuilder
     */
    public NumericalPropagatorBuilder copy() {
        final NumericalPropagatorBuilder copyBuilder =
                        new NumericalPropagatorBuilder(createInitialOrbit(),
                                                       builder,
                                                       getPositionAngle(),
                                                       getPositionScale());
        copyBuilder.setAttitudeProvider(attProvider);
        copyBuilder.setMass(mass);
        for (ForceModel model : forceModels) {
            copyBuilder.addForceModel(model);
        }
        return copyBuilder;
    }

    /** Get the integrator builder.
     * @return the integrator builder
     * @since 9.2
     */
    public ODEIntegratorBuilder getIntegratorBuilder()
    {
        return builder;
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
        forceModels.add(model);
        for (final ParameterDriver driver : model.getParametersDrivers()) {
            addSupportedParameter(driver);
        }
    }

    /** Get the mass.
     * @return the mass
     * @since 9.2
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

    /** Get the attitudeProvider.
     * @return the attitude provider
     * @since 9.2
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

    /** {@inheritDoc} */
    public NumericalPropagator buildPropagator(final double[] normalizedParameters) {

        setParameters(normalizedParameters);
        final Orbit           orbit    = createInitialOrbit();
        final Attitude        attitude = attProvider.getAttitude(orbit, orbit.getDate(), getFrame());
        final SpacecraftState state    = new SpacecraftState(orbit, attitude, mass);

        final NumericalPropagator propagator = new NumericalPropagator(builder.buildIntegrator(orbit, getOrbitType()));
        propagator.setOrbitType(getOrbitType());
        propagator.setPositionAngleType(getPositionAngle());
        propagator.setAttitudeProvider(attProvider);
        for (ForceModel model : forceModels) {
            propagator.addForceModel(model);
        }
        propagator.resetInitialState(state);

        return propagator;
    }

    /** {@inheritDoc} */
    public BatchLSModel buildLSModel(final IntegratedPropagatorBuilder[] builders,
                            final List<ObservedMeasurement<?>> measurements,
                            final ParameterDriversList estimatedMeasurementsParameters,
                            final ModelObserver observer) {
        return new BatchLSModel(builders, measurements, estimatedMeasurementsParameters, observer);
    }

    /** {@inheritDoc} */
    public KalmanModel buildKalmanModel(final List<IntegratedPropagatorBuilder> propagatorBuilders,
                                  final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                                  final ParameterDriversList estimatedMeasurementsParameters) {
        return new KalmanModel(propagatorBuilders, covarianceMatricesProviders, estimatedMeasurementsParameters);
    }

}
