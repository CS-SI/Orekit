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
import org.orekit.propagation.analytical.gnss.data.QZSSLegacyNavigationMessage;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

/**
 * Container for RTCM 1044 data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class Rtcm1044Data extends RtcmEphemerisData {

    /** QZSS navigation message. */
    private QZSSLegacyNavigationMessage qzssNavigationMessage;

    /** QZSS Time of clock. */
    private double qzssToc;

    /** QZSS code on L2 Channel. */
    private int qzssCodeOnL2;

    /** QZSS fit interval. */
    private int qzssFitInterval;

    /** Constructor. */
    public Rtcm1044Data() {
        // Nothing to do ...
    }

    /**
     * Get the QZSS navigation message corresponding to the current RTCM data.
     * <p>
     * This object can be used to initialize a {@link GNSSPropagator}
     * <p>
     * This method uses the {@link DataContext#getDefault()} to initialize
     * the time scales used to configure the reference epochs of the navigation
     * message.
     *
     * @return the QZSS navigation message
     */
    @DefaultDataContext
    public QZSSLegacyNavigationMessage getQzssNavigationMessage() {
        return getQzssNavigationMessage(DataContext.getDefault().getTimeScales());
    }

    /**
     * Get the QZSS navigation message corresponding to the current RTCM data.
     * <p>
     * This object can be used to initialize a {@link GNSSPropagator}
     * <p>
     * When calling this method, the reference epochs of the navigation message
     * (i.e. ephemeris and clock epochs) are initialized using the provided time scales.
     *
     * @param timeScales time scales to use for initializing epochs
     * @return the QZSS navigation message
     */
    public QZSSLegacyNavigationMessage getQzssNavigationMessage(final TimeScales timeScales) {

        // Satellite system
        final SatelliteSystem system = SatelliteSystem.QZSS;

        // Week number and time of ephemeris
        final int    week = qzssNavigationMessage.getWeek();
        final double toe  = qzssNavigationMessage.getTime();

        // Set the ephemeris reference data
        qzssNavigationMessage.setDate(new GNSSDate(week, toe, system, timeScales).getDate());
        qzssNavigationMessage.setEpochToc(new GNSSDate(week, qzssToc, system, timeScales).getDate());

        // Return the navigation message
        return qzssNavigationMessage;

    }

    /**
     * Set the QZSS navigation message.
     * @param qzssNavigationMessage the QZSS navigation message to set
     */
    public void setQzssNavigationMessage(final QZSSLegacyNavigationMessage qzssNavigationMessage) {
        this.qzssNavigationMessage = qzssNavigationMessage;
    }

    /**
     * Get the QZSS time of clock.
     * <p>
     * The QZSS time of clock is given in seconds since
     * the beginning of the QZSS week.
     * </p>
     * @return the QZSS time of clock
     */
    public double getQzssToc() {
        return qzssToc;
    }

    /**
     * Set the QZSS time of clock.
     * @param toc the time of clock to set
     */
    public void setQzssToc(final double toc) {
        this.qzssToc = toc;
    }

    /**
     * Get the QZSS code on L2 Channel.
     * @return the QZSS code on L2
     */
    public int getQzssCodeOnL2() {
        return qzssCodeOnL2;
    }

    /**
     * Set the QZSS code on L2.
     * @param qzssCodeOnL2 the code to set
     */
    public void setQzssCodeOnL2(final int qzssCodeOnL2) {
        this.qzssCodeOnL2 = qzssCodeOnL2;
    }

    /**
     * Get the QZSS fit interval.
     * @return the QZSS fit interval
     */
    public int getQzssFitInterval() {
        return qzssFitInterval;
    }

    /**
     * Set the QZSS fit interval.
     * @param qzssFitInterval the QZSS fit interval to set
     */
    public void setQzssFitInterval(final int qzssFitInterval) {
        this.qzssFitInterval = qzssFitInterval;
    }

}
