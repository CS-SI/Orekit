/* Copyright 2002-2023 Joseph Reed
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Joseph Reed licenses this file to You under the Apache License, Version 2.0
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

import java.util.Map.Entry;
import java.util.TreeMap;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.spherical.twod.Circle;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.LoxodromeArc;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;

/** Builder class, enabling incremental building of an {@link PVCoordinatesProvider}
 * instance using waypoints defined on an ellipsoid.
 *
 * Given a series of waypoints ({@code (date, point)} tuples),
 * build a {@link PVCoordinatesProvider} representing the path.
 * The static methods provide implementations for the most common path definitions
 * (cartesian, great-circle, loxodrome). If these methods are insufficient,
 * the public constructor provides a way to customize the path definition.
 *
 * This class connects the path segments using the {@link AggregatedPVCoordinatesProvider}.
 * As such, no effort is made to smooth the velocity between segments.
 * While position is unaffected, the velocity may be discontinuous between adjacent time points.
 * Thus, care should be taken when modeling paths with abrupt direction changes
 * (e.g. fast-moving aircraft); understand how the {@link PVCoordinatesProvider}
 * will be used in the particular application.
 *
 * @author Joe Reed
 * @since 11.3
 */
public class WaypointPVBuilder {

    /** Factory used to create intermediate pv providers between waypoints. */
    private final InterpolationFactory factory;

    /** Central body, on which the waypoints are defined. */
    private final OneAxisEllipsoid body;

    /** Set of waypoints, indexed by time. */
    private final TreeMap<AbsoluteDate, GeodeticPoint> waypoints;

    /** Whether the resulting provider should be invalid or constant prior to the first waypoint. */
    private boolean invalidBefore;

    /** Whether the resulting provider should be invalid or constant after to the last waypoint. */
    private boolean invalidAfter;

    /** Create a new instance.
     * @param factory The factory used to create the intermediate coordinate providers between waypoints.
     * @param body The central body, on which the way points are defined.
     */
    public WaypointPVBuilder(final InterpolationFactory factory, final OneAxisEllipsoid body) {
        this.factory       = factory;
        this.body          = body;
        this.waypoints     = new TreeMap<>();
        this.invalidBefore = true;
        this.invalidAfter  = true;
    }

    /** Construct a waypoint builder interpolating points using a linear cartesian interpolation.
     *
     * @param body the reference ellipsoid on which the waypoints are defined.
     * @return the waypoint builder
     */
    public static WaypointPVBuilder cartesianBuilder(final OneAxisEllipsoid body) {
        return new WaypointPVBuilder(CartesianWaypointPVProv::new, body);
    }

    /** Construct a waypoint builder interpolating points using a loxodrome (or Rhumbline).
     *
     * @param body the reference ellipsoid on which the waypoints are defined.
     * @return the waypoint builder
     */
    public static WaypointPVBuilder loxodromeBuilder(final OneAxisEllipsoid body) {
        return new WaypointPVBuilder(LoxodromeWaypointPVProv::new, body);
    }

    /** Construct a waypoint builder interpolating points using a great-circle.
     *
     * The altitude of the intermediate points is linearly interpolated from the bounding waypoints.
     * Extrapolating before the first waypoint or after the last waypoint may result in undefined altitudes.
     *
     * @param body the reference ellipsoid on which the waypoints are defined.
     * @return the waypoint builder
     */
    public static WaypointPVBuilder greatCircleBuilder(final OneAxisEllipsoid body) {
        return new WaypointPVBuilder(GreatCircleWaypointPVProv::new, body);
    }

    /** Add a waypoint.
     *
     * @param point the waypoint location
     * @param date the waypoint time
     * @return this instance
     */
    public WaypointPVBuilder addWaypoint(final GeodeticPoint point, final AbsoluteDate date) {
        waypoints.put(date, point);
        return this;
    }

    /** Indicate the resulting {@link PVCoordinatesProvider} should be invalid before the first waypoint.
     *
     * @return this instance
     */
    public WaypointPVBuilder invalidBefore() {
        invalidBefore = true;
        return this;
    }

    /** Indicate the resulting {@link PVCoordinatesProvider} provide
     * a constant location of the first waypoint prior to the first time.
     *
     * @return this instance
     */
    public WaypointPVBuilder constantBefore() {
        invalidBefore = false;
        return this;
    }

    /** Indicate the resulting {@link PVCoordinatesProvider} should be invalid after the last waypoint.
     *
     * @return this instance
     */
    public WaypointPVBuilder invalidAfter() {
        invalidAfter = true;
        return this;
    }

    /** Indicate the resulting {@link PVCoordinatesProvider} provide
     * a constant location of the last waypoint after to the last time.
     *
     * @return this instance
     */
    public WaypointPVBuilder constantAfter() {
        invalidAfter = false;
        return this;
    }

    /** Build a {@link PVCoordinatesProvider} from the waypoints added to this builder.
     *
     * @return the coordinates provider instance.
     */
    public PVCoordinatesProvider build() {
        final PVCoordinatesProvider initialProvider = createInitial(waypoints.firstKey(),
                                                                    waypoints.firstEntry().getValue());
        final AggregatedPVCoordinatesProvider.Builder builder = new AggregatedPVCoordinatesProvider.Builder(initialProvider);

        Entry<AbsoluteDate, GeodeticPoint> previousEntry = null;
        for (final Entry<AbsoluteDate, GeodeticPoint> entry: waypoints.entrySet()) {
            if (previousEntry != null) {
                builder.addPVProviderAfter(previousEntry.getKey(),
                                           factory.create(previousEntry.getKey(),
                                                          previousEntry.getValue(),
                                                          entry.getKey(),
                                                          entry.getValue(),
                                                          body),
                                           true);
            }
            previousEntry = entry;
        }
        // add the point so we're valid at the final waypoint
        builder.addPVProviderAfter(previousEntry.getKey(),
                                   new ConstantPVCoordinatesProvider(previousEntry.getValue(), body),
                                   true);
        // add the final propagator after the final waypoint
        builder.addPVProviderAfter(previousEntry.getKey().shiftedBy(Double.MIN_VALUE),
                                   createFinal(previousEntry.getKey(), previousEntry.getValue()),
                                   true);

        return builder.build();
    }

    /** Create the initial provider.
     *
     * This method uses the internal {@code validBefore} flag to
     * either return an invalid PVCoordinatesProvider or a constant one.
     *
     * @param firstDate the date at which the first waypoint is reached
     *                  and this provider will no longer be called
     * @param firstPoint the first waypoint
     * @return the coordinate provider
     */
    protected PVCoordinatesProvider createInitial(final AbsoluteDate firstDate, final GeodeticPoint firstPoint) {
        if (invalidBefore) {
            return new AggregatedPVCoordinatesProvider.InvalidPVProvider();
        }
        else {
            return new ConstantPVCoordinatesProvider(firstPoint, body);
        }
    }

    /** Create the final provider.
     *
     * This method uses the internal {@code validAfter} flag to
     * either return an invalid PVCoordinatesProvider or a constant one.
     *
     * @param lastDate the date at which the last waypoint is reached
     *                 and this provider will be called
     * @param lastPoint the last waypoint
     * @return the coordinate provider
     */
    protected PVCoordinatesProvider createFinal(final AbsoluteDate lastDate, final GeodeticPoint lastPoint) {
        if (invalidAfter) {
            return new AggregatedPVCoordinatesProvider.InvalidPVProvider();
        }
        else {
            return new ConstantPVCoordinatesProvider(lastPoint, body);
        }
    }

    /**
     * Factory interface, creating the {@link PVCoordinatesProvider} instances between the provided waypoints.
     */
    @FunctionalInterface
    public interface InterpolationFactory {

        /** Create a {@link PVCoordinatesProvider} which interpolates between the provided waypoints.
         *
         * @param date1 the first waypoint's date
         * @param point1 the first waypoint's location
         * @param date2 the second waypoint's date
         * @param point2 the second waypoint's location
         * @param body the body on which the waypoints are defined
         * @return a {@link PVCoordinatesProvider} providing the locations at times between the waypoints.
         */
        PVCoordinatesProvider create(AbsoluteDate date1, GeodeticPoint point1,
                                     AbsoluteDate date2, GeodeticPoint point2,
                                     OneAxisEllipsoid body);
    }

    /**
     * Coordinate provider interpolating along the great-circle between two points.
     */
    static class GreatCircleWaypointPVProv implements PVCoordinatesProvider {

        /** Great circle estimation. */
        private final Circle circle;
        /** Duration between the two points (seconds). */
        private final double duration;
        /** Phase along the circle of the first point. */
        private final double phase0;
        /** Phase length from the first point to the second. */
        private final double phaseLength;
        /** Time at which interpolation results in the initial point. */
        private final AbsoluteDate t0;
        /** Body on which the great circle is defined. */
        private final OneAxisEllipsoid body;
        /** Phase of one second. */
        private double oneSecondPhase;
        /** Altitude of the initial point. */
        private double initialAltitude;
        /** Time-derivative of the altitude. */
        private double altitudeSlope;

        /** Class constructor. Aligns to the {@link InterpolationFactory} functional interface.
         *
         * @param date1 the first waypoint's date
         * @param point1 the first waypoint's location
         * @param date2 the second waypoint's date
         * @param point2 the second waypoint's location
         * @param body the body on which the waypoints are defined
         * @see InterpolationFactory
         */
        GreatCircleWaypointPVProv(final AbsoluteDate date1, final GeodeticPoint point1,
                                  final AbsoluteDate date2, final GeodeticPoint point2,
                                  final OneAxisEllipsoid body) {
            this.t0 = date1;
            this.duration = date2.durationFrom(date1);
            this.body = body;
            final S2Point s0 = toSpherical(point1);
            final S2Point s1 = toSpherical(point2);
            circle = new Circle(s0, s1, 1e-9);

            phase0 = circle.getPhase(s0.getVector());
            phaseLength = circle.getPhase(s1.getVector()) - phase0;

            oneSecondPhase = phaseLength / duration;
            altitudeSlope = (point2.getAltitude() - point1.getAltitude()) / duration;
            initialAltitude = point1.getAltitude();
        }

        @Override
        public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
            final double d = date.durationFrom(t0);
            final double fraction = d / duration;
            final double phase = fraction * phaseLength;

            final S2Point sp = new S2Point(circle.getPointAt(phase0 + phase));
            final GeodeticPoint point = toGeodetic(sp, initialAltitude + d * altitudeSlope);
            final Vector3D p = body.transform(point);

            return body.getBodyFrame().getStaticTransformTo(frame, date).transformPosition(p);

        }

        @Override
        public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
            final double d = date.durationFrom(t0);
            final double fraction = d / duration;
            final double phase = fraction * phaseLength;

            final S2Point sp = new S2Point(circle.getPointAt(phase0 + phase));
            final GeodeticPoint point = toGeodetic(sp, initialAltitude + d * altitudeSlope);
            final Vector3D p = body.transform(point);

            // add 1 second to get another point along the circle, to use for velocity
            final S2Point sp2 = new S2Point(circle.getPointAt(phase0 + phase + oneSecondPhase));
            final GeodeticPoint point2 = toGeodetic(sp2, initialAltitude + (d + 1) * altitudeSlope);
            final Vector3D p2 = body.transform(point2);
            final Vector3D v = p2.subtract(p);

            final TimeStampedPVCoordinates tpv = new TimeStampedPVCoordinates(date, p, v);
            return body.getBodyFrame().getTransformTo(frame, date).transformPVCoordinates(tpv);
        }

        /** Converts the given geodetic point to a point on the 2-sphere.
         * @param point input geodetic point
         * @return a point on the 2-sphere
         */
        static S2Point toSpherical(final GeodeticPoint point) {
            return new S2Point(point.getLongitude(), 0.5 * FastMath.PI - point.getLatitude());
        }

        /** Converts a 2-sphere point to a geodetic point.
         * @param point point on the 2-sphere
         * @param alt point altitude
         * @return a geodetic point
         */
        static GeodeticPoint toGeodetic(final S2Point point, final double alt) {
            return new GeodeticPoint(0.5 * FastMath.PI - point.getPhi(), point.getTheta(), alt);
        }
    }

    /**
     * Coordinate provider interpolating along the loxodrome between two points.
     */
    static class LoxodromeWaypointPVProv implements PVCoordinatesProvider {

        /** Arc along which the interpolation occurs. */
        private final LoxodromeArc arc;
        /** Time at which the interpolation begins (at arc start). */
        private final AbsoluteDate t0;
        /** Total duration to get the length of the arc (seconds). */
        private final double duration;
        /** Velocity along the arc (m/s). */
        private final double velocity;

        /** Class constructor. Aligns to the {@link InterpolationFactory} functional interface.
         *
         * @param date1 the first waypoint's date
         * @param point1 the first waypoint's location
         * @param date2 the second waypoint's date
         * @param point2 the second waypoint's location
         * @param body the body on which the waypoints are defined
         * @see InterpolationFactory
         */
        LoxodromeWaypointPVProv(final AbsoluteDate date1, final GeodeticPoint point1, final AbsoluteDate date2,
                final GeodeticPoint point2, final OneAxisEllipsoid body) {
            this.arc = new LoxodromeArc(point1, point2, body);
            this.t0 = date1;
            this.duration = date2.durationFrom(date1);
            this.velocity = arc.getDistance() / duration;
        }

        @Override
        public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
            final double fraction = date.durationFrom(t0) / duration;
            final GeodeticPoint point = arc.calculatePointAlongArc(fraction);
            final Vector3D p = arc.getBody().transform(point);

            return arc.getBody().getBodyFrame().getStaticTransformTo(frame, date).transformPosition(p);
        }

        @Override
        public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
            final double fraction = date.durationFrom(t0) / duration;
            final GeodeticPoint point = arc.calculatePointAlongArc(fraction);
            final Vector3D p = arc.getBody().transform(point);
            final Vector3D vp = arc.getBody().transform(
                    new TopocentricFrame(arc.getBody(), point, "frame")
                        .pointAtDistance(arc.getAzimuth(), 0, velocity));

            final TimeStampedPVCoordinates tpv = new TimeStampedPVCoordinates(date, p, vp.subtract(p));
            return arc.getBody().getBodyFrame().getTransformTo(frame, date).transformPVCoordinates(tpv);
        }
    }

    /**
     * Coordinate provider interpolating along the cartesian (3-space) line between two points.
     */
    static class CartesianWaypointPVProv implements PVCoordinatesProvider {

        /** Date at which the position is valid. */
        private final AbsoluteDate t0;
        /** Initial point. */
        private final Vector3D p0;
        /** Velocity. */
        private final Vector3D vel;
        /** Frame in which the point and velocity are defined. */
        private final Frame sourceFrame;

        /** Class constructor. Aligns to the {@link InterpolationFactory} functional interface.
         *
         * @param date1 the first waypoint's date
         * @param point1 the first waypoint's location
         * @param date2 the second waypoint's date
         * @param point2 the second waypoint's location
         * @param body the body on which the waypoints are defined
         * @see InterpolationFactory
         */
        CartesianWaypointPVProv(final AbsoluteDate date1, final GeodeticPoint point1,
                                final AbsoluteDate date2, final GeodeticPoint point2,
                                final OneAxisEllipsoid body) {
            this.t0 = date1;
            this.p0 = body.transform(point1);
            this.vel = body.transform(point2).subtract(p0).scalarMultiply(1. / date2.durationFrom(t0));
            this.sourceFrame = body.getBodyFrame();
        }

        @Override
        public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
            final double d = date.durationFrom(t0);
            final Vector3D p = p0.add(vel.scalarMultiply(d));
            return sourceFrame.getStaticTransformTo(frame, date).transformPosition(p);
        }

        @Override
        public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
            final double d = date.durationFrom(t0);
            final Vector3D p = p0.add(vel.scalarMultiply(d));
            final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, p, vel);
            return sourceFrame.getTransformTo(frame, date).transformPVCoordinates(pv);
        }

    }
}
