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
package org.orekit.models.earth.tessellation;

import java.util.List;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Pair;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.LatitudeExtremumDetector;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class used to orient tiles along an orbit track.
 * @see ConstantAzimuthAiming
 * @author Luc Maisonobe
 */
public class AlongTrackAiming implements TileAiming {

    /** Number of sampling steps for the half-track. */
    private static final int SAMPLING_STEPS = 1000;

    /** Ground track over one half orbit. */
    private final List<Pair<GeodeticPoint, TimeStampedPVCoordinates>> halfTrack;

    /** Simple constructor.
     * @param ellipsoid ellipsoid body on which the zone is defined
     * @param orbit orbit along which tiles should be aligned
     * @param isAscending indicator for zone tiling with respect to ascending
     * or descending orbits
     * @exception OrekitException if some frame conversion fails
     */
    public AlongTrackAiming(final OneAxisEllipsoid ellipsoid, final Orbit orbit, final boolean isAscending)
        throws OrekitException {
        this.halfTrack      = findHalfTrack(orbit, ellipsoid, isAscending);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D alongTileDirection(final Vector3D point, final GeodeticPoint gp)
        throws OrekitException {

        final double lStart = halfTrack.get(0).getFirst().getLatitude();
        final double lEnd   = halfTrack.get(halfTrack.size() - 1).getFirst().getLatitude();
        // check the point can be reached
        if (gp.getLatitude() < FastMath.min(lStart, lEnd) || gp.getLatitude() > FastMath.max(lStart, lEnd)) {
            throw new OrekitException(OrekitMessages.OUT_OF_RANGE_LATITUDE,
                                      FastMath.toDegrees(gp.getLatitude()),
                                      FastMath.toDegrees(FastMath.min(lStart, lEnd)),
                                      FastMath.toDegrees(FastMath.max(lStart, lEnd)));
        }

        // bracket the point in the half track sample
        int    iInf = 0;
        int    iSup = halfTrack.size() - 1;
        while (iSup - iInf > 1) {
            final int iMiddle = (iSup + iInf) / 2;
            if ((lStart < lEnd) ^ (halfTrack.get(iMiddle).getFirst().getLatitude() > gp.getLatitude())) {
                // the specified latitude is in the second half
                iInf = iMiddle;
            } else {
                // the specified latitude is in the first half
                iSup = iMiddle;
            }
        }

        // ensure we can get points at iStart, iStart + 1, iStart + 2 and iStart + 3
        final int iStart = FastMath.max(0, FastMath.min(iInf - 1, halfTrack.size() - 4));

        // interpolate ground sliding point at specified latitude
        final HermiteInterpolator interpolator = new HermiteInterpolator();
        for (int i = iStart; i < iStart + 4; ++i) {
            final Vector3D position = halfTrack.get(i).getSecond().getPosition();
            final Vector3D velocity = halfTrack.get(i).getSecond().getVelocity();
            interpolator.addSamplePoint(halfTrack.get(i).getFirst().getLatitude(),
                                        new double[] {
                                            position.getX(), position.getY(), position.getZ(),
                                            velocity.getX(), velocity.getY(), velocity.getZ()
                                        });
        }
        final DerivativeStructure[] p  = interpolator.value(new DerivativeStructure(1, 1, 0, gp.getLatitude()));

        // extract interpolated ground position/velocity
        final Vector3D position = new Vector3D(p[0].getValue(),
                                               p[1].getValue(),
                                               p[2].getValue());
        final Vector3D velocity = new Vector3D(p[3].getValue(),
                                               p[4].getValue(),
                                               p[5].getValue());

        // adjust longitude to match the specified one
        final Rotation rotation      = new Rotation(Vector3D.PLUS_K, position, Vector3D.PLUS_K, point);
        final Vector3D fixedVelocity = rotation.applyTo(velocity);

        // the tile direction is aligned with sliding point velocity
        return fixedVelocity.normalize();

    }

    /** Find the ascending or descending part of an orbit track.
     * @param orbit orbit along which tiles should be aligned
     * @param ellipsoid ellipsoid over which track is sampled
     * @param isAscending indicator for zone tiling with respect to ascending
     * or descending orbits
     * @return time stamped ground points on the selected half track
     * @exception OrekitException if some frame conversion fails
     */
    private static List<Pair<GeodeticPoint, TimeStampedPVCoordinates>> findHalfTrack(final Orbit orbit,
                                                                                     final OneAxisEllipsoid ellipsoid,
                                                                                     final boolean isAscending)
        throws OrekitException {

        // find the span of the next half track
        final Propagator propagator = new KeplerianPropagator(orbit);
        final HalfTrackSpanHandler handler = new HalfTrackSpanHandler(isAscending);
        final LatitudeExtremumDetector detector =
                        new LatitudeExtremumDetector(0.25 * orbit.getKeplerianPeriod(), 1.0e-3, ellipsoid).
                        withHandler(handler).
                        withMaxIter(100);
        propagator.addEventDetector(detector);
        propagator.propagate(orbit.getDate().shiftedBy(3 * orbit.getKeplerianPeriod()));

        // sample the half track
        propagator.clearEventsDetectors();
        final HalfTrackSampler sampler = new HalfTrackSampler(ellipsoid);
        propagator.setMasterMode(handler.getEnd().durationFrom(handler.getStart()) / SAMPLING_STEPS, sampler);
        propagator.propagate(handler.getStart(), handler.getEnd());

        return sampler.getHalfTrack();

    }

}
