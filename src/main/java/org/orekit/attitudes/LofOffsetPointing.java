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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldLine;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


/**
 * This class provides a default attitude provider.

 * <p>
 * The attitude pointing law is defined by an attitude provider and
 * the satellite axis vector chosen for pointing.
 * <p>
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class LofOffsetPointing extends GroundPointing {

    /** Rotation from local orbital frame. */
    private final AttitudeProvider attitudeLaw;

    /** Body shape. */
    private final BodyShape shape;

    /** Chosen satellite axis for pointing, given in satellite frame. */
    private final Vector3D satPointingVector;

    /** Creates new instance.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param shape Body shape
     * @param attLaw Attitude law
     * @param satPointingVector satellite vector defining the pointing direction
     * @since 7.1
     */
    public LofOffsetPointing(final Frame inertialFrame, final BodyShape shape,
                             final AttitudeProvider attLaw, final Vector3D satPointingVector) {
        super(inertialFrame, shape.getBodyFrame());
        this.shape = shape;
        this.attitudeLaw = attLaw;
        this.satPointingVector = satPointingVector;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) {
        return attitudeLaw.getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                        final FieldAbsoluteDate<T> date, final Frame frame) {
        return attitudeLaw.getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                final AbsoluteDate date, final Frame frame) {

        // sample intersection points in current date neighborhood
        final double h  = 0.1;
        final List<TimeStampedPVCoordinates> sample = new ArrayList<>();
        Transform centralRefToBody = null;
        for (int i = -1; i < 2; ++i) {

            final AbsoluteDate shifted = date.shiftedBy(i * h);

            // transform from specified reference frame to spacecraft frame
            final Transform refToSc =
                            new Transform(shifted,
                                          new Transform(shifted, pvProv.getPVCoordinates(shifted, frame).negate()),
                                          new Transform(shifted, attitudeLaw.getAttitude(pvProv, shifted, frame).getOrientation()));

            // transform from specified reference frame to body frame
            final Transform refToBody = frame.getTransformTo(shape.getBodyFrame(), shifted);
            if (i == 0) {
                centralRefToBody = refToBody;
            }

            sample.add(losIntersectionWithBody(new Transform(shifted, refToSc.getInverse(), refToBody)));

        }

        // use interpolation to compute properly the time-derivatives
        final TimeStampedPVCoordinates targetBody =
                TimeStampedPVCoordinates.interpolate(date, CartesianDerivativesFilter.USE_P, sample);

        // convert back to caller specified frame
        return centralRefToBody.getInverse().transformPVCoordinates(targetBody);

    }

    /** {@inheritDoc} */
    public <T extends RealFieldElement<T>> TimeStampedFieldPVCoordinates<T> getTargetPV(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                        final FieldAbsoluteDate<T> date,
                                                                                        final Frame frame) {

        // sample intersection points in current date neighborhood
        final double h  = 0.1;
        final List<TimeStampedFieldPVCoordinates<T>> sample = new ArrayList<>();
        FieldTransform<T> centralRefToBody = null;
        for (int i = -1; i < 2; ++i) {

            final FieldAbsoluteDate<T> shifted = date.shiftedBy(i * h);

            // transform from specified reference frame to spacecraft frame
            final FieldTransform<T> refToSc =
                            new FieldTransform<>(shifted,
                                                 new FieldTransform<>(shifted, pvProv.getPVCoordinates(shifted, frame).negate()),
                                                 new FieldTransform<>(shifted, attitudeLaw.getAttitude(pvProv, shifted, frame).getOrientation()));

            // transform from specified reference frame to body frame
            final FieldTransform<T> refToBody = frame.getTransformTo(shape.getBodyFrame(), shifted);
            if (i == 0) {
                centralRefToBody = refToBody;
            }

            sample.add(losIntersectionWithBody(new FieldTransform<>(shifted, refToSc.getInverse(), refToBody)));

        }

        // use interpolation to compute properly the time-derivatives
        final TimeStampedFieldPVCoordinates<T> targetBody =
                        TimeStampedFieldPVCoordinates.interpolate(date, CartesianDerivativesFilter.USE_P, sample);

        // convert back to caller specified frame
        return centralRefToBody.getInverse().transformPVCoordinates(targetBody);

    }

    /** Compute line of sight intersection with body.
     * @param scToBody transform from spacecraft frame to body frame
     * @return intersection point in body frame (only the position is set!)
     */
    private TimeStampedPVCoordinates losIntersectionWithBody(final Transform scToBody) {

        // compute satellite pointing axis and position/velocity in body frame
        final Vector3D pointingBodyFrame = scToBody.transformVector(satPointingVector);
        final Vector3D pBodyFrame        = scToBody.transformPosition(Vector3D.ZERO);

        // Line from satellite following pointing direction
        // we use arbitrarily the Earth radius as a scaling factor, it could be anything else
        final Line pointingLine = new Line(pBodyFrame,
                                           pBodyFrame.add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                          pointingBodyFrame),
                                           1.0e-10);

        // Intersection with body shape
        final GeodeticPoint gpIntersection =
            shape.getIntersectionPoint(pointingLine, pBodyFrame, shape.getBodyFrame(), scToBody.getDate());
        final Vector3D pIntersection =
            (gpIntersection == null) ? null : shape.transform(gpIntersection);

        // Check there is an intersection and it is not in the reverse pointing direction
        if ((pIntersection == null) ||
            (Vector3D.dotProduct(pIntersection.subtract(pBodyFrame), pointingBodyFrame) < 0)) {
            throw new OrekitException(OrekitMessages.ATTITUDE_POINTING_LAW_DOES_NOT_POINT_TO_GROUND);
        }

        return new TimeStampedPVCoordinates(scToBody.getDate(),
                                            pIntersection, Vector3D.ZERO, Vector3D.ZERO);

    }

    /** Compute line of sight intersection with body.
     * @param scToBody transform from spacecraft frame to body frame
     * @param <T> type of the field elements
     * @return intersection point in body frame (only the position is set!)
     */
    private <T extends RealFieldElement<T>> TimeStampedFieldPVCoordinates<T> losIntersectionWithBody(final FieldTransform<T> scToBody) {

        // compute satellite pointing axis and position/velocity in body frame
        final FieldVector3D<T> pointingBodyFrame = scToBody.transformVector(satPointingVector);
        final FieldVector3D<T> pBodyFrame        = scToBody.transformPosition(Vector3D.ZERO);

        // Line from satellite following pointing direction
        // we use arbitrarily the Earth radius as a scaling factor, it could be anything else
        final FieldLine<T> pointingLine = new FieldLine<>(pBodyFrame,
                                                          pBodyFrame.add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                         pointingBodyFrame),
                                                          1.0e-10);

        // Intersection with body shape
        final FieldGeodeticPoint<T> gpIntersection =
            shape.getIntersectionPoint(pointingLine, pBodyFrame, shape.getBodyFrame(), scToBody.getFieldDate());
        final FieldVector3D<T> pIntersection =
            (gpIntersection == null) ? null : shape.transform(gpIntersection);

        // Check there is an intersection and it is not in the reverse pointing direction
        if ((pIntersection == null) ||
            (FieldVector3D.dotProduct(pIntersection.subtract(pBodyFrame), pointingBodyFrame).getReal() < 0)) {
            throw new OrekitException(OrekitMessages.ATTITUDE_POINTING_LAW_DOES_NOT_POINT_TO_GROUND);
        }

        final FieldVector3D<T> zero = FieldVector3D.getZero(scToBody.getFieldDate().getField());
        return new TimeStampedFieldPVCoordinates<>(scToBody.getDate(),
                                                   pIntersection, zero, zero);

    }

}
