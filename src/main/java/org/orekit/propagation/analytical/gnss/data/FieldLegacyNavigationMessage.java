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
 * Container for data contained in a GPS/QZNSS legacy navigation message.
 * @param <T> type of the field elements
 * @param <O> type of the orbital elements (non-field version)
 * @author Luc Maisonobe
 * @since 13.0
 */
public abstract class FieldLegacyNavigationMessage<T extends CalculusFieldElement<T>,
                                                   O extends LegacyNavigationMessage<O>>
    extends FieldAbstractNavigationMessage<T, O> {

    /** Issue of Data, Ephemeris. */
    private final int iode;

    /** Issue of Data, Clock. */
    private final int iodc;

    /** The user SV accuracy (m). */
    private final T svAccuracy;

    /** Satellite health status. */
    private final int svHealth;

    /** Fit interval. */
    private final int fitInterval;

    /** Codes on L2 channel.
     * @since 14.0
     */
    private final int l2Codes;

    /** L2 P data flags.
     * @since 14.0
     */
    private final int l2PFlags;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    protected FieldLegacyNavigationMessage(final Field<T> field, final O original) {
        super(field, original);
        iode        = original.getIODE();
        iodc        = original.getIODC();
        svAccuracy  = field.getZero().newInstance(original.getSvAccuracy());
        svHealth    = original.getSvHealth();
        fitInterval = original.getFitInterval();
        l2Codes     = original.getL2Codes();
        l2PFlags    = original.getL2PFlags();
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    protected <V extends CalculusFieldElement<V>> FieldLegacyNavigationMessage(final Function<V, T> converter,
                                                                               final FieldLegacyNavigationMessage<V, O> original) {
        super(converter, original);
        iode        = original.getIODE();
        iodc        = original.getIODC();
        svAccuracy  = converter.apply(original.getSvAccuracy());
        svHealth    = original.getSvHealth();
        fitInterval = original.getFitInterval();
        l2Codes     = original.getL2Codes();
        l2PFlags    = original.getL2PFlags();
    }

    /**
     * Getter for the Issue Of Data Ephemeris (IODE).
     * @return the Issue Of Data Ephemeris (IODE)
     */
    public int getIODE() {
        return iode;
    }

    /**
     * Getter for the Issue Of Data Clock (IODC).
     * @return the Issue Of Data Clock (IODC)
     */
    public int getIODC() {
        return iodc;
    }

    /**
     * Getter for the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public T getSvAccuracy() {
        return svAccuracy;
    }

    /**
     * Getter for the satellite health status.
     * @return the satellite health status
     */
    public int getSvHealth() {
        return svHealth;
    }

    /**
     * Getter for the fit interval.
     * @return the fit interval
     */
    public int getFitInterval() {
        return fitInterval;
    }

    /** Get the codes on L2 channel.
     * @return codes on L2 channel
     * @since 14.0
     */
    public int getL2Codes() {
        return l2Codes;
    }

    /** Get the L2 P data flags.
     * @return L2 P data flags
     * @since 14.0
     */
    public int getL2PFlags() {
        return l2PFlags;
    }

}
