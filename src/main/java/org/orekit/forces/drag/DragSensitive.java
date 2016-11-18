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
package org.orekit.forces.drag;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.LocalizedODEFormats;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Interface for spacecraft that are sensitive to atmospheric drag forces.
 *
 * @see DragForce
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public interface DragSensitive {

    /** Parameter name for drag coefficient enabling jacobian processing. */
    String DRAG_COEFFICIENT = "drag coefficient";

    /** Get the drivers for supported parameters.
     * @return parameters drivers
     * @since 8.0
     */
    ParameterDriver[] getDragParametersDrivers();

    /** Compute the acceleration due to drag.
     * <p>
     * The computation includes all spacecraft specific characteristics
     * like shape, area and coefficients.
     * </p>
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass current mass
     * @param density atmospheric density at spacecraft position
     * @param relativeVelocity relative velocity of atmosphere with respect to spacecraft,
     * in the same inertial frame as spacecraft orbit (m/s)
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     * @throws OrekitException if acceleration cannot be computed
     */
    Vector3D dragAcceleration(AbsoluteDate date, Frame frame, Vector3D position,
                              Rotation rotation, double mass,
                              double density, Vector3D relativeVelocity)
        throws OrekitException;

    /** Compute the acceleration due to drag.
     * <p>
     * The computation includes all spacecraft specific characteristics
     * like shape, area and coefficients.
     * </p>
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass current mass
     * @param density atmospheric density at spacecraft position
     * @param relativeVelocity relative velocity of atmosphere with respect to spacecraft,
     * in the same inertial frame as spacecraft orbit (m/s)
     * @param <T> instance of a RealFieldElement
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     * @throws OrekitException if acceleration cannot be computed
     */
    <T extends RealFieldElement<T>> FieldVector3D<T> dragAcceleration(FieldAbsoluteDate<T> date, Frame frame,
                                                                      FieldVector3D<T> position,
                                                                      FieldRotation<T> rotation, T mass,
                                                                      T density, FieldVector3D<T> relativeVelocity)
        throws OrekitException;
    /** Compute the acceleration due to drag, with state derivatives.
     * <p>
     * The computation includes all spacecraft specific characteristics
     * like shape, area and coefficients.
     * </p>
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass spacecraft mass
     * @param density atmospheric density at spacecraft position
     * @param relativeVelocity relative velocity of atmosphere with respect to spacecraft,
     * in the same inertial frame as spacecraft orbit (m/s)
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     * @throws OrekitException if acceleration cannot be computed
     */
    FieldVector3D<DerivativeStructure> dragAcceleration(AbsoluteDate date, Frame frame, FieldVector3D<DerivativeStructure> position,
                                                        FieldRotation<DerivativeStructure> rotation, DerivativeStructure mass,
                                                        DerivativeStructure density, FieldVector3D<DerivativeStructure> relativeVelocity)
        throws OrekitException;

    /** Compute acceleration due to drag, with parameters derivatives.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass current mass
     * @param density atmospheric density at spacecraft position
     * @param relativeVelocity relative velocity of atmosphere with respect to spacecraft,
     * in the same inertial frame as spacecraft orbit (m/s)
     * @param paramName name of the parameter with respect to which derivatives are required
     * @return spacecraft acceleration in the same inertial frame as spacecraft orbit (m/s²)
     * @exception OrekitException if derivatives cannot be computed
     */
    FieldVector3D<DerivativeStructure> dragAcceleration(AbsoluteDate date, Frame frame, Vector3D position,
                                Rotation rotation, double mass,
                                double density, Vector3D relativeVelocity, String paramName)
        throws OrekitException;

    /** Get the names of the supported parameters.
     * @return parameters names
     * @deprecated as of 8.0, replaced with {@link #getDragParametersDrivers()}
     */
    @Deprecated
    default List<String> getDragParametersNames() {
        final List<String> names = new ArrayList<String>();
        for (final ParameterDriver driver : getDragParametersDrivers()) {
            names.add(driver.getName());
        }
        return names;
    }

    /** Set the absorption coefficient.
     * @param value absorption coefficient
     * @exception OrekitException if parameter cannot be set
     * @deprecated as of 8.0, replaced with {@link #getDragParametersDrivers()}
     */
    @Deprecated
    default void setDragCoefficient(double value) throws OrekitException {
        for (final ParameterDriver driver : getDragParametersDrivers()) {
            if (driver.getName().equals(DRAG_COEFFICIENT)) {
                driver.setValue(value);
                return;
            }
        }
        throw new OrekitException(LocalizedODEFormats.UNKNOWN_PARAMETER, DRAG_COEFFICIENT);
    }

    /** Get the absorption coefficient.
     * @return absorption coefficient
     * @exception OrekitException if parameter cannot be set
     * @deprecated as of 8.0, replaced with {@link #getDragParametersDrivers()}
     */
    @Deprecated
    default double getDragCoefficient() throws OrekitException {
        for (final ParameterDriver driver : getDragParametersDrivers()) {
            if (driver.getName().equals(DRAG_COEFFICIENT)) {
                return driver.getValue();
            }
        }
        throw new OrekitException(LocalizedODEFormats.UNKNOWN_PARAMETER, DRAG_COEFFICIENT);
    }

}
