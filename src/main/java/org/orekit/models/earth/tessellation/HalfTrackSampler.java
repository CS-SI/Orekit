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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.util.Pair;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Sampler for half track span.
 * @since 7.1
 * @see AlongTrackAiming
 * @author Luc Maisonobe
 */
class HalfTrackSampler implements OrekitFixedStepHandler {

    /** Ellipsoid over which track is sampled. */
    private final OneAxisEllipsoid ellipsoid;

    /** Half track sample. */
    private final List<Pair<GeodeticPoint, TimeStampedPVCoordinates>> halfTrack;

    /** Simple constructor.
     * @param ellipsoid ellipsoid over which track is sampled
     */
    HalfTrackSampler(final OneAxisEllipsoid ellipsoid) {
        this.ellipsoid = ellipsoid;
        this.halfTrack = new ArrayList<Pair<GeodeticPoint, TimeStampedPVCoordinates>>();
    }

    /** Get half track sample.
     * @return half track sample
     */
    public List<Pair<GeodeticPoint, TimeStampedPVCoordinates>> getHalfTrack() {
        return halfTrack;
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final SpacecraftState currentState, final boolean isLast)
        throws OrekitException {

        // find the sliding ground point below spacecraft
        final TimeStampedPVCoordinates pv       = currentState.getPVCoordinates(ellipsoid.getBodyFrame());
        final TimeStampedPVCoordinates groundPV = ellipsoid.projectToGround(pv, ellipsoid.getBodyFrame());

        // geodetic coordinates
        final GeodeticPoint gp =
                        ellipsoid.transform(groundPV.getPosition(), ellipsoid.getBodyFrame(), currentState.getDate());

        halfTrack.add(new Pair<GeodeticPoint, TimeStampedPVCoordinates>(gp, groundPV));

    }

}
