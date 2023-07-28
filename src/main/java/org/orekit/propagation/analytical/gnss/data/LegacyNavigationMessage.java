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
package org.orekit.propagation.analytical.gnss.data;

/**
 * Container for data contained in a GPS/QZNSS legacy navigation message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class LegacyNavigationMessage extends AbstractNavigationMessage implements GNSSClockElements {

    /** Identifier for message type. */
    public static final String LNAV = "LNAV";

    /** Issue of Data, Ephemeris. */
    private int iode;

    /** Issue of Data, Clock. */
    private int iodc;

    /** Group Delay Differential (s). */
    private double tgd;

    /** The user SV accuracy (m). */
    private double svAccuracy;

    /** Satellite health status. */
    private int svHealth;

    /** Fit interval.
     * @since 12.0
     */
    private int fitInterval;

    /**
     * Constructor.
     * @param mu Earth's universal gravitational parameter
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weekNumber number of weeks in the GNSS cycle
     */
    protected LegacyNavigationMessage(final double mu,
                                      final double angularVelocity,
                                      final int weekNumber) {
        super(mu, angularVelocity, weekNumber);
    }

    /**
     * Getter for the Issue Of Data Ephemeris (IODE).
     * @return the Issue Of Data Ephemeris (IODE)
     */
    public int getIODE() {
        return iode;
    }

    /**
     * Setter for the Issue of Data Ephemeris.
     * @param value the IODE to set
     */
    public void setIODE(final double value) {
        // The value is given as a floating number in the navigation message
        this.iode = (int) value;
    }

    /**
     * Getter for the Issue Of Data Clock (IODC).
     * @return the Issue Of Data Clock (IODC)
     */
    public int getIODC() {
        return iodc;
    }

    /**
     * Setter for the Issue of Data Clock.
     * @param value the IODC to set
     */
    public void setIODC(final int value) {
        this.iodc = value;
    }

    /**
     * Getter for the Group Delay Differential (s).
     * @return the Group Delay Differential in seconds
     */
    public double getTGD() {
        return tgd;
    }

    /**
     * Setter for the Group Delay Differential (s).
     * @param time the group delay differential to set
     */
    public void setTGD(final double time) {
        this.tgd = time;
    }

    /**
     * Getter for the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public double getSvAccuracy() {
        return svAccuracy;
    }

    /**
     * Setter for the user SV accuracy.
     * @param svAccuracy the value to set
     */
    public void setSvAccuracy(final double svAccuracy) {
        this.svAccuracy = svAccuracy;
    }

    /**
     * Getter for the satellite health status.
     * @return the satellite health status
     */
    public int getSvHealth() {
        return svHealth;
    }

    /**
     * Setter for the satellite health status.
     * @param svHealth the value to set
     */
    public void setSvHealth(final int svHealth) {
        this.svHealth = svHealth;
    }

    /**
     * Getter for the fit interval.
     * @return the fit interval
     * @since 12.0
     */
    public int getFitInterval() {
        return fitInterval;
    }

    /**
     * Setter for the fit interval.
     * @param fitInterval fit interval
     * @since 12.0
     */
    public void setFitInterval(final int fitInterval) {
        this.fitInterval = fitInterval;
    }

}
