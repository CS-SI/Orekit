/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.TimeScales;

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
public class BeidouAlmanac extends AbstractAlmanac<BeidouAlmanac> {

    /** Health status. */
    private int health;

    /**
     * Build a new almanac.
     * @param timeScales known time scales
     * @param system     satellite system to consider for interpreting week number
     *                   (may be different from real system, for example in Rinex nav weeks
     *                   are always according to GPS)
     */
    public BeidouAlmanac(final TimeScales timeScales, final SatelliteSystem system) {
        super(GNSSConstants.BEIDOU_MU, GNSSConstants.BEIDOU_AV, GNSSConstants.BEIDOU_WEEK_NB, timeScales, system);
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>>
        FieldBeidouAlmanac<T> uninitializedField(Field<T> field) {
        return new FieldBeidouAlmanac<>(field, getTimeScales(), getSystem());
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>>
        void fillUp(final Field<T> field, final FieldGnssOrbitalElements<T, ?> fielded) {
        super.fillUp(field, fielded);
        @SuppressWarnings("unchecked")
        final FieldBeidouAlmanac<T> converted = (FieldBeidouAlmanac<T>) fielded;
        converted.setHealth(getHealth());
    }

    /** {@inheritDoc} */
    @Override
    protected BeidouAlmanac uninitializedCopy() {
        return new BeidouAlmanac(getTimeScales(), getSystem());
    }

    /**
     * Sets the Square Root of Semi-Major Axis (m^1/2).
     * <p>
     * In addition, this method set the value of the Semi-Major Axis.
     * </p>
     * @param sqrtA the Square Root of Semi-Major Axis (m^1/2)
     */
    public void setSqrtA(final double sqrtA) {
        setSma(sqrtA * sqrtA);
    }

    /**
     * Sets the Inclination Angle at Reference Time (rad).
     *
     * @param inc the orbit reference inclination
     * @param dinc the correction of orbit reference inclination at reference time
     */
    public void setI0(final double inc, final double dinc) {
        setI0(inc + dinc);
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
