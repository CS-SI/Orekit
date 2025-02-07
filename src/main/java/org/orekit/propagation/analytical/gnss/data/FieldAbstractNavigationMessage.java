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
 * @see FieldIRNSSNavigationMessage
 */
public abstract class FieldAbstractNavigationMessage<T extends CalculusFieldElement<T>,
                                                     O extends AbstractNavigationMessage<O>>
    extends FieldAbstractAlmanac<T, O> {

    /** Mean Motion Difference from Computed Value. */
    private T deltaN0;

    /** Time of clock epoch. */
    private FieldAbsoluteDate<T> epochToc;

    /** Transmission time. */
    private T transmissionTime;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    protected FieldAbstractNavigationMessage(final Field<T> field, final O original) {
        super(field, original);
        setDeltaN0(field.getZero().newInstance(original.getDeltaN0()));
        setEpochToc(new FieldAbsoluteDate<>(field, original.getEpochToc()));
        setTransmissionTime(field.getZero().newInstance(original.getTransmissionTime()));
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    protected <V extends CalculusFieldElement<V>> FieldAbstractNavigationMessage(final Function<V, T> converter,
                                                                                 final FieldAbstractNavigationMessage<V, O> original) {
        super(converter, original);
        setDeltaN0(converter.apply(original.getDeltaN0()));
        setEpochToc(new FieldAbsoluteDate<>(getMu().getField(), original.getEpochToc().toAbsoluteDate()));
        setTransmissionTime(converter.apply(original.getTransmissionTime()));
    }

    /**
     * Getter for Square Root of Semi-Major Axis (√m).
     * @return Square Root of Semi-Major Axis (√m)
     */
    public T getSqrtA() {
        return FastMath.sqrt(getSma());
    }

    /**
     * Setter for the Square Root of Semi-Major Axis (√m).
     * <p>
     * In addition, this method set the value of the Semi-Major Axis.
     * </p>
     * @param sqrtA the Square Root of Semi-Major Axis (√m)
     */
    public void setSqrtA(final T sqrtA) {
        setSma(sqrtA.square());
    }

    /** {@inheritDoc} */
    @Override
    public T getDeltaN0() {
        return deltaN0;
    }

    /**
     * Setter for the delta of satellite mean motion.
     * @param deltaN0 the value to set
     */
    public void setDeltaN0(final T deltaN0) {
        this.deltaN0 = deltaN0;
    }

    /**
     * Getter for the time of clock epoch.
     * @return the time of clock epoch
     */
    public FieldAbsoluteDate<T> getEpochToc() {
        return epochToc;
    }

    /**
     * Setter for the time of clock epoch.
     * @param epochToc the epoch to set
     */
    public void setEpochToc(final FieldAbsoluteDate<T> epochToc) {
        this.epochToc = epochToc;
    }

    /**
     * Getter for transmission time.
     * @return transmission time
     */
    public T getTransmissionTime() {
        return transmissionTime;
    }

    /**
     * Setter for transmission time.
     * @param transmissionTime transmission time
     */
    public void setTransmissionTime(final T transmissionTime) {
        this.transmissionTime = transmissionTime;
    }

}
