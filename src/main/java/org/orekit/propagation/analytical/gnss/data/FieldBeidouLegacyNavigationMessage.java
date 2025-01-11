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
 * Container for data contained in a BeiDou navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldBeidouLegacyNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldAbstractNavigationMessage<T, BeidouLegacyNavigationMessage> {

    /** Age of Data, Ephemeris. */
    private int aode;

    /** Age of Data, Clock. */
    private int aodc;

    /** B1/B3 Group Delay Differential (s). */
    private T tgd1;

    /** B2/B3 Group Delay Differential (s). */
    private T tgd2;

    /** The user SV accuracy (m). */
    private T svAccuracy;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldBeidouLegacyNavigationMessage(final Field<T> field, final BeidouLegacyNavigationMessage original) {
        super(field, original);
        setAODE(field.getZero().newInstance(original.getAODE()));
        setAODC(field.getZero().newInstance(original.getAODC()));
        setTGD1(field.getZero().newInstance(original.getTGD1()));
        setTGD2(field.getZero().newInstance(original.getTGD2()));
        setSvAccuracy(field.getZero().newInstance(original.getSvAccuracy()));
    }

    /** {@inheritDoc} */
    @Override
    public BeidouLegacyNavigationMessage toNonField() {
        return new BeidouLegacyNavigationMessage(this);
    }

    /**
     * Getter for the Age Of Data Clock (AODC).
     * @return the Age Of Data Clock (AODC)
     */
    public int getAODC() {
        return aodc;
    }

    /**
     * Setter for the age of data clock.
     * @param aod the age of data to set
     */
    public void setAODC(final T aod) {
        // The value is given as a floating number in the navigation message
        this.aodc = (int) aod.getReal();
    }

    /**
     * Getter for the Age Of Data Ephemeris (AODE).
     * @return the Age Of Data Ephemeris (AODE)
     */
    public int getAODE() {
        return aode;
    }

    /**
     * Setter for the age of data ephemeris.
     * @param aod the age of data to set
     */
    public void setAODE(final T aod) {
        // The value is given as a floating number in the navigation message
        this.aode = (int) aod.getReal();
    }

    /**
     * Getter for the estimated group delay differential TGD1 for B1I signal.
     * @return the estimated group delay differential TGD1 for B1I signal (s)
     */
    public T getTGD1() {
        return tgd1;
    }

    /**
     * Setter for the B1/B3 Group Delay Differential (s).
     * @param tgd the group delay differential to set
     */
    public void setTGD1(final T tgd) {
        this.tgd1 = tgd;
    }

    /**
     * Getter for the estimated group delay differential TGD for B2I signal.
     * @return the estimated group delay differential TGD2 for B2I signal (s)
     */
    public T getTGD2() {
        return tgd2;
    }

    /**
     * Setter for the B2/B3 Group Delay Differential (s).
     * @param tgd the group delay differential to set
     */
    public void setTGD2(final T tgd) {
        this.tgd2 = tgd;
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

}
