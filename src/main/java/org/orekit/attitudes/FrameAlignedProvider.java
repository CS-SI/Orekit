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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.frames.FieldTransform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;


/**
 * This class handles an attitude provider aligned with a frame or a fixed offset to it.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 */
public class FrameAlignedProvider implements AttitudeProvider {

    /** Fixed satellite frame. */
    private final Frame satelliteFrame;

    /** Creates new instance.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param rotation rotation from EME2000 to the desired satellite frame
     * @see #FrameAlignedProvider(Rotation, Frame)
     */
    @DefaultDataContext
    public FrameAlignedProvider(final Rotation rotation) {
        this(rotation, DataContext.getDefault().getFrames().getEME2000());
    }

    /**
     * Creates new instance aligned with the given frame.
     *
     * @param frame the reference frame for the attitude.
     */
    public FrameAlignedProvider(final Frame frame) {
        // it is faster to use the frame directly here rather than call the other
        // constructor because of the == shortcut in frame.getTransformTo
        this.satelliteFrame = frame;
    }

    /**
     * Creates new instance with a fixed attitude in the given frame.
     *
     * @param rotation  rotation from {@code reference} to the desired satellite frame
     * @param reference frame for {@code rotation}.
     * @since 10.1
     */
    public FrameAlignedProvider(final Rotation rotation,
                                final Frame reference) {
        satelliteFrame =
            new Frame(reference,
                      new Transform(AbsoluteDate.ARBITRARY_EPOCH, rotation), null, false);
    }

    /**
     * Creates an attitude provider aligned with the given frame.
     *
     * <p>This attitude provider returned by this method is designed to be as fast as
     * possible for when attitude is irrelevant while still being a valid implementation
     * of {@link AttitudeProvider}. To ensure good performance the specified attitude
     * reference frame should be the same frame used for propagation so that computing the
     * frame transformation is trivial.
     *
     * @param satelliteFrame with which the satellite is aligned.
     * @return new attitude provider aligned with the given frame.
     * @since 11.0
     */
    public static AttitudeProvider of(final Frame satelliteFrame) {
        return new FrameAlignedProvider(satelliteFrame);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date,
                                final Frame frame) {
        final Transform t = frame.getTransformTo(satelliteFrame, date);
        return new Attitude(date, frame, t.getRotation(), t.getRotationRate(), t.getRotationAcceleration());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>>FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                           final FieldAbsoluteDate<T> date,
                                                                           final Frame frame) {
        final FieldTransform<T> t = frame.getTransformTo(satelliteFrame, date);
        return new FieldAttitude<>(date, frame, t.getRotation(), t.getRotationRate(), t.getRotationAcceleration());
    }

    /** {@inheritDoc} */
    @Override
    public Rotation getAttitudeRotation(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
        return frame.getStaticTransformTo(satelliteFrame, date).getRotation();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                    final FieldAbsoluteDate<T> date,
                                                                                    final Frame frame) {
        return frame.getStaticTransformTo(satelliteFrame, date).getRotation();
    }

}
