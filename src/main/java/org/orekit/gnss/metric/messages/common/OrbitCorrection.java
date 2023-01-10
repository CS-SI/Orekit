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
package org.orekit.gnss.metric.messages.common;

/**
 * Container for SSR orbit correction data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class OrbitCorrection {

    /** Radial orbit correction for broadcast ephemeris (m). */
    private final double deltaOrbitRadial;

    /** Along-Track orbit correction for broadcast ephemeris (m). */
    private final double deltaOrbitAlongTrack;

    /** Cross-Track orbit correction for broadcast ephemeris (m). */
    private final double deltaOrbitCrossTrack;

    /** Velocity of Radial orbit correction for broadcast ephemeris. (m/s). */
    private final double dotOrbitDeltaRadial;

    /** Velocity of Along-Track orbit correction for broadcast ephemeris (m/s). */
    private final double dotOrbitDeltaAlongTrack;

    /** Velocity of Cross-Track orbit correction for broadcast ephemeris (m/s). */
    private final double dotOrbitDeltaCrossTrack;

    /**
     * Constructor.
     * @param dRadial radial orbit correction for broadcast ephemeris (m)
     * @param dAlongTrack along-Track orbit correction for broadcast ephemeris (m)
     * @param dCrossTrack cross-Track orbit correction for broadcast ephemeris (m)
     * @param dotRadial velocity of Radial orbit correction for broadcast ephemeris. (m/s)
     * @param dotAlongTrack velocity of Along-Track orbit correction for broadcast ephemeris (m/s)
     * @param dotCrossTrack velocity of Cross-Track orbit correction for broadcast ephemeris (m/s)
     */
    public OrbitCorrection(final double dRadial, final double dAlongTrack,
                           final double dCrossTrack, final double dotRadial,
                           final double dotAlongTrack, final double dotCrossTrack) {
        // Initialize fields
        this.deltaOrbitRadial        = dRadial;
        this.deltaOrbitAlongTrack    = dAlongTrack;
        this.deltaOrbitCrossTrack    = dCrossTrack;
        this.dotOrbitDeltaRadial     = dotRadial;
        this.dotOrbitDeltaAlongTrack = dotAlongTrack;
        this.dotOrbitDeltaCrossTrack = dotCrossTrack;
    }

    /**
     * Get the radial orbit correction for broadcast ephemeris.
     * <p>
     * The reference time t0 is SSR Epoch Time (IDF003) plus ½ SSR Update Interval.
     * </p>
     * @return the radial orbit correction for broadcast ephemeris in meters
     */
    public double getDeltaOrbitRadial() {
        return deltaOrbitRadial;
    }

    /**
     * Get the along-track orbit correction for broadcast ephemeris.
     * <p>
     * The reference time t0 is SSR Epoch Time (IDF003) plus ½ SSR Update Interval.
     * </p>
     * @return the along-track orbit correction for broadcast ephemeris in meters
     */
    public double getDeltaOrbitAlongTrack() {
        return deltaOrbitAlongTrack;
    }

    /**
     * Get the cross-track orbit correction for broadcast ephemeris.
     * <p>
     * The reference time t0 is SSR Epoch Time (IDF003) plus ½ SSR Update Interval.
     * </p>
     * @return the cross-track orbit correction for broadcast ephemeris
     */
    public double getDeltaOrbitCrossTrack() {
        return deltaOrbitCrossTrack;
    }

    /**
     * Get the velocity of radial orbit correction for broadcast ephemeris.
     * <p>
     * The reference time t0 is SSR Epoch Time (IDF003) plus ½ SSR Update Interval.
     * </p>
     * @return the velocity of Radial orbit correction for broadcast ephemeris in m/s
     */
    public double getDotOrbitDeltaRadial() {
        return dotOrbitDeltaRadial;
    }

    /**
     * Get the velocity of along-track orbit correction for broadcast ephemeris.
     * <p>
     * The reference time t0 is SSR Epoch Time (IDF003) plus ½ SSR Update Interval.
     * </p>
     * @return the velocity of along-track orbit correction for broadcast ephemeris in m/s
     */
    public double getDotOrbitDeltaAlongTrack() {
        return dotOrbitDeltaAlongTrack;
    }

    /**
     * Get the velocity of cross-track orbit correction for broadcast ephemeris.
     * <p>
     * The reference time t0 is SSR Epoch Time (IDF003) plus ½ SSR Update Interval.
     * </p>
     * @return the velocity of cross-track orbit correction for broadcast ephemeris in m/s
     */
    public double getDotOrbitDeltaCrossTrack() {
        return dotOrbitDeltaCrossTrack;
    }

}
