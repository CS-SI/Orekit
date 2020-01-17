/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.forces;

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;

/** This class implements a parametric acceleration.
 * <p>Parametric accelerations are intended to model lesser-known
 * forces, estimating a few defining parameters from a parametric
 * function using orbit determination. Typical parametric functions
 * are polynomial (often limited to a constant term) and harmonic
 * (often with either orbital period or half orbital period).</p>
 * <p>An important operational example is the infamous GPS Y-bias,
 * which is thought to be related to a radiator thermal radiation.
 * Other examples could be to model leaks that produce roughly constant
 * trust in some spacecraft-related direction.</p>
 * <p>The acceleration direction is considered constant in either:
 * </p>
 * <ul>
 *   <li>inertial frame</li>
 *   <li>spacecraft frame</li>
 *   <li>a dedicated attitude frame overriding spacecraft attitude
 *   (this could for example be used to model solar arrays orientation
 *   if the force is related to solar arrays)</li>
 * </ul>
 * <p>
 * If the direction of the acceleration is unknown, then three instances
 * of this class should be used, one along the X axis, one along the Y
 * axis and one along the Z axis and their parameters estimated as usual.
 * </p>
 * @since 9.0
 * @author Luc Maisonobe
 */
public abstract class AbstractParametricAcceleration extends AbstractForceModel {

    /** Direction of the acceleration in defining frame. */
    private final Vector3D direction;

    /** Flag for inertial acceleration direction. */
    private final boolean isInertial;

    /** The attitude to override, if set. */
    private final AttitudeProvider attitudeOverride;

    /** Simple constructor.
     * @param direction acceleration direction in overridden spacecraft frame
     * @param isInertial if true, direction is defined in the same inertial
     * frame used for propagation (i.e. {@link SpacecraftState#getFrame()}),
     * otherwise direction is defined in spacecraft frame (i.e. using the
     * propagation {@link
     * org.orekit.propagation.Propagator#setAttitudeProvider(AttitudeProvider)
     * attitude law})
     * @param attitudeOverride provider for attitude used to compute acceleration
     * direction
     */
    protected AbstractParametricAcceleration(final Vector3D direction,  final boolean isInertial,
                                             final AttitudeProvider attitudeOverride) {
        this.direction        = direction;
        this.isInertial       = isInertial;
        this.attitudeOverride = attitudeOverride;
    }

    /** Check if direction is inertial.
     * @return true if direction is inertial
     */
    protected boolean isInertial() {
        return isInertial;
    }

    /** Compute the signed amplitude of the acceleration.
     * <p>
     * The acceleration is the direction multiplied by the signed amplitude. So if
     * signed amplitude is negative, the acceleratin is towards the opposite of the
     * direction specified at construction.
     * </p>
     * @param state current state information: date, kinematics, attitude
     * @param parameters values of the force model parameters
     * @return norm of the acceleration
     */
    protected abstract double signedAmplitude(SpacecraftState state, double[] parameters);

    /** Compute the signed amplitude of the acceleration.
     * <p>
     * The acceleration is the direction multiplied by the signed amplitude. So if
     * signed amplitude is negative, the acceleratin is towards the opposite of the
     * direction specified at construction.
     * </p>
     * @param state current state information: date, kinematics, attitude
     * @param parameters values of the force model parameters
     * @param <T> type of the elements
     * @return norm of the acceleration
     */
    protected abstract <T extends RealFieldElement<T>> T signedAmplitude(FieldSpacecraftState<T> state, T[] parameters);

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState state, final double[] parameters) {

        final Vector3D inertialDirection;
        if (isInertial) {
            // the acceleration direction is already defined in the inertial frame
            inertialDirection = direction;
        } else {
            final Attitude attitude;
            if (attitudeOverride == null) {
                // the acceleration direction is defined in spacecraft frame as set by the propagator
                attitude = state.getAttitude();
            } else {
                // the acceleration direction is defined in a dedicated frame
                attitude = attitudeOverride.getAttitude(state.getOrbit(), state.getDate(), state.getFrame());
            }
            inertialDirection = attitude.getRotation().applyInverseTo(direction);
        }

        return new Vector3D(signedAmplitude(state, parameters), inertialDirection);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> state,
                                                                         final T[] parameters) {

        final FieldVector3D<T> inertialDirection;
        if (isInertial) {
            // the acceleration direction is already defined in the inertial frame
            inertialDirection = new FieldVector3D<>(state.getDate().getField(), direction);
        } else {
            final FieldAttitude<T> attitude;
            if (attitudeOverride == null) {
                // the acceleration direction is defined in spacecraft frame as set by the propagator
                attitude = state.getAttitude();
            } else {
                // the acceleration direction is defined in a dedicated frame
                attitude = attitudeOverride.getAttitude(state.getOrbit(), state.getDate(), state.getFrame());
            }
            inertialDirection = attitude.getRotation().applyInverseTo(direction);
        }

        return new FieldVector3D<>(signedAmplitude(state, parameters), inertialDirection);

    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        return Stream.empty();
    }

}
