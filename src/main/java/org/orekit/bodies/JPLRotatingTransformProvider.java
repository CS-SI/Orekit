/* Copyright 2002-2026 CS Group
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
package org.orekit.bodies;

import java.util.concurrent.TimeUnit;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeOffset;

/** Class for JPL body-fixed frame transform providers.
 * @author Luc Maisonobe
 * @author Romain Serra
 * @since 13.1.8
 */
class JPLRotatingTransformProvider implements TransformProvider {

    /** Time step for finite differences. */
    private static final int DT = 10;

    /** IAU pole. */
    private final IAUPole iauPole;

    /**
     * Constructor.
     * @param iauPole IAU pole
     */
    JPLRotatingTransformProvider(final IAUPole iauPole) {
        this.iauPole       = iauPole;
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {
        final TimeOffset dt = new TimeOffset(DT, TimeUnit.SECONDS);
        final double w0 = iauPole.getPrimeMeridianAngle(date);
        final double w1 = iauPole.getPrimeMeridianAngle(date.shiftedBy(dt));
        return new Transform(date, new Rotation(Vector3D.PLUS_K, w0, RotationConvention.FRAME_TRANSFORM),
                new Vector3D((w1 - w0) / dt.toDouble(), Vector3D.PLUS_K));
    }

    /** {@inheritDoc} */
    @Override
    public StaticTransform getStaticTransform(final AbsoluteDate date) {
        final double w0 = iauPole.getPrimeMeridianAngle(date);
        return StaticTransform.of(date, new Rotation(Vector3D.PLUS_K, w0, RotationConvention.FRAME_TRANSFORM));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        final TimeOffset dt = new TimeOffset(DT, TimeUnit.SECONDS);
        final T w0 = iauPole.getPrimeMeridianAngle(date);
        final T w1 = iauPole.getPrimeMeridianAngle(date.shiftedBy(dt));
        return new FieldTransform<>(date,
                new FieldRotation<>(FieldVector3D.getPlusK(date.getField()), w0, RotationConvention.FRAME_TRANSFORM),
                new FieldVector3D<>(w1.subtract(w0).divide(dt.toDouble()), Vector3D.PLUS_K));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {
        final T w0 = iauPole.getPrimeMeridianAngle(date);
        return FieldStaticTransform.of(date,
                new FieldRotation<>(FieldVector3D.getPlusK(date.getField()), w0, RotationConvention.FRAME_TRANSFORM));
    }

}
