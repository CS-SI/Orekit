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
import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;


/**
 * Container for data contained in a NavIC navigation message.
 * @author Luc Maisonobe
 * @since 13.0
 */
public class NavICL1NvNavigationMessage
    extends AbstractNavigationMessage<NavICL1NvNavigationMessage> {

    /** Message type.
     * @since 14.0
     */
    public static final String L1NV = "L1NV";

    /** Reference signal flag. */
    private final int referenceSignalFlag;

    /** User Range Accuracy Index.
     * @since 14.0
     */
    private final int urai;

    /** L1 SPS health.
     * @since 14.0
     */
    private final int l1SpsHealth;

    /** Estimated group delay differential TGD for S-L5 correction. */
    private final double tgdSL5;

    /** Inter Signal Delay for S L1P. */
    private final double iscSL1P;

    /** Inter Signal Delay for L1D L1P. */
    private final double iscL1DL1P;

    /** Inter Signal Delay for L1P S. */
    private final double iscL1PS;

    /** Inter Signal Delay for L1D S. */
    private final double iscL1DS;

    /** Constructor.
     * @param timeScales          known time scales
     * @param system              satellite system to consider for interpreting week number
     *                            (may be different from real system, for example in Rinex nav, weeks
     *                            are always according to GPS)
     * @param type                message type
     * @param prn                 PRN number of the satellite
     * @param orbit               Keplerian orbit in Earth-frozen frame
     * @param aDot                change rate in semi-major axis (m/s)
     * @param deltaN0             delta of satellite mean motion
     * @param deltaN0Dot          change rate in Δn₀
     * @param iDot                inclination rate (rad/s)
     * @param omegaDot            rate of right ascension (rad/s)
     * @param cuc                 amplitude of the cosine harmonic correction term to the argument of latitude
     * @param cus                 amplitude of the sine harmonic correction term to the argument of latitude
     * @param crc                 amplitude of the cosine harmonic correction term to the orbit radius
     * @param crs                 amplitude of the sine harmonic correction term to the orbit radius
     * @param cic                 amplitude of the cosine harmonic correction term to the inclination
     * @param cis                 amplitude of the sine harmonic correction term to the inclination
     * @param af0                 zero-th order clock correction (s)
     * @param af1                 first order clock correction (s/s)
     * @param af2                 second order clock correction (s/s²)
     * @param tgd                 group delay differential TGD for L1-L2 correction
     * @param toc                 time of clock
     * @param epochToc            time of clock epoch
     * @param transmissionTime    transmission time
     * @param referenceSignalFlag reference signal flag
     * @param urai                User Range Accuracy Index
     * @param l1SpsHealth         L1 SPS health
     * @param tgdSL5              estimated group delay differential TGD for S-L5 correction
     * @param iscSL1P             inter signal delay for S L1P
     * @param iscL1DL1P           inter signal delay for L1D L1P
     * @param iscL1PS             inter signal delay for L1P S
     * @param iscL1DS             inter signal delay for L1D S
     */
    public NavICL1NvNavigationMessage(final TimeScales timeScales, final SatelliteSystem system, final String type,
                                      final int prn, final KeplerianOrbit orbit, final double aDot,
                                      final double deltaN0, final double deltaN0Dot,
                                      final double iDot, final double omegaDot,
                                      final double cuc, final double cus,
                                      final double crc, final double crs,
                                      final double cic, final double cis,
                                      final double af0, final double af1, final double af2,
                                      final double tgd, final double toc,
                                      final AbsoluteDate epochToc, final double transmissionTime,
                                      final int referenceSignalFlag,
                                      final int urai, final int l1SpsHealth,
                                      final double tgdSL5,
                                      final double iscSL1P, final double iscL1DL1P,
                                      final double iscL1PS, final double iscL1DS) {
        super(GNSSConstants.NAVIC_AV, GNSSConstants.NAVIC_WEEK_NB,
              timeScales, system, type, prn, orbit,
              aDot, deltaN0, deltaN0Dot, iDot, omegaDot, cuc, cus, crc, crs, cic, cis,
              af0, af1, af2, tgd, toc, epochToc, transmissionTime);
        this.referenceSignalFlag = referenceSignalFlag;
        this.urai                = urai;
        this.l1SpsHealth         = l1SpsHealth;
        this.tgdSL5              = tgdSL5;
        this.iscSL1P             = iscSL1P;
        this.iscL1DL1P           = iscL1DL1P;
        this.iscL1PS             = iscL1PS;
        this.iscL1DS             = iscL1DS;
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param original regular field instance
     */
    public <T extends CalculusFieldElement<T>> NavICL1NvNavigationMessage(final FieldNavicL1NvNavigationMessage<T> original) {
        super(original);
        referenceSignalFlag = original.getReferenceSignalFlag();
        urai                = original.getUrai();
        l1SpsHealth         = original.getL1SpsHealth();
        tgdSL5              = original.getTGDSL5().getReal();
        iscSL1P             = original.getIscSL1P().getReal();
        iscL1DL1P           = original.getIscL1DL1P().getReal();
        iscL1PS             = original.getIscL1PS().getReal();
        iscL1DS             = original.getIscL1DS().getReal();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>, F extends FieldGnssOrbitalElements<T, NavICL1NvNavigationMessage, F>>
        F toField(final FieldKeplerianOrbit<T> orbit) {
        return (F) new FieldNavicL1NvNavigationMessage<>(orbit, this);
    }

    /** Get the reference signal flag.
     * @return reference signal flag
     */
    public int getReferenceSignalFlag() {
        return referenceSignalFlag;
    }

    /** Get User Range Accuracy Index.
     * @return User Range Accuracy Index
     * @since 14.0
     */
    public int getUrai() {
        return urai;
    }

    /** Get L1 SPS health.
     * @return L1 SPS health
     * @since 14.0
     */
    public int getL1SpsHealth() {
        return l1SpsHealth;
    }

    /** Get the estimated group delay differential TGD for S-L5 correction.
     * @return estimated group delay differential TGD for S-L3 correction (s)
     */
    public double getTGDSL5() {
        return tgdSL5;
    }

    /** Get the inter Signal Delay for S L1P.
     * @return inter signal delay
     */
    public double getIscSL1P() {
        return iscSL1P;
    }

    /** Get the inter Signal Delay for L1D L1P.
     * @return inter signal delay
     */
    public double getIscL1DL1P() {
        return iscL1DL1P;
    }

    /** Get the inter Signal Delay for L1P S.
     * @return inter signal delay
     */
    public double getIscL1PS() {
        return iscL1PS;
    }

    /** Get the inter Signal Delay for L1D S.
     * @return inter signal delay
     */
    public double getIscL1DS() {
        return iscL1DS;
    }

    /** {@inheritDoc} */
    @Override
    public NavICL1NvNavigationMessageFactory baseFactory(final Frame inertial, final Frame bodyFixed) {
        return new NavICL1NvNavigationMessageFactory(getTimeScales(), getSystem(), getType(),
                                                     inertial, bodyFixed);
    }

}
