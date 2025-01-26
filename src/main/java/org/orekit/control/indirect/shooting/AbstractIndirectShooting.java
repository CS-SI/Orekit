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
package org.orekit.control.indirect.shooting;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.FieldOrdinaryDifferentialEquation;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.nonstiff.FieldExplicitRungeKuttaIntegrator;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.FieldExplicitRungeKuttaIntegratorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.FieldAbsoluteDate;

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
        final NumericalPropagator propagator =
              new NumericalPropagator(integrator, propagationSettings.getAttitudeProvider());
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
        for (final ForceModel forceModel: propagationSettings.getForceModels()) {
            propagator.addForceModel(forceModel);
        }
        final AdditionalDerivativesProvider derivativesProvider = propagationSettings.getAdjointDynamicsProvider()
                .buildAdditionalDerivativesProvider();
        propagator.addAdditionalDerivativesProvider(derivativesProvider);
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
            return integratorBuilder.buildIntegrator(orbit, orbit.getType(), NumericalPropagator.DEFAULT_POSITION_ANGLE_TYPE);
        } else {
            return integratorBuilder.buildIntegrator(initialState.getAbsPVA());
        }
    }

    /**
     * Create Field integrator.
     * @param initialState initial state
     * @param <T> field type
     * @return integrator.
     */
    protected <T extends CalculusFieldElement<T>> FieldExplicitRungeKuttaIntegrator<T> buildFieldIntegrator(final FieldSpacecraftState<T> initialState) {
        final Field<T> field = initialState.getMass().getField();
        final FieldExplicitRungeKuttaIntegratorBuilder<T> integratorBuilder = propagationSettings.getIntegrationSettings()
            .getFieldIntegratorBuilder(field);
        if (initialState.isOrbitDefined()) {
            final FieldOrbit<T> orbit = initialState.getOrbit();
            return integratorBuilder.buildIntegrator(field, orbit.toOrbit(), orbit.getType(),
                    NumericalPropagator.DEFAULT_POSITION_ANGLE_TYPE);
        } else {
            return integratorBuilder.buildIntegrator(initialState.getAbsPVA());
        }
    }

    /**
     * Build Ordinary Differential Equation for Field.
     * @param referenceDate date defining the origin of times
     * @param <T> field type
     * @return Field ODE
     * @since 13.0
     */
    protected abstract  <T extends CalculusFieldElement<T>> FieldOrdinaryDifferentialEquation<T> buildFieldODE(FieldAbsoluteDate<T> referenceDate);
}
