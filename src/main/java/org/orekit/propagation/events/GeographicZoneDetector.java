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

import org.hipparchus.geometry.enclosing.EnclosingBall;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/** Detector for entry/exit of a zone defined by geographic boundaries.
 * <p>This detector identifies when a spacecraft crosses boundaries of
 * general shapes defined on the surface of the globe. Typical shapes
 * of interest can be countries, land masses or physical areas like
 * the south atlantic anomaly. Shapes can be arbitrarily complicated:
 * convex or non-convex, in one piece or several non-connected islands,
 * they can include poles, they can have holes like the Caspian Sea (this
 * would be a hole only if one is interested in land masses, of course).
 * Complex shapes involve of course more computing time than simple shapes.</p>
 * @see FootprintOverlapDetector
 * @author Luc Maisonobe
 * @since 6.2
 */
public class GeographicZoneDetector extends AbstractDetector<GeographicZoneDetector> {

    /** Body on which the geographic zone is defined. */
    private BodyShape body;

    /** Zone definition. */
    private final SphericalPolygonsSet zone;

    /** Spherical cap surrounding the zone. */
    private final EnclosingBall<Sphere2D, S2Point> cap;

    /** Margin to apply to the zone. */
    private final double margin;

    /** Build a new detector.
     * <p>The new instance uses default values for maximal checking interval
     * ({@link #DEFAULT_MAXCHECK}) and convergence threshold ({@link
     * #DEFAULT_THRESHOLD}).</p>
     * @param body body on which the geographic zone is defined
     * @param zone geographic zone to consider
     * @param margin angular margin to apply to the zone
     */
    public GeographicZoneDetector(final BodyShape body,
                                  final SphericalPolygonsSet zone,  final double margin) {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, body, zone, margin);
    }

    /** Build a detector.
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param body body on which the geographic zone is defined
     * @param zone geographic zone to consider
     * @param margin angular margin to apply to the zone
     */
    public GeographicZoneDetector(final double maxCheck, final double threshold,
                                  final BodyShape body,
                                  final SphericalPolygonsSet zone,  final double margin) {
        this(s -> maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnIncreasing(),
             body, zone, zone.getEnclosingCap(), margin);
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
     * @param cap spherical cap surrounding the zone
     * @param margin angular margin to apply to the zone
     */
    protected GeographicZoneDetector(final AdaptableInterval maxCheck, final double threshold,
                                     final int maxIter, final EventHandler handler,
                                     final BodyShape body,
                                     final SphericalPolygonsSet zone,
                                     final EnclosingBall<Sphere2D, S2Point> cap,
                                     final double margin) {
        super(maxCheck, threshold, maxIter, handler);
        this.body   = body;
        this.zone   = zone;
        this.cap    = cap;
        this.margin = margin;
    }

    /** {@inheritDoc} */
    @Override
    protected GeographicZoneDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                            final int newMaxIter, final EventHandler newHandler) {
        return new GeographicZoneDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                          body, zone, cap, margin);
    }

    /**
     * Setup the detector margin.
     * @param newMargin angular margin to apply to the zone
     * @return a new detector with updated configuration (the instance is not changed)
     */
    public GeographicZoneDetector withMargin(final double newMargin) {
        return new GeographicZoneDetector(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), getHandler(),
                                          body, zone, cap, newMargin);
    }

    /** Get the body on which the geographic zone is defined.
     * @return body on which the geographic zone is defined
     */
    public BodyShape getBody() {
        return body;
    }

    /** Get the geographic zone.
     * @return the geographic zone
     */
    public SphericalPolygonsSet getZone() {
        return zone;
    }

    /** Get the angular margin to apply (radians).
     * @return the angular margin to apply (radians)
     */
    public double getMargin() {
        return margin;
    }

    /** Compute the value of the detection function.
     * <p>
     * The value is the signed distance to boundary, minus the margin. It is
     * positive if the spacecraft is outside of the zone and negative if it is inside.
     * </p>
     * @param s the current state information: date, kinematics, attitude
     * @return signed distance to boundary minus the margin
     */
    public double g(final SpacecraftState s) {

        // convert state to geodetic coordinates
        final GeodeticPoint gp = body.transform(s.getPosition(),
                                                s.getFrame(), s.getDate());

        // map the point to a sphere (geodetic coordinates have already taken care of ellipsoid flatness)
        final S2Point s2p = new S2Point(gp.getLongitude(), 0.5 * FastMath.PI - gp.getLatitude());

        // for faster computation, we start using only the surrounding cap, to filter out
        // far away points (which correspond to most of the points if the zone is small)
        final double crudeDistance = cap.getCenter().distance(s2p) - cap.getRadius();
        if (crudeDistance - margin > FastMath.max(FastMath.abs(margin), 0.01)) {
            // we know we are strictly outside of the zone,
            // use the crude distance to compute the (positive) return value
            return crudeDistance - margin;
        }

        // we are close, we need to compute carefully the exact offset
        // project the point to the closest zone boundary
        return zone.projectToBoundary(s2p).getOffset() - margin;

    }

}
