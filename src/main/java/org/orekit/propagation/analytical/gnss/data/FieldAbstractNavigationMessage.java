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
import org.hipparchus.util.FastMath;
import org.orekit.time.FieldAbsoluteDate;

import java.util.function.Function;

/**
 * Base class for GNSS navigation messages.
 * @param <T> type of the field elements
 * @param <O> type of the orbital elements (non-field version)
 * @author Luc Maisonobe
 * @since 13.0
 *
 * @see FieldGPSLegacyNavigationMessage
 * @see FieldGalileoNavigationMessage
 * @see FieldBeidouLegacyNavigationMessage
 * @see FieldQZSSLegacyNavigationMessage
 * @see FieldNavicLegacyNavigationMessage
 */
public abstract class FieldAbstractNavigationMessage<T extends CalculusFieldElement<T>,
                                                     O extends AbstractNavigationMessage<O>>
    extends FieldGnssOrbitalElements<T, O> {

    /** Time of clock epoch. */
    private final FieldAbsoluteDate<T> epochToc;

    /** Transmission time. */
    private final T transmissionTime;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    protected FieldAbstractNavigationMessage(final Field<T> field, final O original) {
        super(field, original);
        epochToc         = new FieldAbsoluteDate<>(field, original.getEpochToc());
        transmissionTime = field.getZero().newInstance(original.getTransmissionTime());
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    protected <V extends CalculusFieldElement<V>> FieldAbstractNavigationMessage(final Function<V, T> converter,
                                                                                 final FieldAbstractNavigationMessage<V, O> original) {
        super(converter, original);
        epochToc         = new FieldAbsoluteDate<>(getToc().getField(), original.getEpochToc().toAbsoluteDate());
        transmissionTime = converter.apply(original.getTransmissionTime());
    }

    /**
     * Getter for Square Root of Semi-Major Axis (√m).
     * @return Square Root of Semi-Major Axis (√m)
     */
    public T getSqrtA() {
        return FastMath.sqrt(getOrbit().getA());
    }

    /**
     * Getter for the time of clock epoch.
     * @return the time of clock epoch
     */
    public FieldAbsoluteDate<T> getEpochToc() {
        return epochToc;
    }

    /**
     * Getter for transmission time.
     * @return transmission time
     */
    public T getTransmissionTime() {
        return transmissionTime;
    }

}
