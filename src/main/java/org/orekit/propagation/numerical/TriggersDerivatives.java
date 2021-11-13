/* Copyright 2002-2021 CS GROUP
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.forces.ForceModel;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.trigger.AbstractManeuverTriggers;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggers;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggersResetter;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.ParameterDrivenDateIntervalDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DateDriver;

/** Helper for computing the partial derivatives with respect to maneuvers triggers dates.
 * @author Luc Maisonobe
 * @since 11.1
 */
class TriggersDerivatives {

    /** Name of the main partial derivatives state. */
    private final String pdeName;

    /** Computers for triggers derivatives. */
    private Map<Integer, Computer> triggers;

    /** Simple constructor.
     * @param pdeName name of the main partial derivatives state
     * @param map parameters to columns numbers map
     * @param forceModels force models used by propagation
     */
    TriggersDerivatives(final String pdeName, final Map<String, Integer> map, final List<ForceModel> forceModels) {

        this.pdeName = pdeName;

        this.triggers = new HashMap<>();

        // single-out the very specific drivers used by maneuvers trigger dates
        forceModels.
            stream().
            filter(fm -> fm instanceof Maneuver).
            map(fm -> (Maneuver) fm).
            forEach(m -> manageManeuver(m, map));

    }

    /** Manage a maneuver.
     * @param maneuver maneuver force model
     * @param map parameters to columns numbers map
     */
    private void manageManeuver(final Maneuver maneuver, final Map<String, Integer> map) {
        final ManeuverTriggers maneuverTriggers = maneuver.getManeuverTriggers();
        if (maneuverTriggers instanceof AbstractManeuverTriggers) {

            // FIXME: when issue https://gitlab.orekit.org/orekit/orekit/-/issues/854 is solved
            // the previous if statement and the following cast should be removed as the following
            // code should really be done for all ManeuverTriggers and not only AbstractManeuverTriggers
            final AbstractManeuverTriggers amt = (AbstractManeuverTriggers) maneuverTriggers;

            amt.getEventsDetectors().
                filter(d -> d instanceof ParameterDrivenDateIntervalDetector).
                map (d -> (ParameterDrivenDateIntervalDetector) d).
                forEach(d -> {
                    manageDateDriver(maneuver, amt, d.getStartDriver(), map, d.getThreshold());
                    manageDateDriver(maneuver, amt, d.getStopDriver(),  map, d.getThreshold());
                });

        }
    }

    /** Manage a date driver.
     * @param maneuver maneuver force model
     * @param amt trigger to which the driver is bound
     * @param driver date driver
     * @param map parameters to columns numbers map
     * @param threshold event detector threshold
     */
    private void manageDateDriver(final Maneuver maneuver, final AbstractManeuverTriggers amt,
                                  final DateDriver driver, final Map<String, Integer> map,
                                  final double threshold) {
        if (driver.isSelected()) {
            final int      column   = map.get(driver.getName());
            final Computer computer = new Computer(maneuver, column, threshold);
            amt.addResetter(computer);
            triggers.put(column, computer);
        }
    }

    /** Check if a Jacobian column is managed by this helper.
     * @param column column to check
     * @return true if the column is managed by this helper
     */
    public boolean isManagedColumn(final int column) {
        return triggers.containsKey(column);
    }

    /** Compute partial derivatives with respect to a trigger parameter.
     * @param column Jacobian column
     * @param pDot flat array containing derivatives of the Jacobian matrices
     * (STM first then Jacobian with respect to parameters)
     */
    public void updateDerivatives(final int column, final double[] pDot) {
        triggers.get(column).updateDerivatives(pDot);
    }

    /** Computer for maneuvers triggers derivatives.
     * <p>
     * Let (t₀, y₀) be the state at propagation start, (t₁, y₁) be the state at maneuver trigger time
     * and (tₜ, yₜ) be the state at any arbitrary time during propagation, and fₘ(tₜ, yₜ) be the
     * contribution of the maneuver to the global ODE dy/dt = f(t, y).
     * </p>
     * <p>
     * We are interested in the Jacobian column ∂yₜ/∂t₁. We compute it as a part of an extended ODE,
     * so we need some initial state and a time derivative.
     * </p>
     * <p>
     * Before trigger time t₁ (according to propagation direction), ∂yₜ/∂t₁ = 0 as we have not yet
     * reached t₁. After trigger time t₁, ∂yₜ/∂t₁ = ± ∂yₜ/∂y₁ fₘ(t₁, y₁), where the sign depends on
     * t₁ being a start or stop trigger.
     * </p>
     * <p>
     * At trigger time t₁, we must reset the Jacobian columns to ∂y₁/∂t₁ = ± Id fₘ(t₁, y₁).
     * As maneuver trigger is a discrete event, this reset corresponds to a discontinuity in
     * the Jacobian colums. We also store the maneuver contribution fₘ(t₁, y₁) as we will need
     * it throughout the remaining propagation.
     * </p>
     * <p>
     * After trigger time t₁, we use the time derivative d(∂yₜ/∂t₁)/dt = ± d(∂yₜ/∂y₁)/dt fₘ(t₁, y₁),
     * which means that we just have to multiply the derivative of the current state transition
     * matrix at time tₜ with the maneuver contribution fₘ(t₁, y₁) at time t₁. It may seem strange
     * that the two factors are not taken at the same time, but this is correct.
     * </p>
     */
    private class Computer implements ManeuverTriggersResetter {

        /** Maneuver force model. */
        private final Maneuver maneuver;

        /** Index of the column in the Jacobian with respect to parameters. */
        private final int column;

        /** Event detector threshold. */
        private final double threshold;

        /** Indicator for trigger. */
        private boolean triggered;

        /** Start/stop indicator. */
        private boolean isStart;

        /** Contribution of maneuver at trigger time ±fₘ(t₁, y₁). */
        private Vector3D sfm;

        /** Simple constructor.
         * @param maneuver maneuver force model
         * @param column index of the column in the Jacobian with respect to parameters
         * @param threshold event detector threshold
         */
        Computer(final Maneuver maneuver, final int column, final double threshold) {
            this.maneuver  = maneuver;
            this.column    = column;
            this.threshold = threshold;
        }

        /** Update Jacobian derivatives.
         * @param pDot flat array containing derivatives of the Jacobian matrices
         * (STM first then Jacobian with respect to parameters)
         */
        private void updateDerivatives(final double[] pDot) {
            final int    stateDim  = 6;
            final int    columnTop = stateDim * stateDim + column;
            if (triggered) {
                // compute d(∂yₜ/∂t₁)/dt = ± d(∂yₜ/∂y₁)/dt fₘ(t₁, y₁)
                pDot[columnTop    ] = pDot[ 3] * sfm.getX() + pDot[ 4] * sfm.getY() +  pDot[ 5] * sfm.getZ();
                pDot[columnTop + 1] = pDot[ 9] * sfm.getX() + pDot[10] * sfm.getY() +  pDot[11] * sfm.getZ();
                pDot[columnTop + 2] = pDot[15] * sfm.getX() + pDot[16] * sfm.getY() +  pDot[17] * sfm.getZ();
                pDot[columnTop + 3] = pDot[21] * sfm.getX() + pDot[22] * sfm.getY() +  pDot[23] * sfm.getZ();
                pDot[columnTop + 4] = pDot[27] * sfm.getX() + pDot[28] * sfm.getY() +  pDot[29] * sfm.getZ();
                pDot[columnTop + 5] = pDot[33] * sfm.getX() + pDot[34] * sfm.getY() +  pDot[35] * sfm.getZ();
            } else {
                // before trigger time, derivatives are identically 0
                Arrays.fill(pDot, columnTop, columnTop + stateDim, 0.0);
            }
        }

        /** {@inheritDoc}*/
        @Override
        public void init(final SpacecraftState initialState, final AbsoluteDate target) {
            triggered = false;
        }

        /** {@inheritDoc}*/
        @Override
        public void maneuverTriggered(final SpacecraftState state, final boolean start) {
            isStart = start;
        }

        /** {@inheritDoc}*/
        @Override
        public SpacecraftState resetState(final SpacecraftState state) {

            // get the partial derivatives state
            final double[] p = state.getAdditionalState(pdeName);

            // get the acceleration near trigger time
            final SpacecraftState stateWhenFiring = state.shiftedBy((isStart ? +2 : -2) * threshold);
            final Vector3D        acceleration    = maneuver.acceleration(stateWhenFiring, maneuver.getParameters());
            sfm = isStart ? acceleration.negate() : acceleration;

            // initialize derivatives computation
            final int    stateDim  = 6;
            final int    columnTop = stateDim * stateDim + column;
            p[columnTop    ] = 0.0;
            p[columnTop + 1] = 0.0;
            p[columnTop + 2] = 0.0;
            p[columnTop + 3] = sfm.getX();
            p[columnTop + 4] = sfm.getY();
            p[columnTop + 5] = sfm.getZ();

            triggered = true;

            // return updated state, with initial value for Jacobian
            return state.addAdditionalState(pdeName, p);

        }

    }

}

