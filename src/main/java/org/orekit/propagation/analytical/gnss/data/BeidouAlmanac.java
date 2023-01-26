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
 * Class for BeiDou almanac.
 *
 * @see "BeiDou Navigation Satellite System, Signal In Space, Interface Control Document,
 *      Version 2.1, Table 5-12"
 *
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public class BeidouAlmanac extends AbstractAlmanac {

    /** Health status. */
    private int health;

    /**
     * Build a new almanac.
     */
    public BeidouAlmanac() {
        super(GNSSConstants.BEIDOU_MU, GNSSConstants.BEIDOU_AV, GNSSConstants.BEIDOU_WEEK_NB);
    }

    /**
     * Sets the Square Root of Semi-Major Axis (m^1/2).
     * <p>
     * In addition, this method set the value of the Semi-Major Axis.
     * </p>
     * @param sqrtA the Square Root of Semi-Major Axis (m^1/2)
     */
    public void setSqrtA(final double sqrtA) {
        super.setSma(sqrtA * sqrtA);
    }

    /**
     * Sets the Inclination Angle at Reference Time (rad).
     *
     * @param inc the orbit reference inclination
     * @param dinc the correction of orbit reference inclination at reference time
     */
    public void setI0(final double inc, final double dinc) {
        super.setI0(inc + dinc);
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

}
