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
 * Container for data contained in a Galileo navigation message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class GalileoNavigationMessage extends AbstractNavigationMessage {

    /** Issue of Data of the navigation batch. */
    private int iodNav;

    /** Data source.
     * @since 12.0
     */
    private int dataSource;

    /** E1/E5a broadcast group delay (s). */
    private double bgbE1E5a;

    /** E5b/E1 broadcast group delay (s). */
    private double bgdE5bE1;

    /** Signal in space accuracy. */
    private double sisa;

    /** Satellite health status. */
    private double svHealth;

    /** Constructor. */
    public GalileoNavigationMessage() {
        super(GNSSConstants.GALILEO_MU, GNSSConstants.GALILEO_AV, GNSSConstants.GALILEO_WEEK_NB);
    }

    /**
     * Getter for the the Issue Of Data (IOD).
     * @return the Issue Of Data (IOD)
     */
    public int getIODNav() {
        return iodNav;
    }

    /**
     * Setter for the Issue of Data of the navigation batch.
     * @param iod the IOD to set
     */
    public void setIODNav(final int iod) {
        this.iodNav = iod;
    }

    /**
     * Getter for the the data source.
     * @return the data source
     * @since 12.0
     */
    public int getDataSource() {
        return dataSource;
    }

    /**
     * Setter for the data source.
     * @param dataSource data source
     * @since 12.0
     */
    public void setDataSource(final int dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Getter for the E1/E5a broadcast group delay.
     * @return the E1/E5a broadcast group delay (s)
     */
    public double getBGDE1E5a() {
        return bgbE1E5a;
    }

    /**
     * Setter for the E1/E5a broadcast group delay (s).
     * @param bgd the E1/E5a broadcast group delay to set
     */
    public void setBGDE1E5a(final double bgd) {
        this.bgbE1E5a = bgd;
    }

    /**
     * Setter for the E5b/E1 broadcast group delay (s).
     * @param bgd the E5b/E1 broadcast group delay to set
     */
    public void setBGDE5bE1(final double bgd) {
        this.bgdE5bE1 = bgd;
    }

    /**
     * Getter for the the Broadcast Group Delay E5b/E1.
     * @return the Broadcast Group Delay E5b/E1 (s)
     */
    public double getBGDE5bE1() {
        return bgdE5bE1;
    }

    /**
     * Getter for the signal in space accuracy (m).
     * @return the signal in space accuracy
     */
    public double getSisa() {
        return sisa;
    }

    /**
     * Setter for the signal in space accuracy.
     * @param sisa the sisa to set
     */
    public void setSisa(final double sisa) {
        this.sisa = sisa;
    }

    /**
     * Getter for the SV health status.
     * @return the SV health status
     */
    public double getSvHealth() {
        return svHealth;
    }

    /**
     * Setter for the SV health status.
     * @param svHealth the SV health status to set
     */
    public void setSvHealth(final double svHealth) {
        this.svHealth = svHealth;
    }

}
