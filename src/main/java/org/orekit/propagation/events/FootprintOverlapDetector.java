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
package org.orekit.propagation.events;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.enclosing.EnclosingBall;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.spherical.twod.Edge;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.geometry.spherical.twod.Vertex;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.StaticTransform;
import org.orekit.geometry.fov.FieldOfView;
import org.orekit.models.earth.tessellation.DivertedSingularityAiming;
import org.orekit.models.earth.tessellation.EllipsoidTessellator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/** Detector triggered by geographical region entering/leaving a spacecraft sensor
 * {@link FieldOfView Field Of View}.
 * <p>
 * This detector is a mix between to {@link FieldOfViewDetector} and {@link
 * GeographicZoneDetector}. Similar to the first detector above, it triggers events
 * related to entry/exit of targets in a Field Of View, taking attitude into account.
 * Similar to the second detector above, its target is an entire geographic region
 * (which can even be split in several non-connected patches and can have holes).
 * </p>
 * <p>
 * This detector is typically used for ground observation missions with agile
 * satellites than can look away from nadir.
 * </p>
 * <p>The default implementation behavior is to {@link Action#CONTINUE continue}
 * propagation at FOV entry and to {@link Action#STOP stop} propagation
 * at FOV exit. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @see FieldOfViewDetector
 * @see GeographicZoneDetector
 * @author Luc Maisonobe
 * @since 7.1
 */
public class FootprintOverlapDetector extends AbstractDetector<FootprintOverlapDetector> {

    /** Field of view. */
    private final FieldOfView fov;

    /** Body on which the geographic zone is defined. */
    private final OneAxisEllipsoid body;

    /** Geographic zone to consider. */
    private final SphericalPolygonsSet zone;

    /** Linear step used for sampling the geographic zone. */
    private final double samplingStep;

    /** Sampling of the geographic zone. */
    private final List<SamplingPoint> sampledZone;

    /** Center of the spherical cap surrounding the zone. */
    private final Vector3D capCenter;

    /** Cosine of the radius of the spherical cap surrounding the zone. */
    private final double capCos;

    /** Sine of the radius of the spherical cap surrounding the zone. */
    private final double capSin;

    /** Build a new instance.
     * <p>The maximal interval between distance to FOV boundary checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param fov sensor field of view
     * @param body body on which the geographic zone is defined
     * @param zone geographic zone to consider
     * @param samplingStep linear step used for sampling the geographic zone (in meters)
     * @since 10.1
     */
    public FootprintOverlapDetector(final FieldOfView fov,
                                    final OneAxisEllipsoid body,
                                    final SphericalPolygonsSet zone,
                                    final double samplingStep) {
        this(s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
             new StopOnIncreasing(),
             fov, body, zone, samplingStep, sample(body, zone, samplingStep));
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param body body on which the geographic zone is defined
     * @param zone geographic zone to consider
     * @param fov sensor field of view
     * @param sampledZone sampling of the geographic zone
     * @param samplingStep linear step used for sampling the geographic zone (in meters)
     */
    protected FootprintOverlapDetector(final AdaptableInterval maxCheck, final double threshold,
                                       final int maxIter, final EventHandler handler,
                                       final FieldOfView fov,
                                       final OneAxisEllipsoid body,
                                       final SphericalPolygonsSet zone,
                                       final double samplingStep,
                                       final List<SamplingPoint> sampledZone) {

        super(maxCheck, threshold, maxIter, handler);
        this.fov          = fov;
        this.body         = body;
        this.samplingStep = samplingStep;
        this.zone         = zone;
        this.sampledZone  = sampledZone;

        final EnclosingBall<Sphere2D, S2Point> cap = zone.getEnclosingCap();
        final SinCos sc = FastMath.sinCos(cap.getRadius());
        this.capCenter    = cap.getCenter().getVector();
        this.capCos       = sc.cos();
        this.capSin       = sc.sin();

    }

    /** Sample the region.
     * @param body body on which the geographic zone is defined
     * @param zone geographic zone to consider
     * @param samplingStep  linear step used for sampling the geographic zone (in meters)
     * @return sampling points
     */
    private static List<SamplingPoint> sample(final OneAxisEllipsoid body,
                                              final SphericalPolygonsSet zone,
                                              final double samplingStep) {

        final List<SamplingPoint> sampledZone = new ArrayList<SamplingPoint>();

        // sample the zone boundary
        final List<Vertex> boundary = zone.getBoundaryLoops();
        for (final Vertex loopStart : boundary) {
            int count = 0;
            for (Vertex v = loopStart; count == 0 || v != loopStart; v = v.getOutgoing().getEnd()) {
                ++count;
                final Edge edge = v.getOutgoing();
                final int n = (int) FastMath.ceil(edge.getLength() * body.getEquatorialRadius() / samplingStep);
                for (int i = 0; i < n; ++i) {
                    final S2Point intermediate = new S2Point(edge.getPointAt(i * edge.getLength() / n));
                    final GeodeticPoint gp = new GeodeticPoint(0.5 * FastMath.PI - intermediate.getPhi(),
                                                               intermediate.getTheta(), 0.0);
                    sampledZone.add(new SamplingPoint(body.transform(gp), gp.getZenith()));
                }
            }
        }

        // sample the zone interior
        final EllipsoidTessellator tessellator =
                        new EllipsoidTessellator(body, new DivertedSingularityAiming(zone), 1);
        final List<List<GeodeticPoint>> gpSample = tessellator.sample(zone, samplingStep, samplingStep);
        for (final List<GeodeticPoint> list : gpSample) {
            for (final GeodeticPoint gp : list) {
                sampledZone.add(new SamplingPoint(body.transform(gp), gp.getZenith()));
            }
        }

        return sampledZone;

    }

    /** {@inheritDoc} */
    @Override
    protected FootprintOverlapDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                              final int newMaxIter,
                                              final EventHandler newHandler) {
        return new FootprintOverlapDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                            fov, body, zone, samplingStep, sampledZone);
    }

    /** Get the geographic zone triggering the events.
     * <p>
     * The zone is mapped on the unit sphere
     * </p>
     * @return geographic zone triggering the events
     */
    public SphericalPolygonsSet getZone() {
        return zone;
    }

    /** Get the Field Of View.
     * @return Field Of View
     * @since 10.1
     */
    public FieldOfView getFOV() {
        return fov;
    }

    /** Get the body on which the geographic zone is defined.
     * @return body on which the geographic zone is defined
     */
    public BodyShape getBody() {
        return body;
    }

    /** {@inheritDoc}
     * <p>
     * The g function value is the minimum offset among the region points
     * with respect to the Field Of View boundary. It is positive if all region
     * points are outside of the Field Of View, and negative if at least some
     * of the region points are inside of the Field Of View. The minimum is
     * computed by sampling the region, considering only the points for which
     * the spacecraft is above the horizon. The accuracy of the detection
     * depends on the linear sampling step set at detector construction. If
     * the spacecraft is below horizon for all region points, an arbitrary
     * positive value is returned.
     * </p>
     * <p>
     * As per the previous definition, when the region enters the Field Of
     * View, a decreasing event is generated, and when the region leaves
     * the Field Of View, an increasing event is generated.
     * </p>
     */
    public double g(final SpacecraftState s) {

        // initial arbitrary positive value
        double value = FastMath.PI;

        // get spacecraft position in body frame
        final Vector3D      scBody      = s.getPosition(body.getBodyFrame());

        // map the point to a sphere
        final GeodeticPoint gp          = body.transform(scBody, body.getBodyFrame(), s.getDate());
        final S2Point       s2p         = new S2Point(gp.getLongitude(), 0.5 * FastMath.PI - gp.getLatitude());

        // for faster computation, we start using only the surrounding cap, to filter out
        // far away points (which correspond to most of the points if the zone is small)
        final Vector3D p   = s2p.getVector();
        final double   dot = Vector3D.dotProduct(p, capCenter);
        if (dot < capCos) {
            // the spacecraft is outside of the cap, look for the closest cap point
            final Vector3D t     = p.subtract(dot, capCenter).normalize();
            final Vector3D close = new Vector3D(capCos, capCenter, capSin, t);
            if (Vector3D.dotProduct(p, close) < -0.01) {
                // the spacecraft is not visible from the cap edge,
                // even taking some margin into account for sphere/ellipsoid different shapes
                // this induces no points in the zone can see the spacecraft,
                // we can return the arbitrary initial positive value without performing further computation
                return value;
            }
        }

        // the spacecraft may be visible from some points in the zone, check them all
        final StaticTransform bodyToSc = StaticTransform.compose(
                s.getDate(),
                body.getBodyFrame().getStaticTransformTo(s.getFrame(), s.getDate()),
                s.toTransform());
        for (final SamplingPoint point : sampledZone) {
            final Vector3D lineOfSightBody = point.getPosition().subtract(scBody);
            if (Vector3D.dotProduct(lineOfSightBody, point.getZenith()) <= 0) {
                // spacecraft is above this sample point local horizon
                // get line of sight in spacecraft frame
                final double offset = fov.offsetFromBoundary(bodyToSc.transformVector(lineOfSightBody),
                                                             0.0, VisibilityTrigger.VISIBLE_ONLY_WHEN_FULLY_IN_FOV);
                value = FastMath.min(value, offset);
            }
        }

        return value;

    }

    /** Container for sampling points. */
    private static class SamplingPoint {

        /** Position of the point. */
        private final Vector3D position;

        /** Zenith vector of the point. */
        private final Vector3D zenith;

        /** Simple constructor.
         * @param position position of the point
         * @param zenith zenith vector of the point
         */
        SamplingPoint(final Vector3D position, final Vector3D zenith) {
            this.position = position;
            this.zenith   = zenith;
        }

        /** Get the point position.
         * @return point position
         */
        public Vector3D getPosition() {
            return position;
        }

        /** Get the point zenith vector.
         * @return point zenith vector
         */
        public Vector3D getZenith() {
            return zenith;
        }

    }

}
