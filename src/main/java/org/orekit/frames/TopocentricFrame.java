/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolver;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TrackingCoordinates;


/** Topocentric frame.
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
public class TopocentricFrame extends Frame implements ExtendedPositionProvider {

    /** Body shape on which the local point is defined. */
    private final BodyShape parentShape;

    /** Geodetic point where the topocentric frame is defined. */
    private final GeodeticPoint point;

    /** Cartesian point where the topocentric frame is defined.
     * @since 12.0
     */
    private final Vector3D cartesianPoint;

    /** Simple constructor.
     * @param parentShape body shape on which the local point is defined
     * @param point local surface point where topocentric frame is defined
     * @param name the string representation
     */
    public TopocentricFrame(final BodyShape parentShape, final GeodeticPoint point,
                            final String name) {

        super(parentShape.getBodyFrame(),
                new Transform(AbsoluteDate.ARBITRARY_EPOCH,
                        new Transform(AbsoluteDate.ARBITRARY_EPOCH,
                                parentShape.transform(point).negate()),
                        new Transform(AbsoluteDate.ARBITRARY_EPOCH,
                                new Rotation(point.getEast(), point.getZenith(),
                                        Vector3D.PLUS_I, Vector3D.PLUS_K),
                                Vector3D.ZERO)),
                name, false);
        this.parentShape    = parentShape;
        this.point          = point;
        this.cartesianPoint = getTransformProvider().
                getStaticTransform(AbsoluteDate.ARBITRARY_EPOCH).
                getInverse().
                transformPosition(Vector3D.ZERO);
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
    public GeodeticPoint getPoint() {
        return point;
    }

    /** Get the surface point defining the origin of the frame.
     * @return surface point defining the origin of the frame in body frame
     * @since 12.0
     */
    public Vector3D getCartesianPoint() {
        return cartesianPoint;
    }

    /** Get the surface point defining the origin of the frame.
     * @param <T> type of the elements
     * @param field of the elements
     * @return surface point defining the origin of the frame
     * @since 9.3
     */
    public <T extends CalculusFieldElement<T>> FieldGeodeticPoint<T> getPoint(final Field<T> field) {
        final T zero = field.getZero();
        return new FieldGeodeticPoint<>(zero.newInstance(point.getLatitude()),
                zero.newInstance(point.getLongitude()),
                zero.newInstance(point.getAltitude()));
    }

    /** Get the zenith direction of topocentric frame, expressed in parent shape frame.
     * <p>The zenith direction is defined as the normal to local horizontal plane.</p>
     * @return unit vector in the zenith direction
     * @see #getNadir()
     */
    public Vector3D getZenith() {
        return point.getZenith();
    }

    /** Get the nadir direction of topocentric frame, expressed in parent shape frame.
     * <p>The nadir direction is the opposite of zenith direction.</p>
     * @return unit vector in the nadir direction
     * @see #getZenith()
     */
    public Vector3D getNadir() {
        return point.getNadir();
    }

    /** Get the north direction of topocentric frame, expressed in parent shape frame.
     * <p>The north direction is defined in the horizontal plane
     * (normal to zenith direction) and following the local meridian.</p>
     * @return unit vector in the north direction
     * @see #getSouth()
     */
    public Vector3D getNorth() {
        return point.getNorth();
    }

    /** Get the south direction of topocentric frame, expressed in parent shape frame.
     * <p>The south direction is the opposite of north direction.</p>
     * @return unit vector in the south direction
     * @see #getNorth()
     */
    public Vector3D getSouth() {
        return point.getSouth();
    }

    /** Get the east direction of topocentric frame, expressed in parent shape frame.
     * <p>The east direction is defined in the horizontal plane
     * in order to complete direct triangle (east, north, zenith).</p>
     * @return unit vector in the east direction
     * @see #getWest()
     */
    public Vector3D getEast() {
        return point.getEast();
    }

    /** Get the west direction of topocentric frame, expressed in parent shape frame.
     * <p>The west direction is the opposite of east direction.</p>
     * @return unit vector in the west direction
     * @see #getEast()
     */
    public Vector3D getWest() {
        return point.getWest();
    }

    /** Get the tracking coordinates of a point with regards to the local point.
     * @param extPoint point for which elevation shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return tracking coordinates of the point
     * @since 12.0
     */
    public TrackingCoordinates getTrackingCoordinates(final Vector3D extPoint, final Frame frame,
                                                      final AbsoluteDate date) {

        // transform given point from given frame to topocentric frame
        final Vector3D extPointTopo = transformPoint(extPoint, frame, date);

        final double azimuth = computeAzimuthFromTopoPoint(extPointTopo);

        return new TrackingCoordinates(azimuth, extPointTopo.getDelta(), extPointTopo.getNorm());

    }

    /** Get the tracking coordinates of a point with regards to the local point.
     * @param <T> type of the field elements
     * @param extPoint point for which elevation shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return tracking coordinates of the point
     * @since 12.0
     */
    public <T extends CalculusFieldElement<T>> FieldTrackingCoordinates<T> getTrackingCoordinates(final FieldVector3D<T> extPoint,
                                                                                                  final Frame frame,
                                                                                                  final FieldAbsoluteDate<T> date) {

        // Transform given point from given frame to topocentric frame
        final FieldVector3D<T> extPointTopo = transformPoint(extPoint, frame, date);

        final T azimuth = computeAzimuthFromTopoPoint(extPointTopo);

        return new FieldTrackingCoordinates<>(azimuth, extPointTopo.getDelta(), extPointTopo.getNorm());

    }

    /** Get the elevation of a point with regards to the local point.
     * <p>The elevation is the angle between the local horizontal and
     * the direction from local point to given point.</p>
     * @param extPoint point for which elevation shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return elevation of the point
     */
    public double getElevation(final Vector3D extPoint, final Frame frame,
                               final AbsoluteDate date) {

        // Transform given point from given frame to topocentric frame
        final Vector3D extPointTopo = transformPoint(extPoint, frame, date);

        return extPointTopo.getDelta();
    }

    /** Get the elevation of a point with regards to the local point.
     * <p>The elevation is the angle between the local horizontal and
     * the direction from local point to given point.</p>
     * @param <T> type of the elements
     * @param extPoint point for which elevation shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return elevation of the point
     * @since 9.3
     */
    public <T extends CalculusFieldElement<T>> T getElevation(final FieldVector3D<T> extPoint, final Frame frame,
                                                              final FieldAbsoluteDate<T> date) {

        // Transform given point from given frame to topocentric frame
        final FieldVector3D<T> extPointTopo = transformPoint(extPoint, frame, date);

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
     */
    public double getAzimuth(final Vector3D extPoint, final Frame frame,
                             final AbsoluteDate date) {

        // Transform given point from given frame to topocentric frame
        final Vector3D extPointTopo = transformPoint(extPoint, frame, date);

        return computeAzimuthFromTopoPoint(extPointTopo);

    }

    /** Get the azimuth of a point with regards to the topocentric frame center point.
     * <p>The azimuth is the angle between the North direction at local point and
     * the projection in local horizontal plane of the direction from local point
     * to given point. Azimuth angles are counted clockwise, i.e positive towards the East.</p>
     * @param <T> type of the elements
     * @param extPoint point for which elevation shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return azimuth of the point
     * @since 9.3
     */
    public <T extends CalculusFieldElement<T>> T getAzimuth(final FieldVector3D<T> extPoint, final Frame frame,
                                                            final FieldAbsoluteDate<T> date) {

        // Transform given point from given frame to topocentric frame
        final FieldVector3D<T> extPointTopo = transformPoint(extPoint, frame, date);

        return computeAzimuthFromTopoPoint(extPointTopo);

    }

    /** Get the range of a point with regards to the topocentric frame center point.
     * @param extPoint point for which range shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return range (distance) of the point
     */
    public double getRange(final Vector3D extPoint, final Frame frame,
                           final AbsoluteDate date) {

        // Transform given point from given frame to topocentric frame
        final Vector3D extPointTopo = transformPoint(extPoint, frame, date);

        return extPointTopo.getNorm();

    }

    /** Get the range of a point with regards to the topocentric frame center point.
     * @param <T> type of the elements
     * @param extPoint point for which range shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return range (distance) of the point
     * @since 9.3
     */
    public <T extends CalculusFieldElement<T>> T getRange(final FieldVector3D<T> extPoint, final Frame frame,
                                                          final FieldAbsoluteDate<T> date) {

        // Transform given point from given frame to topocentric frame
        final FieldVector3D<T> extPointTopo = transformPoint(extPoint, frame, date);

        return extPointTopo.getNorm();

    }

    /** Get the range rate of a point with regards to the topocentric frame center point.
     * @param extPV point/velocity for which range rate shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return range rate of the point (positive if point departs from frame)
     */
    public double getRangeRate(final PVCoordinates extPV, final Frame frame,
                               final AbsoluteDate date) {

        // Transform given point from given frame to topocentric frame
        final KinematicTransform t = frame.getKinematicTransformTo(this, date);
        final PVCoordinates extPVTopo = t.transformOnlyPV(extPV);

        // Compute range rate (doppler) : relative rate along the line of sight
        return Vector3D.dotProduct(extPVTopo.getPosition(), extPVTopo.getVelocity()) /
                extPVTopo.getPosition().getNorm();

    }

    /** Get the range rate of a point with regards to the topocentric frame center point.
     * @param <T> type of the elements
     * @param extPV point/velocity for which range rate shall be computed
     * @param frame frame in which the point is defined
     * @param date computation date
     * @return range rate of the point (positive if point departs from frame)
     * @since 9.3
     */
    public <T extends CalculusFieldElement<T>> T getRangeRate(final FieldPVCoordinates<T> extPV, final Frame frame,
                                                              final FieldAbsoluteDate<T> date) {

        // Transform given point from given frame to topocentric frame
        final FieldKinematicTransform<T> t = frame.getKinematicTransformTo(this, date);
        final FieldPVCoordinates<T> extPVTopo = t.transformOnlyPV(extPV);

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
    public GeodeticPoint computeLimitVisibilityPoint(final double radius,
                                                     final double azimuth, final double elevation) {
        try {
            // convergence threshold on point position: 1mm
            final double deltaP = 0.001;
            final UnivariateSolver solver =
                    new BracketingNthOrderBrentSolver(deltaP / Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                            deltaP, deltaP, 5);

            // find the distance such that a point in the specified direction and at the solved-for
            // distance is exactly at the specified radius
            final double distance = solver.solve(1000, new UnivariateFunction() {
                /** {@inheritDoc} */
                public double value(final double x) {
                    final GeodeticPoint gp = pointAtDistance(azimuth, elevation, x);
                    return parentShape.transform(gp).getNorm() - radius;
                }
            }, 0, 2 * radius);

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
    public GeodeticPoint pointAtDistance(final double azimuth, final double elevation,
                                         final double distance) {
        final SinCos scAz  = FastMath.sinCos(azimuth);
        final SinCos scEl  = FastMath.sinCos(elevation);
        final Vector3D  observed = new Vector3D(distance * scEl.cos() * scAz.sin(),
                distance * scEl.cos() * scAz.cos(),
                distance * scEl.sin());
        return parentShape.transform(observed, this, AbsoluteDate.ARBITRARY_EPOCH);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        return getStaticTransformTo(frame, date).transformPosition(Vector3D.ZERO);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {
        return getStaticTransformTo(frame, date).transformPosition(Vector3D.ZERO);
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        return getTransformTo(frame, date).transformPVCoordinates(new TimeStampedPVCoordinates(date,
                Vector3D.ZERO,
                Vector3D.ZERO,
                Vector3D.ZERO));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date,
                                                                                                 final Frame frame) {
        final FieldVector3D<T> zero = FieldVector3D.getZero(date.getField());
        return getTransformTo(frame, date).transformPVCoordinates(new TimeStampedFieldPVCoordinates<>(date, zero, zero, zero));
    }

    /** Get the topocentric position from {@link TrackingCoordinates}.
     * @param coords The coordinates that are to be converted.
     * @return The topocentric coordinates.
     * @since 12.1
     */
    public static Vector3D getTopocentricPosition(final TrackingCoordinates coords) {
        return getTopocentricPosition(coords.getAzimuth(), coords.getElevation(), coords.getRange());
    }

    /** Get the topocentric position from {@link FieldTrackingCoordinates}.
     * @param coords The coordinates that are to be converted.
     * @param <T> Type of the field coordinates.
     * @return The topocentric coordinates.
     * @since 12.1
     */
    public static <T extends CalculusFieldElement<T>> FieldVector3D<T> getTopocentricPosition(final FieldTrackingCoordinates<T> coords) {
        return getTopocentricPosition(coords.getAzimuth(), coords.getElevation(), coords.getRange());
    }

    /**
     * Gets the topocentric position from a set of az/el/ra coordinates.
     * @param azimuth the angle of rotation around the vertical axis, going East.
     * @param elevation the elevation angle from the local horizon.
     * @param range the distance from the goedetic position.
     * @return the topocentric position.
     * @since 12.1
     */
    private static Vector3D getTopocentricPosition(final double azimuth, final double elevation, final double range) {
        final SinCos sinCosAz = FastMath.sinCos(azimuth);
        final SinCos sinCosEL = FastMath.sinCos(elevation);
        return new Vector3D(range * sinCosEL.cos() * sinCosAz.sin(), range * sinCosEL.cos() * sinCosAz.cos(), range * sinCosEL.sin());
    }

    /**
     * Gets the topocentric position from a set of az/el/ra coordinates.
     * @param azimuth the angle of rotation around the vertical axis, going East.
     * @param elevation the elevation angle from the local horizon.
     * @param range the distance from the geodetic position.
     * @return the topocentric position.
     * @param <T> the type of the az/el/ra coordinates.
     * @since 12.1
     */
    private static <T extends CalculusFieldElement<T>> FieldVector3D<T> getTopocentricPosition(final T azimuth, final T elevation, final T range) {
        final FieldSinCos<T> sinCosAz = FastMath.sinCos(azimuth);
        final FieldSinCos<T> sinCosEl = FastMath.sinCos(elevation);
        return new FieldVector3D<>(
                range.multiply(sinCosEl.cos()).multiply(sinCosAz.sin()),
                range.multiply(sinCosEl.cos()).multiply(sinCosAz.cos()),
                range.multiply(sinCosEl.sin())
        );
    }

    /** Transform point in topocentric frame.
     * @param extPoint point
     * @param date current date
     * @param frame the frame where to define the position
     * @return transformed point in topocentric frame
     */
    private Vector3D transformPoint(final Vector3D extPoint, final Frame frame, final AbsoluteDate date) {
        final StaticTransform t = frame.getStaticTransformTo(this, date);
        return t.transformPosition(extPoint);
    }

    /** Transform point in topocentric frame.
     * @param <T> type of the field elements
     * @param extPoint point
     * @param date current date
     * @param frame the frame where to define the position
     * @return transformed point in topocentric frame
     */
    private <T extends CalculusFieldElement<T>> FieldVector3D<T> transformPoint(final FieldVector3D<T> extPoint,
                                                                                final Frame frame,
                                                                                final FieldAbsoluteDate<T> date) {
        final FieldStaticTransform<T> t = frame.getStaticTransformTo(this, date);
        return t.transformPosition(extPoint);
    }

    /** Compute azimuth from topocentric point.
     * @param extPointTopo topocentric point
     * @return azimuth
     */
    private double computeAzimuthFromTopoPoint(final Vector3D extPointTopo) {
        final double azimuth = FastMath.atan2(extPointTopo.getX(), extPointTopo.getY());
        if (azimuth < 0.0) {
            return azimuth + MathUtils.TWO_PI;
        } else {
            return azimuth;
        }
    }

    /** Compute azimuth from topocentric point.
     * @param <T> type of the field elements
     * @param extPointTopo topocentric point
     * @return azimuth
     */
    private <T extends CalculusFieldElement<T>> T computeAzimuthFromTopoPoint(final FieldVector3D<T> extPointTopo) {
        final T azimuth = FastMath.atan2(extPointTopo.getX(), extPointTopo.getY());
        if (azimuth.getReal() < 0.0) {
            return azimuth.add(MathUtils.TWO_PI);
        } else {
            return azimuth;
        }
    }

}
