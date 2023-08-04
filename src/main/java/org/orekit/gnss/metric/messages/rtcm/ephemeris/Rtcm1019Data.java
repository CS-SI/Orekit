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
import org.orekit.propagation.analytical.gnss.data.GPSLegacyNavigationMessage;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

/**
 * Container for RTCM 1019 data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class Rtcm1019Data extends RtcmEphemerisData {

    /** GPS navigation message. */
    private GPSLegacyNavigationMessage gpsNavigationMessage;

    /** GPS Time of clock. */
    private double gpsToc;

    /** GPS code on L2. */
    private int gpsCodeOnL2;

    /** GPS L2 P data flag. */
    private boolean gpsL2PDataFlag;

    /** GPS fit interval. */
    private int gpsFitInterval;

    /** Constructor. */
    public Rtcm1019Data() {
        // Nothing to do ...
    }

    /**
     * Get the GPS navigation message corresponding to the current RTCM data.
     * <p>
     * This object can be used to initialize a {@link GNSSPropagator}
     * <p>
     * This method uses the {@link DataContext#getDefault()} to initialize
     * the time scales used to configure the reference epochs of the navigation
     * message.
     *
     * @return the GPS navigation message
     */
    @DefaultDataContext
    public GPSLegacyNavigationMessage getGpsNavigationMessage() {
        return getGpsNavigationMessage(DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the GPS navigation message corresponding to the current RTCM data.
     * <p>
     * This object can be used to initialize a {@link GNSSPropagator}
     * <p>
     * When calling this method, the reference epochs of the navigation message
     * (i.e. ephemeris and clock epochs) are initialized using the provided time scales.
     *
     * @param timeScales time scales to use for initializing epochs
     * @return the GPS navigation message
     */
    public GPSLegacyNavigationMessage getGpsNavigationMessage(final TimeScales timeScales) {

        // Satellite system
        final SatelliteSystem system = SatelliteSystem.GPS;

        // Week number and time of ephemeris
        final int    week = gpsNavigationMessage.getWeek();
        final double toe  = gpsNavigationMessage.getTime();

        // Set the ephemeris reference data
        gpsNavigationMessage.setDate(new GNSSDate(week, toe, system, timeScales).getDate());
        gpsNavigationMessage.setEpochToc(new GNSSDate(week, gpsToc, system, timeScales).getDate());

        // Return the navigation message
        return gpsNavigationMessage;

    }

    /**
     * Set the GPS navigation message.
     * @param gpsNavigationMessage the GPS navigation message to set
     */
    public void setGpsNavigationMessage(final GPSLegacyNavigationMessage gpsNavigationMessage) {
        this.gpsNavigationMessage = gpsNavigationMessage;
    }

    /**
     * Get the GPS time of clock.
     * <p>
     * The GPS time of clock is given in seconds since
     * the beginning of the GPS week.
     * </p>
     * @return the GPS time of clock
     */
    public double getGpsToc() {
        return gpsToc;
    }

    /**
     * Set the GPS time of clock.
     * @param toc the time of clock to set
     */
    public void setGpsToc(final double toc) {
        this.gpsToc = toc;
    }

    /**
     * Get the GPS code on L2.
     * <ul>
     *   <li>0: Reserved</li>
     *   <li>1: P code on</li>
     *   <li>2: C/A code on</li>
     *   <li>3: L2C on</li>
     * </ul>
     * @return the GPS code on L2
     */
    public int getGpsCodeOnL2() {
        return gpsCodeOnL2;
    }

    /**
     * Set the GPS code on L2.
     * @param gpsCodeOnL2 the code to set
     */
    public void setGpsCodeOnL2(final int gpsCodeOnL2) {
        this.gpsCodeOnL2 = gpsCodeOnL2;
    }

    /**
     * Get the GPS L2 P-Code data flag.
     * @return true L2 P-Code NAV data ON
     */
    public boolean getGpsL2PDataFlag() {
        return gpsL2PDataFlag;
    }

    /**
     * Set the GPS L2 P-code data flag.
     * @param gpsL2PDataFlag the flag to set
     */
    public void setGpsL2PDataFlag(final boolean gpsL2PDataFlag) {
        this.gpsL2PDataFlag = gpsL2PDataFlag;
    }

    /**
     * Get the GPS fit interval.
     * @return the GPS fit interval
     */
    public int getGpsFitInterval() {
        return gpsFitInterval;
    }

    /**
     * Set the GPS fit interval.
     * @param gpsFitInterval the GPS fit interval to set
     */
    public void setGpsFitInterval(final int gpsFitInterval) {
        this.gpsFitInterval = gpsFitInterval;
    }

}
