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

import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.control.indirect.shooting.boundary.CartesianBoundaryConditionChecker;
import org.orekit.control.indirect.shooting.boundary.FixedTimeBoundaryOrbits;
import org.orekit.control.indirect.shooting.boundary.FixedTimeCartesianBoundaryStates;
import org.orekit.control.indirect.shooting.propagation.ShootingPropagationSettings;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Abstract class for indirect single shooting methods with Cartesian coordinates for fixed time fixed boundary.
 * Terminal mass is assumed to be free, thus corresponding adjoint must vanish at terminal time.
 * On the other hand, other terminal adjoint variables are free because the Cartesian state is fixed.
 *
 * @author Romain Serra
 * @since 12.2
 * @see org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider
 * @see org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider
 */
public abstract class AbstractFixedBoundaryCartesianSingleShooting extends AbstractFixedInitialCartesianSingleShooting {

    /** Default value for defects scaling. */
    private static final double DEFAULT_SCALE = 1.;

    /** Terminal Cartesian coordinates. */
    private final TimeStampedPVCoordinates terminalCartesianState;

    /** Condition checker. */
    private final CartesianBoundaryConditionChecker conditionChecker;

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
        super(propagationSettings, buildInitialStateTemplate(boundaryConditions.getInitialCartesianState(),
                propagationSettings));
        this.conditionChecker = conditionChecker;
        this.terminalCartesianState = boundaryConditions.getTerminalCartesianState()
                .getPVCoordinates(propagationSettings.getPropagationFrame());
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
        super(propagationSettings, buildInitialStateTemplate(boundaryConditions.getInitialOrbit(),
                propagationSettings));
        this.conditionChecker = conditionChecker;
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
     * @param propagationSettings propagation settings
     * @return template propagation state
     */
    private static SpacecraftState buildInitialStateTemplate(final Orbit initialOrbit,
                                                             final ShootingPropagationSettings propagationSettings) {
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
     * @param propagationSettings propagation settings
     * @return template propagation state
     */
    private static SpacecraftState buildInitialStateTemplate(final AbsolutePVCoordinates initialCartesianState,
                                                             final ShootingPropagationSettings propagationSettings) {
        final Frame frame = propagationSettings.getPropagationFrame();
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame,
                initialCartesianState.getPVCoordinates(frame));
        final Attitude attitude = propagationSettings.getAttitudeProvider()
                .getAttitude(absolutePVCoordinates, absolutePVCoordinates.getDate(), absolutePVCoordinates.getFrame());
        return new SpacecraftState(absolutePVCoordinates, attitude);
    }

    /** {@inheritDoc} */
    @Override
    public ShootingBoundaryOutput computeCandidateSolution(final SpacecraftState initialState,
                                                           final int iterationCount) {
        final NumericalPropagator propagator = buildPropagator(initialState);
        final SpacecraftState actualTerminalState = propagator.propagate(getTerminalCartesianState().getDate());
        final boolean converged = checkConvergence(actualTerminalState);
        return new ShootingBoundaryOutput(converged, iterationCount, initialState, getPropagationSettings(),
                actualTerminalState);
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

}
