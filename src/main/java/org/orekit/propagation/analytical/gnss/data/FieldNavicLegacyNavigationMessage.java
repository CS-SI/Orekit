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
 * Container for data contained in an NavIC navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldNavicLegacyNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldAbstractNavigationMessage<T, NavICLegacyNavigationMessage>  {

    /** Issue of Data, Ephemeris and Clock. */
    private int iodec;

    /** User range accuracy (m). */
    private T ura;

    /** Satellite health status. */
    private T svHealth;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldNavicLegacyNavigationMessage(final Field<T> field, final NavICLegacyNavigationMessage original) {
        super(field, original);
        setIODEC(field.getZero().newInstance(original.getIODEC()));
        setURA(field.getZero().newInstance(original.getURA()));
        setSvHealth(field.getZero().newInstance(original.getSvHealth()));
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldNavicLegacyNavigationMessage(final Function<V, T> converter,
                                                                                 final FieldNavicLegacyNavigationMessage<V> original) {
        super(converter, original);
        setIODEC(getMu().newInstance(original.getIODEC()));
        setURA(converter.apply(original.getURA()));
        setSvHealth(converter.apply(original.getSvHealth()));
    }

    /** {@inheritDoc} */
    @Override
    public NavICLegacyNavigationMessage toNonField() {
        return new NavICLegacyNavigationMessage(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, G extends FieldGnssOrbitalElements<U, NavICLegacyNavigationMessage>>
        G changeField(final Function<T, U> converter) {
        return (G) new FieldNavicLegacyNavigationMessage<>(converter, this);
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
