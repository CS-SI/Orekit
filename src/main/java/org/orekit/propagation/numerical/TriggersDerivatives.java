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
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
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
 * After trigger time t₁ (according to propagation direction), ∂yₜ/∂t₁ = ± ∂yₜ/∂y₁ fₘ(t₁, y₁),
 * where the sign depends on t₁ being a start or stop trigger. This equation is however an already
 * integrated equation, we cannot insert it at the appropriate column in the ODE managed by {@link
 * PartialDerivativesEquations}). We also don't have ∂yₜ/∂y₁ available is t₁≠t₀, we only have ∂yₜ/∂y₀
 * and d(∂yₜ/∂y₀)/dt that are computed by {@link PartialDerivativesEquations}.
 * </p>
 * <p>
 * In order to recover the state transition matrix with respect to intermediate time t₁, we use
 * the classical state transition matrix composition rule: ∂yₜ/∂y₀ = ∂yₜ/∂y₁ ∂y₁/∂y₀. From this
 * rule and its time derivative, we get ∂yₜ/∂y₁ = ∂yₜ/∂y₀ (∂y₁/∂y₀)⁻¹ and
 * d(∂yₜ/∂y₁)/dt = d(∂yₜ/∂y₀)/dt (∂y₁/∂y₀)⁻¹.
 * </p>
 * <p>So the partial
 * derivatives computation as an ordinary differential equation is performed as follows:
 * </p>
 * <dl>
 *   <dt>before trigger time t₁<dt>
 *   <dd>∂yₜ/∂t₁ = 0 and d(∂yₜ/∂t₁)/dt = 0</dd>
 *   <dt>at trigger time t₁</dt>
 *   <dd>we reset the Jacobian columns to ∂y₁/∂t₁ = ± Id fₘ(t₁, y₁). As maneuver trigger is a discrete
 *   event, this reset corresponds to a discontinuity in the Jacobian columns. We also store some
 *   intermediate variables that will be used in the next item.</dd>
 *   <dt>after trigger time t₁</dt>
 *   <dd>we use the time derivative d(∂yₜ/∂t₁)/dt = ± d(∂yₜ/∂y₀)/dt (∂y₁/∂y₀)⁻¹ fₘ(t₁, y₁) =
 *   d(∂yₜ/∂y₀)/dt scm₁, where scm₁ is the signed contribution of maneuver at t₁ and is computed
 *   at trigger time by solving ∂y₁/∂y₀ scm₁ = ±fₘ(t₁, y₁)</dd>
 * </dl>
 * @author Luc Maisonobe
 * @since 11.1
 */
class TriggersDerivatives {

    /** Threshold for decomposing state transition matrix at trigger time. */
    private static final double DECOMPOSITION_THRESHOLD = 1.0e-10;

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
                    manageDateDriver(maneuver, amt, d.getStartDriver(), true,  map, d.getThreshold());
                    manageDateDriver(maneuver, amt, d.getStopDriver(),  false, map, d.getThreshold());
                });

        }
    }

    /** Manage a date driver.
     * @param maneuver maneuver force model
     * @param amt trigger to which the driver is bound
     * @param driver date driver
     * @param start if true, the driver is a maneuver start
     * @param map parameters to columns numbers map
     * @param threshold event detector threshold
     */
    private void manageDateDriver(final Maneuver maneuver, final AbstractManeuverTriggers amt,
                                  final DateDriver driver, final boolean start,
                                  final Map<String, Integer> map, final double threshold) {
        if (driver.isSelected()) {
            final int      column   = map.get(driver.getName());
            final Computer computer = new Computer(start, maneuver, column, threshold);
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

    /** Computer for maneuvers triggers derivatives. */
    private class Computer implements ManeuverTriggersResetter {

        /** Start/stop management flag. */
        private boolean manageStart;

        /** Maneuver force model. */
        private final Maneuver maneuver;

        /** Sign for acceleration. */
        private final double sign;

        /** Index of the column in the Jacobian with respect to parameters. */
        private final int column;

        /** Event detector threshold. */
        private final double threshold;

        /** Signed contribution of maneuver at trigger time ±(∂y₁/∂y₀)⁻¹ fₘ(t₁, y₁). */
        private double[] scm;

        /** Indicator for trigger. */
        private boolean triggered;

        /** Simple constructor.
         * @param manageStart if true, we compute derivatives with respect to maneuver start
         * @param maneuver maneuver force model
         * @param column index of the column in the Jacobian with respect to parameters
         * @param threshold event detector threshold
         */
        Computer(final boolean manageStart, final Maneuver maneuver, final int column, final double threshold) {
            this.manageStart = manageStart;
            this.maneuver    = maneuver;
            this.sign        = manageStart ? -1 : +1;
            this.column      = column;
            this.threshold   = threshold;
        }

        /** Update Jacobian derivatives.
         * @param pDot flat array containing derivatives of the Jacobian matrices
         * (STM first then Jacobian with respect to parameters)
         */
        private void updateDerivatives(final double[] pDot) {
            final int    stateDim  = 6;
            final int    columnTop = stateDim * stateDim + column;
            if (triggered) {
                // compute d(∂yₜ/∂t₁)/dt = ± d(∂yₜ/∂y0)/dt (∂y₁/∂y₀)⁻¹ fₘ(t₁, y₁)
                pDot[columnTop    ] = pDot[ 0] * scm[0] + pDot[ 1] * scm[1] +  pDot[ 2] * scm[2] +
                                      pDot[ 3] * scm[3] + pDot[ 4] * scm[4] +  pDot[ 5] * scm[5];
                pDot[columnTop + 1] = pDot[ 6] * scm[0] + pDot[ 7] * scm[1] +  pDot[ 8] * scm[2] +
                                      pDot[ 9] * scm[3] + pDot[10] * scm[4] +  pDot[11] * scm[5];
                pDot[columnTop + 2] = pDot[12] * scm[0] + pDot[13] * scm[1] +  pDot[14] * scm[2] +
                                      pDot[15] * scm[3] + pDot[16] * scm[4] +  pDot[17] * scm[5];
                pDot[columnTop + 3] = pDot[18] * scm[0] + pDot[19] * scm[1] +  pDot[20] * scm[2] +
                                      pDot[21] * scm[3] + pDot[22] * scm[4] +  pDot[23] * scm[5];
                pDot[columnTop + 4] = pDot[24] * scm[0] + pDot[25] * scm[1] +  pDot[26] * scm[2] +
                                      pDot[27] * scm[3] + pDot[28] * scm[4] +  pDot[29] * scm[5];
                pDot[columnTop + 5] = pDot[30] * scm[0] + pDot[31] * scm[1] +  pDot[32] * scm[2] +
                                      pDot[33] * scm[3] + pDot[34] * scm[4] +  pDot[35] * scm[5];
            } else {
                // before trigger time, derivatives are identically 0
                Arrays.fill(pDot, columnTop, columnTop + stateDim, 0.0);
            }
        }

        /** {@inheritDoc}*/
        @Override
        public void init(final SpacecraftState initialState, final AbsoluteDate target) {
            scm       = null;
            triggered = false;
        }

        /** {@inheritDoc}*/
        @Override
        public void maneuverTriggered(final SpacecraftState state, final boolean start) {
            triggered = start == manageStart;
        }

        /** {@inheritDoc}*/
        @Override
        public SpacecraftState resetState(final SpacecraftState state) {

            if (!triggered) {
                // this is not the maneuver trigger we expected (start vs. stop)
                return state;
            }

            // get complete partial derivatives state
            final double[] p = state.getAdditionalState(pdeName);

            // extract the state transition matrix ∂y₁/∂y₀ at trigger time
            final int stateDim  = 6;
            final RealMatrix dY1dY0 = MatrixUtils.createRealMatrix(stateDim, stateDim);
            int index = 0;
            for (int i = 0; i < stateDim; ++i) {
                for (int j = 0; j < stateDim; ++j) {
                    dY1dY0.setEntry(i, j, p[index++]);
                }
            }

            // get the acceleration near trigger time
            final SpacecraftState stateWhenFiring = state.shiftedBy(-2 * sign * threshold);
            final Vector3D        acceleration    = maneuver.acceleration(stateWhenFiring, maneuver.getParameters());

            // initialize derivatives computation
            final RealVector rhs = MatrixUtils.createRealVector(stateDim);
            rhs.setEntry(3, sign * acceleration.getX());
            rhs.setEntry(4, sign * acceleration.getY());
            rhs.setEntry(5, sign * acceleration.getZ());
            final int columnTop = stateDim * stateDim + column;
            System.arraycopy(rhs.toArray(), 0, p, columnTop, stateDim);

            // store contribution factor for derivatives scm = ±(∂y₁/∂y₀)⁻¹ fₘ(t₁, y₁)
            scm       = new QRDecomposition(dY1dY0, DECOMPOSITION_THRESHOLD).
                            getSolver().
                            solve(rhs).
                            toArray();

            // return updated state, with initial value for Jacobian
            return state.addAdditionalState(pdeName, p);

        }

    }

}

