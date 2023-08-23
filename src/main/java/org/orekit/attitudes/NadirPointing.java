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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.time.TimeInterpolator;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinatesHermiteInterpolator;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinatesHermiteInterpolator;

/**
 * This class handles nadir pointing attitude provider.

 * <p>
 * This class represents the attitude provider where the satellite z axis is
 * pointing to the vertical of the ground point under satellite.</p>
 * <p>
 * The object <code>NadirPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     GroundPointing
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class NadirPointing extends GroundPointing {

    /** Body shape.  */
    private final BodyShape shape;

    /** Creates new instance.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param shape Body shape
     * @since 7.1
     */
    public NadirPointing(final Frame inertialFrame, final BodyShape shape) {
        // Call constructor of superclass
        super(inertialFrame, shape.getBodyFrame());
        this.shape = shape;
    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                final AbsoluteDate date, final Frame frame) {

        // transform from specified reference frame to body frame
        final Transform refToBody = frame.getTransformTo(shape.getBodyFrame(), date);

        // sample intersection points in current date neighborhood
        final double h  = 0.01;
        final List<TimeStampedPVCoordinates> sample = new ArrayList<>();
        sample.add(nadirRef(pvProv.getPVCoordinates(date.shiftedBy(-2 * h), frame), refToBody.staticShiftedBy(-2 * h)));
        sample.add(nadirRef(pvProv.getPVCoordinates(date.shiftedBy(-h),     frame), refToBody.staticShiftedBy(-h)));
        sample.add(nadirRef(pvProv.getPVCoordinates(date,                   frame), refToBody));
        sample.add(nadirRef(pvProv.getPVCoordinates(date.shiftedBy(+h),     frame), refToBody.staticShiftedBy(+h)));
        sample.add(nadirRef(pvProv.getPVCoordinates(date.shiftedBy(+2 * h), frame), refToBody.staticShiftedBy(+2 * h)));

        // create interpolator
        final TimeInterpolator<TimeStampedPVCoordinates> interpolator =
                new TimeStampedPVCoordinatesHermiteInterpolator(sample.size(), CartesianDerivativesFilter.USE_P);

        // use interpolation to compute properly the time-derivatives
        return interpolator.interpolate(date, sample);

    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getTargetPV(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                            final FieldAbsoluteDate<T> date,
                                                                                            final Frame frame) {

        // zero
        final T zero = date.getField().getZero();

        // transform from specified reference frame to body frame
        final FieldTransform<T> refToBody = frame.getTransformTo(shape.getBodyFrame(), date);

        // sample intersection points in current date neighborhood
        final double h  = 0.01;
        final List<TimeStampedFieldPVCoordinates<T>> sample = new ArrayList<>();
        sample.add(nadirRef(pvProv.getPVCoordinates(date.shiftedBy(-2 * h), frame), refToBody.staticShiftedBy(zero.add(-2 * h))));
        sample.add(nadirRef(pvProv.getPVCoordinates(date.shiftedBy(-h),     frame), refToBody.staticShiftedBy(zero.add(-h))));
        sample.add(nadirRef(pvProv.getPVCoordinates(date,                   frame), refToBody));
        sample.add(nadirRef(pvProv.getPVCoordinates(date.shiftedBy(+h),     frame), refToBody.staticShiftedBy(zero.add(+h))));
        sample.add(nadirRef(pvProv.getPVCoordinates(date.shiftedBy(+2 * h), frame), refToBody.staticShiftedBy(zero.add(+2 * h))));

        // create interpolator
        final FieldTimeInterpolator<TimeStampedFieldPVCoordinates<T>, T> interpolator =
                new TimeStampedFieldPVCoordinatesHermiteInterpolator<>(sample.size(), CartesianDerivativesFilter.USE_P);

        // use interpolation to compute properly the time-derivatives
        return interpolator.interpolate(date, sample);

    }

    /** Compute ground point in nadir direction, in reference frame.
     * @param scRef spacecraft coordinates in reference frame
     * @param refToBody transform from reference frame to body frame
     * @return intersection point in body frame (only the position is set!)
     */
    private TimeStampedPVCoordinates nadirRef(final TimeStampedPVCoordinates scRef,
                                              final StaticTransform refToBody) {

        final Vector3D satInBodyFrame = refToBody.transformPosition(scRef.getPosition());

        // satellite position in geodetic coordinates
        final GeodeticPoint gpSat = shape.transform(satInBodyFrame, getBodyFrame(), scRef.getDate());

        // nadir position in geodetic coordinates
        final GeodeticPoint gpNadir = new GeodeticPoint(gpSat.getLatitude(), gpSat.getLongitude(), 0.0);

        // nadir point position in body frame
        final Vector3D pNadirBody = shape.transform(gpNadir);

        // nadir point position in reference frame
        final Vector3D pNadirRef = refToBody.getInverse().transformPosition(pNadirBody);

        return new TimeStampedPVCoordinates(scRef.getDate(), pNadirRef, Vector3D.ZERO, Vector3D.ZERO);

    }

    /** Compute ground point in nadir direction, in reference frame.
     * @param scRef spacecraft coordinates in reference frame
     * @param refToBody transform from reference frame to body frame
     * @param <T> type of the field elements
     * @return intersection point in body frame (only the position is set!)
     * @since 9.0
     */
    private <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> nadirRef(final TimeStampedFieldPVCoordinates<T> scRef,
                                                                                          final FieldStaticTransform<T> refToBody) {

        final FieldVector3D<T> satInBodyFrame = refToBody.transformPosition(scRef.getPosition());

        // satellite position in geodetic coordinates
        final FieldGeodeticPoint<T> gpSat = shape.transform(satInBodyFrame, getBodyFrame(), scRef.getDate());

        // nadir position in geodetic coordinates
        final FieldGeodeticPoint<T> gpNadir = new FieldGeodeticPoint<>(gpSat.getLatitude(), gpSat.getLongitude(),
                                                                       gpSat.getAltitude().getField().getZero());

        // nadir point position in body frame
        final FieldVector3D<T> pNadirBody = shape.transform(gpNadir);

        // nadir point position in reference frame
        final FieldVector3D<T> pNadirRef = refToBody.getInverse().transformPosition(pNadirBody);

        final FieldVector3D<T> zero = FieldVector3D.getZero(gpSat.getAltitude().getField());
        return new TimeStampedFieldPVCoordinates<>(scRef.getDate(), pNadirRef, zero, zero);

    }

}
