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
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.FieldOrdinaryDifferentialEquation;
import org.hipparchus.ode.nonstiff.FieldExplicitRungeKuttaIntegrator;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.shooting.propagation.AdjointDynamicsProvider;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.FieldAdditionalDerivativesProvider;
import org.orekit.propagation.integration.FieldCombinedDerivatives;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.PropagationStepRecorder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinates;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract class for indirect single shooting methods with fixed initial Cartesian state.
 * Inheritors must implement the iteration update, assuming derivatives are needed.
 *
 * @author Romain Serra
 * @since 13.0
 * @see CartesianAdjointDerivativesProvider
 * @see org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider
 */
public abstract class AbstractFixedInitialCartesianSingleShooting extends AbstractIndirectShooting {

    /** Template for initial state. Contains the initial Cartesian coordinates. */
    private final SpacecraftState initialSpacecraftStateTemplate;

    /**
     * Step handler to record propagation steps.
     */
    private final PropagationStepRecorder propagationStepRecorder;

    /** Scales for automatic differentiation variables. */
    private double[] scales;

    /**
     * Constructor with boundary conditions as orbits.
     * @param propagationSettings propagation settings
     * @param initialSpacecraftStateTemplate template for initial propagation state
     */
    protected AbstractFixedInitialCartesianSingleShooting(final ShootingPropagationSettings propagationSettings,
                                                          final SpacecraftState initialSpacecraftStateTemplate) {
        super(propagationSettings);
        this.initialSpacecraftStateTemplate = initialSpacecraftStateTemplate;
        this.propagationStepRecorder = new PropagationStepRecorder();
    }

    /**
     * Build unit scales.
     * @return scales
     */
    private double[] getUnitScales() {
        final double[] unitScales = new double[getPropagationSettings().getAdjointDynamicsProvider().getDimension()];
        Arrays.fill(unitScales, 1.);
        return unitScales;
    }

    /**
     * Protected getter for the differentiation scales.
     * @return scales for variable scales
     */
    protected double[] getScales() {
        return scales;
    }

    /**
     * Returns the maximum number of iterations.
     * @return maximum iterations
     */
    public abstract int getMaximumIterationCount();

    /** {@inheritDoc} */
    @Override
    public ShootingBoundaryOutput solve(final double initialMass, final double[] initialGuess) {
        return solve(initialMass, initialGuess, getUnitScales());
    }

    /**
     * Solve for the boundary conditions, given an initial mass and an initial guess for the adjoint variables.
     * Uses scales for automatic differentiation.
     * @param initialMass initial mass
     * @param initialGuess initial guess
     * @param userScales scales
     * @return boundary problem solution
     */
    public ShootingBoundaryOutput solve(final double initialMass, final double[] initialGuess,
                                        final double[] userScales) {
        scales = userScales.clone();
        final SpacecraftState initialState = createStateWithMassAndAdjoint(initialMass, initialGuess);
        final ShootingBoundaryOutput initialGuessSolution = computeCandidateSolution(initialState, 0);
        if (initialGuessSolution.isConverged()) {
            return initialGuessSolution;
        } else {
            return iterate(initialGuessSolution);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected NumericalPropagator buildPropagator(final SpacecraftState initialState) {
        final NumericalPropagator propagator = super.buildPropagator(initialState);
        propagator.setStepHandler(propagationStepRecorder);
        final CartesianAdjointDerivativesProvider derivativesProvider = (CartesianAdjointDerivativesProvider)
            getPropagationSettings().getAdjointDynamicsProvider().buildAdditionalDerivativesProvider();
        derivativesProvider.getCost().getEventDetectors().forEach(propagator::addEventDetector);
        return propagator;
    }

    /**
     * Form solution container with input initial state.
     * @param initialState initial state
     * @param iterationCount iteration count
     * @return candidate solution
     */
    public abstract ShootingBoundaryOutput computeCandidateSolution(SpacecraftState initialState, int iterationCount);

    /**
     * Iterate on initial guess to solve boundary problem.
     * @param initialGuess initial guess
     * @return solution (or null pointer if not converged)
     */
    private ShootingBoundaryOutput iterate(final ShootingBoundaryOutput initialGuess) {
        int iterationCount = 1;
        SpacecraftState initialState = initialGuess.getInitialState();
        ShootingBoundaryOutput candidateSolution = initialGuess;
        final String adjointName = getPropagationSettings().getAdjointDynamicsProvider().getAdjointName();
        double[] initialAdjoint = initialState.getAdditionalState(adjointName).clone();
        final double initialMass = initialState.getMass();
        while (iterationCount < getMaximumIterationCount()) {
            if (candidateSolution.isConverged()) {
                return candidateSolution;
            }
            final FieldSpacecraftState<Gradient> fieldInitialState = createFieldInitialStateWithMassAndAdjoint(initialMass,
                initialAdjoint);
            initialState = fieldInitialState.toSpacecraftState();
            final FieldSpacecraftState<Gradient> fieldTerminalState = propagateField(fieldInitialState);
            initialAdjoint = updateShootingVariables(initialAdjoint, fieldTerminalState);
            if (Double.isNaN(initialAdjoint[0])) {
                return candidateSolution;
            } else {
                candidateSolution = computeCandidateSolution(initialState, iterationCount);
            }
            iterationCount++;
        }
        return candidateSolution;
    }

    /**
     * Propagate in Field. Does not use a full propagator (only the integrator, moving step by step using the history of non-Field propagation).
     * It is faster as adaptive step and event detection are particularly slow with Field.
     * @param fieldInitialState initial state
     * @return terminal state
     */
    private FieldSpacecraftState<Gradient> propagateField(final FieldSpacecraftState<Gradient> fieldInitialState) {
        final FieldAbsoluteDate<Gradient> initialDate = fieldInitialState.getDate();
        final Field<Gradient> field = initialDate.getField();
        final FieldExplicitRungeKuttaIntegrator<Gradient> fieldIntegrator = buildFieldIntegrator(fieldInitialState);
        final FieldOrdinaryDifferentialEquation<Gradient> fieldODE = buildFieldODE(fieldInitialState.getDate());
        final AdjointDynamicsProvider dynamicsProvider = getPropagationSettings().getAdjointDynamicsProvider();
        AbsoluteDate date = initialDate.toAbsoluteDate();
        // build initial state as array
        Gradient[] integrationState = MathArrays.buildArray(field, fieldODE.getDimension());
        final FieldPVCoordinates<Gradient> pvCoordinates = fieldInitialState.getPVCoordinates();
        System.arraycopy(pvCoordinates.getPosition().toArray(), 0, integrationState, 0, 3);
        System.arraycopy(pvCoordinates.getVelocity().toArray(), 0, integrationState, 3, 3);
        integrationState[6] = fieldInitialState.getMass();
        System.arraycopy(fieldInitialState.getAdditionalState(dynamicsProvider.getAdjointName()), 0, integrationState,
                7, dynamicsProvider.getDimension());
        // step-by-step integration
        final List<AbsoluteDate> stepDates = propagationStepRecorder.copyStates().stream().map(SpacecraftState::getDate)
                .collect(Collectors.toList());
        for (final AbsoluteDate stepDate: stepDates) {
            final Gradient time = initialDate.durationFrom(date).negate();
            final Gradient nextTime = initialDate.durationFrom(stepDate).negate();
            integrationState = fieldIntegrator.singleStep(fieldODE, time, integrationState, nextTime);
            date = new AbsoluteDate(stepDate);
        }
        // turn terminal state into Orekit object
        final Gradient[] terminalCartesian = Arrays.copyOfRange(integrationState, 0, 6);
        final Gradient[] terminalAdjoint = Arrays.copyOfRange(integrationState, 7, integrationState.length);
        return createFieldState(new FieldAbsoluteDate<>(field, date), terminalCartesian, integrationState[6],
                terminalAdjoint);
    }

    /**
     * Create initial state with input mass.
     * @param initialMass initial mass
     * @return state
     */
    protected SpacecraftState createInitialStateWithMass(final double initialMass) {
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
    private SpacecraftState createStateWithMassAndAdjoint(final double initialMass, final double[] initialAdjoint) {
        final String adjointName = getPropagationSettings().getAdjointDynamicsProvider().getAdjointName();
        return createInitialStateWithMass(initialMass).addAdditionalState(adjointName, initialAdjoint);
    }

    /**
     * Create initial Gradient state with input mass and adjoint vector, the latter being the independent variables.
     * @param initialMass initial mass
     * @param initialAdjoint initial adjoint variables
     * @return state
     */
    protected FieldSpacecraftState<Gradient> createFieldInitialStateWithMassAndAdjoint(final double initialMass,
                                                                                       final double[] initialAdjoint) {
        final int parametersNumber = initialAdjoint.length;
        final GradientField field = GradientField.getField(parametersNumber);
        final FieldSpacecraftState<Gradient> stateWithoutAdjoint = new FieldSpacecraftState<>(field,
                createInitialStateWithMass(initialMass));
        final Gradient[] fieldInitialAdjoint = MathArrays.buildArray(field, initialAdjoint.length);
        for (int i = 0; i < parametersNumber; i++) {
            fieldInitialAdjoint[i] = Gradient.variable(parametersNumber, i, 0.).multiply(getScales()[i]).add(initialAdjoint[i]);
        }
        final String adjointName = getPropagationSettings().getAdjointDynamicsProvider().getAdjointName();
        return stateWithoutAdjoint.addAdditionalState(adjointName, fieldInitialAdjoint);
    }

    /**
     * Create state.
     * @param date epoch
     * @param cartesian Cartesian variables
     * @param mass mass
     * @param adjoint adjoint variables
     * @param <T> field type
     * @return state
     */
    protected <T extends CalculusFieldElement<T>> FieldSpacecraftState<T> createFieldState(final FieldAbsoluteDate<T> date,
                                                                                           final T[] cartesian,
                                                                                           final T mass,
                                                                                           final T[] adjoint) {
        final Field<T> field = mass.getField();
        final Frame frame = getPropagationSettings().getPropagationFrame();
        final FieldVector3D<T> position = new FieldVector3D<>(Arrays.copyOfRange(cartesian, 0, 3));
        final FieldVector3D<T> velocity = new FieldVector3D<>(Arrays.copyOfRange(cartesian, 3, 6));
        final FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity);
        final FieldSpacecraftState<T> stateWithoutAdjoint;
        if (initialSpacecraftStateTemplate.isOrbitDefined()) {
            final T mu = field.getZero().newInstance(initialSpacecraftStateTemplate.getOrbit().getMu());
            final FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(pvCoordinates, frame, date, mu);
            final FieldAttitude<T> attitude = getPropagationSettings().getAttitudeProvider().getAttitude(orbit,
                    orbit.getDate(), orbit.getFrame());
            stateWithoutAdjoint = new FieldSpacecraftState<>(orbit, attitude, mass);
        } else {
            final FieldAbsolutePVCoordinates<T> absolutePVCoordinates = new FieldAbsolutePVCoordinates<>(frame, date,
                    pvCoordinates);
            final FieldAttitude<T> attitude = getPropagationSettings().getAttitudeProvider().getAttitude(absolutePVCoordinates,
                    absolutePVCoordinates.getDate(), absolutePVCoordinates.getFrame());
            stateWithoutAdjoint = new FieldSpacecraftState<>(absolutePVCoordinates, attitude, mass);
        }
        final String adjointName = getPropagationSettings().getAdjointDynamicsProvider().getAdjointName();
        return stateWithoutAdjoint.addAdditionalState(adjointName, adjoint);
    }

    /**
     * Update shooting variables.
     * @param originalShootingVariables original shooting variables (before update)
     * @param fieldTerminalState final state of gradient propagation
     * @return updated shooting variables
     */
    protected abstract double[] updateShootingVariables(double[] originalShootingVariables,
                                                        FieldSpacecraftState<Gradient> fieldTerminalState);

    /**
     * Build Ordinary Differential Equation for Field.
     * @param referenceDate date defining the origin of times
     * @param <T> field type
     * @return Field ODE
     * @since 13.0
     */
    protected <T extends CalculusFieldElement<T>> FieldOrdinaryDifferentialEquation<T> buildFieldODE(final FieldAbsoluteDate<T> referenceDate) {
        final Field<T> field = referenceDate.getField();
        final AdjointDynamicsProvider adjointDynamicsProvider = getPropagationSettings().getAdjointDynamicsProvider();
        final FieldAdditionalDerivativesProvider<T> derivativesProvider = adjointDynamicsProvider
                .buildFieldAdditionalDerivativesProvider(field);

        return new FieldOrdinaryDifferentialEquation<T>() {
            @Override
            public int getDimension() {
                return 7 + adjointDynamicsProvider.getDimension();
            }

            @Override
            public T[] computeDerivatives(final T t, final T[] y) {
                // build state
                final T[] cartesian = Arrays.copyOfRange(y, 0, 6);
                final FieldAbsoluteDate<T> date = referenceDate.shiftedBy(t);
                final Field<T> field = date.getField();
                final T zero = field.getZero();
                final T[] adjoint = Arrays.copyOfRange(y, 7, y.length);
                final FieldSpacecraftState<T> state = createFieldState(date, cartesian, y[6], adjoint);
                // compute derivatives
                final T[] yDot = MathArrays.buildArray(field, getDimension());
                yDot[0] = zero.add(y[3]);
                yDot[1] = zero.add(y[4]);
                yDot[2] = zero.add(y[5]);
                FieldVector3D<T> totalAcceleration = FieldVector3D.getZero(field);
                for (final ForceModel forceModel: getPropagationSettings().getForceModels()) {
                    final FieldVector3D<T> acceleration = forceModel.acceleration(state, forceModel.getParameters(field));
                    totalAcceleration = totalAcceleration.add(acceleration);
                }
                yDot[3] = totalAcceleration.getX();
                yDot[4] = totalAcceleration.getY();
                yDot[5] = totalAcceleration.getZ();
                final FieldCombinedDerivatives<T> combinedDerivatives = derivativesProvider.combinedDerivatives(state);
                final T[] cartesianDotContribution = combinedDerivatives.getMainStateDerivativesIncrements();
                for (int i = 0; i < cartesianDotContribution.length; i++) {
                    yDot[i] = yDot[i].add(cartesianDotContribution[i]);
                }
                final T[] adjointDot = combinedDerivatives.getAdditionalDerivatives();
                System.arraycopy(adjointDot, 0, yDot, yDot.length - adjointDot.length, adjointDot.length);
                return yDot;
            }
        };
    }
}
