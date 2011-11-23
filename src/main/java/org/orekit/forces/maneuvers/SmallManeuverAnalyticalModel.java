/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.forces.maneuvers;

import java.io.Serializable;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** Analytical model for small maneuvers.
 * <p>The aim of this model is to compute quickly the effect at date t<sub>1</sub>
 * of a small maneuver performed at an earlier date t<sub>0</sub>. This effect is
 * computed analytically using two Jacobian matrices:
 * <ol>
 *   <li>J<sub>0</sub>: Jacobian of Keplerian or equinoctial elements with respect
 *   to cartesian parameters at date t<sub>0</sub></li> allows to compute
 *   maneuver effect as an orbital elements change,
 *   <li>J<sub>1/0</sub></li>: Jacobian of Keplerian or equinoctial elements
 *   at date t<sub>1</sub> with respect to Keplerian or equinoctial elements
 *   at date t<sub>0</sub> allows to propagate the orbital elements change to
 *   final date.
 * </ol>
 * </p>
 * <p>
 * The second Jacobian, J<sub>1/0</sub></li>, is computed using a simple Keplerian
 * model, i.e. it is the identity except for the mean motion row which also includes
 * an off-diagonal element due to semi-major axis change.
 * </p>
 * <p>
 * The orbital elements change at date t<sub>1</sub> can be added to orbital elements
 * extracted from state, and the final elements taking account the changes are then
 * converted back to appropriate type, which may be different from Keplerian or
 * equinoctial elements.
 * </p>
 * @author Luc Maisonobe
 */
public class SmallManeuverAnalyticalModel implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 5046690115016896090L;

    /** Maneuver date. */
    final AbsoluteDate t0;

    /** Inertial velocity increment. */
    final Vector3D inertialDV;

    /** Inertial frame in which the velocity increment is defined. */
    final Frame inertialFrame;

    /** Mass change ratio. */
    private final double massRatio;

    /** Type of orbit used for internal Jacobians. */
    final OrbitType type;

    /** Keplerian (or equinoctial) differential change due to maneuver. */
    final double[] delta;

    /** Mean motion change. */
    final double deltaN;

    /** Build a maneuver defined in spacecraft frame.
     * @param state0 state at maneuver date, <em>before</em> the maneuver
     * is performed
     * @param dV velocity increment in spacecraft frame
     * @param isp engine specific impulse (s)
     * @exception OrekitException if spacecraft frame cannot be transformed
     */
    
    public SmallManeuverAnalyticalModel(final SpacecraftState state0,
                                        final Vector3D dV, final double isp)
        throws OrekitException {
        this(state0, state0.getFrame(),
             state0.getAttitude().getRotation().applyInverseTo(dV),
             isp);
    }

    /** Build a maneuver defined in user-specified frame.
     * @param state0 state at maneuver date, <em>before</em> the maneuver
     * is performed
     * @param frame frame in which velocity increment is defined
     * @param dV velocity increment in specified frame
     * @param isp engine specific impulse (s)
     * @exception OrekitException if velocity increment frame cannot be transformed
     */
    
    public SmallManeuverAnalyticalModel(final SpacecraftState state0, final Frame frame,
                                        final Vector3D dV, final double isp)
        throws OrekitException {

        this.t0        = state0.getDate();
        this.massRatio = FastMath.exp(-dV.getNorm() / (Constants.G0_STANDARD_GRAVITY * isp));

        // use equinoctial orbit type if possible, Keplerian if nearly hyperbolic orbits
        type = (state0.getE() < 0.9) ? OrbitType.EQUINOCTIAL : OrbitType.KEPLERIAN;

        // compute initial Jacobian
        final double[][] jacobian = new double[6][6];
        final Orbit orbit0 = type.convertType(state0.getOrbit());
        orbit0.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        // compute maneuver effect on Keplerian (or equinoctial) elements
        inertialFrame = state0.getFrame();
        inertialDV = frame.getTransformTo(inertialFrame, t0).transformVector(dV);
        delta = new double[6];
        for (int i = 0; i < delta.length; ++i) {
            delta[i] = jacobian[i][3] * inertialDV.getX() +
                       jacobian[i][4] * inertialDV.getY() +
                       jacobian[i][5] * inertialDV.getZ();
        }

        // compute mean motion change: dM(t1) = dM(t0) + deltaN * (t1 - t0)
        final double mu = state0.getMu();
        final double a  = state0.getA();
        deltaN = -1.5 * delta[0] * FastMath.sqrt(mu / a) / (a * a);

    }

    /** Get the date of the maneuver.
     * @return date of the maneuver
     */
    public AbsoluteDate getDate() {
        return t0;
    }

    /** Get the velocity increment of the maneuver.
     * @return velocity increment in a state-dependent inertial frame
     * @see SmallManeuverAnalyticalModel#getInertialFrame()
     */
    public Vector3D getInertialDV() {
        return inertialDV;
    }

    /** Get the inertial frame in which the velocity increment is defined.
     * @return inertial frame in which the velocity increment is defined
     * @see #getInertialDV()
     */
    public Frame getInertialFrame() {
        return inertialFrame;
    }

    /** Compute the effect of the maneuver on a spacecraft state.
     * @param state1 original spacecraft state at t<sub>1</sub>,
     * without maneuver
     * @return spacecraft state at t<sub>1</sub>, taking the maneuver
     * into account if t<sub>1</sub> &gt; t<sub>0</sub>
     */
    public SpacecraftState applyManeuver(final SpacecraftState state1) {

        if (state1.getDate().compareTo(t0) <= 0) {
            // the maneuver has not occurred yet, don't change anything
            return state1;
        }

        // convert current orbital state to Keplerian or equinoctial elements
        final double[] parameters = new double[6];
        type.mapOrbitToArray(type.convertType(state1.getOrbit()),
                             PositionAngle.MEAN, parameters);

        // add maneuver effect
        for (int i = 0; i < parameters.length; ++i) {
            parameters[i] += delta[i];
        }
        parameters[5] += deltaN * state1.getDate().durationFrom(t0);

        // build updated orbit as Keplerian or equinoctial elements
        final Orbit updated = type.mapArrayToOrbit(parameters, PositionAngle.MEAN,
                                                   state1.getDate(), state1.getMu(),
                                                   state1.getFrame());

        // return a new state with the same orbit type as provided
        return new SpacecraftState(state1.getOrbit().getType().convertType(updated),
                                   state1.getAttitude(), updateMass(state1.getMass()));

    }

    /** Update a spacecraft mass due to maneuver.
     * @param mass masse before maneuver
     * @return mass after maneuver
     */
    public double updateMass(final double mass) {
        return massRatio * mass;
    }

}
