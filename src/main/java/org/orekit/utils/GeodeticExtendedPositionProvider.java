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
package org.orekit.utils;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;


/** Position provider from a given fixed point w.r.t. to a body.
 * @author Romain Serra
 * @since 14.0
 * @see PVCoordinatesProvider
 * @see GeodeticPoint
 */
public class GeodeticExtendedPositionProvider implements ExtendedPositionProvider {

    /** Body shape on which the local point is defined. */
    private final BodyShape bodyShape;

    /** Geodetic point. */
    private final GeodeticPoint geodeticPoint;

    /** Cartesian point corresponding to geodetic one. */
    private final Vector3D cartesianPoint;

    /** Simple constructor.
     * @param bodyShape body shape on which the local point is defined
     * @param point local surface point
     */
    public GeodeticExtendedPositionProvider(final BodyShape bodyShape, final GeodeticPoint point) {
        this.bodyShape = bodyShape;
        this.geodeticPoint = point;
        this.cartesianPoint = bodyShape.transform(point);
    }

    /** Simple constructor.
     * @param bodyShape body shape on which the local point is defined
     * @param point local surface point
     */
    public GeodeticExtendedPositionProvider(final BodyShape bodyShape, final Vector3D point) {
        this.bodyShape = bodyShape;
        this.geodeticPoint = bodyShape.transform(point, bodyShape.getBodyFrame(), AbsoluteDate.ARBITRARY_EPOCH);
        this.cartesianPoint = point;
    }

    /** Get the body shape on which the local point is defined.
     * @return body shape on which the local point is defined
     */
    public BodyShape getBodyShape() {
        return bodyShape;
    }

    /** Get the surface point.
     * @return surface point
     */
    public GeodeticPoint getGeodeticPoint() {
        return geodeticPoint;
    }

    /** Get the surface point.
     * @return surface point in body frame
     */
    public Vector3D getCartesianPoint() {
        return cartesianPoint;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        if (bodyShape.getBodyFrame() == frame) {
            return cartesianPoint;
        }
        return bodyShape.getBodyFrame().getStaticTransformTo(frame, date).transformPosition(cartesianPoint);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {
        if (bodyShape.getBodyFrame() == frame) {
            return new FieldVector3D<>(date.getField(), cartesianPoint);
        }
        return bodyShape.getBodyFrame().getStaticTransformTo(frame, date).transformPosition(cartesianPoint);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Frame frame) {
        if (bodyShape.getBodyFrame() == frame) {
            return Vector3D.ZERO;
        }
        final PVCoordinates pvCoordinates = new PVCoordinates(cartesianPoint);
        return bodyShape.getBodyFrame().getKinematicTransformTo(frame, date).transformOnlyPV(pvCoordinates).getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getVelocity(final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {
        if (bodyShape.getBodyFrame() == frame) {
            return FieldVector3D.getZero(date.getField());
        }
        final Field<T> field = date.getField();
        final FieldVector3D<T> fieldCartesianPoint = new FieldVector3D<>(field, cartesianPoint);
        final FieldPVCoordinates<T> fieldPVCoordinates = new FieldPVCoordinates<>(fieldCartesianPoint, FieldVector3D.getZero(field));
        return bodyShape.getBodyFrame().getKinematicTransformTo(frame, date).transformOnlyPV(fieldPVCoordinates).getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        final TimeStampedPVCoordinates pvCoordinates = new TimeStampedPVCoordinates(date, cartesianPoint, Vector3D.ZERO);
        if (bodyShape.getBodyFrame() == frame) {
            return pvCoordinates;
        }
        return bodyShape.getBodyFrame().getTransformTo(frame, date).transformPVCoordinates(pvCoordinates);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date,
                                                                                                 final Frame frame) {
        final Field<T> field = date.getField();
        final FieldVector3D<T> fieldCartesianPoint = new FieldVector3D<>(field, cartesianPoint);
        final TimeStampedFieldPVCoordinates<T> fieldPVCoordinates = new TimeStampedFieldPVCoordinates<>(date,
                new FieldPVCoordinates<>(fieldCartesianPoint, FieldVector3D.getZero(field)));
        if (bodyShape.getBodyFrame() == frame) {
            return fieldPVCoordinates;
        }
        return bodyShape.getBodyFrame().getTransformTo(frame, date).transformPVCoordinates(fieldPVCoordinates);
    }

}
