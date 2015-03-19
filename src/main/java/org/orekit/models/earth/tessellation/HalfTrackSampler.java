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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;

/** Sampler for half track span.
 * @since 7.1
 * @see AlongTrackTessellator
 * @author Luc Maisonobe
 */
class HalfTrackSampler implements OrekitFixedStepHandler {

    /** Ellipsoid over which track is sampled. */
    private final OneAxisEllipsoid ellipsoid;

    /** Half track sample. */
    private final List<Pair<AbsoluteDate, GeodeticPoint>> halfTrack;

    /** Simple constructor.
     * @param ellipsoid ellipsoid over which track is sampled
     */
    public HalfTrackSampler(final OneAxisEllipsoid ellipsoid) {
        this.ellipsoid = ellipsoid;
        this.halfTrack = new ArrayList<Pair<AbsoluteDate, GeodeticPoint>>();
    }

    /** Get half track sample.
     * @return half track sample
     */
    public List<Pair<AbsoluteDate, GeodeticPoint>> getHalfTrack() {
        return halfTrack;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final SpacecraftState currentState, final boolean isLast)
        throws PropagationException {
        try {
            final GeodeticPoint gp = ellipsoid.transform(currentState.getPVCoordinates(ellipsoid.getBodyFrame()).getPosition(),
                                                         ellipsoid.getBodyFrame(), currentState.getDate());
            halfTrack.add(new Pair<AbsoluteDate, GeodeticPoint>(currentState.getDate(), gp));
        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }
    }

}
