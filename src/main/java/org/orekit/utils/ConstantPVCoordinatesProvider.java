/* Copyright 2002-2023 Joseph Reed
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Joseph Reed licenses this file to You under the Apache License, Version 2.0
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
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Provider based on a single point.
 *
 * When {@link #getPVCoordinates(AbsoluteDate, Frame)} is called, the constant
 * point will be translated to the destination frame and returned. This behavior is
 * different than {@link AbsolutePVCoordinates#getPVCoordinates(AbsoluteDate, Frame)} (which
 * uses {@link AbsolutePVCoordinates#shiftedBy(double) shiftedBy()} internally.). Use
 * this class when no shifting should be performed (e.g. representing a fixed point on the ground).
 *
 * @author Joe Reed
 * @since 11.3
 */
public class ConstantPVCoordinatesProvider implements PVCoordinatesProvider {

    /** The position/velocity/acceleration point. */
    private final PVCoordinates pva;

    /** The frame in which pva is defined. */
    private final Frame sourceFrame;

    /** Create the PVCoordinatesProvider from a fixed point in a frame.
     *
     * @param pos the fixed position in the frame
     * @param frame the frame in which {@code pva} is defined
     */
    public ConstantPVCoordinatesProvider(final Vector3D pos, final Frame frame) {
        this(new PVCoordinates(pos), frame);
    }

    /** Create a the provider from a fixed lat/lon/alt on a central body.
     *
     * This method is provided as convienience for
     * {@code new ConstantPVCoordinatesProvider(body.transform(pos), body.getBodyFrame())}.
     *
     * @param pos the position relative to the ellipsoid's surface
     * @param body the reference ellipsoid
     */
    public ConstantPVCoordinatesProvider(final GeodeticPoint pos, final OneAxisEllipsoid body) {
        this(body.transform(pos), body.getBodyFrame());
    }

    /** Create the PVCoordinatesProvider from a fixed point in a frame.
     *
     * @param pva the point in the frame
     * @param frame the frame in which {@code pva} is defined
     */
    public ConstantPVCoordinatesProvider(final PVCoordinates pva, final Frame frame) {
        this.pva = pva;
        this.sourceFrame = frame;
    }

    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        return sourceFrame.getStaticTransformTo(frame, date).transformPosition(pva.getPosition());
    }

    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        final PVCoordinates pv = sourceFrame.getTransformTo(frame, date).transformPVCoordinates(pva);

        return new TimeStampedPVCoordinates(date, pv);
    }
}
