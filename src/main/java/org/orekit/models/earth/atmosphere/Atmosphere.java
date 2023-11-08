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
package org.orekit.models.earth.atmosphere;

import java.io.Serializable;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.FieldTransform;
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
     */
    double getDensity(AbsoluteDate date, Vector3D position, Frame frame);

    /** Get the local density.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @param <T> instance of CalculusFieldElement
     * @return local density (kg/m³)
     */
    <T extends CalculusFieldElement<T>> T getDensity(FieldAbsoluteDate<T> date, FieldVector3D<T> position, Frame frame);

    /** Get the inertial velocity of atmosphere molecules.
     * <p>By default, atmosphere is supposed to have a null
     * velocity in the central body frame.</p>
     *
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return velocity (m/s) (defined in the same frame as the position)
     */
    default Vector3D getVelocity(AbsoluteDate date, Vector3D position, Frame frame) {
        final Transform     bodyToFrame = getFrame().getTransformTo(frame, date);
        final Vector3D      posInBody   = bodyToFrame.toStaticTransform().getInverse().transformPosition(position);
        final PVCoordinates pvBody      = new PVCoordinates(posInBody, Vector3D.ZERO);
        final PVCoordinates pvFrame     = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

    /** Get the inertial velocity of atmosphere molecules.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @param <T> instance of CalculusFieldElement
     * @return velocity (m/s) (defined in the same frame as the position)
     */
    default <T extends CalculusFieldElement<T>> FieldVector3D<T> getVelocity(FieldAbsoluteDate<T> date, FieldVector3D<T> position, Frame frame) {
        final FieldTransform<T>     bodyToFrame = getFrame().getTransformTo(frame, date);
        final FieldVector3D<T>      posInBody   = bodyToFrame.toStaticTransform().getInverse().transformPosition(position);
        final FieldPVCoordinates<T> pvBody      = new FieldPVCoordinates<>(posInBody,
                FieldVector3D.getZero(date.getField()));
        final FieldPVCoordinates<T> pvFrame     = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

}
