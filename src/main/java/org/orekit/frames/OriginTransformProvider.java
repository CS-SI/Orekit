/* Contributed in the public domain.
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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.orekit.files.ccsds.definitions.ModifiedFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Transform provider that shifts the origin and keeps the orientation of
 * another {@link Frame}.
 *
 * @author Evan M. Ward
 * @see ModifiedFrame
 * @since 14.0
 */
public class OriginTransformProvider implements TransformProvider {

    /** The new origin. */
    private final ExtendedPositionProvider origin;

    /** The original frame, specifying the orientation. */
    private final Frame frame;

    /**
     * Create a transform provider to change the origin of an existing frame.
     *
     * @param frame  the existing frame that specifies the orientation.
     * @param origin the new origin.
     */
    public OriginTransformProvider(final ExtendedPositionProvider origin,
                                   final Frame frame) {
        this.origin = origin;
        this.frame = frame;
    }

    @Override
    public Transform getTransform(final AbsoluteDate date) {
        return new Transform(date, origin.getPVCoordinates(date, frame).negate());
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(
            final FieldAbsoluteDate<T> date) {
        return new FieldTransform<>(
                date,
                origin.getPVCoordinates(date, frame).negate());
    }

    @Override
    public KinematicTransform getKinematicTransform(final AbsoluteDate date) {
        return KinematicTransform
                .of(date, origin.getPVCoordinates(date, frame).negate());
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldKinematicTransform<T>
        getKinematicTransform(final FieldAbsoluteDate<T> date) {

        return FieldKinematicTransform
                .of(date, origin.getPVCoordinates(date, frame).negate());
    }

    @Override
    public StaticTransform getStaticTransform(final AbsoluteDate date) {
        return StaticTransform
                .of(date, origin.getPosition(date, frame).negate());
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T>
        getStaticTransform(final FieldAbsoluteDate<T> date) {

        return FieldStaticTransform
                .of(date, origin.getPosition(date, frame).negate());
    }
}
