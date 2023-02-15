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
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

/**
 * Container for RTCM 1045 data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class Rtcm1045Data extends RtcmEphemerisData {

    /** Galileo navigation message. */
    private GalileoNavigationMessage galileoNavigationMessage;

    /** Galileo Time of clock. */
    private double galileoToc;

    /** Galileo NAV Data Validity Status. */
    private int galileoDataValidityStatus;

    /** Constructor. */
    public Rtcm1045Data() {
        // Nothing to do ...
    }

    /**
     * Get the Galileo navigation message corresponding to the current RTCM data.
     * <p>
     * This object can be used to initialize a {@link GNSSPropagator}
     * <p>
     * This method uses the {@link DataContext#getDefault()} to initialize
     * the time scales used to configure the reference epochs of the navigation
     * message.
     *
     * @return the Galileo navigation message
     */
    @DefaultDataContext
    public GalileoNavigationMessage getGalileoNavigationMessage() {
        return getGalileoNavigationMessage(DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the Galileo navigation message corresponding to the current RTCM data.
     * <p>
     * This object can be used to initialize a {@link GNSSPropagator}
     * <p>
     * When calling this method, the reference epochs of the navigation message
     * (i.e. ephemeris and clock epochs) are initialized using the provided time scales.
     *
     * @param timeScales time scales to use for initializing epochs
     * @return the Galileo navigation message
     */
    public GalileoNavigationMessage getGalileoNavigationMessage(final TimeScales timeScales) {

        // Satellite system
        final SatelliteSystem system = SatelliteSystem.GALILEO;

        // Week number and time of ephemeris
        final int    week = galileoNavigationMessage.getWeek();
        final double toe  = galileoNavigationMessage.getTime();

        // Set the ephemeris reference data
        galileoNavigationMessage.setDate(new GNSSDate(week, toe, system, timeScales).getDate());
        galileoNavigationMessage.setEpochToc(new GNSSDate(week, galileoToc, system, timeScales).getDate());

        // Return the navigation message
        return galileoNavigationMessage;

    }

    /**
     * Set the Galileo navigation message.
     * @param galileoNavigationMessage the Galileo navigation message to set
     */
    public void setGalileoNavigationMessage(final GalileoNavigationMessage galileoNavigationMessage) {
        this.galileoNavigationMessage = galileoNavigationMessage;
    }

    /**
     * Get the Galileo time of clock.
     * <p>
     * The Galileo time of clock is given in seconds since
     * the beginning of the Galileo week.
     * </p>
     * @return the Galileo time of clock
     */
    public double getGalileoToc() {
        return galileoToc;
    }

    /**
     * Set the Galileo time of clock.
     * @param toc the time of clock to set
     */
    public void setGalileoToc(final double toc) {
        this.galileoToc = toc;
    }

    /**
     * Get the Galileo data validity status.
     * @return the Galileo data validity status
     */
    public int getGalileoDataValidityStatus() {
        return galileoDataValidityStatus;
    }

    /**
     * Set the Galileo data validity status.
     * @param galileoDataValidityStatus the validity status to set
     */
    public void setGalileoDataValidityStatus(final int galileoDataValidityStatus) {
        this.galileoDataValidityStatus = galileoDataValidityStatus;
    }

}
