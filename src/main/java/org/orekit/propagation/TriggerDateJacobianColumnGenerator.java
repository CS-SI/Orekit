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
package org.orekit.propagation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggersResetter;
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
 * propagation, and \(f_m(t_t, y_t)\) be the contribution of the maneuver to the global
 * ODE \(\frac{dy}{dt} = f(t, y)\). We are interested in the Jacobian column
 * \(\frac{\partial y_t}{\partial t_1}\).
 * </p>
 * <p>
 * After trigger time \(t_1\) (according to propagation direction),
 * \[\frac{\partial y_t}{\partial t_1} = \pm \frac{\partial y_t}{\partial y_1} f_m(t_1, y_1)\]
 * where the sign depends on \(t_1\) being a start or stop trigger.
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
 * The Jacobian column can therefore be computed using the following closed-form expression:
 * \[\frac{\partial y_t}{\partial t_1}
 * = \pm \frac{\partial y_t}{\partial y_0} \left(\frac{\partial y_1}{\partial y_0}\right)^{-1} f_m(t_1, y_1)
 * = \frac{\partial y_t}{\partial y_0} c_1\]
 * where \(c_1\) is the signed contribution of maneuver at \(t_1\) and is computed at trigger time
 * by solving \(\frac{\partial y_1}{\partial y_0} c_1 = \pm f_m(t_1, y_1)\).
 * </p>
 * <p>
 * As the column is generated using a closed-form expression, this generator implements
 * the {@link AdditionalStateProvider} interface and stores the column directly
 * in the primary state during propagation.
 * </p>
 * <p>
 * The implementation takes care to <em>not</em> resetting anything at propagation start.
 * This allows to get proper Jacobian if we interrupt propagation in the middle of a maneuver
 * and restart propagation where it left.
 * </p>
 * @author Luc Maisonobe
 * @since 11.1
 * @see MedianDateJacobianColumnGenerator
 * @see DurationJacobianColumnGenerator
 */
public class TriggerDateJacobianColumnGenerator
    implements AdditionalStateProvider, ManeuverTriggersResetter {

    /** Dimension of the state. */
    private static final int STATE_DIMENSION = 6;

    /** Threshold for decomposing state transition matrix at trigger time. */
    private static final double DECOMPOSITION_THRESHOLD = 1.0e-10;

    /** Zero state used before maneuver is triggered. */
    private static final double[] ZERO = new double[STATE_DIMENSION];

    /** Name of the state for State Transition Matrix. */
    private final String stmName;

    /** Name of the parameter corresponding to the column. */
    private final String columnName;

    /** Start/stop management flag. */
    private boolean manageStart;

    /** Maneuver force model. */
    private final Maneuver maneuver;

    /** Sign for acceleration. */
    private final double sign;

    /** Event detector threshold. */
    private final double threshold;

    /** Signed contribution of maneuver at trigger time ±(∂y₁/∂y₀)⁻¹ fₘ(t₁, y₁). */
    private TimeSpanMap<double[]> contribution;

    /** Indicator for trigger. */
    private boolean triggered;

    /** Indicator for forward propagation. */
    private boolean forward;

    /** Simple constructor.
     * @param stmName name of State Transition Matrix state
     * @param columnName name of the parameter corresponding to the column
     * @param manageStart if true, we compute derivatives with respect to maneuver start
     * @param maneuver maneuver force model
     * @param threshold event detector threshold
     */
    public TriggerDateJacobianColumnGenerator(final String stmName, final String columnName,
                                              final boolean manageStart, final Maneuver maneuver,
                                              final double threshold) {
        this.stmName      = stmName;
        this.columnName   = columnName;
        this.manageStart  = manageStart;
        this.maneuver     = maneuver;
        this.sign         = manageStart ? -1 : +1;
        this.threshold    = threshold;
        this.contribution = null;
        this.forward      = true;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return columnName;
    }

    /** {@inheritDoc}
     * <p>
     * The column state can be computed only if the State Transition Matrix state is available.
     * </p>
     */
    @Override
    public boolean yield(final SpacecraftState state) {
        return !state.hasAdditionalState(stmName);
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
            triggered    = false;
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
        return c == null ? ZERO : getStm(state).operate(c);
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

        // get the acceleration near trigger time
        final SpacecraftState stateWhenFiring = state.shiftedBy(-2 * sign * threshold);
        final Vector3D        acceleration    = maneuver.acceleration(stateWhenFiring, maneuver.getParameters());

        // initialize derivatives computation
        final RealVector rhs = MatrixUtils.createRealVector(STATE_DIMENSION);
        rhs.setEntry(3, (forward ? sign : -sign) * acceleration.getX());
        rhs.setEntry(4, (forward ? sign : -sign) * acceleration.getY());
        rhs.setEntry(5, (forward ? sign : -sign) * acceleration.getZ());

        // get State Transition Matrix with respect to Cartesian parameters at trigger time
        final RealMatrix dY1dY0 = getStm(state);

        // store contribution factor for derivatives scm = ±(∂y₁/∂y₀)⁻¹ fₘ(t₁, y₁)
        final double[] c = new QRDecomposition(dY1dY0, DECOMPOSITION_THRESHOLD).getSolver().solve(rhs).toArray();
        if (forward) {
            contribution.addValidAfter(c, state.getDate());
        } else {
            contribution.addValidBefore(c, state.getDate());
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

