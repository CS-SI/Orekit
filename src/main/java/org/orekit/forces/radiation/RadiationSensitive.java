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
package org.orekit.forces.radiation;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

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

    /** Get the drivers for supported parameters.
     * @return parameters drivers
     * @since 8.0
     */
    ParameterDriver[] getRadiationParametersDrivers();

    /** Compute the acceleration due to radiation pressure.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass current mass
     * @param flux radiation flux in the same inertial frame as spacecraft orbit
     * @param parameters values of the force model parameters
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     */
    Vector3D radiationPressureAcceleration(AbsoluteDate date, Frame frame, Vector3D position,
                                           Rotation rotation, double mass, Vector3D flux,
                                           double[] parameters);

    /** Compute the acceleration due to radiation pressure.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass current mass
     * @param flux radiation flux in the same inertial frame as spacecraft orbit
     * @param parameters values of the force model parameters
     * @param <T> extends RealFieldElement
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     */
    <T extends RealFieldElement<T>> FieldVector3D<T> radiationPressureAcceleration(FieldAbsoluteDate<T> date, Frame frame, FieldVector3D<T> position,
                                                                                   FieldRotation<T> rotation, T mass, FieldVector3D<T> flux,
                                                                                   T[] parameters);
}
