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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Provider using simple {@link PVCoordinates#shiftedBy(double)}  shiftedBy} and frame transforms for evolution.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class ShiftingPVCoordinatesProvider implements PVCoordinatesProvider {

    /** Reference coordinates. */
    private final TimeStampedPVCoordinates referencePV;

    /** Frame in which {@link #referencePV} is defined. */
    private final Frame referenceFrame;

    /** Simple constructor.
     * @param referencePV reference coordinates
     * @param referenceFrame frame in which {@code reference} is defined
     */
    public ShiftingPVCoordinatesProvider(final TimeStampedPVCoordinates referencePV,
                                         final Frame referenceFrame) {
        this.referencePV    = referencePV;
        this.referenceFrame = referenceFrame;
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date,
                                                     final Frame frame) {
        final TimeStampedPVCoordinates shifted = referencePV.shiftedBy(date.durationFrom(referencePV));
        return referenceFrame.getTransformTo(frame, date).transformPVCoordinates(shifted);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        final double duration = date.durationFrom(referencePV);
        final Vector3D position = referencePV.getPosition().add((referencePV.getVelocity().add(referencePV.getAcceleration().scalarMultiply(duration / 2))).scalarMultiply(duration));
        return referenceFrame.getStaticTransformTo(frame, date).transformPosition(position);
    }
}
