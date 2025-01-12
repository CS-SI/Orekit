/* Copyright 2022-2025 Luc Maisonobe
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

import java.util.function.Function;

/**
 * Class for Galileo almanac.
 *
 * @see "European GNSS (Galileo) Open Service, Signal In Space,
 *      Interface Control Document, Table 75"
 *
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 *
 */
public class FieldGalileoAlmanac<T extends CalculusFieldElement<T>>
    extends FieldAbstractAlmanac<T, GalileoAlmanac> {

    /** Satellite E5a signal health status. */
    private int healthE5a;

    /** Satellite E5b signal health status. */
    private int healthE5b;

    /** Satellite E1-B/C signal health status. */
    private int healthE1;

    /** Almanac Issue Of Data. */
    private int iod;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldGalileoAlmanac(final Field<T> field, final GalileoAlmanac original) {
        super(field, original);
        setHealthE5a(original.getHealthE5a());
        setHealthE5b(original.getHealthE5b());
        setHealthE1(original.getHealthE1());
        setIOD(original.getIOD());
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldGalileoAlmanac(final Function<V, T> converter,
                                                                   final FieldGalileoAlmanac<V> original) {
        super(converter, original);
        setHealthE5a(original.getHealthE5a());
        setHealthE5b(original.getHealthE5b());
        setHealthE1(original.getHealthE1());
        setIOD(original.getIOD());
    }

    /** {@inheritDoc} */
    @Override
    public GalileoAlmanac toNonField() {
        return new GalileoAlmanac(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, G extends FieldGnssOrbitalElements<U, GalileoAlmanac>>
        G changeField(final Function<T, U> converter) {
        return (G) new FieldGalileoAlmanac<>(converter, this);
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
