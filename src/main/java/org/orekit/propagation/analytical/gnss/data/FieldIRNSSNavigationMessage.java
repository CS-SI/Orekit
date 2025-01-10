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
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.TimeScales;

/**
 * Container for data contained in an IRNSS navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldIRNSSNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldAbstractNavigationMessage<T, FieldIRNSSNavigationMessage<T>, IRNSSNavigationMessage>  {

    /** Issue of Data, Ephemeris and Clock. */
    private int iodec;

    /** User range accuracy (m). */
    private T ura;

    /** Satellite health status. */
    private T svHealth;

    /** Constructor.
     * @param field      field to which elements belong
     * @param timeScales known time scales
     * @param system     satellite system to consider for interpreting week number
     *                   (may be different from real system, for example in Rinex nav, weeks
     *                   are always according to GPS)
     */
    public FieldIRNSSNavigationMessage(final Field<T> field,
                                       final TimeScales timeScales, final SatelliteSystem system) {
        super(field.getZero().newInstance(GNSSConstants.IRNSS_MU), GNSSConstants.IRNSS_AV, GNSSConstants.IRNSS_WEEK_NB,
              timeScales, system);
    }

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldIRNSSNavigationMessage(final Field<T> field, final IRNSSNavigationMessage original) {
        super(field, original);
        setIODEC(field.getZero().newInstance(original.getIODEC()));
        setURA(field.getZero().newInstance(original.getURA()));
        setSvHealth(field.getZero().newInstance(original.getSvHealth()));
    }

    /**  {@inheritDoc} */
    @Override
    protected FieldIRNSSNavigationMessage<T> uninitializedCopy() {
        return new FieldIRNSSNavigationMessage<>(getMu().getField(), getTimeScales(), getSystem());
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
    public void setIODEC(final T value) {
        // The value is given as a floating number in the navigation message
        this.iodec = (int) value.getReal();
    }

    /**
     * Getter for the user range accuray (meters).
     * @return the user range accuracy
     */
    public T getURA() {
        return ura;
    }

    /**
     * Setter for the user range accuracy.
     * @param accuracy the value to set
     */
    public void setURA(final T accuracy) {
        this.ura = accuracy;
    }

    /**
     * Getter for the satellite health status.
     * @return the satellite health status
     */
    public T getSvHealth() {
        return svHealth;
    }

    /**
     * Setter for the satellite health status.
     * @param svHealth the value to set
     */
    public void setSvHealth(final T svHealth) {
        this.svHealth = svHealth;
    }

}
