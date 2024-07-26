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

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.Attitude;
import org.orekit.control.indirect.shooting.boundary.CartesianBoundaryConditionChecker;
import org.orekit.control.indirect.shooting.boundary.FixedTimeBoundaryOrbits;
import org.orekit.control.indirect.shooting.boundary.FixedTimeCartesianBoundaryStates;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Abstract class for indirect single shooting methods with Cartesian coordinates for fixed time fixed boundary.
 * Inheritors must implement the iteration update, assuming derivatives are needed.
 * Terminal mass is assumed to be free, thus corresponding adjoint must vanish at terminal time.
 * On the other hand, other terminal adjoint variables are free because the Cartesian state is fixed.
 *
 * @author Romain Serra
 * @since 12.2
 * @see org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider
 * @see org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider
 */
public abstract class AbstractFixedBoundaryCartesianSingleShooting extends AbstractIndirectShooting {

    /** Template for initial state. Contains the initial Cartesian coordinates. */
    private final SpacecraftState initialSpacecraftStateTemplate;

    /** Terminal Cartesian coordinates. */
    private final TimeStampedPVCoordinates terminalCartesianState;

    /** Condition checker. */
    private final CartesianBoundaryConditionChecker conditionChecker;

    /** Tolerance for convergence on terminal mass adjoint, if applicable to dynamics. */
    private double toleranceMassAdjoint = DEFAULT_TOLERANCE_MASS_ADJOINT;

    /**
     * Constructor with boundary conditions as orbits.
     * @param propagationSettings propagation settings
     * @param boundaryConditions boundary conditions as {@link FixedTimeCartesianBoundaryStates}
     * @param conditionChecker boundary condition checker
     */
    protected AbstractFixedBoundaryCartesianSingleShooting(final ShootingPropagationSettings propagationSettings,
                                                           final FixedTimeCartesianBoundaryStates boundaryConditions,
                                                           final CartesianBoundaryConditionChecker conditionChecker) {
        super(propagationSettings);
        this.conditionChecker = conditionChecker;
        this.initialSpacecraftStateTemplate = buildInitialStateTemplate(boundaryConditions.getInitialCartesianState());
        this.terminalCartesianState = boundaryConditions.getTerminalCartesianState().getPVCoordinates(propagationSettings.getPropagationFrame());
    }

    /**
     * Constructor with boundary conditions as orbits.
     * @param propagationSettings propagation settings
     * @param boundaryConditions boundary conditions as {@link FixedTimeBoundaryOrbits}
     * @param conditionChecker boundary condition checker
     */
    protected AbstractFixedBoundaryCartesianSingleShooting(final ShootingPropagationSettings propagationSettings,
                                                           final FixedTimeBoundaryOrbits boundaryConditions,
                                                           final CartesianBoundaryConditionChecker conditionChecker) {
        super(propagationSettings);
        this.conditionChecker = conditionChecker;
        this.initialSpacecraftStateTemplate = buildInitialStateTemplate(boundaryConditions.getInitialOrbit());
        this.terminalCartesianState = boundaryConditions.getTerminalOrbit().getPVCoordinates(propagationSettings.getPropagationFrame());
    }

    /**
     * Getter for the boundary condition checker.
     * @return checker
     */
    protected CartesianBoundaryConditionChecker getConditionChecker() {
        return conditionChecker;
    }

    /**
     * Getter for the target terminal Cartesian state vector.
     * @return expected terminal state
     */
    protected TimeStampedPVCoordinates getTerminalCartesianState() {
        return terminalCartesianState;
    }

    /**
     * Setter for mass adjoint tolerance.
     * @param toleranceMassAdjoint new tolerance value
     */
    public void setToleranceMassAdjoint(final double toleranceMassAdjoint) {
        this.toleranceMassAdjoint = FastMath.abs(toleranceMassAdjoint);
    }

    /**
     * Create template initial state (without adjoint varialbles) for propagation from orbits.
     * @param initialOrbit initial orbit
     * @return template propagation state
     */
    private SpacecraftState buildInitialStateTemplate(final Orbit initialOrbit) {
        final Frame frame = getPropagationSettings().getPropagationFrame();
        final CartesianOrbit cartesianOrbit = new CartesianOrbit(initialOrbit.getPVCoordinates(frame), frame,
            initialOrbit.getDate(), initialOrbit.getMu());
        final Attitude attitude = getPropagationSettings().getAttitudeProvider()
                .getAttitude(cartesianOrbit, cartesianOrbit.getDate(), cartesianOrbit.getFrame());
        return new SpacecraftState(cartesianOrbit, attitude);
    }

    /**
     * Create template initial state (without adjoint varialbles) for propagation.
     * @param initialCartesianState initial Cartesian state
     * @return template propagation state
     */
    private SpacecraftState buildInitialStateTemplate(final AbsolutePVCoordinates initialCartesianState) {
        final Frame frame = getPropagationSettings().getPropagationFrame();
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame,
                initialCartesianState.getPVCoordinates(frame));
        final Attitude attitude = getPropagationSettings().getAttitudeProvider()
                .getAttitude(absolutePVCoordinates, absolutePVCoordinates.getDate(), absolutePVCoordinates.getFrame());
        return new SpacecraftState(absolutePVCoordinates, attitude);
    }

    /** {@inheritDoc} */
    @Override
    public ShootingBoundaryOutput solve(final double initialMass, final double[] initialGuess) {
        // check initial guess
        final SpacecraftState initialState = createStateWithMassAndAdjoint(initialMass, initialGuess);
        final ShootingBoundaryOutput initialGuessSolution = computeCandidateSolution(initialState, 0);
        final SpacecraftState actualTerminalState = initialGuessSolution.getTerminalState();
        if (checkConvergence(actualTerminalState)) {
            return initialGuessSolution;
        } else {
            return iterate(initialMass, initialGuess);
        }
    }

    /**
     * Form solution container with input initial state.
     * @param initialState initial state
     * @param iterationCount iteration count
     * @return candidate solution
     */
    private ShootingBoundaryOutput computeCandidateSolution(final SpacecraftState initialState,
                                                            final int iterationCount) {
        final NumericalPropagator propagator = buildPropagator(initialState);
        final SpacecraftState actualTerminalState = propagator.propagate(getTerminalCartesianState().getDate());
        final boolean converged = checkConvergence(actualTerminalState);
        return new ShootingBoundaryOutput(converged, iterationCount, initialState, getPropagationSettings(), actualTerminalState);
    }

    /**
     * Iterate on initial guess to solve boundary problem.
     * @param initialMass initial mass
     * @param initialGuess initial guess for initial adjoint variables
     * @return solution (or null pointer if not converged)
     */
    private ShootingBoundaryOutput iterate(final double initialMass, final double[] initialGuess) {
        double[] initialAdjoint = initialGuess.clone();
        int iterationCount = 0;
        int maximumIterationCount = getConditionChecker().getMaximumIterationCount();
        SpacecraftState initialState = createStateWithMassAndAdjoint(initialMass, initialGuess);
        while (iterationCount < maximumIterationCount) {
            final FieldSpacecraftState<Gradient> fieldInitialState = createFieldStateWithMassAndAdjoint(initialMass,
                initialAdjoint);
            final Field<Gradient> field = fieldInitialState.getDate().getField();
            final FieldAbsoluteDate<Gradient> fieldTerminalDate = new FieldAbsoluteDate<>(field, getTerminalCartesianState().getDate());
            final FieldNumericalPropagator<Gradient> fieldPropagator = buildFieldPropagator(fieldInitialState);
            final FieldSpacecraftState<Gradient> fieldTerminalState = fieldPropagator.propagate(fieldTerminalDate);
            initialState = fieldInitialState.toSpacecraftState();
            if (checkConvergence(fieldTerminalState.toSpacecraftState())) {
                // make sure non-Field version also satisfies convergence criterion
                final ShootingBoundaryOutput solution = computeCandidateSolution(initialState, iterationCount);
                if (solution.isConverged()) {
                    return solution;
                }
            }
            initialAdjoint = updateAdjoint(initialAdjoint, fieldTerminalState);
            iterationCount++;
            maximumIterationCount = getConditionChecker().getMaximumIterationCount();
        }
        return computeCandidateSolution(initialState, maximumIterationCount);
    }

    /**
     * Checks convergence.
     * @param actualTerminalState achieved terminal state
     * @return convergence flag
     */
    private boolean checkConvergence(final SpacecraftState actualTerminalState) {
        final boolean isCartesianConverged = getConditionChecker().isConverged(getTerminalCartesianState(),
                actualTerminalState.getPVCoordinates());
        if (isCartesianConverged) {
            final String adjointName = getPropagationSettings().getAdjointDerivativesProvider().getAdjointName();
            final double[] terminalAdjoint = actualTerminalState.getAdditionalState(adjointName);
            if (terminalAdjoint.length == 7) {
                return FastMath.abs(terminalAdjoint[6]) < toleranceMassAdjoint;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Create initial state with input mass and adjoint vector.
     * @param initialMass initial mass
     * @param initialAdjoint initial adjoint variables
     * @return state
     */
    protected SpacecraftState createStateWithMassAndAdjoint(final double initialMass, final double[] initialAdjoint) {
        final String adjointName = getPropagationSettings().getAdjointDerivativesProvider().getAdjointName();
        return createStateWithMass(initialMass).addAdditionalState(adjointName, initialAdjoint);
    }

    /**
     * Create initial state with input mass.
     * @param initialMass initial mass
     * @return state
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
     * Create initial state with input mass and adjoint vector.
     * @param initialMass initial mass
     * @param initialAdjoint initial adjoint variables
     * @return state
     */
    protected FieldSpacecraftState<Gradient> createFieldStateWithMassAndAdjoint(final double initialMass,
                                                                                final double[] initialAdjoint) {
        final int parametersNumber = initialAdjoint.length;
        final GradientField field = GradientField.getField(parametersNumber);
        final FieldSpacecraftState<Gradient> stateWithoutAdjoint = new FieldSpacecraftState<>(field, createStateWithMass(initialMass));
        final Gradient[] fieldInitialAdjoint = MathArrays.buildArray(field, initialAdjoint.length);
        for (int i = 0; i < parametersNumber; i++) {
            fieldInitialAdjoint[i] = Gradient.variable(parametersNumber, i, initialAdjoint[i]);
        }
        final String adjointName = getPropagationSettings().getAdjointDerivativesProvider()
            .buildFieldAdditionalDerivativesProvider(field).getName();
        return stateWithoutAdjoint.addAdditionalState(adjointName, fieldInitialAdjoint);
    }

    /**
     * Update initial adjoint vector.
     * @param originalInitialAdjoint initial adjoint before update
     * @param fieldTerminalState propagated field state at terminal date
     * @return updated initial adjoint vector
     */
    protected abstract double[] updateAdjoint(double[] originalInitialAdjoint,
                                              FieldSpacecraftState<Gradient> fieldTerminalState);
}
