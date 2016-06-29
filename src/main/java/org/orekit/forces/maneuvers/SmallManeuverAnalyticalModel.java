/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AdapterPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** Analytical model for small maneuvers.
 * <p>The aim of this model is to compute quickly the effect at date t₁
 * of a small maneuver performed at an earlier date t₀. Both the
 * direct effect of the maneuver and the Jacobian of this effect with respect to
 * maneuver parameters are available.
 * </p>
 * <p>These effect are computed analytically using two Jacobian matrices:
 * <ol>
 *   <li>J₀: Jacobian of Keplerian or equinoctial elements with respect
 *   to cartesian parameters at date t₀ allows to compute
 *   maneuver effect as a change in orbital elements at maneuver date t₀,</li>
 *   <li>J<sub>1/0</sub>: Jacobian of Keplerian or equinoctial elements
 *   at date t₁ with respect to Keplerian or equinoctial elements
 *   at date t₀ allows to propagate the change in orbital elements
 *   to final date t₁.</li>
 * </ol>
 *
 * <p>
 * The second Jacobian, J<sub>1/0</sub>, is computed using a simple Keplerian
 * model, i.e. it is the identity except for the mean motion row which also includes
 * an off-diagonal element due to semi-major axis change.
 * </p>
 * <p>
 * The orbital elements change at date t₁ can be added to orbital elements
 * extracted from state, and the final elements taking account the changes are then
 * converted back to appropriate type, which may be different from Keplerian or
 * equinoctial elements.
 * </p>
 * <p>
 * Note that this model takes <em>only</em> Keplerian effects into account. This means
 * that using only this class to compute an inclination maneuver in Low Earth Orbit will
 * <em>not</em> change ascending node drift rate despite inclination has changed (the
 * same would be true for a semi-major axis change of course). In order to take this
 * drift into account, an instance of {@link
 * org.orekit.propagation.analytical.J2DifferentialEffect J2DifferentialEffect}
 * must be used together with an instance of this class.
 * </p>
 * @author Luc Maisonobe
 */
public class SmallManeuverAnalyticalModel
    implements AdapterPropagator.DifferentialEffect {

    /** State at maneuver date (before maneuver occurrence). */
    private final SpacecraftState state0;

    /** Inertial velocity increment. */
    private final Vector3D inertialDV;

    /** Mass change ratio. */
    private final double massRatio;

    /** Type of orbit used for internal Jacobians. */
    private final OrbitType type;

    /** Initial Keplerian (or equinoctial) Jacobian with respect to maneuver. */
    private final double[][] j0;

    /** Time derivative of the initial Keplerian (or equinoctial) Jacobian with respect to maneuver. */
    private double[][] j0Dot;

    /** Mean anomaly change factor. */
    private final double ksi;

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

        this.state0    = state0;
        this.massRatio = FastMath.exp(-dV.getNorm() / (Constants.G0_STANDARD_GRAVITY * isp));

        // use equinoctial orbit type if possible, Keplerian if nearly hyperbolic orbits
        type = (state0.getE() < 0.9) ? OrbitType.EQUINOCTIAL : OrbitType.KEPLERIAN;

        // compute initial Jacobian
        final double[][] fullJacobian = new double[6][6];
        j0 = new double[6][3];
        final Orbit orbit0 = type.convertType(state0.getOrbit());
        orbit0.getJacobianWrtCartesian(PositionAngle.MEAN, fullJacobian);
        for (int i = 0; i < j0.length; ++i) {
            System.arraycopy(fullJacobian[i], 3, j0[i], 0, 3);
        }

        // use lazy evaluation for j0Dot, as it is used only when Jacobians are evaluated
        j0Dot = null;

        // compute maneuver effect on Keplerian (or equinoctial) elements
        inertialDV = frame.getTransformTo(state0.getFrame(), state0.getDate()).transformVector(dV);

        // compute mean anomaly change: dM(t1) = dM(t0) + ksi * da * (t1 - t0)
        final double mu = state0.getMu();
        final double a  = state0.getA();
        ksi = -1.5 * FastMath.sqrt(mu / a) / (a * a);

    }

    /** Get the date of the maneuver.
     * @return date of the maneuver
     */
    public AbsoluteDate getDate() {
        return state0.getDate();
    }

    /** Get the inertial velocity increment of the maneuver.
     * @return velocity increment in a state-dependent inertial frame
     * @see #getInertialFrame()
     */
    public Vector3D getInertialDV() {
        return inertialDV;
    }

    /** Get the inertial frame in which the velocity increment is defined.
     * @return inertial frame in which the velocity increment is defined
     * @see #getInertialDV()
     */
    public Frame getInertialFrame() {
        return state0.getFrame();
    }

    /** Compute the effect of the maneuver on an orbit.
     * @param orbit1 original orbit at t₁, without maneuver
     * @return orbit at t₁, taking the maneuver
     * into account if t₁ &gt; t₀
     * @see #apply(SpacecraftState)
     * @see #getJacobian(Orbit, PositionAngle, double[][])
     */
    public Orbit apply(final Orbit orbit1) {

        if (orbit1.getDate().compareTo(state0.getDate()) <= 0) {
            // the maneuver has not occurred yet, don't change anything
            return orbit1;
        }

        return updateOrbit(orbit1);

    }

    /** Compute the effect of the maneuver on a spacecraft state.
     * @param state1 original spacecraft state at t₁,
     * without maneuver
     * @return spacecraft state at t₁, taking the maneuver
     * into account if t₁ &gt; t₀
     * @see #apply(Orbit)
     * @see #getJacobian(Orbit, PositionAngle, double[][])
     */
    public SpacecraftState apply(final SpacecraftState state1) {

        if (state1.getDate().compareTo(state0.getDate()) <= 0) {
            // the maneuver has not occurred yet, don't change anything
            return state1;
        }

        return new SpacecraftState(updateOrbit(state1.getOrbit()),
                                   state1.getAttitude(), updateMass(state1.getMass()));

    }

    /** Compute the effect of the maneuver on an orbit.
     * @param orbit1 original orbit at t₁, without maneuver
     * @return orbit at t₁, always taking the maneuver into account
     */
    private Orbit updateOrbit(final Orbit orbit1) {

        // compute maneuver effect
        final double dt = orbit1.getDate().durationFrom(state0.getDate());
        final double x  = inertialDV.getX();
        final double y  = inertialDV.getY();
        final double z  = inertialDV.getZ();
        final double[] delta = new double[6];
        for (int i = 0; i < delta.length; ++i) {
            delta[i] = j0[i][0] * x + j0[i][1] * y + j0[i][2] * z;
        }
        delta[5] += ksi * delta[0] * dt;

        // convert current orbital state to Keplerian or equinoctial elements
        final double[] parameters = new double[6];
        type.mapOrbitToArray(type.convertType(orbit1), PositionAngle.MEAN, parameters);
        for (int i = 0; i < delta.length; ++i) {
            parameters[i] += delta[i];
        }

        // build updated orbit as Keplerian or equinoctial elements
        final Orbit o = type.mapArrayToOrbit(parameters, PositionAngle.MEAN,
                                             orbit1.getDate(), orbit1.getMu(),
                                             orbit1.getFrame());

        // convert to required type
        return orbit1.getType().convertType(o);

    }

    /** Compute the Jacobian of the orbit with respect to maneuver parameters.
     * <p>
     * The Jacobian matrix is a 6x4 matrix. Element jacobian[i][j] corresponds to
     * the partial derivative of orbital parameter i with respect to maneuver
     * parameter j. The rows order is the same order as used in {@link
     * Orbit#getJacobianWrtCartesian(PositionAngle, double[][]) Orbit.getJacobianWrtCartesian}
     * method. Columns (0, 1, 2) correspond to the velocity increment coordinates
     * (ΔV<sub>x</sub>, ΔV<sub>y</sub>, ΔV<sub>z</sub>) in the
     * inertial frame returned by {@link #getInertialFrame()}, and column 3
     * corresponds to the maneuver date t₀.
     * </p>
     * @param orbit1 original orbit at t₁, without maneuver
     * @param positionAngle type of the position angle to use
     * @param jacobian placeholder 6x4 (or larger) matrix to be filled with the Jacobian, if matrix
     * is larger than 6x4, only the 6x4 upper left corner will be modified
     * @see #apply(Orbit)
     * @exception OrekitException if time derivative of the initial Jacobian cannot be computed
     */
    public void getJacobian(final Orbit orbit1, final PositionAngle positionAngle,
                            final double[][] jacobian)
        throws OrekitException {

        final double dt = orbit1.getDate().durationFrom(state0.getDate());
        if (dt < 0) {
            // the maneuver has not occurred yet, Jacobian is null
            for (int i = 0; i < 6; ++i) {
                Arrays.fill(jacobian[i], 0, 4, 0.0);
            }
            return;
        }

        // derivatives of Keplerian/equinoctial elements with respect to velocity increment
        final double x  = inertialDV.getX();
        final double y  = inertialDV.getY();
        final double z  = inertialDV.getZ();
        for (int i = 0; i < 6; ++i) {
            System.arraycopy(j0[i], 0, jacobian[i], 0, 3);
        }
        for (int j = 0; j < 3; ++j) {
            jacobian[5][j] += ksi * dt * j0[0][j];
        }

        // derivatives of Keplerian/equinoctial elements with respect to date
        evaluateJ0Dot();
        for (int i = 0; i < 6; ++i) {
            jacobian[i][3] = j0Dot[i][0] * x + j0Dot[i][1] * y + j0Dot[i][2] * z;
        }
        final double da = j0[0][0] * x + j0[0][1] * y + j0[0][2] * z;
        jacobian[5][3] += ksi * (jacobian[0][3] * dt - da);

        if (orbit1.getType() != type || positionAngle != PositionAngle.MEAN) {

            // convert to derivatives of cartesian parameters
            final double[][] j2         = new double[6][6];
            final double[][] pvJacobian = new double[6][4];
            final Orbit updated         = updateOrbit(orbit1);
            type.convertType(updated).getJacobianWrtParameters(PositionAngle.MEAN, j2);
            for (int i = 0; i < 6; ++i) {
                for (int j = 0; j < 4; ++j) {
                    pvJacobian[i][j] = j2[i][0] * jacobian[0][j] + j2[i][1] * jacobian[1][j] +
                                       j2[i][2] * jacobian[2][j] + j2[i][3] * jacobian[3][j] +
                                       j2[i][4] * jacobian[4][j] + j2[i][5] * jacobian[5][j];
                }
            }

            // convert to derivatives of specified parameters
            final double[][] j3 = new double[6][6];
            updated.getJacobianWrtCartesian(positionAngle, j3);
            for (int j = 0; j < 4; ++j) {
                for (int i = 0; i < 6; ++i) {
                    jacobian[i][j] = j3[i][0] * pvJacobian[0][j] + j3[i][1] * pvJacobian[1][j] +
                                     j3[i][2] * pvJacobian[2][j] + j3[i][3] * pvJacobian[3][j] +
                                     j3[i][4] * pvJacobian[4][j] + j3[i][5] * pvJacobian[5][j];
                }
            }

        }

    }

    /** Lazy evaluation of the initial Jacobian time derivative.
     * @exception OrekitException if initial orbit cannot be shifted
     */
    private void evaluateJ0Dot() throws OrekitException {

        if (j0Dot == null) {

            j0Dot = new double[6][3];
            final double dt = 1.0e-5 / state0.getKeplerianMeanMotion();
            final Orbit orbit = type.convertType(state0.getOrbit());

            // compute shifted Jacobians
            final double[][] j0m1 = new double[6][6];
            orbit.shiftedBy(-1 * dt).getJacobianWrtCartesian(PositionAngle.MEAN, j0m1);
            final double[][] j0p1 = new double[6][6];
            orbit.shiftedBy(+1 * dt).getJacobianWrtCartesian(PositionAngle.MEAN, j0p1);

            // evaluate derivative by finite differences
            for (int i = 0; i < j0Dot.length; ++i) {
                final double[] m1Row    = j0m1[i];
                final double[] p1Row    = j0p1[i];
                final double[] j0DotRow = j0Dot[i];
                for (int j = 0; j < 3; ++j) {
                    j0DotRow[j] = (p1Row[j + 3] - m1Row[j + 3]) / (2 * dt);
                }
            }

        }

    }

    /** Update a spacecraft mass due to maneuver.
     * @param mass masse before maneuver
     * @return mass after maneuver
     */
    public double updateMass(final double mass) {
        return massRatio * mass;
    }

}
