/* Copyright 2002-2021 CS GROUP
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
package org.orekit.gnss.metric.messages.ssr.igm;

/**
 * Container for SSR IGM01 data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIgm01Data extends SsrIgmData {

    /** GNSS IOD. */
    private int gnssIod;

    /** Radial orbit correction for broadcast ephemeris (m). */
    private double deltaOrbitRadial;

    /** Along-Track orbit correction for broadcast ephemeris (m). */
    private double deltaOrbitAlongTrack;

    /** Cross-Track orbit correction for broadcast ephemeris (m). */
    private double deltaOrbitCrossTrack;

    /** Velocity of Radial orbit correction for broadcast ephemeris. (m/s). */
    private double dotOrbitDeltaRadial;

    /** Velocity of Along-Track orbit correction for broadcast ephemeris (m/s). */
    private double dotOrbitDeltaAlongTrack;

    /** Velocity of Cross-Track orbit correction for broadcast ephemeris (m/s). */
    private double dotOrbitDeltaCrossTrack;

    /** Constructor. */
    public SsrIgm01Data() {
        // Nothing to do ...
    }

    /**
     * Get the GNSS IOD.
     * <p>
     * Users have to interpret the IOD value depending the
     * satellite system of the current message.
     * </p>
     * @return the GNSS IOD
     */
    public int getGnssIod() {
        return gnssIod;
    }

    /**
     * Set the GNSS IOD.
     * @param gnssIod the GNSS IOD to set
     */
    public void setGnssIod(final int gnssIod) {
        this.gnssIod = gnssIod;
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
     * Set the radial orbit correction for broadcast ephemeris.
     * @param deltaOrbitRadial the correction to set in meters
     */
    public void setDeltaOrbitRadial(final double deltaOrbitRadial) {
        this.deltaOrbitRadial = deltaOrbitRadial;
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
     * Set the along-track orbit correction for broadcast ephemeris.
     * @param deltaOrbitAlongTrack the correction to set in meters
     */
    public void setDeltaOrbitAlongTrack(final double deltaOrbitAlongTrack) {
        this.deltaOrbitAlongTrack = deltaOrbitAlongTrack;
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
     * Set the cross-track orbit correction for broadcast ephemeris.
     * @param deltaOrbitCrossTrack the correction to set in meters
     */
    public void setDeltaOrbitCrossTrack(final double deltaOrbitCrossTrack) {
        this.deltaOrbitCrossTrack = deltaOrbitCrossTrack;
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
     * Set the velocity of radial orbit correction for broadcast ephemeris.
     * @param dotOrbitDeltaRadial the correction to set in m/s
     */
    public void setDotOrbitDeltaRadial(final double dotOrbitDeltaRadial) {
        this.dotOrbitDeltaRadial = dotOrbitDeltaRadial;
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
     * Set the velocity of along-track orbit correction for broadcast ephemeris.
     * @param dotOrbitDeltaAlongTrack the correction to set in m/s
     */
    public void setDotOrbitDeltaAlongTrack(final double dotOrbitDeltaAlongTrack) {
        this.dotOrbitDeltaAlongTrack = dotOrbitDeltaAlongTrack;
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

    /**
     * Set the velocity of cross-track orbit correction for broadcast ephemeris.
     * @param dotOrbitDeltaCrossTrack the correction to set in m/s
     */
    public void setDotOrbitDeltaCrossTrack(final double dotOrbitDeltaCrossTrack) {
        this.dotOrbitDeltaCrossTrack = dotOrbitDeltaCrossTrack;
    }

}
