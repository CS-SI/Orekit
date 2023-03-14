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
package org.orekit.gnss.metric.messages.rtcm.ephemeris;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.GNSSPropagator;
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

/**
 * Container for RTCM 1042 data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class Rtcm1042Data extends RtcmEphemerisData {

    /** Beidou navigation message. */
    private BeidouLegacyNavigationMessage beidouNavigationMessage;

    /** Beidou Time of clock. */
    private double beidouToc;

    /** Satellite health status. */
    private double svHealth;

    /** Constructor. */
    public Rtcm1042Data() {
        // Nothing to do ...
    }

    /**
     * Get the Beidou navigation message corresponding to the current RTCM data.
     * <p>
     * This object can be used to initialize a {@link GNSSPropagator}
     * <p>
     * This method uses the {@link DataContext#getDefault()} to initialize
     * the time scales used to configure the reference epochs of the navigation
     * message.
     *
     * @return the Beidou navigation message
     */
    @DefaultDataContext
    public BeidouLegacyNavigationMessage getBeidouNavigationMessage() {
        return getBeidouNavigationMessage(DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the Beidou navigation message corresponding to the current RTCM data.
     * <p>
     * This object can be used to initialize a {@link GNSSPropagator}
     * <p>
     * When calling this method, the reference epochs of the navigation message
     * (i.e. ephemeris and clock epochs) are initialized using the provided time scales.
     *
     * @param timeScales time scales to use for initializing epochs
     * @return the Beidou navigation message
     */
    public BeidouLegacyNavigationMessage getBeidouNavigationMessage(final TimeScales timeScales) {

        // Satellite system
        final SatelliteSystem system = SatelliteSystem.BEIDOU;

        // Week number and time of ephemeris
        final int    week = beidouNavigationMessage.getWeek();
        final double toe  = beidouNavigationMessage.getTime();

        // Set the ephemeris reference data
        beidouNavigationMessage.setDate(new GNSSDate(week, toe, system, timeScales).getDate());
        beidouNavigationMessage.setEpochToc(new GNSSDate(week, beidouToc, system, timeScales).getDate());

        // Return the navigation message
        return beidouNavigationMessage;

    }

    /**
     * Set the Beidou navigation message.
     * @param beidouNavigationMessage the Beidou navigation message to set
     */
    public void setBeidouNavigationMessage(final BeidouLegacyNavigationMessage beidouNavigationMessage) {
        this.beidouNavigationMessage = beidouNavigationMessage;
    }

    /**
     * Get the Beidou time of clock.
     * <p>
     * The Beidou time of clock is given in seconds since
     * the beginning of the Beidou week.
     * </p>
     * @return the Beidou time of clock
     */
    public double getBeidouToc() {
        return beidouToc;
    }

    /**
     * Set the Beidou time of clock.
     * @param toc the time of clock to set
     */
    public void setBeidouToc(final double toc) {
        this.beidouToc = toc;
    }

    /**
     * Get the satellite health status.
     * @return the satellite health status
     */
    public double getSvHealth() {
        return svHealth;
    }

    /**
     * Set the satellite health status.
     * @param svHealth the health status to set
     */
    public void setSvHealth(final double svHealth) {
        this.svHealth = svHealth;
    }

}
