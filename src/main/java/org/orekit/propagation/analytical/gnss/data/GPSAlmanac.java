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
 * This class holds a GPS almanac as read from SEM or YUMA files.
 *
 * <p>Depending on the source (SEM or YUMA), some fields may be filled in or not.
 * An almanac read from a YUMA file doesn't hold SVN number, average URA and satellite
 * configuration.</p>
 *
 * @author Pascal Parraud
 * @since 8.0
 *
 */
public class GPSAlmanac extends AbstractAlmanac implements GNSSClockElements {

    /** Source of the almanac. */
    private String src;

    /** SVN number. */
    private int svn;

    /** Health status. */
    private int health;

    /** Average URA. */
    private int ura;

    /** Satellite configuration. */
    private int config;

    /**
     * Constructor.
     */
    public GPSAlmanac() {
        super(GNSSConstants.GPS_MU, GNSSConstants.GPS_AV, GNSSConstants.GPS_WEEK_NB);
    }

    /**
     * Setter for the Square Root of Semi-Major Axis (m^1/2).
     * <p>
     * In addition, this method set the value of the Semi-Major Axis.
     * </p>
     * @param sqrtA the Square Root of Semi-Major Axis (m^1/2)
     */
    public void setSqrtA(final double sqrtA) {
        super.setSma(sqrtA * sqrtA);
    }

    /**
     * Gets the source of this GPS almanac.
     * <p>Sources can be SEM or YUMA, when the almanac is read from a file.</p>
     *
     * @return the source of this GPS almanac
     */
    public String getSource() {
        return src;
    }

    /**
     * Sets the source of this GPS almanac.
     *
     * @param source the source of this GPS almanac
     */
    public void setSource(final String source) {
        this.src = source;
    }

    /**
     * Gets the satellite "SVN" reference number.
     *
     * @return the satellite "SVN" reference number
     */
    public int getSVN() {
        return svn;
    }

    /**
     * Sets the "SVN" reference number.
     *
     * @param svnNumber the number to set
     */
    public void setSVN(final int svnNumber) {
        this.svn = svnNumber;
    }

    /**
     * Gets the Health status.
     *
     * @return the Health status
     */
    public int getHealth() {
        return health;
    }

    /**
     * Sets the health status.
     *
     * @param health the health status to set
     */
    public void setHealth(final int health) {
        this.health = health;
    }

    /**
     * Gets the average URA number.
     *
     * @return the average URA number
     */
    public int getURA() {
        return ura;
    }

    /**
     * Sets the average URA number.
     *
     * @param uraNumber the URA number to set
     */
    public void setURA(final int uraNumber) {
        this.ura = uraNumber;
    }

    /**
     * Gets the satellite configuration.
     *
     * @return the satellite configuration
     */
    public int getSatConfiguration() {
        return config;
    }

    /**
     * Sets the satellite configuration.
     *
     * @param satConfiguration the satellite configuration to set
     */
    public void setSatConfiguration(final int satConfiguration) {
        this.config = satConfiguration;
    }

    /**
     * Gets for the Group Delay Differential (s).
     *
     * @return the Group Delay Differential in seconds
     */
    public double getTGD() {
        return 0.0;
    }

}
