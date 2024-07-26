/* Copyright 2022-2024 Romain Serra
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
package org.orekit.control.indirect.shooting;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.ODEIntegrator;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.FieldODEIntegratorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;

/**
 * Abstract class for indirect shooting methods with numerical propagation.
 *
 * @author Romain Serra
 * @since 12.2
 */
public abstract class AbstractIndirectShooting {

    /** Default value for convergence tolerance on mass adjoint variable. */
    public static final double DEFAULT_TOLERANCE_MASS_ADJOINT = 1e-10;

    /** Propagation settings. */
    private final ShootingPropagationSettings propagationSettings;

    /**
     * Constructor.
     * @param propagationSettings propagation settings
     */
    protected AbstractIndirectShooting(final ShootingPropagationSettings propagationSettings) {
        this.propagationSettings = propagationSettings;
    }

    /**
     * Getter for the propagation settings.
     * @return propagation settings
     */
    public ShootingPropagationSettings getPropagationSettings() {
        return propagationSettings;
    }

    /**
     * Solve for the boundary conditions, given an initial mass and an initial guess for the adjoint variables.
     * @param initialMass initial mass
     * @param initialGuess initial guess
     * @return boundary problem solution
     */
    public abstract ShootingBoundaryOutput solve(double initialMass, double[] initialGuess);

    /**
     * Create numerical propagator.
     * @param initialState initial state
     * @return numerical propagator
     */
    protected NumericalPropagator buildPropagator(final SpacecraftState initialState) {
        final ODEIntegrator integrator = buildIntegrator(initialState);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setIgnoreCentralAttraction(true);
        propagator.setInitialState(initialState);
        propagator.setIgnoreCentralAttraction(false);
        propagator.removeForceModels();
        if (initialState.isOrbitDefined()) {
            propagator.setOrbitType(initialState.getOrbit().getType());
        } else {
            if (propagationSettings.getForceModels().stream().noneMatch(NewtonianAttraction.class::isInstance)) {
                propagator.setIgnoreCentralAttraction(true);
            }
            propagator.setOrbitType(null);
        }
        propagator.setAttitudeProvider(propagationSettings.getAttitudeProvider());
        for (final ForceModel forceModel: propagationSettings.getForceModels()) {
            propagator.addForceModel(forceModel);
        }
        propagator.addAdditionalDerivativesProvider(propagationSettings.getAdjointDerivativesProvider()
            .buildAdditionalDerivativesProvider());
        return propagator;
    }

    /**
     * Create integrator.
     * @param initialState initial state
     * @return integrator
     */
    private ODEIntegrator buildIntegrator(final SpacecraftState initialState) {
        final ODEIntegratorBuilder integratorBuilder = propagationSettings.getIntegrationSettings().getIntegratorBuilder();
        if (initialState.isOrbitDefined()) {
            final Orbit orbit = initialState.getOrbit();
            return integratorBuilder.buildIntegrator(orbit, orbit.getType());
        } else {
            return integratorBuilder.buildIntegrator(initialState.getAbsPVA());
        }
    }

    /**
     * Create Gradient numerical propagator.
     * @param initialState initial state
     * @return numerical propagator.
     */
    protected FieldNumericalPropagator<Gradient> buildFieldPropagator(final FieldSpacecraftState<Gradient> initialState) {
        final Field<Gradient> field = initialState.getDate().getField();
        final FieldODEIntegrator<Gradient> integrator = buildFieldIntegrator(initialState);
        final FieldNumericalPropagator<Gradient> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setIgnoreCentralAttraction(true);
        propagator.removeForceModels();
        propagator.setInitialState(initialState);
        propagator.setIgnoreCentralAttraction(false);
        if (initialState.isOrbitDefined()) {
            propagator.setOrbitType(initialState.getOrbit().getType());
        } else {
            propagator.setOrbitType(null);
            if (propagationSettings.getForceModels().stream().noneMatch(NewtonianAttraction.class::isInstance)) {
                propagator.setIgnoreCentralAttraction(true);
            }
        }
        propagator.setAttitudeProvider(propagationSettings.getAttitudeProvider());
        for (final ForceModel forceModel: propagationSettings.getForceModels()) {
            propagator.addForceModel(forceModel);
        }
        propagator.addAdditionalDerivativesProvider(propagationSettings.getAdjointDerivativesProvider()
            .buildFieldAdditionalDerivativesProvider(field));
        return propagator;
    }

    /**
     * Create Field integrator.
     * @param initialState initial state
     * @param <T> field type
     * @return integrator.
     */
    private <T extends CalculusFieldElement<T>> FieldODEIntegrator<T> buildFieldIntegrator(final FieldSpacecraftState<T> initialState) {
        final FieldODEIntegratorBuilder<T> integratorBuilder = propagationSettings.getIntegrationSettings()
            .getFieldIntegratorBuilder(initialState.getMass().getField());
        if (initialState.isOrbitDefined()) {
            final FieldOrbit<T> orbit = initialState.getOrbit();
            return integratorBuilder.buildIntegrator(orbit, orbit.getType());
        } else {
            return integratorBuilder.buildIntegrator(initialState.getAbsPVA());
        }
    }
}
