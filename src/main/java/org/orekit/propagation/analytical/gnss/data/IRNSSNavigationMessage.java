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
 * Container for data contained in an IRNSS navigation message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class IRNSSNavigationMessage extends AbstractNavigationMessage  {

    /** Issue of Data, Ephemeris and Clock. */
    private int iodec;

    /** Group Delay Differential (s). */
    private double tgd;

    /** User range accuracy (m). */
    private double ura;

    /** Satellite health status. */
    private double svHealth;

    /** Constructor. */
    public IRNSSNavigationMessage() {
        super(GNSSConstants.IRNSS_MU, GNSSConstants.IRNSS_AV, GNSSConstants.IRNSS_WEEK_NB);
    }

    /**
     * Getter for the Issue Of Data Ephemeris and Clock (IODEC).
     * @return the Issue Of Data Ephemeris and Clock (IODEC)
     */
    public int getIODEC() {
        return iodec;
    }

    /**
     * Setter for the Issue of Data, Ephemeris and Clock.
     * @param value the IODEC to set
     */
    public void setIODEC(final double value) {
        // The value is given as a floating number in the navigation message
        this.iodec = (int) value;
    }

    /**
     * Getter for the estimated group delay differential TGD for L5-S correction.
     * @return the estimated group delay differential TGD for L5-S correction (s)
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
     * Getter for the user range accuray (meters).
     * @return the user range accuracy
     */
    public double getURA() {
        return ura;
    }

    /**
     * Setter for the user range accuracy.
     * @param accuracy the value to set
     */
    public void setURA(final double accuracy) {
        this.ura = accuracy;
    }

    /**
     * Getter for the satellite health status.
     * @return the satellite health status
     */
    public double getSvHealth() {
        return svHealth;
    }

    /**
     * Setter for the satellite health status.
     * @param svHealth the value to set
     */
    public void setSvHealth(final double svHealth) {
        this.svHealth = svHealth;
    }

}
