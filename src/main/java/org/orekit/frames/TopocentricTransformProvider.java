/* Copyright 2022-2026 Romain Serra
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
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Topocentric frame transform provider (towards parent body-fixed, body-centered).
 * @author Romain Serra
 * @since 14.0
 */
public class TopocentricTransformProvider implements TransformProvider {

    /** Geodetic point. */
    private final GeodeticPoint geodeticPoint;

    /** Body shape where point is attached. */
    private final BodyShape bodyShape;

    /**
     * Constructor.
     * @param geodeticPoint geodetic point
     * @param bodyShape body shape
     */
    public TopocentricTransformProvider(final GeodeticPoint geodeticPoint, final BodyShape bodyShape) {
        this.geodeticPoint = geodeticPoint;
        this.bodyShape = bodyShape;
    }

    /**
     * Getter for the body shape.
     * @return shape
     */
    public BodyShape getBodyShape() {
        return bodyShape;
    }

    @Override
    public StaticTransform getStaticTransform(final AbsoluteDate date) {
        return StaticTransform.of(date, buildTranslation(), buildRotation(geodeticPoint));
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final FieldGeodeticPoint<T> fieldGeodeticPoint = new FieldGeodeticPoint<>(field, geodeticPoint);
        return FieldStaticTransform.of(date, buildFieldTranslation(bodyShape, fieldGeodeticPoint),
                buildFieldRotation(fieldGeodeticPoint));
    }

    @Override
    public Transform getTransform(final AbsoluteDate date) {
        return new Transform(date, buildTranslation(), buildRotation(geodeticPoint));
    }

    private Vector3D buildTranslation() {
        return bodyShape.transform(geodeticPoint).negate();
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final FieldGeodeticPoint<T> fieldGeodeticPoint = new FieldGeodeticPoint<>(field, geodeticPoint);
        return getTransform(bodyShape, date, fieldGeodeticPoint);
    }

    /**
     * Method building a Field transform from body shape and point.
     * @param shape body shape
     * @param date date
     * @param fieldGeodeticPoint geodetic point
     * @return transform
     * @param <T> field type
     */
    public static  <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final BodyShape shape,
                                                                                      final FieldAbsoluteDate<T> date,
                                                                                      final FieldGeodeticPoint<T> fieldGeodeticPoint) {
        return new FieldTransform<>(date, buildFieldTranslation(shape, fieldGeodeticPoint),
                buildFieldRotation(fieldGeodeticPoint));
    }

    private static <T extends CalculusFieldElement<T>> FieldVector3D<T> buildFieldTranslation(final BodyShape shape,
                                                                                              final FieldGeodeticPoint<T> fieldGeodeticPoint) {
        return shape.transform(fieldGeodeticPoint).negate();
    }

    private static Rotation buildRotation(final GeodeticPoint point) {
        return new Rotation(point.getEast(), point.getZenith(), Vector3D.PLUS_I, Vector3D.PLUS_K);
    }

    private static <W extends CalculusFieldElement<W>> FieldRotation<W> buildFieldRotation(final FieldGeodeticPoint<W> fieldGeodeticPoint) {
        final Field<W> field = fieldGeodeticPoint.getAltitude().getField();
        return new FieldRotation<>(fieldGeodeticPoint.getEast(), fieldGeodeticPoint.getZenith(),
                FieldVector3D.getPlusI(field), FieldVector3D.getPlusK(field));
    }
}
