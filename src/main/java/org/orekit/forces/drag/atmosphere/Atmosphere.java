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
package org.orekit.forces.drag.atmosphere;

import java.io.Serializable;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;


/** Interface for atmospheric models.
 * @author Luc Maisonobe
 */
public interface Atmosphere extends Serializable {

    /** Get the frame of the central body.
     * @return frame of the central body.
     * @since 6.0
     */
    Frame getFrame();

    /** Get the local density.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return local density (kg/m³)
     * @exception OrekitException if date is out of range of solar activity model
     * or if some frame conversion cannot be performed
     */
    double getDensity(AbsoluteDate date, Vector3D position, Frame frame)
        throws OrekitException;

    /** Get the local density.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @param <T> instance of RealFieldElement
     * @return local density (kg/m³)
     * @exception OrekitException if date is out of range of solar activity model
     * or if some frame conversion cannot be performed
     */
    <T extends RealFieldElement<T>> T getDensity(FieldAbsoluteDate<T> date, FieldVector3D<T> position, Frame frame)
        throws OrekitException;

    /** Get the inertial velocity of atmosphere molecules.
     * <p>By default, atmosphere is supposed to have a null
     * velocity in the central body frame.</p>
     *
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return velocity (m/s) (defined in the same frame as the position)
     * @exception OrekitException if some conversion cannot be performed
     */
    default Vector3D getVelocity(AbsoluteDate date, Vector3D position, Frame frame)
        throws OrekitException {
        final Transform bodyToFrame = getFrame().getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

    /** Get the inertial velocity of atmosphere molecules.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @param <T> instance of RealFieldElement
     * @return velocity (m/s) (defined in the same frame as the position)
     * @exception OrekitException if some conversion cannot be performed
     */
    default <T extends RealFieldElement<T>> FieldVector3D<T> getVelocity(FieldAbsoluteDate<T> date, FieldVector3D<T> position, Frame frame)
        throws OrekitException {
        final T zero = position.getX().getField().getZero();
        final Transform bodyToFrame = getFrame().getTransformTo(frame, date.toAbsoluteDate());
        final FieldVector3D<T> posInBody    = bodyToFrame.getInverse().transformPosition(position);
        final FieldVector3D<T> vectorZero = new FieldVector3D<T>(zero, zero, zero);
        final FieldPVCoordinates<T> pvBody  = new FieldPVCoordinates<T>(posInBody, vectorZero);
        final FieldPVCoordinates<T> pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

}
