/* Copyright 2022-2026 Luc Maisonobe
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
 * Container for data contained in a Galileo navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldGalileoNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldAbstractNavigationMessage<T, GalileoNavigationMessage> {

    /** Issue of Data of the navigation batch. */
    private final int iodNav;

    /** Data source. */
    private final int dataSource;

    /** E1/E5a broadcast group delay (s). */
    private final T bgbE1E5a;

    /** E5b/E1 broadcast group delay (s). */
    private final T bgdE5bE1;

    /** Signal in space accuracy. */
    private final T sisa;

    /** Satellite health status. */
    private final T svHealth;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldGalileoNavigationMessage(final Field<T> field, final GalileoNavigationMessage original) {
        super(field, original);
        iodNav     = original.getIODNav();
        dataSource = original.getDataSource();
        bgbE1E5a   = field.getZero().newInstance(original.getBGDE1E5a());
        bgdE5bE1   = field.getZero().newInstance(original.getBGDE5bE1());
        sisa       = field.getZero().newInstance(original.getSisa());
        svHealth   = field.getZero().newInstance(original.getSvHealth());
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldGalileoNavigationMessage(final Function<V, T> converter,
                                                                             final FieldGalileoNavigationMessage<V> original) {
        super(converter, original);
        iodNav     = original.getIODNav();
        dataSource = original.getDataSource();
        bgbE1E5a   = converter.apply(original.getBGDE1E5a());
        bgdE5bE1   = converter.apply(original.getBGDE5bE1());
        sisa       = converter.apply(original.getSisa());
        svHealth   = converter.apply(original.getSvHealth());
    }

    /** {@inheritDoc} */
    @Override
    public GalileoNavigationMessage toNonField() {
        return new GalileoNavigationMessage(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, G extends FieldGnssOrbitalElements<U, GalileoNavigationMessage>>
        G changeField(final Function<T, U> converter) {
        return (G) new FieldGalileoNavigationMessage<>(converter, this);
    }

    /**
     * Getter for the the Issue Of Data (IOD).
     * @return the Issue Of Data (IOD)
     */
    public int getIODNav() {
        return iodNav;
    }

    /**
     * Getter for the the data source.
     * @return the data source
     */
    public int getDataSource() {
        return dataSource;
    }

    /**
     * Getter for the E1/E5a broadcast group delay.
     * @return the E1/E5a broadcast group delay (s)
     */
    public T getBGDE1E5a() {
        return bgbE1E5a;
    }

    /**
     * Getter for the the Broadcast Group Delay E5b/E1.
     * @return the Broadcast Group Delay E5b/E1 (s)
     */
    public T getBGDE5bE1() {
        return bgdE5bE1;
    }

    /**
     * Getter for the signal in space accuracy (m).
     * @return the signal in space accuracy
     */
    public T getSisa() {
        return sisa;
    }

    /**
     * Getter for the SV health status.
     * @return the SV health status
     */
    public T getSvHealth() {
        return svHealth;
    }

}
