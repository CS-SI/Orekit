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
package org.orekit.forces.radiation;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Interface for spacecraft that are sensitive to radiation pressure forces.
 *
 * @see SolarRadiationPressure
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public interface RadiationSensitive {

    /** Parameter name for absorption coefficient. */
    String ABSORPTION_COEFFICIENT = "absorption coefficient";

    /** Parameter name for reflection coefficient. */
    String REFLECTION_COEFFICIENT = "reflection coefficient";

    /** Compute the acceleration due to radiation pressure.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass current mass
     * @param flux radiation flux in the same inertial frame as spacecraft orbit
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     * @throws OrekitException if acceleration cannot be computed
     */
    Vector3D radiationPressureAcceleration(AbsoluteDate date, Frame frame, Vector3D position,
                                           Rotation rotation, double mass, Vector3D flux)
        throws OrekitException;

    /** Compute the acceleration due to radiation pressure, with state derivatives.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass spacecraft mass
     * @param flux radiation flux in the same inertial frame as spacecraft orbit
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     * @throws OrekitException if acceleration cannot be computed
     */
    FieldVector3D<DerivativeStructure> radiationPressureAcceleration(AbsoluteDate date, Frame frame, FieldVector3D<DerivativeStructure> position,
                                                                     FieldRotation<DerivativeStructure> rotation, DerivativeStructure mass,
                                                                     FieldVector3D<DerivativeStructure> flux)
        throws OrekitException;

    /** Compute the acceleration due to radiation pressure, with parameters derivatives.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass current mass
     * @param flux radiation flux in the same inertial frame as spacecraft orbit
     * @param paramName name of the parameter with respect to which derivatives are required
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     * @throws OrekitException if acceleration cannot be computed
     */
    FieldVector3D<DerivativeStructure> radiationPressureAcceleration(AbsoluteDate date, Frame frame, Vector3D position,
                                                                     Rotation rotation, double mass, Vector3D flux,
                                                                     String paramName)
        throws OrekitException;

    /** Set the absorption coefficient.
     * @param value absorption coefficient
     */
    void setAbsorptionCoefficient(double value);

    /** Get the absorption coefficient.
     * @return absorption coefficient
     */
    double getAbsorptionCoefficient();

    /** Set the specular reflection coefficient.
     * @param value specular reflection coefficient
     */
    void setReflectionCoefficient(double value);

    /** Get the specular reflection coefficient.
     * @return reflection coefficient
     */
    double getReflectionCoefficient();

}
