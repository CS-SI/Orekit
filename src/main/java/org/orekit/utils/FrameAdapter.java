/* Copyright 2022-2025 Luc Maisonobe
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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Adapter from {@link Frame} to {@link ExtendedPositionProvider}.
 * <p>
 * The moving point is the origin of the adapted frame.
 * </p>
 * <p>
 * This class is roughly the inverse of {@link ExtendedPositionProviderAdapter}
 * </p>
 * @see ExtendedPositionProviderAdapter
 * @since 12.0
 * @author Luc Maisonobe
 */
public class FrameAdapter implements ExtendedPositionProvider {

    /** Frame whose origin coordinates are desired. */
    private final Frame originFrame;

    /** Simple constructor.
     * @param originFrame frame whose origin coordinates are desired
     */
    public FrameAdapter(final Frame originFrame) {
        this.originFrame = originFrame;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        return originFrame.getStaticTransformTo(frame, date).transformPosition(Vector3D.ZERO);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {
        return originFrame.getStaticTransformTo(frame, date).transformPosition(Vector3D.ZERO);
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        return new TimeStampedPVCoordinates(date,
                originFrame.getTransformTo(frame, date).transformPVCoordinates(PVCoordinates.ZERO));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date,
                                                                                                 final Frame frame) {
        return new TimeStampedFieldPVCoordinates<>(date,
                originFrame.getTransformTo(frame, date).transformPVCoordinates(PVCoordinates.ZERO));
    }
}
