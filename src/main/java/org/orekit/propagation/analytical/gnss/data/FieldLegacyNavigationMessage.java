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
 * Container for data contained in a GPS/QZNSS legacy navigation message.
 * @param <T> type of the field elements
 * @param <O> type of the orbital elements (non-field version)
 * @author Luc Maisonobe
 * @since 13.0
 */
public abstract class FieldLegacyNavigationMessage<T extends CalculusFieldElement<T>,
                                                   O extends LegacyNavigationMessage<O>>
    extends FieldAbstractNavigationMessage<T, O>
    implements FieldGNSSClockElements<T> {

    /** Issue of Data, Ephemeris. */
    private int iode;

    /** Issue of Data, Clock. */
    private int iodc;

    /** The user SV accuracy (m). */
    private T svAccuracy;

    /** Satellite health status. */
    private int svHealth;

    /** Fit interval. */
    private int fitInterval;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldLegacyNavigationMessage(final Field<T> field, final O original) {
        super(field, original);
        setIODE(getMu().newInstance(original.getIODE()));
        setIODC(original.getIODC());
        setSvAccuracy(getMu().newInstance(original.getSvAccuracy()));
        setSvHealth(original.getSvHealth());
        setFitInterval(original.getFitInterval());
    }

    /**
     * Getter for the Issue Of Data Ephemeris (IODE).
     * @return the Issue Of Data Ephemeris (IODE)
     */
    public int getIODE() {
        return iode;
    }

    /**
     * Setter for the Issue of Data Ephemeris.
     * @param value the IODE to set
     */
    public void setIODE(final T value) {
        // The value is given as a floating number in the navigation message
        this.iode = (int) value.getReal();
    }

    /**
     * Getter for the Issue Of Data Clock (IODC).
     * @return the Issue Of Data Clock (IODC)
     */
    public int getIODC() {
        return iodc;
    }

    /**
     * Setter for the Issue of Data Clock.
     * @param value the IODC to set
     */
    public void setIODC(final int value) {
        this.iodc = value;
    }

    /**
     * Getter for the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public T getSvAccuracy() {
        return svAccuracy;
    }

    /**
     * Setter for the user SV accuracy.
     * @param svAccuracy the value to set
     */
    public void setSvAccuracy(final T svAccuracy) {
        this.svAccuracy = svAccuracy;
    }

    /**
     * Getter for the satellite health status.
     * @return the satellite health status
     */
    public int getSvHealth() {
        return svHealth;
    }

    /**
     * Setter for the satellite health status.
     * @param svHealth the value to set
     */
    public void setSvHealth(final int svHealth) {
        this.svHealth = svHealth;
    }

    /**
     * Getter for the fit interval.
     * @return the fit interval
     */
    public int getFitInterval() {
        return fitInterval;
    }

    /**
     * Setter for the fit interval.
     * @param fitInterval fit interval
     */
    public void setFitInterval(final int fitInterval) {
        this.fitInterval = fitInterval;
    }

}
