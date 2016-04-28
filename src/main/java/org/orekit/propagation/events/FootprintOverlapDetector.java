/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.enclosing.EnclosingBall;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.spherical.twod.Edge;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.geometry.spherical.twod.Vertex;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Transform;
import org.orekit.models.earth.tessellation.ConstantAzimuthAiming;
import org.orekit.models.earth.tessellation.EllipsoidTessellator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.utils.SphericalPolygonsSetTransferObject;

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
 * <p>The default implementation behavior is to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#CONTINUE continue}
 * propagation at FOV entry and to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#STOP stop} propagation
 * at FOV exit. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @see FieldOfViewDetector
 * @see GeographicZoneDetector
 * @author Luc Maisonobe
 * @since 7.1
 */
public class FootprintOverlapDetector extends AbstractDetector<FootprintOverlapDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150112L;

    /** Field of view. */
    private final transient FieldOfView fov;

    /** Body on which the geographic zone is defined. */
    private final OneAxisEllipsoid body;

    /** Geographic zone to consider. */
    private final transient SphericalPolygonsSet zone;

    /** Linear step used for sampling the geographic zone. */
    private final double samplingStep;

    /** Sampling of the geographic zone. */
    private final transient List<SamplingPoint> sampledZone;

    /** Center of the spherical cap surrounding the zone. */
    private final transient Vector3D capCenter;

    /** Cosine of the radius of the spherical cap surrounding the zone. */
    private final transient double capCos;

    /** Sine of the radius of the spherical cap surrounding the zone. */
    private final transient double capSin;

    /** Build a new instance.
     * <p>The maximal interval between distance to FOV boundary checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param fov sensor field of view
     * @param body body on which the geographic zone is defined
     * @param zone geographic zone to consider
     * @param samplingStep linear step used for sampling the geographic zone (in meters)
     * @exception OrekitException if the geographic zone cannot be sampled
     */
    public FootprintOverlapDetector(final FieldOfView fov,
                                    final OneAxisEllipsoid body,
                                    final SphericalPolygonsSet zone,
                                    final double samplingStep)
        throws OrekitException {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
             new StopOnIncreasing<FootprintOverlapDetector>(),
             fov, body, zone, samplingStep, sample(body, zone, samplingStep));
    }

    /** Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param body body on which the geographic zone is defined
     * @param zone geographic zone to consider
     * @param fov sensor field of view
     * @param sampledZone sampling of the geographic zone
     * @param samplingStep linear step used for sampling the geographic zone (in meters)
     */
    private FootprintOverlapDetector(final double maxCheck, final double threshold,
                                     final int maxIter, final EventHandler<? super FootprintOverlapDetector> handler,
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
        this.capCenter    = cap.getCenter().getVector();
        this.capCos       = FastMath.cos(cap.getRadius());
        this.capSin       = FastMath.sin(cap.getRadius());

    }

    /** Sample the region.
     * @param body body on which the geographic zone is defined
     * @param zone geographic zone to consider
     * @param samplingStep  linear step used for sampling the geographic zone (in meters)
     * @return sampling points
     * @throws OrekitException if the region cannot be sampled
     */
    private static List<SamplingPoint> sample(final OneAxisEllipsoid body,
                                              final SphericalPolygonsSet zone,
                                              final double samplingStep)
        throws OrekitException {

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
                        new EllipsoidTessellator(body, new ConstantAzimuthAiming(body, 0.0), 4);
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
    protected FootprintOverlapDetector create(final double newMaxCheck, final double newThreshold,
                                              final int newMaxIter,
                                              final EventHandler<? super FootprintOverlapDetector> newHandler) {
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
     */
    public FieldOfView getFieldOfView() {
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
    public double g(final SpacecraftState s) throws OrekitException {

        // initial arbitrary positive value
        double value = FastMath.PI;

        // get spacecraft position in body frame
        final Vector3D      scBody      = s.getPVCoordinates(body.getBodyFrame()).getPosition();

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
        final Transform bodyToSc = new Transform(s.getDate(),
                                                 body.getBodyFrame().getTransformTo(s.getFrame(), s.getDate()),
                                                 s.toTransform());
        for (final SamplingPoint point : sampledZone) {
            final Vector3D lineOfSightBody = point.getPosition().subtract(scBody);
            if (Vector3D.dotProduct(lineOfSightBody, point.getZenith()) <= 0) {
                // spacecraft is above this sample point local horizon
                // get line of sight in spacecraft frame
                final double offset = fov.offsetFromBoundary(bodyToSc.transformVector(lineOfSightBody));
                value = FastMath.min(value, offset);
            }
        }

        return value;

    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DTO(this);
    }

    /** Internal class used only for serialization. */
    private static class DTO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20150112L;

        /** Max check interval. */
        private final double maxCheck;

        /** Convergence threshold. */
        private final double threshold;

        /** Maximum number of iterations in the event time search. */
        private final int maxIter;

        /** Body on which the geographic zone is defined. */
        private final OneAxisEllipsoid body;

        /** Field Of View. */
        private final FieldOfView fov;

        /** Proxy for geographic zone. */
        private final SphericalPolygonsSetTransferObject zone;

        /** Linear step used for sampling the geographic zone. */
        private final double samplingStep;

        /** Simple constructor.
         * @param detector instance to serialize
         */
        private DTO(final FootprintOverlapDetector detector) {
            this.maxCheck     = detector.getMaxCheckInterval();
            this.threshold    = detector.getThreshold();
            this.maxIter      = detector.getMaxIterationCount();
            this.fov          = detector.fov;
            this.body         = detector.body;
            this.zone         = new SphericalPolygonsSetTransferObject(detector.zone);
            this.samplingStep = detector.samplingStep;
        }

        /** Replace the deserialized data transfer object with a {@link FootprintOverlapDetector}.
         * @return replacement {@link FootprintOverlapDetector}
         */
        private Object readResolve() {
            try {
                return new FootprintOverlapDetector(fov, body, zone.rebuildZone(), samplingStep).
                                withMaxCheck(maxCheck).
                                withThreshold(threshold).
                                withMaxIter(maxIter);
            } catch (OrekitException oe) {
                // this should never happen as the region as already been sampled before serialization
                throw new OrekitInternalError(oe);
            }
        }

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
