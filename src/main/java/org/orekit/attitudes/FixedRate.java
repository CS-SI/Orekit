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
package org.orekit.attitudes;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;


/**
 * This class handles a simple attitude provider at constant rate around a fixed axis.
 * <p>This attitude provider is a simple linear extrapolation from an initial
 * orientation, a rotation axis and a rotation rate. All this elements can be
 * specified as a simple {@link Attitude reference attitude}.</p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 */
public class FixedRate implements AttitudeProvider {

    /** Reference attitude.  */
    private final Attitude referenceAttitude;

    /** Creates a new instance.
     * @param referenceAttitude attitude at reference date
     */
    public FixedRate(final Attitude referenceAttitude) {
        this.referenceAttitude = referenceAttitude;
    }

    /** {@inheritDoc} */
    @Override
    public Rotation getAttitudeRotation(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                        final Frame frame) {
        final Rotation rotation = getShiftedAttitude(date).getRotation();
        final StaticTransform transform = referenceAttitude.getReferenceFrame().getStaticTransformTo(frame, date);
        return rotation.compose(transform.getRotation(), RotationConvention.FRAME_TRANSFORM);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) {
        final Attitude shifted = getShiftedAttitude(date);
        return shifted.withReferenceFrame(frame);
    }

    /**
     * Get shifted reference attitude.
     * @param date date of shift
     * @return shifted attitude
     */
    private Attitude getShiftedAttitude(final AbsoluteDate date) {
        final double timeShift = date.durationFrom(referenceAttitude.getDate());
        return referenceAttitude.shiftedBy(timeShift);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                    final FieldAbsoluteDate<T> date,
                                                                                    final Frame frame) {
        final FieldRotation<T> rotation = getShiftedAttitude(date).getRotation();
        final FieldStaticTransform<T> transform = referenceAttitude.getReferenceFrame().getStaticTransformTo(frame, date);
        return rotation.compose(transform.getRotation(), RotationConvention.FRAME_TRANSFORM);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                            final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {
        final FieldAttitude<T> shifted = getShiftedAttitude(date);
        return shifted.withReferenceFrame(frame);
    }

    /**
     * Get shifted reference attitude.
     * @param date date of shift
     * @param <T> field type
     * @return shifted attitude
     */
    private <T extends CalculusFieldElement<T>> FieldAttitude<T> getShiftedAttitude(final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final T timeShift = date.durationFrom(referenceAttitude.getDate());
        return new FieldAttitude<>(field, referenceAttitude).shiftedBy(timeShift);
    }

    /** Get the reference attitude.
     * @return reference attitude
     */
    public Attitude getReferenceAttitude() {
        return referenceAttitude;
    }

}
