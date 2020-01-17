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
package org.orekit.attitudes;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;


/**
 * This class handles an inertial attitude provider.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 */
public class InertialProvider implements AttitudeProvider {


    /** Dummy attitude provider, perfectly aligned with the EME2000 frame.
     *
     * <p>This field uses the {@link DataContext#getDefault() default data context}.
     *
     * @see #InertialProvider(Rotation, Frame)
     * @see #InertialProvider(Frame)
     */
    @DefaultDataContext
    public static final InertialProvider EME2000_ALIGNED =
        new InertialProvider(Rotation.IDENTITY);

    /** Fixed satellite frame. */
    private final Frame satelliteFrame;

    /** Creates new instance.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param rotation rotation from EME2000 to the desired satellite frame
     * @see #InertialProvider(Rotation, Frame)
     */
    @DefaultDataContext
    public InertialProvider(final Rotation rotation) {
        this(rotation, DataContext.getDefault().getFrames().getEME2000());
    }

    /**
     * Creates new instance aligned with the given frame.
     *
     * @param frame the reference frame for the attitude.
     */
    public InertialProvider(final Frame frame) {
        this(Rotation.IDENTITY, frame);
    }

    /**
     * Creates new instance with a fixed attitude in the given frame.
     *
     * @param rotation  rotation from {@code reference} to the desired satellite frame
     * @param reference frame for {@code rotation}.
     * @since 10.1
     */
    public InertialProvider(final Rotation rotation,
                            final Frame reference) {
        satelliteFrame =
            new Frame(reference,
                    new Transform(AbsoluteDate.ARBITRARY_EPOCH, rotation), null, false);
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) {
        final Transform t = frame.getTransformTo(satelliteFrame, date);
        return new Attitude(date, frame, t.getRotation(), t.getRotationRate(), t.getRotationAcceleration());
    }

    /** {@inheritDoc} */
    public <T extends RealFieldElement<T>>FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                       final FieldAbsoluteDate<T> date, final Frame frame) {
        final FieldTransform<T> t = frame.getTransformTo(satelliteFrame, date);
        return new FieldAttitude<>(date, frame, t.getRotation(), t.getRotationRate(), t.getRotationAcceleration());
    }

}
