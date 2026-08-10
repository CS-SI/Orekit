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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldAngularCoordinates;

/**
 * Transform provider used to match the orientation of a given frame.
 *
 * @author Evan M. Ward
 * @see OriginTransformProvider
 * @see org.orekit.files.ccsds.definitions.ModifiedFrame
 * @since 14.0
 */
public class AngularTransformProvider implements TransformProvider {

    /** Parent frame. */
    private final Frame parent;
    /** Orientation provider. */
    private final Frame orientation;

    /**
     * Construct a transform provide that generates angular transforms from
     * {@code parent} to {@code orientation}. Useful for creating a new frame
     * centered on {@code parent} and with the same orientation as
     * {@code orientation}.
     *
     * @param parent      frame.
     * @param orientation provider.
     */
    public AngularTransformProvider(final Frame parent,
                                    final Frame orientation) {
        this.parent = parent;
        this.orientation = orientation;
    }

    @Override
    public Transform getTransform(final AbsoluteDate date) {
        final AngularCoordinates angular =
                parent.getTransformTo(orientation, date).getAngular();
        return new Transform(date, angular);
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T>
        getTransform(final FieldAbsoluteDate<T> date) {

        final FieldAngularCoordinates<T> angular =
                parent.getTransformTo(orientation, date).getAngular();
        return new FieldTransform<>(date, angular);
    }

    @Override
    public KinematicTransform getKinematicTransform(final AbsoluteDate date) {
        final KinematicTransform xform =
                parent.getKinematicTransformTo(orientation, date);
        return KinematicTransform
                .of(date, xform.getRotation(), xform.getRotationRate());
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldKinematicTransform<T>
        getKinematicTransform(final FieldAbsoluteDate<T> date) {

        final FieldKinematicTransform<T> xform =
                parent.getKinematicTransformTo(orientation, date);
        return FieldKinematicTransform
                .of(date, xform.getRotation(), xform.getRotationRate());
    }

    @Override
    public StaticTransform getStaticTransform(final AbsoluteDate date) {
        final StaticTransform xform =
                parent.getStaticTransformTo(orientation, date);
        return StaticTransform.of(date, xform.getRotation());
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T>
        getStaticTransform(final FieldAbsoluteDate<T> date) {

        final FieldStaticTransform<T> xform =
                parent.getStaticTransformTo(orientation, date);
        return FieldStaticTransform.of(date, xform.getRotation());
    }

}
