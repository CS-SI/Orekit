/* Copyright 2002-2017 CS Systèmes d'Information
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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.ParameterObserver;

/** Builder for numerical propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class NumericalPropagatorBuilder extends AbstractPropagatorBuilder {

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
     * @exception OrekitException if parameters drivers cannot be scaled
     * @since 8.0
     */
    public NumericalPropagatorBuilder(final Orbit referenceOrbit,
                                      final ODEIntegratorBuilder builder,
                                      final PositionAngle positionAngle,
                                      final double positionScale)
        throws OrekitException {
        super(referenceOrbit, positionAngle, positionScale, true);
        this.builder     = builder;
        this.forceModels = new ArrayList<ForceModel>();
        this.mass        = Propagator.DEFAULT_MASS;
        this.attProvider = Propagator.DEFAULT_LAW;
    }

    /** Check if Newtonian attraction force model is available.
     * <p>
     * Newtonian attraction is always the last force model in the list.
     * </p>
     * @return true if Newtonian attraction force model is available
     */
    public boolean hasNewtonianAttraction() {
        final int last = forceModels.size() - 1;
        return last >= 0 && forceModels.get(last) instanceof NewtonianAttraction;
    }

    /** Get all the force models, perturbing forces and Newtonian attraction included.
     * @return list of perturbing force models, with Newtonian attraction being the
     * last one
     * @see #addForceModel(ForceModel)
     * @see #setMu(double)
     */
    public List<ForceModel> getAllForceModels() {
        return Collections.unmodifiableList(forceModels);
    }

    /** Set the attitude provider.
     * @param attitudeProvider attitude provider
     */
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        this.attProvider = attitudeProvider;
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
     * @param model perturbing {@link ForceModel} to add
     * @exception OrekitException if model parameters cannot be set
     */
    public void addForceModel(final ForceModel model)
        throws OrekitException {
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

        // Add the force model's parameter drivers to the list of propagation drivers in the builder
        for (final ParameterDriver driver : model.getParametersDrivers()) {
            addSupportedParameter(driver);
        }
    }

    /** {@inheritDoc} */
    public NumericalPropagator buildPropagator(final double[] normalizedParameters)
        throws OrekitException {

        setParameters(normalizedParameters);
        final Orbit           orbit    = createInitialOrbit();
        final Attitude        attitude = attProvider.getAttitude(orbit, orbit.getDate(), getFrame());
        final SpacecraftState state    = new SpacecraftState(orbit, attitude, mass);

        // Check that the builder as an attraction force model
        // If not, add a simple central one
//        if (!hasNewtonianAttraction()) {
//            addForceModel(new NewtonianAttraction(getMu()));
//        }
        

        final NumericalPropagator propagator = new NumericalPropagator(builder.buildIntegrator(orbit, getOrbitType()));
        propagator.setOrbitType(getOrbitType());
        propagator.setPositionAngleType(getPositionAngle());
        propagator.setAttitudeProvider(attProvider);

        for (ForceModel model : forceModels) {
            propagator.addForceModel(model);
        }
        
//        //debug
//        final DelegatingDriver muDriver = getPropagationParametersDrivers().getDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT);
//        if (!(muDriver == null) && muDriver.isSelected()) {
//            propagator.setMu(getMu());
//            
//            final List<ForceModel> forceModels  = propagator.getAllForceModels();
//            forceModels.get(forceModels.size()-1).getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT).setSelected(true);
//        }
//        
//        //debug

        propagator.resetInitialState(state);

        return synchronizeMuDriverSelection(propagator);
    }
    
    /**
     * @throws OrekitException 
     * 
     */
    public NumericalPropagator synchronizeMuDriverSelection(final NumericalPropagator propagator)
                    throws OrekitException {

        for (DelegatingDriver delegating : getPropagationParametersDrivers().getDrivers()) {
            if (delegating.getName().equals(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT) &&
                delegating.isSelected()) {
                final List<ForceModel> propagatorForceModels  = propagator.getAllForceModels();
                propagatorForceModels.get(propagatorForceModels.size() - 1).getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT).setSelected(true);
            }
        }
        return propagator;
    }
}
