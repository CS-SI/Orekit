/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.RealFieldUnivariateFunction;
import org.hipparchus.analysis.solvers.AllowedSolution;
import org.hipparchus.analysis.solvers.BracketedRealFieldUnivariateSolver;
import org.hipparchus.analysis.solvers.FieldBracketingNthOrderBrentSolver;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


/** Field Topocentric frame.
 * <p>Frame associated to a position near the surface of a body shape.</p>
 * <p>
 * The origin of the frame is at the defining {@link GeodeticPoint geodetic point}
 * location, and the right-handed canonical trihedra is:
 * </p>
 * <ul>
 *   <li>X axis in the local horizontal plane (normal to zenith direction) and
 *   following the local parallel towards East</li>
 *   <li>Y axis in the horizontal plane (normal to zenith direction) and
 *   following the local meridian towards North</li>
 *   <li>Z axis towards Zenith direction</li>
 * </ul>
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class FieldTopocentricFrame<T extends RealFieldElement<T>> extends Frame implements FieldPVCoordinatesProvider<T> {

    /** Serializable UID. */
    private static final long serialVersionUID = -5997915708080966466L;

    /** Body shape on which the local point is defined. */
    private final BodyShape parentShape;

    /** Point where the topocentric frame is defined. */
    private final FieldGeodeticPoint<T> point;

    /** Simple constructor.
     * @param parentShape body shape on which the local point is defined
     * @param point local surface point where topocentric frame is defined
     * @param name the string representation
     */
    public FieldTopocentricFrame(final BodyShape parentShape, final FieldGeodeticPoint<T> point,
                            final String name) {

        super(parentShape.getBodyFrame(),
              new Transform(AbsoluteDate.J2000_EPOCH,
                            new Transform(AbsoluteDate.J2000_EPOCH,
                                          parentShape.transform(point).negate().toVector3D()),
                            new Transform(AbsoluteDate.J2000_EPOCH,
                                          new Rotation(point.getEast().toVector3D(), point.getZenith().toVector3D(),
                                                       Vector3D.PLUS_I, Vector3D.PLUS_K),
                                          Vector3D.ZERO)),
              name, false);
        this.parentShape = parentShape;
        this.point = point;
    }

    /** Get the body shape on which the local point is defined.
     * @return body shape on which the local point is defined
     */
    public BodyShape getParentShape() {
        return parentShape;
    }

    /** Get the surface point defining the origin of the frame.
     * @return surface point defining the origin of the frame
     */
    public FieldGeodeticPoint<T> getPoint() {
        return point;
    }

    /** Get the zenith direction of topocentric frame, expressed in parent shape frame.
     * <p>The zenith direction is defined as the normal to local horizontal plane.</p>
     * @return unit vector in the zenith direction
     * @see #getNadir()
     */
    public FieldVector3D<T> getZenith() {
        return point.getZenith();
    }

    /** Get the nadir direction of topocentric frame, expressed in parent shape frame.
     * <p>The nadir direction is the opposite of zenith direction.</p>
     * @return unit vector in the nadir direction
     * @see #getZenith()
     */
    public FieldVector3D<T> getNadir() {
        return point.getNadir();
    }

   /** Get the north direction of topocentric frame, expressed in parent shape frame.
     * <p>The north direction is defined in the horizontal plane
     * (normal to zenith direction) and following the local meridian.</p>
     * @return unit vector in the north direction
     * @see #getSouth()
     */
    public FieldVector3D<T> getNorth() {
        return point.getNorth();
    }

    /** Get the south direction of topocentric frame, expressed in parent shape frame.
     * <p>The south direction is the opposite of north direction.</p>
     * @return unit vector in the south direction
     * @see #getNorth()
     */
    public FieldVector3D<T> getSouth() {
        return point.getSouth();
    }

    /** Get the east direction of topocentric frame, expressed in parent shape frame.
     * <p>The east direction is defined in the horizontal plane
     * in order to complete direct triangle (east, north, zenith).</p>
     * @return unit vector in the east direction
     * @see #getWest()
     */
    public FieldVector3D<T> getEast() {
        return point.getEast();
    }

    /** Get the west direction of topocentric frame, expressed in parent shape frame.
     * <p>The west direction is the opposite of east direction.</p>
     * @return unit vector in the west direction
     * @see #getEast()
     */
    public FieldVector3D<T> getWest() {
        return point.getWest();
    }

    /** Get the elevation of a point with regards to the local point.
     * <p>The elevation is the angle between the local horizontal and
     * the direction from local point to given point.</p>
     * @param extPoint point for which elevation shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return elevation of the point
     * @since 9.3
     */
    public T getElevation(final FieldVector3D<T> extPoint, final Frame frame,
                                                          final FieldAbsoluteDate<T> date) {

        // Transform given point from given frame to topocentric frame
        final FieldTransform<T> t = frame.getTransformTo(this, date);
        final FieldVector3D<T> extPointTopo = t.transformPosition(extPoint);

        // Elevation angle is PI/2 - angle between zenith and given point direction
        return extPointTopo.getDelta();
    }

    /** Get the azimuth of a point with regards to the topocentric frame center point.
     * <p>The azimuth is the angle between the North direction at local point and
     * the projection in local horizontal plane of the direction from local point
     * to given point. Azimuth angles are counted clockwise, i.e positive towards the East.</p>
     * @param extPoint point for which elevation shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return azimuth of the point
     * @since 9.3
     */
    public T getAzimuth(final FieldVector3D<T> extPoint, final Frame frame,
                                                        final FieldAbsoluteDate<T> date) {

        // Transform given point from given frame to topocentric frame
        final FieldTransform<T> t = getTransformTo(frame, date).getInverse();
        final FieldVector3D<T> extPointTopo = t.transformPosition(extPoint);

        // Compute azimuth
        T azimuth = FastMath.atan2(extPointTopo.getX(), extPointTopo.getY());
        if (azimuth.getReal() < 0.) {
            azimuth = azimuth.add(MathUtils.TWO_PI);
        }
        return azimuth;

    }

    /** Get the range of a point with regards to the topocentric frame center point.
     * @param extPoint point for which range shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return range (distance) of the point
     * @since 9.3
     */
    public T getRange(final FieldVector3D<T> extPoint, final Frame frame,
                                                      final FieldAbsoluteDate<T> date) {

        // Transform given point from given frame to topocentric frame
        final FieldTransform<T> t = frame.getTransformTo(this, date);
        final FieldVector3D<T> extPointTopo = t.transformPosition(extPoint);

        // Compute range
        return extPointTopo.getNorm();

    }

    /** Get the range rate of a point with regards to the topocentric frame center point.
     * @param extPV point/velocity for which range rate shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return range rate of the point (positive if point departs from frame)
     * @since 9.3
     */
    public T getRangeRate(final FieldPVCoordinates<T> extPV, final Frame frame,
                                                          final FieldAbsoluteDate<T> date) {

        // Transform given point from given frame to topocentric frame
        final FieldTransform<T> t = frame.getTransformTo(this, date);
        final FieldPVCoordinates<T> extPVTopo = t.transformPVCoordinates(extPV);

        // Compute range rate (doppler) : relative rate along the line of sight
        return FieldVector3D.dotProduct(extPVTopo.getPosition(), extPVTopo.getVelocity()).divide(
               extPVTopo.getPosition().getNorm());

    }

    /**
     * Compute the limit visibility point for a satellite in a given direction.
     * <p>
     * This method can be used to compute visibility circles around ground stations
     * for example, using a simple loop on azimuth, with either a fixed elevation
     * or an elevation that depends on azimuth to take ground masks into account.
     * </p>
     * @param radius satellite distance to Earth center
     * @param azimuth pointing azimuth from station
     * @param elevation pointing elevation from station
     * @return limit visibility point for the satellite
     */
    public FieldGeodeticPoint<T> computeLimitVisibilityPoint(final T radius,
                                                     final T azimuth, final T elevation) {
        try {
            // convergence threshold on point position: 1mm
            final T deltaP = radius.multiply(0).add(0.001);
            final BracketedRealFieldUnivariateSolver<T> solver =
                    new FieldBracketingNthOrderBrentSolver<>(deltaP.divide(Constants.WGS84_EARTH_EQUATORIAL_RADIUS),
                                                      deltaP, deltaP, 5);

            // find the distance such that a point in the specified direction and at the solved-for
            // distance is exactly at the specified radius
            final T distance = solver.solve(1000, new RealFieldUnivariateFunction<T>() {
                /** {@inheritDoc} */
                public T value(final T x) {
                    final FieldGeodeticPoint<T> gp = pointAtDistance(azimuth, elevation, x);
                    return parentShape.transform(gp).getNorm().subtract(radius);
                }
            }, radius.multiply(0), radius.multiply(2), AllowedSolution.ANY_SIDE);

            // return the limit point
            return pointAtDistance(azimuth, elevation, distance);

        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        }
    }

    /** Compute the point observed from the station at some specified distance.
     * @param azimuth pointing azimuth from station
     * @param elevation pointing elevation from station
     * @param distance distance to station
     * @return observed point
     */
    public FieldGeodeticPoint<T> pointAtDistance(final T azimuth, final T elevation,
                                         final T distance) {
        final Field<T> field = azimuth.getField();
        final T cosAz = azimuth.cos();
        final T sinAz = azimuth.sin();
        final T cosEl = elevation.cos();
        final T sinEl = elevation.sin();
        final FieldVector3D<T>  observed = new FieldVector3D<>(distance.multiply(cosEl).multiply(sinAz),
                                                               distance.multiply(cosEl).multiply(cosAz),
                                                               distance.multiply(sinEl));
        return parentShape.transform(observed, this, FieldAbsoluteDate.getJ2000Epoch(field));
    }

    /** Get the {@link PVCoordinates} of the topocentric frame origin in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @return position/velocity of the topocentric frame origin (m and m/s)
     */
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date, final Frame frame) {
        final Field<T> field = date.getField();
        return getTransformTo(frame, date).transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(date,
                                                                  FieldVector3D.getZero(field),
                                                                  FieldVector3D.getZero(field),
                                                                  FieldVector3D.getZero(field)));
    }
}
