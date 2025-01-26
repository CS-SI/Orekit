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
package org.orekit.forces.empirical;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
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
public class ParametricAcceleration extends AbstractParametricAcceleration {

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
        super(direction, isInertial, attitudeOverride);
        this.accelerationModel = accelerationModel;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        final List<ParameterDriver> parameterDrivers = new ArrayList<>(accelerationModel.getParametersDrivers());
        if (getAttitudeOverride() != null) {
            parameterDrivers.addAll(getAttitudeOverride().getParametersDrivers());
        }
        return parameterDrivers;
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
        final Vector3D inertialDirection = getAccelerationDirection(state);

        // Call the acceleration model to compute the acceleration
        return new Vector3D(accelerationModel.signedAmplitude(state, parameters), inertialDirection);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> state,
                                                                             final T[] parameters) {
        final FieldVector3D<T> inertialDirection = getAccelerationDirection(state);

        // Call the acceleration model to compute the acceleration
        return new FieldVector3D<>(accelerationModel.signedAmplitude(state, parameters), inertialDirection);

    }

}
