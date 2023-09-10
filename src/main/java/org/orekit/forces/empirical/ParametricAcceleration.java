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
package org.orekit.forces.empirical;

import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

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
 * @since 10.3
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @author Melina Vanel
 */
public class ParametricAcceleration implements ForceModel {

    /** Direction of the acceleration in defining frame. */
    private final Vector3D direction;

    /** Flag for inertial acceleration direction. */
    private final boolean isInertial;

    /** The attitude to override, if set. */
    private final AttitudeProvider attitudeOverride;

    /** Acceleration model. */
    private final AccelerationModel accelerationModel;

    /** Simple constructor.
     * @param direction acceleration direction in overridden spacecraft frame
     * @param isInertial if true, direction is defined in the same inertial
     * frame used for propagation (i.e. {@link SpacecraftState#getFrame()}),
     * otherwise direction is defined in spacecraft frame (i.e. using the
     * propagation {@link
     * org.orekit.propagation.Propagator#setAttitudeProvider(AttitudeProvider)
     * attitude law})
     * @param accelerationModel acceleration model used to compute the contribution of the empirical acceleration
     * direction
     */
    public ParametricAcceleration(final Vector3D direction,
                                  final boolean isInertial,
                                  final AccelerationModel accelerationModel) {
        this(direction, isInertial, null, accelerationModel);
    }

    /** Simple constructor.
     * @param direction acceleration direction in overridden spacecraft frame
     * frame used for propagation (i.e. {@link SpacecraftState#getFrame()}),
     * otherwise direction is defined in spacecraft frame (i.e. using the
     * propagation {@link
     * org.orekit.propagation.Propagator#setAttitudeProvider(AttitudeProvider)
     * attitude law})
     * @param attitudeOverride provider for attitude used to compute acceleration
     * @param accelerationModel acceleration model used to compute the contribution of the empirical acceleration
     * direction
     */
    public ParametricAcceleration(final Vector3D direction,
                                  final AttitudeProvider attitudeOverride,
                                  final AccelerationModel accelerationModel) {
        this(direction, false, attitudeOverride, accelerationModel);
    }

    /** Simple constructor.
     * @param direction acceleration direction in overridden spacecraft frame
     * @param isInertial if true, direction is defined in the same inertial
     * frame used for propagation (i.e. {@link SpacecraftState#getFrame()}),
     * otherwise direction is defined in spacecraft frame (i.e. using the
     * propagation {@link
     * org.orekit.propagation.Propagator#setAttitudeProvider(AttitudeProvider)
     * attitude law})
     * @param attitudeOverride provider for attitude used to compute acceleration
     * @param accelerationModel acceleration model used to compute the contribution of the empirical acceleration
     * direction
     */
    private ParametricAcceleration(final Vector3D direction,
                                   final boolean isInertial,
                                   final AttitudeProvider attitudeOverride,
                                   final AccelerationModel accelerationModel) {
        this.direction         = direction;
        this.isInertial        = isInertial;
        this.attitudeOverride  = attitudeOverride;
        this.accelerationModel = accelerationModel;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return isInertial;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return accelerationModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        accelerationModel.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState state,
                                 final double[] parameters) {

        // Date
        final AbsoluteDate date = state.getDate();

        final Vector3D inertialDirection;
        if (isInertial) {
            // the acceleration direction is already defined in the inertial frame
            inertialDirection = direction;
        } else {
            final Rotation rotation;
            if (attitudeOverride == null) {
                // the acceleration direction is defined in spacecraft frame as set by the propagator
                rotation = state.getAttitude().getRotation();
            } else {
                // the acceleration direction is defined in a dedicated frame
                rotation = attitudeOverride.getAttitudeRotation(state.getOrbit(), date, state.getFrame());
            }
            inertialDirection = rotation.applyInverseTo(direction);
        }

        // Call the acceleration model to compute the acceleration
        return new Vector3D(accelerationModel.signedAmplitude(state, parameters), inertialDirection);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> state,
                                                                         final T[] parameters) {

        // Date
        final FieldAbsoluteDate<T> date = state.getDate();

        final FieldVector3D<T> inertialDirection;
        if (isInertial) {
            // the acceleration direction is already defined in the inertial frame
            inertialDirection = new FieldVector3D<>(date.getField(), direction);
        } else {
            final FieldRotation<T> rotation;
            if (attitudeOverride == null) {
                // the acceleration direction is defined in spacecraft frame as set by the propagator
                rotation = state.getAttitude().getRotation();
            } else {
                // the acceleration direction is defined in a dedicated frame
                rotation = attitudeOverride.getAttitudeRotation(state.getOrbit(), date, state.getFrame());
            }
            inertialDirection = rotation.applyInverseTo(direction);
        }

        // Call the acceleration model to compute the acceleration
        return new FieldVector3D<>(accelerationModel.signedAmplitude(state, parameters), inertialDirection);

    }


    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        return Stream.empty();
    }

}
