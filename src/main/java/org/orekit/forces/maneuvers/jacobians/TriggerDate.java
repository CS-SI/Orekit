/* Copyright 2002-2023 CS GROUP
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
package org.orekit.forces.maneuvers.jacobians;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggersResetter;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/** Generator for one column of a Jacobian matrix for special case of trigger dates.
 * <p>
 * Typical use cases for this are estimation of maneuver start and stop date during
 * either orbit determination or maneuver optimization.
 * </p>
 * <p>
 * Let \((t_0, y_0)\) be the state at propagation start, \((t_1, y_1)\) be the state at
 * maneuver trigger time, \((t_t, y_t)\) be the state at any arbitrary time \(t\) during
 * propagation, and \(f_m(t, y)\) be the contribution of the maneuver to the global
 * ODE \(\frac{dy}{dt} = f(t, y)\). We are interested in the Jacobian column
 * \(\frac{\partial y_t}{\partial t_1}\).
 * </p>
 * <p>
 * There are two parts in this Jacobian: the primary part corresponds to the full contribution
 * of the acceleration due to the maneuver as it is delayed by a small amount \(dt_1\), whereas
 * the secondary part corresponds to change of acceleration after maneuver start as the mass
 * depletion is delayed and therefore the spacecraft mass is different from the mass for nominal
 * start time.
 * </p>
 * <p>
 * The primary part is computed as follows. After trigger time \(t_1\) (according to propagation direction),
 * \[\frac{\partial y_t}{\partial t_1} = \pm \frac{\partial y_t}{\partial y_1} f_m(t_1, y_1)\]
 * where the sign depends on \(t_1\) being a start or stop trigger and propagation being forward
 * or backward.
 * </p>
 * <p>
 * We don't have \(\frac{\partial y_t}{\partial y_1}\) available if \(t_1 \neq t_0\), but we
 * have \(\frac{\partial y_t}{\partial y_0}\) at any time since it can be computed by integrating
 * variational equations for numerical propagation or by other closed form expressions for analytical
 * propagators. We use the classical composition rule to recover the state transition matrix with
 * respect to intermediate time \(t_1\):
 * \[\frac{\partial y_t}{\partial y_0} = \frac{\partial y_t}{\partial y_1} \frac{\partial y_1}{\partial y_0}\]
 * We deduce
 * \[\frac{\partial y_t}{\partial y_1} = \frac{\partial y_t}{\partial y_0} \left(\frac{\partial y_1}{\partial y_0}\right)^{-1}\]
 * </p>
 * <p>
 * The contribution of the primary part to the Jacobian column can therefore be computed using the following
 * closed-form expression:
 * \[\frac{\partial y_t}{\partial t_1}
 * = \pm \frac{\partial y_t}{\partial y_0} \left(\frac{\partial y_1}{\partial y_0}\right)^{-1} f_m(t_1, y_1)
 * = \frac{\partial y_t}{\partial y_0} c_1\]
 * where \(c_1\) is the signed contribution of maneuver at \(t_1\) and is computed at trigger time
 * by solving \(\frac{\partial y_1}{\partial y_0} c_1 = \pm f_m(t_1, y_1)\).
 * </p>
 * <p>
 * As the primary part of the column is generated using a closed-form expression, this generator
 * implements the {@link AdditionalStateProvider} interface and stores the column directly
 * in the primary state during propagation.
 * </p>
 * <p>
 * As the closed-form expression requires picking \(c_1\) at trigger time \(t_1\), it works only
 * if propagation starts outside of the maneuver and passes over \(t_1\) during integration.
 * </p>
 * <p>
 * The secondary part is computed as follows. We have acceleration \(\vec{\Gamma} = \frac{\vec{F}}{m}\) and
 * \(m = m_0 - q (t - t_s)\), where \(m\) is current mass, \(m_0\) is initial mass and \(t_s\) is
 * maneuver trigger time. A delay \(dt_s\) on trigger time induces delaying mass depletion.
 * We get:
 * \[d\vec{\Gamma} = \frac{-\vec{F}}{m^2} dm = \frac{-\vec{F}}{m^2} q dt_s = -\vec{\Gamma}\frac{q}{m} dt_s\]
 * From this total differential, we extract the partial derivative of the acceleration
 * \[\frac{\partial\vec{\Gamma}}{\partial t_s} = -\vec{\Gamma}\frac{q}{m}\]
 * </p>
 * <p>
 * The contribution of the secondary part to the Jacobian column can therefore be computed by integrating
 * the partial derivative of the acceleration, to get the partial derivative of the position.
 * </p>
 * <p>
 * As the secondary part of the column is generated using a differential equation, a separate
 * underlying generator implementing the {@link AdditionalDerivativesProvider} interface is set up to
 * perform the integration during propagation.
 * </p>
 * <p>
 * This generator takes care to sum up the primary and secondary parts so the full column of the Jacobian
 * is computed.
 * </p>
 * <p>
 * The implementation takes care to <em>not</em> resetting \(c_1\) at propagation start.
 * This allows to get proper Jacobian if we interrupt propagation in the middle of a maneuver
 * and restart propagation where it left.
 * </p>
 * @author Luc Maisonobe
 * @since 11.1
 * @see MedianDate
 * @see Duration
 */
public class TriggerDate
    implements AdditionalStateProvider, ManeuverTriggersResetter {

    /** Dimension of the state. */
    private static final int STATE_DIMENSION = 6;

    /** Threshold for decomposing state transition matrix at trigger time. */
    private static final double DECOMPOSITION_THRESHOLD = 1.0e-10;

    /** Name of the state for State Transition Matrix. */
    private final String stmName;

    /** Name of the parameter corresponding to the column. */
    private final String triggerName;

    /** Mass depletion effect. */
    private final MassDepletionDelay massDepletionDelay;

    /** Start/stop management flag. */
    private final boolean manageStart;

    /** Maneuver force model. */
    private final Maneuver maneuver;

    /** Event detector threshold. */
    private final double threshold;

    /** Signed contribution of maneuver at trigger time ±(∂y₁/∂y₀)⁻¹ fₘ(t₁, y₁). */
    private TimeSpanMap<double[]> contribution;

    /** Trigger date. */
    private AbsoluteDate trigger;

    /** Indicator for forward propagation. */
    private boolean forward;

    /** Simple constructor.
     * @param stmName name of State Transition Matrix state
     * @param triggerName name of the parameter corresponding to the trigger date column
     * @param manageStart if true, we compute derivatives with respect to maneuver start
     * @param maneuver maneuver force model
     * @param threshold event detector threshold
     */
    public TriggerDate(final String stmName, final String triggerName, final boolean manageStart,
                       final Maneuver maneuver, final double threshold) {
        this.stmName            = stmName;
        this.triggerName        = triggerName;
        this.massDepletionDelay = new MassDepletionDelay(triggerName, manageStart, maneuver);
        this.manageStart        = manageStart;
        this.maneuver           = maneuver;
        this.threshold          = threshold;
        this.contribution       = null;
        this.trigger            = null;
        this.forward            = true;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return triggerName;
    }

    /** {@inheritDoc}
     * <p>
     * The column state can be computed only if the State Transition Matrix state is available.
     * </p>
     */
    @Override
    public boolean yields(final SpacecraftState state) {
        return !(state.hasAdditionalState(stmName) && state.hasAdditionalState(massDepletionDelay.getName()));
    }

    /** Get the mass depletion effect processor.
     * @return mass depletion effect processor
     */
    public MassDepletionDelay getMassDepletionDelay() {
        return massDepletionDelay;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {

        // note that we reset contribution or triggered ONLY at start or if we change
        // propagation direction
        // this allows to get proper Jacobian if we interrupt propagation
        // in the middle of a maneuver and restart propagation where it left
        final boolean newForward = target.isAfterOrEqualTo(initialState);
        if (contribution == null || (forward ^ newForward)) {
            contribution = new TimeSpanMap<>(null);
            trigger      = null;
        }

        forward = newForward;

    }

    /** {@inheritDoc} */
    @Override
    public double[] getAdditionalState(final SpacecraftState state) {
        // we check contribution rather than triggered because this method
        // is called after maneuverTriggered and before resetState,
        // when preparing the old state to be reset
        final double[] c = contribution == null ? null : contribution.get(state.getDate());
        if (c == null) {
            // no thrust, no effect
            return new double[STATE_DIMENSION];
        } else {

            // primary effect: full maneuver contribution at (delayed) trigger date
            final double[] effect = getStm(state).operate(c);

            // secondary effect: maneuver change throughout thrust as mass depletion is delayed
            final double[] secondary = state.getAdditionalState(massDepletionDelay.getName());

            // sum up both effects
            for (int i = 0; i < effect.length; ++i) {
                effect[i] += secondary[i];
            }

            return effect;

        }
    }

    /** {@inheritDoc}*/
    @Override
    public void maneuverTriggered(final SpacecraftState state, final boolean start) {
        trigger = (start == manageStart) ? state.getDate() : null;
    }

    /** {@inheritDoc}*/
    @Override
    public SpacecraftState resetState(final SpacecraftState state) {

        if (trigger == null) {
            // this is not the maneuver trigger we expected (start vs. stop)
            return state;
        }

        // get the acceleration near trigger time
        final SpacecraftState stateWhenFiring = state.shiftedBy((manageStart ? 2 : -2) * threshold);
        final Vector3D        acceleration    = maneuver.acceleration(stateWhenFiring, maneuver.getParameters(state.getDate()));

        // initialize derivatives computation
        final double     sign = (forward == manageStart) ? -1 : +1;
        final RealVector rhs  = MatrixUtils.createRealVector(STATE_DIMENSION);
        rhs.setEntry(3, sign * acceleration.getX());
        rhs.setEntry(4, sign * acceleration.getY());
        rhs.setEntry(5, sign * acceleration.getZ());

        // get State Transition Matrix with respect to Cartesian parameters at trigger time
        final RealMatrix dY1dY0 = getStm(state);

        // store contribution factor for derivatives scm = ±(∂y₁/∂y₀)⁻¹ fₘ(t₁, y₁)
        final double[] c = new QRDecomposition(dY1dY0, DECOMPOSITION_THRESHOLD).getSolver().solve(rhs).toArray();
        if (forward) {
            contribution.addValidAfter(c, state.getDate(), false);
        } else {
            contribution.addValidBefore(c, state.getDate(), false);
        }

        // return unchanged state
        return state;

    }

    /** Extract State Transition Matrix with respect to Cartesian parameters.
     * @param state state containing the State Transition Matrix
     * @return State Transition Matrix
     */
    private RealMatrix getStm(final SpacecraftState state) {
        final double[] p = state.getAdditionalState(stmName);
        final RealMatrix dYdY0 = MatrixUtils.createRealMatrix(STATE_DIMENSION, STATE_DIMENSION);
        int index = 0;
        for (int i = 0; i < STATE_DIMENSION; ++i) {
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                dYdY0.setEntry(i, j, p[index++]);
            }
        }
        return dYdY0;
    }

}

