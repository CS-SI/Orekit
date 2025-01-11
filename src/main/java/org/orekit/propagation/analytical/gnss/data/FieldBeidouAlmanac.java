/* Copyright 2002-2024 Luc Maisonobe
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

/**
 * Class for BeiDou almanac.
 *
 * @see "BeiDou Navigation Satellite System, Signal In Space, Interface Control Document,
 *      Version 2.1, Table 5-12"
 *
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 *
 */
public class FieldBeidouAlmanac<T extends CalculusFieldElement<T>>
    extends FieldAbstractAlmanac<T, BeidouAlmanac> {

    /** Health status. */
    private int health;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldBeidouAlmanac(final Field<T> field, final BeidouAlmanac original) {
        super(field, original);
        setHealth(original.getHealth());
    }

    /** {@inheritDoc} */
    @Override
    public BeidouAlmanac toNonField() {
        return new BeidouAlmanac(this);
    }

    /**
     * Sets the Square Root of Semi-Major Axis (m^1/2).
     * <p>
     * In addition, this method set the value of the Semi-Major Axis.
     * </p>
     * @param sqrtA the Square Root of Semi-Major Axis (m^1/2)
     */
    public void setSqrtA(final T sqrtA) {
        setSma(sqrtA.square());
    }

    /**
     * Sets the Inclination Angle at Reference Time (rad).
     *
     * @param inc the orbit reference inclination
     * @param dinc the correction of orbit reference inclination at reference time
     */
    public void setI0(final T inc, final T dinc) {
        setI0(inc.add(dinc));
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
