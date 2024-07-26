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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.Attitude;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.FieldODEIntegratorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Defines two-point boundary values for indirect shooting methods with Cartesian coordinates.
 *
 * @author Romain Serra
 * @since 12.2
 * @see org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider
 * @see org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider
 */
public class FixedTimeCartesianSingleShooting {

    /** Propagation settings. */
    private final CartesianShootingPropagationSettings propagationSettings;

    /** Template for initial state. Contains the initial Cartesian coordinates. */
    private final SpacecraftState initialSpacecraftStateTemplate;

    /** Terminal Cartesian coordinates. */
    private final TimeStampedPVCoordinates terminalCartesianState;

    /** Differential corrector. */
    private final ShootingDifferentialCorrection differentialCorrection;

    /**
     * Constructor with boundary conditions as orbits.
     * @param propagationSettings propagation settings
     * @param boundaryConditions boundary conditions as {@link FixedTimeCartesianBoundaryStates}
     * @param differentialCorrection differential correction
     */
    public FixedTimeCartesianSingleShooting(final CartesianShootingPropagationSettings propagationSettings,
                                            final FixedTimeCartesianBoundaryStates boundaryConditions,
                                            final ShootingDifferentialCorrection differentialCorrection) {
        this.propagationSettings = propagationSettings;
        this.differentialCorrection = differentialCorrection;
        this.initialSpacecraftStateTemplate = buildInitialStateTemplate(boundaryConditions.getInitialCartesianState());
        this.terminalCartesianState = boundaryConditions.getTerminalCartesianState().getPVCoordinates(propagationSettings.getPropagationFrame());
    }

    /**
     * Constructor with boundary conditions as orbits.
     * @param propagationSettings propagation settings
     * @param boundaryConditions boundary conditions as {@link FixedTimeBoundaryOrbits}
     * @param differentialCorrection differential correction
     */
    public FixedTimeCartesianSingleShooting(final CartesianShootingPropagationSettings propagationSettings,
                                            final FixedTimeBoundaryOrbits boundaryConditions,
                                            final ShootingDifferentialCorrection differentialCorrection) {
        this.propagationSettings = propagationSettings;
        this.differentialCorrection = differentialCorrection;
        this.initialSpacecraftStateTemplate = buildInitialStateTemplate(boundaryConditions.getInitialOrbit());
        this.terminalCartesianState = boundaryConditions.getTerminalOrbit().getPVCoordinates(propagationSettings.getPropagationFrame());
    }

    /**
     * Create template initial state (without adjoint varialbles) for propagation from orbits.
     * @param initialOrbit initial orbit
     * @return template propagation state
     */
    private SpacecraftState buildInitialStateTemplate(final Orbit initialOrbit) {
        final Frame frame = propagationSettings.getPropagationFrame();
        final CartesianOrbit cartesianOrbit = new CartesianOrbit(initialOrbit.getPVCoordinates(frame), frame,
            initialOrbit.getDate(), initialOrbit.getMu());
        final Attitude attitude = propagationSettings.getAttitudeProvider()
                .getAttitude(cartesianOrbit, cartesianOrbit.getDate(), cartesianOrbit.getFrame());
        return new SpacecraftState(cartesianOrbit, attitude);
    }

    /**
     * Create template initial state (without adjoint varialbles) for propagation.
     * @param initialCartesianState initial Cartesian state
     * @return template propagation state
     */
    private SpacecraftState buildInitialStateTemplate(final AbsolutePVCoordinates initialCartesianState) {
        final Frame frame = propagationSettings.getPropagationFrame();
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame,
                initialCartesianState.getPVCoordinates(frame));
        final Attitude attitude = propagationSettings.getAttitudeProvider()
                .getAttitude(absolutePVCoordinates, absolutePVCoordinates.getDate(), absolutePVCoordinates.getFrame());
        return new SpacecraftState(absolutePVCoordinates, attitude);
    }

    /**
     * Solve for the boundary conditions, given an initial mass and an initial guess for the adjoint variables.
     * @param initialMass initial mass
     * @param initialGuess initial guess
     * @return boundary problem solution
     */
    public CartesianShootingBoundarySolution solve(final double initialMass, final double[] initialGuess) {

        final NumericalPropagator propagator = buildPropagator(initialMass, initialGuess);
        final SpacecraftState initialState = propagator.getInitialState();
        final SpacecraftState actualTerminalState = propagator.propagate(terminalCartesianState.getDate());
        final PVCoordinates pvCoordinates = actualTerminalState.getPVCoordinates();
        if (checkConvergence(pvCoordinates)) {
            return new CartesianShootingBoundarySolution(initialState, actualTerminalState);
        }
        return null;
    }

    /**
     *
     * @param actualTerminalPV
     * @return
     */
    private boolean checkConvergence(final PVCoordinates actualTerminalPV) {
        final double toleranceDistance = 1;
        final double toleranceSpeed = 1e-4;
        return (actualTerminalPV.getPosition().getNorm() <= toleranceDistance && actualTerminalPV.getVelocity().getNorm() <= toleranceSpeed);
    }

    /**
     *
     * @param initialGuess
     * @return
     */
    private CartesianShootingBoundarySolution iterate(final double[] initialGuess) {
        return null;
    }

    /**
     * Create numerical propagator.
     * @param initialMass initial mass
     * @param initialAdjoint initial adjoint variables
     * @return numerical propagator
     */
    private NumericalPropagator buildPropagator(final double initialMass, final double[] initialAdjoint) {
        final ODEIntegrator integrator = buildIntegrator();
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        for (final ForceModel forceModel: propagationSettings.getForceModels()) {
            propagator.addForceModel(forceModel);
        }
        propagator.setAttitudeProvider(propagationSettings.getAttitudeProvider());
        final SpacecraftState stateWithMass = createStateWithMass(initialMass);
        if (initialSpacecraftStateTemplate.isOrbitDefined()) {
            propagator.setOrbitType(OrbitType.CARTESIAN);
        } else {
            propagator.setOrbitType(null);
        }
        final SpacecraftState initialState = stateWithMass.addAdditionalState(propagationSettings.getCartesianCost().getAdjointName(),
                initialAdjoint);
        propagator.setInitialState(initialState);
        return propagator;
    }

    /**
     *
     * @param initialMass
     * @return
     */
    private SpacecraftState createStateWithMass(final double initialMass) {
        if (initialSpacecraftStateTemplate.isOrbitDefined()) {
            return new SpacecraftState(initialSpacecraftStateTemplate.getOrbit(),
                    initialSpacecraftStateTemplate.getAttitude(), initialMass);
        } else {
             return new SpacecraftState(initialSpacecraftStateTemplate.getAbsPVA(),
                    initialSpacecraftStateTemplate.getAttitude(), initialMass);
        }
    }

    /**
     * Create integrator.
     * @return integrator
     */
    private ODEIntegrator buildIntegrator() {
        final ODEIntegratorBuilder integratorBuilder = propagationSettings.getIntegrationSettings().getIntegratorBuilder();
        return integratorBuilder.buildIntegrator(null, null);
    }

    /**
     * Create Gradient numerical propagator.
     * @param field gradient field
     * @param initialMass initial mass
     * @param initialAdjoint initial adjoint variables
     * @return numerical propagator.
     */
    private FieldNumericalPropagator<Gradient> buildFieldPropagator(final GradientField field, final double initialMass,
                                                                    final double[] initialAdjoint) {
        final FieldODEIntegrator<Gradient> integrator = buildFieldIntegrator(field);
        final FieldNumericalPropagator<Gradient> propagator = new FieldNumericalPropagator<>(field, integrator);
        for (final ForceModel forceModel: propagationSettings.getForceModels()) {
            propagator.addForceModel(forceModel);
        }
        propagator.setAttitudeProvider(propagationSettings.getAttitudeProvider());
        final FieldSpacecraftState<Gradient> stateWithMass = new FieldSpacecraftState<>(field, createStateWithMass(initialMass));
        if (initialSpacecraftStateTemplate.isOrbitDefined()) {
            propagator.setOrbitType(OrbitType.CARTESIAN);
        } else {
            propagator.setOrbitType(null);
        }
        final int adjointDimension = initialAdjoint.length;
        final Gradient[] fieldInitialAdjoint = MathArrays.buildArray(field, adjointDimension);
        for (int i = 0; i < adjointDimension; i++) {
            fieldInitialAdjoint[i] = Gradient.variable(adjointDimension, i, initialAdjoint[i]);
        }
        final FieldSpacecraftState<Gradient> initialState = stateWithMass
            .addAdditionalState(propagationSettings.getCartesianCost().getAdjointName(), fieldInitialAdjoint);
        propagator.setInitialState(initialState);
        return propagator;
    }

    /**
     * Create Gradient integrator.
     * @param field gradient field
     * @return integrator.
     */
    private FieldODEIntegrator<Gradient> buildFieldIntegrator(final GradientField field) {
        final FieldODEIntegratorBuilder<Gradient> integratorBuilder = propagationSettings.getIntegrationSettings().getFieldIntegratorBuilder(field);
        return integratorBuilder.buildIntegrator(null, null);
    }
}
