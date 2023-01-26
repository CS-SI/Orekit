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

import org.hipparchus.util.FastMath;

/**
 * Class for Galileo almanac.
 *
 * @see "European GNSS (Galileo) Open Service, Signal In Space,
 *      Interface Control Document, Table 75"
 *
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public class GalileoAlmanac extends AbstractAlmanac {

    /** Nominal inclination (Ref: Galileo ICD - Table 75). */
    private static final double I0 = FastMath.toRadians(56.0);

    /** Nominal semi-major axis in meters (Ref: Galileo ICD - Table 75). */
    private static final double A0 = 29600000;

    /** Satellite E5a signal health status. */
    private int healthE5a;

    /** Satellite E5b signal health status. */
    private int healthE5b;

    /** Satellite E1-B/C signal health status. */
    private int healthE1;

    /** Almanac Issue Of Data. */
    private int iod;

    /**
     * Build a new almanac.
     */
    public GalileoAlmanac() {
        super(GNSSConstants.GALILEO_MU, GNSSConstants.GALILEO_AV, GNSSConstants.GALILEO_WEEK_NB);
    }

    /**
     * Sets the difference between the square root of the semi-major axis
     * and the square root of the nominal semi-major axis.
     * <p>
     * In addition, this method set the value of the Semi-Major Axis.
     * </p>
     * @param dsqa the value to set
     */
    public void setDeltaSqrtA(final double dsqa) {
        final double sqrtA = dsqa + FastMath.sqrt(A0);
        super.setSma(sqrtA * sqrtA);
    }

    /**
     * Sets the the correction of orbit reference inclination at reference time.
     * <p>
     * In addition, this method set the value of the reference inclination.
     * </p>
     * @param dinc correction of orbit reference inclination at reference time in radians
     */
    public void setDeltaInc(final double dinc) {
        super.setI0(I0 + dinc);
    }

    /**
     * Gets the Issue of Data (IOD).
     *
     * @return the Issue Of Data
     */
    public int getIOD() {
        return iod;
    }

    /**
     * Sets the Issue of Data (IOD).
     *
     * @param iodValue the value to set
     */
    public void setIOD(final int iodValue) {
        this.iod = iodValue;
    }

    /**
     * Gets the E1-B/C signal health status.
     *
     * @return the E1-B/C signal health status
     */
    public int getHealthE1() {
        return healthE1;
    }

    /**
     * Sets the E1-B/C signal health status.
     *
     * @param healthE1 health status to set
     */
    public void setHealthE1(final int healthE1) {
        this.healthE1 = healthE1;
    }

    /**
     * Gets the E5a signal health status.
     *
     * @return the E5a signal health status
     */
    public int getHealthE5a() {
        return healthE5a;
    }

    /**
     * Sets the E5a signal health status.
     *
     * @param healthE5a health status to set
     */
    public void setHealthE5a(final int healthE5a) {
        this.healthE5a = healthE5a;
    }

    /**
     * Gets the E5b signal health status.
     *
     * @return the E5b signal health status
     */
    public int getHealthE5b() {
        return healthE5b;
    }

    /**
     * Sets the E5b signal health status.
     *
     * @param healthE5b health status to set
     */
    public void setHealthE5b(final int healthE5b) {
        this.healthE5b = healthE5b;
    }

}
