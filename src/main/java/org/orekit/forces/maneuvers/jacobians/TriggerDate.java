/* Copyright 2002-2025 CS GROUP
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
import org.orekit.forces.ForceModel;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggersResetter;
import org.orekit.propagation.AdditionalDataProvider;
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
 * of the jump in the dynamics due to the maneuver as it is delayed by a small amount \(dt_1\), whereas
 * the secondary part corresponds to change of acceleration after maneuver start as the mass
 * depletion is delayed and therefore the spacecraft mass is different from the mass for nominal
 * start time.
 * This second part is already contained in the first one when the mass is included in the transition matrix
 * (7x7 instead of 6x6).
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
 * implements the {@link AdditionalDataProvider} interface and stores the column directly
 * in the primary state during propagation.
 * </p>
 * <p>
 * As the closed-form expression requires picking \(c_1\) at trigger time \(t_1\), it works only
 * if propagation starts outside of the maneuver and passes over \(t_1\) during integration.
 * </p>
 * <p>
 * The secondary part, if needed (as it is not required if the mass is already included the state transition matrix
 * i.e. when the latter is 7x7), is computed as follows. Let m be the mass and m_s its value at switching time t_s.
 * Let (x,y,z) be the position vector, (vx, vy, vz) the velocity
 * and (ax, ay, az) the total acceleration, we have \(\dot \frac{\partial x} {\partial \partial m_s} = \frac{\partial vx }{\partial m_s}))
 * and similar expressions for y and z. Furthermore, \(\dot \frac{\partial vx}{ \partial \partial m_s} = \frac{\partial ax }{\partial m}
 * . \frac{\partial m }{\partial m_s} \), and symmetric equations for vy and vy. The fact is that \( \frac{\partial m}{ \partial m_s} = 1 \)
 * assuming the mass rate q only depends on time. On the other hand, \( \frac{\partial m_s}{ \partial t_s }= q(t_s) \)/
 * By the chain rule of derivation, one gets the contribution due to the mass depletion delay.
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
public class TriggerDate implements ManeuverTriggersResetter, AdditionalDataProvider<double[]> {

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

    /** State dimension. */
    private final int stateDimension;

    /** Signed contribution of maneuver at trigger time ±(∂y₁/∂y₀)⁻¹ fₘ(t₁, y₁). */
    private TimeSpanMap<double[]> contribution;

    /** Trigger date. */
    private AbsoluteDate trigger;

    /** Indicator for forward propagation. */
    private boolean forward;

    /** Mass rate at trigger (sign depends on propagation direction). Set to zero until the maneuver has actually happened during a propagation. */
    private double signedMassRateAtTrigger = 0.;

    /** Constructor without mass as state variable in transition matrix.
     * @param stmName name of State Transition Matrix state
     * @param triggerName name of the parameter corresponding to the trigger date column
     * @param manageStart if true, we compute derivatives with respect to maneuver start
     * @param maneuver maneuver force model
     * @param threshold event detector threshold
     * @param nonGravitationalForces list of non-gravitational forces, used only if mass is not in STM
     */
    public TriggerDate(final String stmName, final String triggerName, final boolean manageStart,
                       final Maneuver maneuver, final double threshold, final ForceModel... nonGravitationalForces) {
        this(stmName, triggerName, manageStart, maneuver, threshold, false, nonGravitationalForces);
    }

    /** Constructor.
     * @param stmName name of State Transition Matrix state
     * @param triggerName name of the parameter corresponding to the trigger date column
     * @param manageStart if true, we compute derivatives with respect to maneuver start
     * @param maneuver maneuver force model
     * @param threshold event detector threshold
     * @param isMassInStm flag on mass inclusion as state variable in STM
     * @param nonGravitationalForces list of non-gravitational forces, used only if mass is not in STM
     * @since 13.1
     */
    public TriggerDate(final String stmName, final String triggerName, final boolean manageStart,
                       final Maneuver maneuver, final double threshold, final boolean isMassInStm,
                       final ForceModel... nonGravitationalForces) {
        this.stmName            = stmName;
        this.triggerName        = triggerName;
        this.massDepletionDelay = isMassInStm ? null : new MassDepletionDelay(triggerName, manageStart, maneuver, nonGravitationalForces);
        this.manageStart        = manageStart;
        this.maneuver           = maneuver;
        this.threshold          = threshold;
        this.stateDimension     = isMassInStm ? 7 : 6;
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
        if (massDepletionDelay == null) {
            return !state.hasAdditionalData(stmName);
        } else {
            return !(state.hasAdditionalData(stmName) && state.hasAdditionalData(massDepletionDelay.getName()));
        }
    }

    /** Get the mass depletion effect processor. Can be null.
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
            signedMassRateAtTrigger = 0.;
        }

        forward = newForward;

    }

    /** {@inheritDoc} */
    @Override
    public double[] getAdditionalData(final SpacecraftState state) {
        // we check contribution rather than triggered because this method
        // is called after maneuverTriggered and before resetState,
        // when preparing the old state to be reset
        final double[] c = contribution == null ? null : contribution.get(state.getDate());
        if (c == null) {
            // no thrust, no effect
            return new double[stateDimension];
        } else {

            // primary effect: full maneuver contribution at (delayed) trigger date
            final double[] effect = getStm(state).operate(c);

            if (massDepletionDelay != null) {
                // secondary effect: maneuver change throughout thrust as mass depletion is delayed (only needed when mass is not in the STM)
                final double[] secondary = state.getAdditionalState(massDepletionDelay.getName());

                // cumulate both effects
                for (int i = 0; i < effect.length; ++i) {
                    effect[i] += secondary[i] * signedMassRateAtTrigger;
                }
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
        final double[] parameters = maneuver.getParameters(state.getDate());
        final SpacecraftState stateWhenFiring = state.shiftedBy((manageStart ? 2 : -2) * threshold);
        final Vector3D        acceleration    = maneuver.acceleration(stateWhenFiring, parameters);

        // initialize derivatives computation
        final double     sign = (forward == manageStart) ? -1 : +1;
        final RealVector rhs  = MatrixUtils.createRealVector(stateDimension);
        rhs.setEntry(3, sign * acceleration.getX());
        rhs.setEntry(4, sign * acceleration.getY());
        rhs.setEntry(5, sign * acceleration.getZ());
        signedMassRateAtTrigger = sign * maneuver.getPropulsionModel().getMassDerivatives(state, parameters);
        if (stateDimension == 7) {
            rhs.setEntry(6, signedMassRateAtTrigger);
        }

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
        final RealMatrix dYdY0 = MatrixUtils.createRealMatrix(stateDimension, stateDimension);
        int index = 0;
        for (int i = 0; i < stateDimension; ++i) {
            for (int j = 0; j < stateDimension; ++j) {
                dYdY0.setEntry(i, j, p[index++]);
            }
        }
        return dYdY0;
    }

}

