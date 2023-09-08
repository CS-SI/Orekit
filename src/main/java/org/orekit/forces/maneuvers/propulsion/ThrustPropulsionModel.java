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

package org.orekit.forces.maneuvers.propulsion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;

/** Interface for a thrust-based propulsion model.
 * @author Maxime Journot
 * @since 10.2
 */
public interface ThrustPropulsionModel extends PropulsionModel {

    /** Get the specific impulse (s).
     * @param s current spacecraft state
     * @return specific impulse (s).
     */
    default double getIsp(SpacecraftState s) {
        final double flowRate = getFlowRate(s);
        return -getControl3DVectorCostType().evaluate(getThrustVector(s)) / (Constants.G0_STANDARD_GRAVITY * flowRate);
    }

    /** Get the thrust direction in spacecraft frame.
     * <p>
     * Return a zero vector if there is no thrust for given spacecraft state.
     * @param s current spacecraft state
     * @return thrust direction in spacecraft frame
     */
    default Vector3D getDirection(SpacecraftState s) {
        final Vector3D thrustVector = getThrustVector(s);
        final double   norm         = thrustVector.getNorm();
        if (norm <= Precision.EPSILON) {
            return Vector3D.ZERO;
        }
        return thrustVector.scalarMultiply(1. / norm);
    }

    /** Get the thrust vector in spacecraft frame (N).
     * @param s current spacecraft state
     * @return thrust vector in spacecraft frame (N)
     */
    Vector3D getThrustVector(SpacecraftState s);

    /** Get the flow rate (kg/s).
     * @param s current spacecraft state
     * @return flow rate (kg/s)
     */
    double getFlowRate(SpacecraftState s);

    /** Get the thrust vector in spacecraft frame (N).
     * @param s current spacecraft state
     * @param parameters propulsion model parameters
     * @return thrust vector in spacecraft frame (N)
     */
    Vector3D getThrustVector(SpacecraftState s, double[] parameters);

    /** Get the flow rate (kg/s).
     * @param s current spacecraft state
     * @param parameters propulsion model parameters
     * @return flow rate (kg/s)
     */
    double getFlowRate(SpacecraftState s, double[] parameters);

    /** Get the thrust vector in spacecraft frame (N).
     * @param s current spacecraft state
     * @param parameters propulsion model parameters
     * @param <T> extends CalculusFieldElement&lt;T&gt;
     * @return thrust vector in spacecraft frame (N)
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(FieldSpacecraftState<T> s, T[] parameters);

    /** Get the flow rate (kg/s).
     * @param s current spacecraft state
     * @param parameters propulsion model parameters
     * @param <T> extends CalculusFieldElement&lt;T&gt;
     * @return flow rate (kg/s)
     */
    <T extends CalculusFieldElement<T>> T getFlowRate(FieldSpacecraftState<T> s, T[] parameters);

    /** {@inheritDoc}
     * Acceleration is computed here using the thrust vector in S/C frame.
     */
    @Override
    default Vector3D getAcceleration(SpacecraftState s,
                                    final Attitude maneuverAttitude,
                                    double[] parameters) {

        final Vector3D thrustVector = getThrustVector(s, parameters);
        final double thrust = thrustVector.getNorm();
        if (thrust == 0) {
            return Vector3D.ZERO;
        }
        final Vector3D direction = thrustVector.normalize();

        // Compute thrust acceleration in inertial frame
        // It seems under-efficient to rotate direction and apply thrust
        // instead of just rotating the whole thrust vector itself.
        // However it has to be done that way to avoid numerical discrepancies with legacy tests.
        return new Vector3D(thrust / s.getMass(),
                            maneuverAttitude.getRotation().applyInverseTo(direction));
    }

    /** {@inheritDoc}
     * Acceleration is computed here using the thrust vector in S/C frame.
     */
    @Override
    default <T extends CalculusFieldElement<T>> FieldVector3D<T> getAcceleration(FieldSpacecraftState<T> s,
                                                                            final FieldAttitude<T> maneuverAttitude,
                                                                            T[] parameters) {
        // Extract thrust & direction from thrust vector
        final FieldVector3D<T> thrustVector = getThrustVector(s, parameters);
        final T thrust = thrustVector.getNorm();
        if (thrust.isZero()) {
            return FieldVector3D.getZero(s.getDate().getField());
        }
        final FieldVector3D<T> direction = thrustVector.normalize();

        // Compute thrust acceleration in inertial frame
        // It seems under-efficient to rotate direction and apply thrust
        // instead of just rotating the whole thrust vector itself.
        // However it has to be done that way to avoid numerical discrepancies with legacy tests.
        return new FieldVector3D<>(thrust.divide(s.getMass()),
                        maneuverAttitude.getRotation().applyInverseTo(direction));
    }

    /** {@inheritDoc}
     * Mass derivatives are directly extracted here from the flow rate value.
     */
    @Override
    default double getMassDerivatives(SpacecraftState s, double[] parameters) {
        return getFlowRate(s, parameters);
    }

    /** {@inheritDoc}
     * Mass derivatives are directly extracted here from the flow rate value.
     */
    @Override
    default <T extends CalculusFieldElement<T>> T getMassDerivatives(FieldSpacecraftState<T> s, T[] parameters) {
        return getFlowRate(s, parameters);
    }

}
