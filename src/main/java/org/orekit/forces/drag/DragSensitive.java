/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.forces.drag;

import java.io.Serializable;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;

/** Interface for spacecraft that are sensitive to atmospheric drag forces.
 *
 * @see DragForce
 * @author Luc Maisonobe
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public interface DragSensitive extends Serializable {

    /** Compute the acceleration due to drag.
     * <p>
     * The computation includes all spacecraft specific characteristics
     * like shape, area and coefficients.
     * </p>
     * @param state current state information: date, kinematics, attitude
     * @param density atmospheric density at spacecraft position
     * @param relativeVelocity relative velocity of atmosphere with respect to spacecraft,
     * in the same inertial frame as spacecraft orbit (m/s)
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s<sup>2</sup>)
     * @throws OrekitException if acceleration cannot be computed
     */
    Vector3D dragAcceleration(SpacecraftState state, double density, Vector3D relativeVelocity)
        throws OrekitException;

    /** Set the drag coefficient.
     *  @param value drag coefficient
     */
    void setDragCoefficient(double value);

    /** Get the drag coefficient.
     * @return drag coefficient
     */
    double getDragCoefficient();

}
