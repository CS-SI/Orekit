/* Copyright 2002-2021 CS GROUP
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
package org.orekit.gnss.navigation;

import org.orekit.propagation.analytical.gnss.BeidouOrbitalElements;

/**
 * Container for data contained in a BeiDou navigation message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class BeidouNavigationMessage extends AbstractNavigationMessage implements BeidouOrbitalElements {

    /** Age of Data, Ephemeris. */
    private int aode;

    /** Age of Data, Clock. */
    private int aodc;

    /** B1/B3 Group Delay Differential (s). */
    private double tgd1;

    /** B2/B3 Group Delay Differential (s). */
    private double tgd2;

    /** The user SV accuracy (m). */
    private double svAccuracy;

    /** Constructor. */
    public BeidouNavigationMessage() {
        super(BEIDOU_MU);
    }

    /** {@inheritDoc} */
    @Override
    public int getAODC() {
        return aodc;
    }

    /**
     * Setter for the age of data clock.
     * @param aod the age of data to set
     */
    public void setAODC(final double aod) {
        // The value is given as a floating number in the navigation message
        this.aodc = (int) aod;
    }

    /** {@inheritDoc} */
    @Override
    public int getAODE() {
        return aode;
    }

    /**
     * Setter for the age of data ephemeric.
     * @param aod the age of data to set
     */
    public void setAODE(final double aod) {
        // The value is given as a floating number in the navigation message
        this.aode = (int) aod;
    }

    /** {@inheritDoc} */
    @Override
    public double getTGD1() {
        return tgd1;
    }

    /**
     * Setter for the B1/B3 Group Delay Differential (s).
     * @param tgd the group delay differential to set
     */
    public void setTGD1(final double tgd) {
        this.tgd1 = tgd;
    }

    /** {@inheritDoc} */
    @Override
    public double getTGD2() {
        return tgd2;
    }

    /**
     * Setter for the B2/B3 Group Delay Differential (s).
     * @param tgd the group delay differential to set
     */
    public void setTGD2(final double tgd) {
        this.tgd2 = tgd;
    }

    /**
     * Getter for the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public double getSvAccuracy() {
        return svAccuracy;
    }

    /**
     * Setter for the user SV accuracy.
     * @param svAccuracy the value to set
     */
    public void setSvAccuracy(final double svAccuracy) {
        this.svAccuracy = svAccuracy;
    }

}
