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
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.TimeScales;

/**
 * Container for data contained in a NavIC navigation message.
 * @author Luc Maisonobe
 * @since 13.0
 */
public class NavICL1NVNavigationMessage
    extends CivilianNavigationMessage<NavICL1NVNavigationMessage> {

    /** Message type.
     * @since 14.0
     */
    public static final String L1NV = "L1NV";

    /** Reference signal flag. */
    private int referenceSignalFlag;

    /** Estimated group delay differential TGD for S-L5 correction. */
    private double tgdSL5;

    /** Inter Signal Delay for S L1P. */
    private double iscSL1P;

    /** Inter Signal Delay for L1D L1P. */
    private double iscL1DL1P;

    /** Inter Signal Delay for L1P S. */
    private double iscL1PS;

    /** Inter Signal Delay for L1DS. */
    private double iscL1DS;

    /** Constructor.
     * @param timeScales known time scales
     * @param system     satellite system to consider for interpreting week number
     *                   (may be different from real system, for example in Rinex nav, weeks
     *                   are always according to GPS)
     * @param type       message type
     */
    public NavICL1NVNavigationMessage(final TimeScales timeScales, final SatelliteSystem system,
                                      final String type) {
        super(true, GNSSConstants.NAVIC_MU, GNSSConstants.NAVIC_AV, GNSSConstants.NAVIC_WEEK_NB,
              timeScales, system, type);
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param original regular field instance
     */
    public <T extends CalculusFieldElement<T>> NavICL1NVNavigationMessage(final FieldNavicL1NVNavigationMessage<T> original) {
        super(original);
        setReferenceSignalFlag(original.getReferenceSignalFlag());
        setTGDSL5(original.getTGDSL5().getReal());
        setIscSL1P(original.getIscSL1P().getReal());
        setIscL1DL1P(original.getIscL1DL1P().getReal());
        setIscL1PS(original.getIscL1PS().getReal());
        setIscL1DS(original.getIscL1DS().getReal());
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>, F extends FieldGnssOrbitalElements<T, NavICL1NVNavigationMessage>>
        F toField(final Field<T> field) {
        return (F) new FieldNavicL1NVNavigationMessage<>(field, this);
    }

    /** Set reference signal flag.
     * @param referenceSignalFlag reference signal flag
     */
    public void setReferenceSignalFlag(final int referenceSignalFlag) {
        this.referenceSignalFlag = referenceSignalFlag;
    }

    /** Get reference signal flag.
     * @return reference signal flag
     */
    public int getReferenceSignalFlag() {
        return referenceSignalFlag;
    }

    /**
     * Set the estimated group delay differential TGD for S-L5 correction.
     * @param groupDelayDifferential the estimated group delay differential TGD for S-L3 correction (s)
     */
    public void setTGDSL5(final double groupDelayDifferential) {
        this.tgdSL5 = groupDelayDifferential;
    }

    /**
     * Set the estimated group delay differential TGD for S-L5 correction.
     * @return estimated group delay differential TGD for S-L3 correction (s)
     */
    public double getTGDSL5() {
        return tgdSL5;
    }

    /**
     * Getter for inter Signal Delay for S L1P.
     * @return inter signal delay
     */
    public double getIscSL1P() {
        return iscSL1P;
    }

    /**
     * Setter for inter Signal Delay for S L1P.
     * @param delay delay to set
     */
    public void setIscSL1P(final double delay) {
        this.iscSL1P = delay;
    }

    /**
     * Getter for inter Signal Delay for L1D L1P.
     * @return inter signal delay
     */
    public double getIscL1DL1P() {
        return iscL1DL1P;
    }

    /**
     * Setter for inter Signal Delay for L1D L1P.
     * @param delay delay to set
     */
    public void setIscL1DL1P(final double delay) {
        this.iscL1DL1P = delay;
    }

    /**
     * Getter for inter Signal Delay for L1P S.
     * @return inter signal delay
     */
    public double getIscL1PS() {
        return iscL1PS;
    }

    /**
     * Setter for inter Signal Delay for L1P S.
     * @param delay delay to set
     */
    public void setIscL1PS(final double delay) {
        this.iscL1PS = delay;
    }

    /**
     * Getter for inter Signal Delay for L1D S.
     * @return inter signal delay
     */
    public double getIscL1DS() {
        return iscL1DS;
    }

    /**
     * Setter for inter Signal Delay for L1D S.
     * @param delay delay to set
     */
    public void setIscL1DS(final double delay) {
        this.iscL1DS = delay;
    }

}
