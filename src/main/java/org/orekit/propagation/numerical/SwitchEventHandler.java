/* Copyright 2022-2026 Romain Serra
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
package org.orekit.propagation.numerical;


import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.DataDictionary;
import org.orekit.utils.DerivativeStateUtils;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Arrays;
import java.util.List;

/**
 * Class handling the State Transition Matrix update in the presence of dynamics discontinuities.
 * Reference: Eq. (29-30) in Russell, R. P., “Primer Vector Theory Applied to Global Low-Thrust Trade Studies,”
 * Journal of Guidance, Control, and Dynamics, Vol. 30, No. 2, 2007, pp. 460-472.
 *
 * @author Romain Serra
 * @since 13.1
 * @see NumericalPropagator
 */
class SwitchEventHandler implements EventHandler {

    /** Original event handler to wrap. */
    private final EventHandler wrappedHandler;

    /** Matrices harvester. */
    private final NumericalPropagationHarvester matricesHarvester;

    /** Differential equations. */
    private final NumericalTimeDerivativesEquations timeDerivativesEquations;

    /** Attitude provider. */
    private final AttitudeProvider attitudeProvider;

    /** Event detectors in Taylor differential algebra. */
    private final List<FieldEventDetector<Gradient>> fieldDetectors;

    /** Flag on propagation direction. */
    private boolean isForward;

    /** Field version of detector defining dynamics switch. */
    private FieldEventDetector<Gradient> switchFieldDetector;

    /**
     * Constructor.
     * @param wrappedHandler original event handler
     * @param matricesHarvester matrices harvester
     * @param timeDerivativesEquations differential equations
     * @param attitudeProvider attitude provider
     * @param fieldDetectors non-empty event detectors in Taylor differential algebra
     */
    SwitchEventHandler(final EventHandler wrappedHandler, final NumericalPropagationHarvester matricesHarvester,
                       final NumericalTimeDerivativesEquations timeDerivativesEquations,
                       final AttitudeProvider attitudeProvider, final List<FieldEventDetector<Gradient>> fieldDetectors) {
        this.wrappedHandler = wrappedHandler;
        this.matricesHarvester = matricesHarvester;
        this.timeDerivativesEquations = timeDerivativesEquations;
        this.attitudeProvider = attitudeProvider;
        this.fieldDetectors = fieldDetectors;
    }

    @Override
    public SpacecraftState resetState(final EventDetector detector, final SpacecraftState oldState) {
        if (switchFieldDetector == null) {
            // failed to find corresponding Field detector
            return wrappedHandler.resetState(detector, oldState);
        } else {
            final SpacecraftState newState = updateState(oldState);
            switchFieldDetector = null;
            return newState;
        }
    }

    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target, final EventDetector detector) {
        isForward = initialState.getDate().isBeforeOrEqualTo(target);
        switchFieldDetector = null;
        final GradientField field = fieldDetectors.get(0).getThreshold().getField();
        final FieldAbsoluteDate<Gradient> fieldTarget = new FieldAbsoluteDate<>(field, target);
        final FieldSpacecraftState<Gradient> fieldState = new FieldSpacecraftState<>(field, initialState);
        fieldDetectors.forEach(fieldEventDetector -> fieldEventDetector.init(fieldState, fieldTarget));
        EventHandler.super.init(initialState, target, detector);
    }

    @Override
    public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {
        final Action action = wrappedHandler.eventOccurred(s, detector, increasing);
        if (action == Action.RESET_DERIVATIVES) {
            switchFieldDetector = findSwitchDetector(detector, s);
            if (switchFieldDetector != null) {
                return Action.RESET_STATE;
            }
        }
        return action;
    }

    /**
     * Method finding the Field detector corresponding to a non-Field, triggered one.
     * @param detector triggered event detector
     * @param state state
     * @return Field detector
     */
    private FieldEventDetector<Gradient> findSwitchDetector(final EventDetector detector, final SpacecraftState state) {
        final GradientField field = fieldDetectors.get(0).getThreshold().getField();
        final FieldSpacecraftState<Gradient> fieldState = new FieldSpacecraftState<>(field, state);
        fieldDetectors.forEach(fieldDetector -> fieldDetector.reset(fieldState, fieldState.getDate()));
        final double g = detector.g(state);
        return findSwitchDetector(g, fieldState);
    }

    /**
     * Method finding the Field detector corresponding to a non-Field, triggered one.
     * @param g value of triggered detector
     * @param fieldState Field state
     * @return Field detector
     */
    private FieldEventDetector<Gradient> findSwitchDetector(final double g,
                                                            final FieldSpacecraftState<Gradient> fieldState) {
        for (final FieldEventDetector<Gradient> fieldDetector : fieldDetectors) {
            final Gradient fieldG = fieldDetector.g(fieldState);
            if (FastMath.abs(fieldG.getValue() - g) < 1e-11) {
                return fieldDetector;
            }
        }
        return null;
    }

    /**
     * Update state, more precisely the additional data corresponding to the STM.
     * @param stateAtSwitch original state
     * @return updated state
     */
    private SpacecraftState updateState(final SpacecraftState stateAtSwitch) {
        final RealMatrix cartesianFactorMatrix = computeUpdateMatrix(stateAtSwitch);
        // update STM
        final String stmName = matricesHarvester.getStmName();
        final RealMatrix oldStm = matricesHarvester.toSquareMatrix(stateAtSwitch.getAdditionalState(stmName));
        final RealMatrix stm = cartesianFactorMatrix.multiply(oldStm).add(oldStm);
        final DataDictionary additionalData = new DataDictionary(stateAtSwitch.getAdditionalDataValues());
        additionalData.put(stmName, matricesHarvester.toArray(stm.getData()));
        // update model parameters Jacobian if present
        if (!matricesHarvester.getJacobiansColumnsNames().isEmpty()) {
            for (final String parameterName : matricesHarvester.getJacobiansColumnsNames()) {
                final RealVector oldJacobian = MatrixUtils.createRealVector(stateAtSwitch.getAdditionalState(parameterName));
                final RealVector jacobian = cartesianFactorMatrix.operate(oldJacobian).add(oldJacobian);
                additionalData.put(parameterName, jacobian.toArray());
            }
        }
        return stateAtSwitch.withAdditionalData(additionalData);
    }

    /**
     * Compute matrix needed to update STM and the like.
     * @param state state
     * @return matrix
     */
    private RealMatrix computeUpdateMatrix(final SpacecraftState state) {
        final double twoThreshold = switchFieldDetector.getThreshold().getValue() * 2.;
        final double dt = isForward ? -twoThreshold : twoThreshold;
        final SpacecraftState stateBefore = shift(state, dt);
        final double[] derivativesBefore = timeDerivativesEquations.computeTimeDerivatives(stateBefore);
        final SpacecraftState stateAfter = shift(state, -dt);
        final double[] derivativesAfter = timeDerivativesEquations.computeTimeDerivatives(stateAfter);
        final int stateDimension = matricesHarvester.getStateDimension();
        final double[] deltaDerivatives = new double[stateDimension];
        for (int i = 3; i < deltaDerivatives.length; i++) {
            deltaDerivatives[i] = derivativesAfter[i] - derivativesBefore[i];
        }
        final Gradient g = evaluateG(buildCartesianState(state, state.getPVCoordinates()));
        final double gLastDerivative = g.getPartialDerivative(stateDimension);
        final double gDot = isForward ? gLastDerivative : -gLastDerivative;
        final double[] gGradientState = Arrays.copyOfRange(g.getGradient(), 0, stateDimension);
        final RealVector lhs = MatrixUtils.createRealVector(deltaDerivatives);
        final RealVector rhs = MatrixUtils.createRealVector(gGradientState).mapMultiply(1. / gDot);
        return lhs.outerProduct(rhs);
    }

    /**
     * Shift state using first order Taylor expansion for position.
     * @param stateAtSwitch state
     * @param dt time shift
     * @return new state
     */
    private SpacecraftState shift(final SpacecraftState stateAtSwitch, final double dt) {
        final PVCoordinates pvCoordinates = stateAtSwitch.getPVCoordinates();
        final AbsoluteDate shiftedDate = stateAtSwitch.getDate().shiftedBy(dt);
        final TimeStampedPVCoordinates shiftedPV = new TimeStampedPVCoordinates(shiftedDate,
                pvCoordinates.getPosition().add(pvCoordinates.getVelocity().scalarMultiply(dt)), pvCoordinates.getVelocity());
        return buildCartesianState(stateAtSwitch, shiftedPV);
    }

    /**
     * Build state given template and new position-velocity vector.
     * @param templateState template state
     * @param pvCoordinates position-velocity vector
     * @return new state
     */
    private SpacecraftState buildCartesianState(final SpacecraftState templateState,
                                                final TimeStampedPVCoordinates pvCoordinates) {
        final SpacecraftState state;
        final Frame frame = templateState.getFrame();
        if (templateState.isOrbitDefined()) {
            final Orbit templateOrbit = templateState.getOrbit();
            final CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, frame, templateOrbit.getMu());
            final Attitude attitude = attitudeProvider.getAttitude(orbit, pvCoordinates.getDate(), frame);
            state = new SpacecraftState(orbit, attitude);
        } else {
            final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, pvCoordinates);
            final Attitude attitude = attitudeProvider.getAttitude(absolutePVCoordinates,
                    pvCoordinates.getDate(), frame);
            state = new SpacecraftState(absolutePVCoordinates, attitude);
        }
        return state.withMass(templateState.getMass())
                .withAdditionalData(templateState.getAdditionalDataValues())
                .withAdditionalStatesDerivatives(templateState.getAdditionalStatesDerivatives());
    }

    /**
     * Evaluate event function in Taylor algebra.
     * @param state state
     * @return g in Taylor algebra
     */
    private Gradient evaluateG(final SpacecraftState state) {
        final int stateDimension = matricesHarvester.getStateDimension();
        Gradient time = Gradient.variable(stateDimension + 1, stateDimension, 0);
        if (!isForward) {
            time = time.negate();
        }
        final GradientField field = time.getField();
        final FieldSpacecraftState<Gradient> fieldState = DerivativeStateUtils.buildSpacecraftStateGradient(field,
                state, attitudeProvider);
        return switchFieldDetector.g(fieldState.shiftedBy(time));
    }

}

