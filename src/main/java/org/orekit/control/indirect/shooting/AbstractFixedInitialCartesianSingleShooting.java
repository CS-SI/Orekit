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
import org.hipparchus.linear.LUDecomposition;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.FieldOrdinaryDifferentialEquation;
import org.hipparchus.ode.nonstiff.FieldExplicitRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.cost.ControlSwitchDetector;
import org.orekit.control.indirect.adjoint.cost.FieldControlSwitchDetector;
import org.orekit.control.indirect.shooting.propagation.AdjointDynamicsProvider;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventsLogger;
import org.orekit.propagation.events.FieldEventDetector;
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

    /** Event logger. */
    private final EventsLogger eventsLogger;

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
        this.eventsLogger = new EventsLogger();
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
        eventsLogger.clearLoggedEvents();
        derivativesProvider.getCost().getEventDetectors()
                .forEach(detector -> propagator.addEventDetector(eventsLogger.monitorDetector(detector)));
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
        final FieldExplicitRungeKuttaIntegrator<Gradient> fieldIntegrator = buildFieldIntegrator(fieldInitialState);
        final FieldOrdinaryDifferentialEquation<Gradient> fieldODE = buildFieldODE(fieldInitialState.getDate());
        final AdjointDynamicsProvider dynamicsProvider = getPropagationSettings().getAdjointDynamicsProvider();
        AbsoluteDate date = initialDate.toAbsoluteDate();
        Gradient[] integrationState = formatToArray(fieldInitialState, dynamicsProvider.getAdjointName());
        // step-by-step integration
        final List<EventsLogger.LoggedEvent> loggedEvents = eventsLogger.getLoggedEvents();
        final List<AbsoluteDate> stepDates = propagationStepRecorder.copyStates().stream().map(SpacecraftState::getDate)
                .collect(Collectors.toList());
        for (final AbsoluteDate stepDate: stepDates) {
            final Gradient time = initialDate.durationFrom(date).negate();
            final Gradient nextTime = initialDate.durationFrom(stepDate).negate();
            integrationState = fieldIntegrator.singleStep(fieldODE, time, integrationState, nextTime);
            // deal with switch event if any
            if (!loggedEvents.isEmpty()) {
                for (final EventsLogger.LoggedEvent loggedEvent: loggedEvents) {
                    final SpacecraftState loggedState = loggedEvent.getState();
                    if (loggedEvent.getEventDetector() instanceof ControlSwitchDetector && loggedState.getDate().isEqualTo(stepDate)) {
                        final ControlSwitchDetector switchDetector = (ControlSwitchDetector) loggedEvent.getEventDetector();
                        integrationState = findSwitchEventAndUpdateState(date, integrationState,
                                initialDate.toAbsoluteDate(), switchDetector, dynamicsProvider);
                    }
                }
            }
            date = new AbsoluteDate(stepDate);
        }
        return formatFromArray(date, integrationState);
    }

    /**
     * Create initial state with input mass.
     * @param initialMass initial mass
     * @return state
     */
    protected SpacecraftState createInitialStateWithMass(final double initialMass) {
        return initialSpacecraftStateTemplate.withMass(initialMass);
    }

    /**
     * Create initial state with input mass and adjoint vector.
     * @param initialMass initial mass
     * @param initialAdjoint initial adjoint variables
     * @return state
     */
    private SpacecraftState createStateWithMassAndAdjoint(final double initialMass, final double[] initialAdjoint) {
        final String adjointName = getPropagationSettings().getAdjointDynamicsProvider().getAdjointName();
        return createInitialStateWithMass(initialMass).addAdditionalData(adjointName, initialAdjoint);
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
        return stateWithoutAdjoint.addAdditionalData(adjointName, fieldInitialAdjoint);
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
            stateWithoutAdjoint = new FieldSpacecraftState<>(orbit, attitude).withMass(mass);
        } else {
            final FieldAbsolutePVCoordinates<T> absolutePVCoordinates = new FieldAbsolutePVCoordinates<>(frame, date,
                    pvCoordinates);
            final FieldAttitude<T> attitude = getPropagationSettings().getAttitudeProvider().getAttitude(absolutePVCoordinates,
                    absolutePVCoordinates.getDate(), absolutePVCoordinates.getFrame());
            stateWithoutAdjoint = new FieldSpacecraftState<>(absolutePVCoordinates, attitude).withMass(mass);
        }
        final String adjointName = getPropagationSettings().getAdjointDynamicsProvider().getAdjointName();
        return stateWithoutAdjoint.addAdditionalData(adjointName, adjoint);
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

    /**
     * Form array from Orekit object.
     * @param fieldState state
     * @param adjointName adjoint name
     * @return propagation state as array
     */
    private Gradient[] formatToArray(final FieldSpacecraftState<Gradient> fieldState,
                                     final String adjointName) {
        final Gradient[] adjoint = fieldState.getAdditionalState(adjointName);
        final int adjointDimension = adjoint.length;
        final Gradient[] integrationState = MathArrays.buildArray(fieldState.getMass().getField(), 7 + adjointDimension);
        final FieldPVCoordinates<Gradient> pvCoordinates = fieldState.getPVCoordinates();
        System.arraycopy(pvCoordinates.getPosition().toArray(), 0, integrationState, 0, 3);
        System.arraycopy(pvCoordinates.getVelocity().toArray(), 0, integrationState, 3, 3);
        integrationState[6] = fieldState.getMass();
        System.arraycopy(adjoint, 0, integrationState, 7, adjointDimension);
        return integrationState;
    }

    /**
     * Form Orekit object from array.
     * @param date date
     * @param integrationState propagation state as array
     * @return Orekit state
     */
    private FieldSpacecraftState<Gradient> formatFromArray(final AbsoluteDate date, final Gradient[] integrationState) {
        final Gradient[] terminalCartesian = Arrays.copyOfRange(integrationState, 0, 6);
        final Gradient[] terminalAdjoint = Arrays.copyOfRange(integrationState, 7, integrationState.length);
        final Field<Gradient> field = terminalCartesian[0].getField();
        return createFieldState(new FieldAbsoluteDate<>(field, date), terminalCartesian, integrationState[6],
                terminalAdjoint);
    }

    /**
     * Iterate over field switch detectors and find the one that was triggered in the non-Field propagation.
     * Then, use it to update the gradient.
     * @param date date
     * @param integrationState integration variables
     * @param referenceDate date at start of propagation
     * @param switchDetector switch detector
     * @param dynamicsProvider adjoint dynamics provider
     * @return update integration state
     */
    private Gradient[] findSwitchEventAndUpdateState(final AbsoluteDate date, final Gradient[] integrationState,
                                                     final AbsoluteDate referenceDate,
                                                     final ControlSwitchDetector switchDetector,
                                                     final AdjointDynamicsProvider dynamicsProvider) {
        final FieldSpacecraftState<Gradient> fieldState = formatFromArray(date, integrationState);
        final double expectedG = switchDetector.g(fieldState.toSpacecraftState());
        final int shootingVariables = dynamicsProvider.getDimension();
        final int increasedVariables = shootingVariables + 1;
        final GradientField increasedVariablesField = GradientField.getField(increasedVariables);
        final FieldCartesianAdjointDerivativesProvider<Gradient> fieldDerivativesProvider =
                (FieldCartesianAdjointDerivativesProvider<Gradient>) dynamicsProvider.buildFieldAdditionalDerivativesProvider(increasedVariablesField);
        final List<FieldEventDetector<Gradient>> fieldEventDetectors = fieldDerivativesProvider.getCost()
                .getFieldEventDetectors(increasedVariablesField).collect(Collectors.toList());
        for (final FieldEventDetector<Gradient> fieldEventDetector : fieldEventDetectors) {
            if (fieldEventDetector instanceof FieldControlSwitchDetector) {
                final double actualG = fieldEventDetector.g(fieldState).getReal();
                if (FastMath.abs(actualG - expectedG) < 1e-10) {
                    return updateStateWithTaylorMapInversion(date, integrationState, referenceDate, fieldEventDetector);
                }
            }
        }
        return integrationState;
    }

    /**
     * Update the differential information by taking into account that a state-dependent event was triggered.
     * @param date date
     * @param integrationState integration variables
     * @param initialDate date at start of propagation
     * @param fieldDetector switch detector
     * @return updated integration state
     */
    private Gradient[] updateStateWithTaylorMapInversion(final AbsoluteDate date, final Gradient[] integrationState,
                                                         final AbsoluteDate initialDate,
                                                         final FieldEventDetector<Gradient> fieldDetector) {
        // form array with increased gradient size
        final Gradient threshold = fieldDetector.getThreshold();
        final int increasedVariables = threshold.getFreeParameters();
        final GradientField increasedVariablesField = GradientField.getField(increasedVariables);
        final Gradient[] increasedVariablesArray = MathArrays.buildArray(increasedVariablesField,
                integrationState.length);
        for (int i = 0; i < integrationState.length; i++) {
            increasedVariablesArray[i] = integrationState[i].stackVariable();
        }
        // compute rates at switch
        final GradientField field = increasedVariablesArray[0].getField();
        final FieldOrdinaryDifferentialEquation<Gradient> fieldODE = buildFieldODE(new FieldAbsoluteDate<>(field, initialDate));
        final double duration = date.durationFrom(initialDate);
        final Gradient fieldDuration = field.getZero().newInstance(duration);
        final Gradient[] derivatives = fieldODE.computeDerivatives(fieldDuration, increasedVariablesArray);
        // compute differences in rates
        final double[] deltaRates = computeDeltaRates(date, increasedVariablesArray,
                fieldDetector.getThreshold().getValue(), initialDate, fieldODE, derivatives);
        // evaluate event function in Taylor algebra
        final double[] derivativesValues = new double[derivatives.length];
        for (int i = 0; i < derivativesValues.length; i++) {
            derivativesValues[i] = derivatives[i].getValue();
        }
        final Gradient g = evaluateG(date, increasedVariablesArray, initialDate, fieldDetector, derivativesValues);
        // perform last step involving variables swap
        return invertTaylorMap(increasedVariablesArray, deltaRates, g);
    }

    /**
     * Form differences in rates before and after switch.
     * @param date epoch
     * @param integrationState state variables
     * @param threshold event detection threshold
     * @param initialDate initial date
     * @param fieldODE ODE model
     * @param derivatives rates at switch
     * @return rates difference
     */
    private double[] computeDeltaRates(final AbsoluteDate date, final Gradient[] integrationState,
                                       final double threshold, final AbsoluteDate initialDate,
                                       final FieldOrdinaryDifferentialEquation<Gradient> fieldODE,
                                       final Gradient[] derivatives) {
        final double duration = date.durationFrom(initialDate);
        final Gradient fieldDuration = integrationState[0].getField().getZero().newInstance(duration);
        final double tinyStep = threshold * 2.;
        final boolean isForward = date.isAfter(initialDate);
        final double timeShift = isForward ? tinyStep : -tinyStep;
        final Gradient[] derivativesBefore = shiftAndComputeDerivatives(fieldDuration, integrationState, derivatives,
                fieldODE, -timeShift);
        final Gradient[] derivativesAfter = shiftAndComputeDerivatives(fieldDuration, integrationState, derivatives,
                fieldODE, timeShift);
        final double[] deltaRates = new double[integrationState.length];
        for (int i = 0; i < integrationState.length; i++) {
            deltaRates[i] = derivativesBefore[i].getValue() - derivativesAfter[i].getValue();
        }
        return deltaRates;
    }

    /**
     * Shift state variables in time and compute rates.
     * @param fieldDuration duration
     * @param integrationState state variables
     * @param derivatives rates before shift
     * @param fieldODE ODE model
     * @param timeShift shift
     * @return rates
     */
    private Gradient[] shiftAndComputeDerivatives(final Gradient fieldDuration, final Gradient[] integrationState,
                                                  final Gradient[] derivatives,
                                                  final FieldOrdinaryDifferentialEquation<Gradient> fieldODE,
                                                  final double timeShift) {
        final Gradient[] state = integrationState.clone();
        for (int i = 0; i < state.length; i++) {
            state[i] = state[i].add(derivatives[i].multiply(timeShift));
        }
        return fieldODE.computeDerivatives(fieldDuration, state);
    }

    /**
     * Evaluate event function in proper Taylor algebra.
     * @param date date
     * @param increasedVariablesArray integration variables with increased gradient size
     * @param initialDate date at start of propagation
     * @param fieldDetector switch detector
     * @param derivatives rates
     * @return g
     */
    private Gradient evaluateG(final AbsoluteDate date, final Gradient[] increasedVariablesArray,
                               final AbsoluteDate initialDate, final FieldEventDetector<Gradient> fieldDetector,
                               final double[] derivatives) {
        final Field<Gradient> field = increasedVariablesArray[0].getField();
        final int increasedVariables = field.getZero().getFreeParameters();
        final Gradient lastVariable = Gradient.variable(increasedVariables, increasedVariables - 1, 1);
        final boolean isForward = date.isAfterOrEqualTo(initialDate);
        final Gradient dt = isForward ? lastVariable : lastVariable.negate();
        final Gradient[] shiftedVariables = increasedVariablesArray.clone();
        for (int i = 0; i < shiftedVariables.length; i++) {
            shiftedVariables[i] = shiftedVariables[i].add(dt.multiply(derivatives[i]));
        }
        final FieldSpacecraftState<Gradient> fieldState = formatFromArray(date, shiftedVariables);
        return fieldDetector.g(fieldState);
    }

    /**
     * Invert so-called Taylor map to make the event function value an independent variable.
     * Then nullify its variation to keep only the derivatives of interest.
     * @param state integration variables with dt as last gradient variable
     * @param deltaRates differences in rates from dynamics switch
     * @param g event function evaluated with dt as last gradient variable
     * @return update integration variables
     */
    private static Gradient[] invertTaylorMap(final Gradient[] state, final double[] deltaRates, final Gradient g) {
        // swap dt and g as variables of algebra
        final int increasedGradientDimension = g.getFreeParameters();
        final RealMatrix rightMatrix = MatrixUtils.createRealIdentityMatrix(increasedGradientDimension);
        rightMatrix.setRow(rightMatrix.getRowDimension() - 1, g.getGradient());
        final LUDecomposition luDecomposition = new LUDecomposition(rightMatrix, 0.);
        final RealMatrix inverted = luDecomposition.getSolver().getInverse();
        final double[][] leftMatrixCoefficients = new double[state.length][];
        for (int i = 0; i < state.length; i++) {
            leftMatrixCoefficients[i] = state[i].getGradient();
            leftMatrixCoefficients[i][leftMatrixCoefficients[i].length - 1] = deltaRates[i];
        }
        final RealMatrix product = MatrixUtils.createRealMatrix(leftMatrixCoefficients).multiply(inverted);
        // pack into array with original gradient dimension
        final int gradientDimension = increasedGradientDimension - 1;
        final GradientField field = GradientField.getField(gradientDimension);
        final Gradient[] outputState = MathArrays.buildArray(field, state.length);
        for (int i = 0; i < outputState.length; i++) {
            final double[] gradient = new double[gradientDimension];
            System.arraycopy(product.getRow(i), 0, gradient, 0, gradientDimension);
            outputState[i] = new Gradient(state[i].getValue(), gradient);
        }
        return outputState;
    }

}
