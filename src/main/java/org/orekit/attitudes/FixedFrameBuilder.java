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
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;


/** Builder that assumes angular coordinates are given in a fixed frame.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class FixedFrameBuilder implements AttitudeBuilder {

    /** Reference frame for raw attitudes. */
    private final Frame referenceFrame;

    /** Creates new instance.
     * @param referenceFrame reference frame for raw attitudes
     */
    public FixedFrameBuilder(final Frame referenceFrame) {
        this.referenceFrame = referenceFrame;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude build(final Frame frame, final PVCoordinatesProvider pvProv,
                          final TimeStampedAngularCoordinates rawAttitude) {

        final AbsoluteDate date = rawAttitude.getDate();
        final Transform    t    = frame.getTransformTo(referenceFrame, date);
        final TimeStampedAngularCoordinates frame2Ref =
                        new TimeStampedAngularCoordinates(date,
                                                          t.getRotation(),
                                                          t.getRotationRate(),
                                                          t.getRotationAcceleration());

        return new Attitude(frame, rawAttitude.addOffset(frame2Ref));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldAttitude<T>
        build(final Frame frame, final FieldPVCoordinatesProvider<T> pvProv,
              final TimeStampedFieldAngularCoordinates<T> rawAttitude) {

        final FieldAbsoluteDate<T> date = rawAttitude.getDate();
        final FieldTransform<T>    t    = frame.getTransformTo(referenceFrame, date);
        final TimeStampedFieldAngularCoordinates<T> frame2Ref =
                        new TimeStampedFieldAngularCoordinates<>(date,
                                                                 t.getRotation(),
                                                                 t.getRotationRate(),
                                                                 t.getRotationAcceleration());

        return new FieldAttitude<>(frame, rawAttitude.addOffset(frame2Ref));

    }

}
