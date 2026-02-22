/* Copyright 2022-2026 Romain Serra
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
package org.orekit.utils;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;


/** Fixed-position (in a given frame) provider. The point might be fictitious.
 * @author Romain Serra
 * @since 14.0
 * @see PVCoordinatesProvider
 */
public class ConstantPositionProvider implements ExtendedPositionProvider {

    /** Constant Cartesian position vector. */
    private final Vector3D constantPosition;

    /** Reference frame. */
    private final Frame referenceFrame;

    /** Simple constructor.
     * @param constantPosition constant position vector in input frame
     * @param referenceFrame frame where constant position is given
     */
    public ConstantPositionProvider(final Vector3D constantPosition, final Frame referenceFrame) {
        this.constantPosition = constantPosition;
        this.referenceFrame = referenceFrame;
    }

    /**
     * Getter for constant position in reference frame.
     * @return position in reference frame
     */
    public Vector3D getConstantPosition() {
        return constantPosition;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        if (referenceFrame == frame) {
            return constantPosition;
        }
        return referenceFrame.getStaticTransformTo(frame, date).transformPosition(constantPosition);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {
        if (referenceFrame == frame) {
            return new FieldVector3D<>(date.getField(), constantPosition);
        }
        return referenceFrame.getStaticTransformTo(frame, date).transformPosition(constantPosition);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Frame frame) {
        if (referenceFrame == frame) {
            return Vector3D.ZERO;
        }
        final PVCoordinates pvCoordinates = new PVCoordinates(constantPosition);
        return referenceFrame.getKinematicTransformTo(frame, date).transformOnlyPV(pvCoordinates).getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getVelocity(final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {
        if (referenceFrame == frame) {
            return FieldVector3D.getZero(date.getField());
        }
        final Field<T> field = date.getField();
        final FieldVector3D<T> fieldconstantPosition = new FieldVector3D<>(field, constantPosition);
        final FieldPVCoordinates<T> fieldPVCoordinates = new FieldPVCoordinates<>(fieldconstantPosition, FieldVector3D.getZero(field));
        return referenceFrame.getKinematicTransformTo(frame, date).transformOnlyPV(fieldPVCoordinates).getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        final TimeStampedPVCoordinates pvCoordinates = new TimeStampedPVCoordinates(date, constantPosition, Vector3D.ZERO);
        if (referenceFrame == frame) {
            return pvCoordinates;
        }
        return referenceFrame.getTransformTo(frame, date).transformPVCoordinates(pvCoordinates);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date,
                                                                                                 final Frame frame) {
        final Field<T> field = date.getField();
        final FieldVector3D<T> fieldconstantPosition = new FieldVector3D<>(field, constantPosition);
        final TimeStampedFieldPVCoordinates<T> fieldPVCoordinates = new TimeStampedFieldPVCoordinates<>(date,
                new FieldPVCoordinates<>(fieldconstantPosition, FieldVector3D.getZero(field)));
        if (referenceFrame == frame) {
            return fieldPVCoordinates;
        }
        return referenceFrame.getTransformTo(frame, date).transformPVCoordinates(fieldPVCoordinates);
    }

}
