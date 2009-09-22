/* Copyright 2002-2009 CS Communication & Systèmes
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
package org.orekit.forces.radiation;

import java.io.Serializable;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;

/** Interface for spacecraft that are sensitive to radiation pressure forces.
 *
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public interface RadiationSensitive extends Serializable {

    /** Get the cross section sensitive to radiation pressure.
     * @param state current state information: date, kinematics, attitude
     * @param direction direction of the light flux in the spacecraft frame (unit vector)
     * @return surface (m<sup>2</sup>)
     * @throws OrekitException if cross section cannot be computed
     */
    double getRadiationCrossSection(SpacecraftState state, Vector3D direction)
        throws OrekitException;

    /** Get the absorption coefficients vector.
     * @param state current state information: date, kinematics, attitude
     * @param direction direction of the light flux in the spacecraft frame (unit vector)
     * @return absorption coefficients vector in the spacecraft frame
     * the norm of the vector should be equal to the desired absorption coefficient
     * @throws OrekitException if absorption coefficients vector cannot be computed
     */
    Vector3D getAbsorptionCoef(SpacecraftState state, Vector3D direction)
        throws OrekitException;

    /** Get the specular reflection coefficients vector.
     * @param state current state information: date, kinematics, attitude
     * @param direction direction of the light flux in the spacecraft frame (unit vector)
     * @return specular reflection coefficients vector in the spacecraft frame
     * the norm of the vector should be equal to the desired reflection coefficient
     * @throws OrekitException if reflection coefficients vector cannot be computed
     */
    Vector3D getReflectionCoef(SpacecraftState state, Vector3D direction)
        throws OrekitException;

}
