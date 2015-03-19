/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.models.earth.tessellation;

import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Ellipsoid tessellator aligning tiles with along an orbit track.
 * @see ConstantAzimuthTessellator
 * @author Luc Maisonobe
 */
public class AlongTrackTessellator extends EllipsoidTessellator {

    /** Orbit along which tiles should be aligned. */
    private final Orbit orbit;

    /** Ground track over one half orbit. */
    private final List<Pair<AbsoluteDate, GeodeticPoint>> halfTrack;

    /** Minimum latitude reached. */
    private final double minLat;

    /** Maximum latitude reached. */
    private final double maxLat;

    /** Simple constructor.
     * @param ellipsoid ellipsoid body on which the zone is defined
     * @param width tiles width as a distance on surface (in meters)
     * @param length tiles length as a distance on surface (in meters)
     * @param orbit orbit along which tiles should be aligned
     * @param isAscending indicator for zone tiling with respect to ascending
     * or descending orbits
     * @exception OrekitException if some frame conversion fails
     */
    public AlongTrackTessellator(final OneAxisEllipsoid ellipsoid,
                                final double width, final double length,
                                final Orbit orbit, final boolean isAscending)
        throws OrekitException {
        super(ellipsoid, width, length);
        this.orbit          = orbit;
        this.halfTrack      = findHalfTrack(orbit, getEllipsoid(), isAscending);
        final double lStart = halfTrack.get(0).getSecond().getLatitude();
        final double lEnd   = halfTrack.get(halfTrack.size() - 1).getSecond().getLatitude();
        this.minLat         = FastMath.min(lStart, lEnd);
        this.maxLat         = FastMath.max(lStart, lEnd);
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D alongTileDirection(final GeodeticPoint point)
        throws OrekitException {

        // check the point can be reached
        if (point.getLatitude() < minLat || point.getLatitude() > maxLat) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_LATITUDE,
                                      FastMath.toDegrees(point.getLatitude()),
                                      FastMath.toDegrees(minLat),
                                      FastMath.toDegrees(maxLat));
        }

        // bracket the point in the half track sample
        int index = 1;
        double latBefore = halfTrack.get(0).getSecond().getLatitude();
        while (index < halfTrack.size()) {
            final double latAfter = halfTrack.get(index).getSecond().getLatitude();
            if ((point.getLatitude() - latBefore) * (point.getLatitude() - latAfter) <= 0) {
                // we have found a bracketing for the latitude
                break;
            }
            latBefore = latAfter;
            ++index;
        }
        final AbsoluteDate before = halfTrack.get(index - 1).getFirst();
        final AbsoluteDate after  = halfTrack.get(index).getFirst();

        // find the exact point at which spacecraft crosses specified latitude
        final UnivariateSolver solver           = new BracketingNthOrderBrentSolver(1.0e-3, 5);
        final LatitudeCrossing latitudeCrossing = new LatitudeCrossing(point.getLatitude());
        final double           root             = solver.solve(100, latitudeCrossing,
                                                               before.durationFrom(orbit.getDate()),
                                                               after.durationFrom(orbit.getDate()));
        final TimeStampedPVCoordinates rawPV    = latitudeCrossing.getPV(root);

        // adjust longitude to match the specified one
        final Transform t = new Transform(rawPV.getDate(),
                                          new Rotation(Vector3D.PLUS_K, rawPV.getPosition(),
                                                       Vector3D.PLUS_K, getEllipsoid().transform(point)));
        final TimeStampedPVCoordinates alignedPV = t.transformPVCoordinates(rawPV);

        // find the sliding ground point below spacecraft
        final TimeStampedPVCoordinates groundPV =
                getEllipsoid().projectToGround(alignedPV, getEllipsoid().getBodyFrame());

        // the tile direction is aligned with sliding point velocity
        return groundPV.getVelocity().normalize();

    }

    /** Function evaluating to 0 at some latitude. */
    private class LatitudeCrossing implements UnivariateFunction {

        /** Target latitude. */
        private final double latitude;

        /** Simple constructor.
         * @param latitude target latitude
         */
        public LatitudeCrossing(final double latitude) {
            this.latitude = latitude;
        }

        /** Compute the spacecraft position/velocity in body frame.
         * @param dt time offset since reference orbit
         * @return position/velocity in body frame
         * @exception OrekitException if position cannot be computed at specified date
         */
        public TimeStampedPVCoordinates getPV(final double dt) throws OrekitException {
            return orbit.shiftedBy(dt).getPVCoordinates(getEllipsoid().getBodyFrame());
        }

        /** {@inheritDoc} */
        @Override
        public double value(final double dt) throws OrekitExceptionWrapper {
            try {
                final TimeStampedPVCoordinates pv = getPV(dt);
                final GeodeticPoint            gp = getEllipsoid().transform(pv.getPosition(),
                                                                             getEllipsoid().getBodyFrame(),
                                                                             pv.getDate());
                return gp.getLatitude() - latitude;
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

    }

    /** Find the ascending or descending part of an orbit track.
     * @param orbit orbit along which tiles should be aligned
     * @param ellipsoid ellipsoid over which track is sampled
     * @param isAscending indicator for zone tiling with respect to ascending
     * or descending orbits
     * @return time stamped ground points on the selected half track
     * @exception OrekitException if some frame conversion fails
     */
    private static List<Pair<AbsoluteDate, GeodeticPoint>> findHalfTrack(final Orbit orbit,
                                                                         final OneAxisEllipsoid ellipsoid,
                                                                         final boolean isAscending)
        throws OrekitException {

        try {
            // find the span of the next half track
            final Propagator propagator = new KeplerianPropagator(orbit);
            final HalfTrackSpanHandler handler = new HalfTrackSpanHandler(isAscending);
            propagator.addEventDetector(new HalfTrackSpanDetector(0.25 * orbit.getKeplerianPeriod(),
                                                                  1.0e-3, 100, handler, ellipsoid.getBodyFrame()));
            propagator.propagate(orbit.getDate().shiftedBy(3 * orbit.getKeplerianPeriod()));

            // sample the half track
            propagator.clearEventsDetectors();
            final HalfTrackSampler sampler = new HalfTrackSampler(ellipsoid);
            propagator.setMasterMode(handler.getEnd().durationFrom(handler.getStart()) / 100, sampler);

            return sampler.getHalfTrack();

        } catch (PropagationException pe) {
            if (pe.getCause() instanceof OrekitException) {
                throw (OrekitException) pe.getCause();
            } else {
                throw pe;
            }
        }

    }

}
