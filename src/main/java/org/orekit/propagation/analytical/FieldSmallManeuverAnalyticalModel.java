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
package org.orekit.propagation.analytical;

import java.util.Arrays;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

/**
 * Analytical model for small maneuvers.
 * <p>
 * The aim of this model is to compute quickly the effect at date t₁ of a small
 * maneuver performed at an earlier date t₀. Both the direct effect of the
 * maneuver and the Jacobian of this effect with respect to maneuver parameters
 * are available.
 * </p>
 * <p>
 * These effect are computed analytically using two Jacobian matrices:
 * <ol>
 * <li>J₀: Jacobian of Keplerian or equinoctial elements with respect to
 * Cartesian parameters at date t₀ allows to compute maneuver effect as a change
 * in orbital elements at maneuver date t₀,</li>
 * <li>J<sub>1/0</sub>: Jacobian of Keplerian or equinoctial elements at date t₁
 * with respect to Keplerian or equinoctial elements at date t₀ allows to
 * propagate the change in orbital elements to final date t₁.</li>
 * </ol>
 * <p>
 * The second Jacobian, J<sub>1/0</sub>, is computed using a simple Keplerian
 * model, i.e. it is the identity except for the mean motion row which also
 * includes an off-diagonal element due to semi-major axis change.
 * </p>
 * <p>
 * The orbital elements change at date t₁ can be added to orbital elements
 * extracted from state, and the final elements taking account the changes are
 * then converted back to appropriate type, which may be different from
 * Keplerian or equinoctial elements.
 * </p>
 * <p>
 * Note that this model takes <em>only</em> Keplerian effects into account. This
 * means that using only this class to compute an inclination maneuver in Low
 * Earth Orbit will <em>not</em> change ascending node drift rate despite
 * inclination has changed (the same would be true for a semi-major axis change
 * of course). In order to take this drift into account, an instance of
 * {@link org.orekit.propagation.analytical.J2DifferentialEffect
 * J2DifferentialEffect} must be used together with an instance of this class.
 * </p>
 *
 * @author Luc Maisonobe
 * @author Nicolas Fialton (field translation)
 */
public class FieldSmallManeuverAnalyticalModel<T extends RealFieldElement<T>>
    implements
    FieldAdapterPropagator.FieldDifferentialEffect<T> {

    /** State at maneuver date (before maneuver occurrence). */
    private final FieldSpacecraftState<T> state0;

    /** Inertial velocity increment. */
    private final FieldVector3D<T> inertialDV;

    /** Mass change ratio. */
    private final T massRatio;

    /** Type of orbit used for internal Jacobians. */
    private final OrbitType type;

    /** Initial Keplerian (or equinoctial) Jacobian with respect to maneuver. */
    private final T[][] j0;

    /**
     * Time derivative of the initial Keplerian (or equinoctial) Jacobian with
     * respect to maneuver.
     */
    private T[][] j0Dot;

    /** Mean anomaly change factor. */
    private final T ksi;

    /**
     * Build a maneuver defined in spacecraft frame.
     *
     * @param field
     * @param state0 state at maneuver date, <em>before</em> the maneuver is performed
     * @param dV velocity increment in spacecraft frame
     * @param isp engine specific impulse (s)
     */
    public FieldSmallManeuverAnalyticalModel(final Field<T> field,
                                             final FieldSpacecraftState<T> state0,
                                             final FieldVector3D<T> dV,
                                             final T isp) {
        this(field, state0, state0.getFrame(),
             state0.getAttitude().getRotation().applyInverseTo(dV), isp);
    }

    /**
     * Build a maneuver defined in user-specified frame.
     *
     * @param field
     * @param state0 state at maneuver date, <em>before</em> the maneuver is
     *        performed
     * @param frame frame in which velocity increment is defined
     * @param dV velocity increment in specified frame
     * @param isp engine specific impulse (s)
     */
    public FieldSmallManeuverAnalyticalModel(final Field<T> field,
                                             final FieldSpacecraftState<T> state0,
                                             final Frame frame,
                                             final FieldVector3D<T> dV,
                                             final T isp) {
        final T zero = field.getZero();
        this.state0 = state0;
        this.massRatio =
            FastMath.exp(zero.subtract(dV.getNorm())
                .divide(isp.multiply(Constants.G0_STANDARD_GRAVITY)));

        // use equinoctial orbit type if possible, Keplerian if nearly
        // hyperbolic orbits
        type =
            (state0.getE().getReal() < 0.9) ?
                                            OrbitType.EQUINOCTIAL :
                                            OrbitType.KEPLERIAN;

        // compute initial Jacobian
        final T[][] fullJacobian = MathArrays.buildArray(field, 6, 6);
        j0 = MathArrays.buildArray(field, 6, 3);
        final FieldOrbit<T> orbit0 = type.convertType(state0.getOrbit());
        orbit0.getJacobianWrtCartesian(PositionAngle.MEAN, fullJacobian);
        for (int i = 0; i < j0.length; ++i) {
            System.arraycopy(fullJacobian[i], 3, j0[i], 0, 3);
        }

        // use lazy evaluation for j0Dot, as it is used only when Jacobians are
        // evaluated
        j0Dot = null;

        // compute maneuver effect on Keplerian (or equinoctial) elements
        inertialDV =
            frame.getTransformTo(state0.getFrame(), state0.getDate())
                .transformVector(dV);

        // compute mean anomaly change: dM(t1) = dM(t0) + ksi * da * (t1 - t0)
        final T mu = state0.getMu();
        final T a = state0.getA();
        ksi =
            zero.add(-1.5).multiply(FastMath.sqrt(zero.add(mu).divide(a))
                .divide(a.multiply(a)));

    }

    /**
     * Get the date of the maneuver.
     *
     * @return date of the maneuver
     */
    public FieldAbsoluteDate<T> getDate() {
        return state0.getDate();
    }

    /**
     * Get the inertial velocity increment of the maneuver.
     *
     * @return velocity increment in a state-dependent inertial frame
     * @see #getInertialFrame()
     */
    public FieldVector3D<T> getInertialDV() {
        return inertialDV;
    }

    /**
     * Get the inertial frame in which the velocity increment is defined.
     *
     * @return inertial frame in which the velocity increment is defined
     * @see #getInertialDV()
     */
    public Frame getInertialFrame() {
        return state0.getFrame();
    }

    /**
     * Compute the effect of the maneuver on an orbit.
     *
     * @param orbit1 original orbit at t₁, without maneuver
     * @return orbit at t₁, taking the maneuver into account if t₁ &gt; t₀
     * @see #apply(SpacecraftState)
     * @see #getJacobian(Orbit, PositionAngle, double[][])
     */
    public FieldOrbit<T> apply(final FieldOrbit<T> orbit1) {

        if (orbit1.getDate().compareTo(state0.getDate()) <= 0) {
            // the maneuver has not occurred yet, don't change anything
            return orbit1;
        }

        return orbit1.getType().convertType(updateOrbit(orbit1));

    }

    /**
     * Compute the effect of the maneuver on a spacecraft state.
     *
     * @param state1 original spacecraft state at t₁, without maneuver
     * @return spacecraft state at t₁, taking the maneuver into account if t₁
     *         &gt; t₀
     * @see #apply(Orbit)
     * @see #getJacobian(Orbit, PositionAngle, double[][])
     */
    public FieldSpacecraftState<T> apply(final FieldSpacecraftState<T> state1) {

        if (state1.getDate().compareTo(state0.getDate()) <= 0) {
            // the maneuver has not occurred yet, don't change anything
            return state1;
        }

        return new FieldSpacecraftState<>(state1.getOrbit().getType()
            .convertType(updateOrbit(state1.getOrbit())), state1.getAttitude(),
                                          updateMass(state1.getMass()));

    }

    /**
     * Compute the effect of the maneuver on an orbit.
     *
     * @param orbit1 original orbit at t₁, without maneuver
     * @return orbit at t₁, always taking the maneuver into account, always in
     *         the internal type
     */
    private FieldOrbit<T> updateOrbit(final FieldOrbit<T> orbit1) {

        // compute maneuver effect
        final T dt = orbit1.getDate().durationFrom(state0.getDate());
        final T x = inertialDV.getX();
        final T y = inertialDV.getY();
        final T z = inertialDV.getZ();
        final Field<T> field = z.getField();
        final T[] delta = MathArrays.buildArray(field, 6);
        for (int i = 0; i < delta.length; ++i) {
            delta[i] =
                j0[i][0].multiply(x)
                    .add(j0[i][1].multiply(y).add(j0[i][2].multiply(z)));
        }
        delta[5] = delta[5].add(ksi.multiply(delta[0]).multiply(dt));

        // convert current orbital state to Keplerian or equinoctial elements
        final T[] parameters = MathArrays.buildArray(field, 6);
        type.mapOrbitToArray(type.convertType(orbit1), PositionAngle.MEAN,
                             parameters, null);
        for (int i = 0; i < delta.length; ++i) {
            parameters[i] = parameters[i].add(delta[i]);
        }

        // build updated orbit as Keplerian or equinoctial elements
        return type.mapArrayToOrbit(parameters, null, PositionAngle.MEAN,
                                    orbit1.getDate(), orbit1.getMu(),
                                    orbit1.getFrame());

    }

    /**
     * Compute the Jacobian of the orbit with respect to maneuver parameters.
     * <p>
     * The Jacobian matrix is a 6x4 matrix. Element jacobian[i][j] corresponds
     * to the partial derivative of orbital parameter i with respect to maneuver
     * parameter j. The rows order is the same order as used in
     * {@link Orbit#getJacobianWrtCartesian(PositionAngle, double[][])
     * Orbit.getJacobianWrtCartesian} method. Columns (0, 1, 2) correspond to
     * the velocity increment coordinates (ΔV<sub>x</sub>, ΔV<sub>y</sub>,
     * ΔV<sub>z</sub>) in the inertial frame returned by
     * {@link #getInertialFrame()}, and column 3 corresponds to the maneuver
     * date t₀.
     * </p>
     *
     * @param orbit1 original orbit at t₁, without maneuver
     * @param positionAngle type of the position angle to use
     * @param jacobian placeholder 6x4 (or larger) matrix to be filled with the
     *        Jacobian, if matrix is larger than 6x4, only the 6x4 upper left
     *        corner will be modified
     * @see #apply(Orbit)
     */
    public void getJacobian(final FieldOrbit<T> orbit1,
                            final PositionAngle positionAngle,
                            final T[][] jacobian) {

        final T dt = orbit1.getDate().durationFrom(state0.getDate());
        if (dt.getReal() < 0) {
            // the maneuver has not occurred yet, Jacobian is null
            for (int i = 0; i < 6; ++i) {
                Arrays.fill(jacobian[i], 0, 4, dt.getField().getZero());
            }
            return;
        }

        // derivatives of Keplerian/equinoctial elements with respect to
        // velocity
        // increment
        final T x = inertialDV.getX();
        final T y = inertialDV.getY();
        final T z = inertialDV.getZ();
        for (int i = 0; i < 6; ++i) {
            System.arraycopy(j0[i], 0, jacobian[i], 0, 3);
        }
        for (int j = 0; j < 3; ++j) {
            jacobian[5][j] =
                jacobian[5][j].add(ksi.multiply(dt).multiply(j0[0][j]));
        }

        // derivatives of Keplerian/equinoctial elements with respect to date
        evaluateJ0Dot();
        for (int i = 0; i < 6; ++i) {
            jacobian[i][3] =
                j0Dot[i][0].multiply(x).add(j0Dot[i][1].multiply(y))
                    .add(j0Dot[i][2].multiply(z));
        }
        final T da =
            j0[0][0].multiply(x).add(j0[0][1].multiply(y))
                .add(j0[0][2].multiply(z));
        jacobian[5][3] =
            jacobian[5][3]
                .add(ksi.multiply(jacobian[0][3].multiply(dt).subtract(da)));

        if (orbit1.getType() != type || positionAngle != PositionAngle.MEAN) {

            // convert to derivatives of Cartesian parameters
            final Field<T> field = x.getField();
            final T[][] j2 = MathArrays.buildArray(field, 6, 6);
            final T[][] pvJacobian = MathArrays.buildArray(field, 6, 4);
            final FieldOrbit<T> updated = updateOrbit(orbit1);
            updated.getJacobianWrtParameters(PositionAngle.MEAN, j2);
            for (int i = 0; i < 6; ++i) {
                for (int j = 0; j < 4; ++j) {
                    pvJacobian[i][j] =
                        j2[i][0].multiply(jacobian[0][j])
                            .add(j2[i][1].multiply(jacobian[1][j]))
                            .add(j2[i][2].multiply(jacobian[2][j]))
                            .add(j2[i][3].multiply(jacobian[3][j]))
                            .add(j2[i][4].multiply(jacobian[4][j]))
                            .add(j2[i][5].multiply(jacobian[5][j]));
                }
            }

            // convert to derivatives of specified parameters
            final T[][] j3 = MathArrays.buildArray(field, 6, 6);
            orbit1.getType().convertType(updated)
                .getJacobianWrtCartesian(positionAngle, j3);
            for (int j = 0; j < 4; ++j) {
                for (int i = 0; i < 6; ++i) {
                    jacobian[i][j] =
                        j3[i][0].multiply(pvJacobian[0][j])
                            .add(j3[i][1].multiply(pvJacobian[1][j]))
                            .add(j3[i][2].multiply(pvJacobian[2][j]))
                            .add(j3[i][3].multiply(pvJacobian[3][j]))
                            .add(j3[i][4].multiply(pvJacobian[4][j]))
                            .add(j3[i][5].multiply(pvJacobian[5][j]));
                }
            }

        }

    }

    /**
     * Lazy evaluation of the initial Jacobian time derivative.
     */
    private void evaluateJ0Dot() {

        if (j0Dot == null) {
            final Field<T> field = massRatio.getField();
            final T zero = field.getZero();
            j0Dot = MathArrays.buildArray(field, 6, 3);
            final T dt =
                zero.add(1.0e-5)
                    .divide(state0.getOrbit().getKeplerianMeanMotion());
            final FieldOrbit<T> orbit = type.convertType(state0.getOrbit());

            // compute shifted Jacobians
            final T[][] j0m1 = MathArrays.buildArray(field, 6, 6);
            orbit.shiftedBy(dt.multiply(-1))
                .getJacobianWrtCartesian(PositionAngle.MEAN, j0m1);
            final T[][] j0p1 = MathArrays.buildArray(field, 6, 6);
            orbit.shiftedBy(dt.multiply(+1))
                .getJacobianWrtCartesian(PositionAngle.MEAN, j0p1);

            // evaluate derivative by finite differences
            for (int i = 0; i < j0Dot.length; ++i) {
                final T[] m1Row = j0m1[i];
                final T[] p1Row = j0p1[i];
                final T[] j0DotRow = j0Dot[i];
                for (int j = 0; j < 3; ++j) {
                    j0DotRow[j] =
                        (p1Row[j + 3].subtract(m1Row[j + 3]))
                            .divide(dt.multiply(2));
                }
            }

        }

    }

    /**
     * Update a spacecraft mass due to maneuver.
     *
     * @param mass masse before maneuver
     * @return mass after maneuver
     */
    public T updateMass(final T mass) {
        return massRatio.multiply(mass);
    }

}
