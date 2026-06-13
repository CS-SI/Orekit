/* Copyright 2022-2025 Thales Alenia Space
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

import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;

/**
 * Factory for {@link NavICL1NvNavigationMessage}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class NavICL1NvNavigationMessageFactory
    extends AbstractNavigationMessageFactory<NavICL1NvNavigationMessage> {

    /** Reference signal flag. */
    private int referenceSignalFlag;

    /** User Range Accuracy Index. */
    private int urai;

    /** L1 SPS health. */
    private int l1SpsHealth;

    /** Estimated group delay differential TGD for S-L5 correction. */
    private double tgdSL5;

    /** Inter Signal Delay for S L1P. */
    private double iscSL1P;

    /** Inter Signal Delay for L1D L1P. */
    private double iscL1DL1P;

    /** Inter Signal Delay for L1P S. */
    private double iscL1PS;

    /** Inter Signal Delay for L1D S. */
    private double iscL1DS;

    /** Simple constructor.
     * @param timeScales      known time scales
     * @param system          satellite system to use for interpreting week number
     * @param type            message type (null if not a navigation message)
     * @param inertial        reference inertial frame
     * @param bodyFixed       body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @param date            date of the orbital parameters
     */
    public NavICL1NvNavigationMessageFactory(final TimeScales timeScales, final SatelliteSystem system,
                                             final String type, final Frame inertial, final Frame bodyFixed,
                                             final AbsoluteDate date) {
        super(GNSSConstants.NAVIC_AV, GNSSConstants.NAVIC_WEEK_NB, timeScales, system,
              type, inertial, bodyFixed, date, GNSSConstants.NAVIC_MU);
    }

    /** Get the reference signal flag.
     * @return reference signal flag
     */
    public int getReferenceSignalFlag() {
        return referenceSignalFlag;
    }

    /** Set then reference signal flag.
     * @param referenceSignalFlag reference signal flag
     */
    public void setReferenceSignalFlag(final int referenceSignalFlag) {
        this.referenceSignalFlag = referenceSignalFlag;
    }

    /** Get User Range Accuracy Index.
     * @return User Range Accuracy Index
     */
    public int getUrai() {
        return urai;
    }

    /** Set User Range Accuracy Index.
     * @param urai User Range Accuracy Index
     */
    public void setUrai(final int urai) {
        this.urai = urai;
    }

    /** Get L1 SPS health.
     * @return L1 SPS health
     */
    public int getL1SpsHealth() {
        return l1SpsHealth;
    }

    /** Set L1 SPS health.
     * @param l1SpsHealth L1 SPS health
     */
    public void setL1SpsHealth(final int l1SpsHealth) {
        this.l1SpsHealth = l1SpsHealth;
    }

    /** Get the estimated group delay differential TGD for S-L5 correction.
     * @return estimated group delay differential TGD for S-L3 correction (s)
     */
    public double getTGDSL5() {
        return tgdSL5;
    }

    /** Set the estimated group delay differential TGD for S-L5 correction.
     * @param tgdSL5 estimated group delay differential TGD for S-L3 correction (s)
     */
    public void setTGDSL5(final double tgdSL5) {
        this.tgdSL5 = tgdSL5;
    }

    /** Get the inter Signal Delay for S L1P.
     * @return inter signal delay
     */
    public double getIscSL1P() {
        return iscSL1P;
    }

    /** Set the inter Signal Delay for S L1P.
     * @param iscSL1P inter signal delay
     */
    public void setIscSL1P(final double iscSL1P) {
        this.iscSL1P = iscSL1P;
    }

    /** Get the inter Signal Delay for L1D L1P.
     * @return inter signal delay
     */
    public double getIscL1DL1P() {
        return iscL1DL1P;
    }

    /** Set the inter Signal Delay for L1D L1P.
     * @param iscL1DL1P inter signal delay
     */
    public void setIscL1DL1P(final double iscL1DL1P) {
        this.iscL1DL1P = iscL1DL1P;
    }

    /** Get the inter Signal Delay for L1P S.
     * @return inter signal delay
     */
    public double getIscL1PS() {
        return iscL1PS;
    }

    /** Set the inter Signal Delay for L1P S.
     * @param iscL1PS inter signal delay
     */
    public void setIscL1PS(final double iscL1PS) {
        this.iscL1PS = iscL1PS;
    }

    /** Get the inter Signal Delay for L1D S.
     * @return inter signal delay
     */
    public double getIscL1DS() {
        return iscL1DS;
    }

    /** Set the inter Signal Delay for L1D S.
     * @param iscL1DS inter signal delay
     */
    public void setIscL1DS(final double iscL1DS) {
        this.iscL1DS = iscL1DS;
    }

    /** {@inheritDoc} */
    @Override
    public NavICL1NvNavigationMessage createFromDrivers() {
        return new NavICL1NvNavigationMessage(getTimeScales(), getSystem(), getType(), getPrn(), getWeek(),
                                              createOrbitFromDrivers(),
                                              getTimeDriver().getValue(), getADotDriver().getValue(),
                                              getDeltaN0Driver().getValue(), getDeltaN0DotDriver().getValue(),
                                              getIDotDriver().getValue(), getOmegaDotDriver().getValue(),
                                              getCucDriver().getValue(), getCusDriver().getValue(),
                                              getCrcDriver().getValue(), getCrsDriver().getValue(),
                                              getCicDriver().getValue(), getCisDriver().getValue(),
                                              getAf0Driver().getValue(), getAf1Driver().getValue(),
                                              getAf2Driver().getValue(),
                                              getTGD(), getToc(),
                                              getEpochToc(), getTransmissionTime(),
                                              getReferenceSignalFlag(),
                                              getUrai(), getL1SpsHealth(),
                                              getTGDSL5(),
                                              getIscSL1P(), getIscL1DL1P(), getIscL1PS(), getIscL1DS());
    }

}
