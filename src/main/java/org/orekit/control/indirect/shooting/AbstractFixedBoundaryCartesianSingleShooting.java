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
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.FieldOrdinaryDifferentialEquation;
import org.hipparchus.ode.nonstiff.FieldExplicitRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.shooting.boundary.CartesianBoundaryConditionChecker;
import org.orekit.control.indirect.shooting.boundary.FixedTimeBoundaryOrbits;
import org.orekit.control.indirect.shooting.boundary.FixedTimeCartesianBoundaryStates;
import org.orekit.control.indirect.shooting.propagation.AdjointDynamicsProvider;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.FieldCombinedDerivatives;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    /** Default value for defects scaling. */
    private static final double DEFAULT_SCALE = 1.;

    /** Template for initial state. Contains the initial Cartesian coordinates. */
    private final SpacecraftState initialSpacecraftStateTemplate;

    /** Terminal Cartesian coordinates. */
    private final TimeStampedPVCoordinates terminalCartesianState;

    /** Condition checker. */
    private final CartesianBoundaryConditionChecker conditionChecker;

    /** Holder for integration steps. */
    private final List<AbsoluteDate> stepDates = new ArrayList<>();

    /** Scale for velocity defects (m). */
    private double scaleVelocityDefects;

    /** Scale for position defects (m/s). */
    private double scalePositionDefects;

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
        this.scalePositionDefects = DEFAULT_SCALE;
        this.scaleVelocityDefects = DEFAULT_SCALE;
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
        this.scalePositionDefects = DEFAULT_SCALE;
        this.scaleVelocityDefects = DEFAULT_SCALE;
    }

    /**
     * Setter for scale of position defects.
     * @param scalePositionDefects new scale
     */
    public void setScalePositionDefects(final double scalePositionDefects) {
        this.scalePositionDefects = scalePositionDefects;
    }

    /**
     * Getter for scale of position defects.
     * @return scale
     */
    public double getScalePositionDefects() {
        return scalePositionDefects;
    }

    /**
     * Setter for scale of velocity defects.
     * @param scaleVelocityDefects new scale
     */
    public void setScaleVelocityDefects(final double scaleVelocityDefects) {
        this.scaleVelocityDefects = scaleVelocityDefects;
    }

    /**
     * Getter for scale of velocity defects.
     * @return scale
     */
    public double getScaleVelocityDefects() {
        return scaleVelocityDefects;
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
        if (initialGuessSolution.isConverged()) {
            return initialGuessSolution;
        } else {
            return iterate(initialMass, initialGuess);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected NumericalPropagator buildPropagator(final SpacecraftState initialState) {
        final NumericalPropagator propagator = super.buildPropagator(initialState);
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
    private ShootingBoundaryOutput computeCandidateSolution(final SpacecraftState initialState,
                                                            final int iterationCount) {
        stepDates.clear();
        final NumericalPropagator propagator = buildPropagator(initialState);
        propagator.setStepHandler((OrekitStepInterpolator interpolator) ->
                stepDates.add(interpolator.getCurrentState().getDate()));
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
            final ShootingBoundaryOutput candidateSolution = computeCandidateSolution(initialState, iterationCount);
            if (candidateSolution.isConverged()) {
                return candidateSolution;
            }
            final FieldSpacecraftState<Gradient> fieldInitialState = createFieldInitialStateWithMassAndAdjoint(initialMass,
                initialAdjoint);
            initialState = fieldInitialState.toSpacecraftState();
            final FieldSpacecraftState<Gradient> fieldTerminalState = propagateField(fieldInitialState);
            initialAdjoint = updateAdjoint(initialAdjoint, fieldTerminalState);
            if (Double.isNaN(initialAdjoint[0])) {
                return candidateSolution;
            }
            iterationCount++;
            maximumIterationCount = getConditionChecker().getMaximumIterationCount();
        }
        return computeCandidateSolution(initialState, maximumIterationCount);
    }

    /**
     * Propagate in Field. Does not use a full propagator (only the integrator, moving step by step using the history of non-Field propagation).
     * It is faster as adaptive step and event detection are particularly slow with Field.
     * @param fieldInitialState initial state
     * @return terminal state
     * @since 13.0
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
     * Checks convergence.
     * @param actualTerminalState achieved terminal state
     * @return convergence flag
     */
    private boolean checkConvergence(final SpacecraftState actualTerminalState) {
        final boolean isCartesianConverged = getConditionChecker().isConverged(getTerminalCartesianState(),
                actualTerminalState.getPVCoordinates());
        if (isCartesianConverged) {
            final String adjointName = getPropagationSettings().getAdjointDynamicsProvider().getAdjointName();
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
        final String adjointName = getPropagationSettings().getAdjointDynamicsProvider().getAdjointName();
        return createInitialStateWithMass(initialMass).addAdditionalState(adjointName, initialAdjoint);
    }

    /**
     * Create initial state with input mass.
     * @param initialMass initial mass
     * @return state
     */
    private SpacecraftState createInitialStateWithMass(final double initialMass) {
        if (initialSpacecraftStateTemplate.isOrbitDefined()) {
            return new SpacecraftState(initialSpacecraftStateTemplate.getOrbit(),
                initialSpacecraftStateTemplate.getAttitude(), initialMass);
        } else {
            return new SpacecraftState(initialSpacecraftStateTemplate.getAbsPVA(),
                initialSpacecraftStateTemplate.getAttitude(), initialMass);
        }
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
            fieldInitialAdjoint[i] = Gradient.variable(parametersNumber, i, initialAdjoint[i]);
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
     * @since 13.0
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
            final T mu = field.getZero().newInstance(initialSpacecraftStateTemplate.getMu());
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
     * Update initial adjoint vector.
     * @param originalInitialAdjoint original initial adjoint (before update)
     * @param fieldTerminalState final state of gradient propagation
     * @return updated initial adjoint vector
     */
    protected abstract double[] updateAdjoint(double[] originalInitialAdjoint,
                                              FieldSpacecraftState<Gradient> fieldTerminalState);

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> FieldOrdinaryDifferentialEquation<T> buildFieldODE(final FieldAbsoluteDate<T> referenceDate) {
        return new FieldOrdinaryDifferentialEquation<T>() {
            @Override
            public int getDimension() {
                return 7 + getPropagationSettings().getAdjointDynamicsProvider().getDimension();
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
                final FieldCombinedDerivatives<T> combinedDerivatives = getPropagationSettings().getAdjointDynamicsProvider().
                        buildFieldAdditionalDerivativesProvider(field).combinedDerivatives(state);
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
