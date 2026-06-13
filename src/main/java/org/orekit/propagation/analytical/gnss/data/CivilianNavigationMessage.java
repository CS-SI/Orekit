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
import org.orekit.gnss.SatelliteSystem;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;

/**
 * Container for data contained in a GPS/QZNSS civilian navigation message.
 * @param <O> type of the orbital elements
 * @author Luc Maisonobe
 * @since 12.0
 */
public abstract class CivilianNavigationMessage<O extends CivilianNavigationMessage<O>> extends AbstractNavigationMessage<O> implements GNSSClockElements {

    /** Indicator for CNV 2 messages. */
    private final boolean cnv2;

    /** The user SV accuracy (m). */
    private final double svAccuracy;

    /** Satellite health status. */
    private final int svHealth;

    /** Inter Signal Delay for L1 C/A. */
    private final double iscL1CA;

    /** Inter Signal Delay for L1 CD. */
    private final double iscL1CD;

    /** Inter Signal Delay for L1 CP. */
    private final double iscL1CP;

    /** Inter Signal Delay for L2 C. */
    private final double iscL2C;

    /** Inter Signal Delay for L5I. */
    private final double iscL5I5;

    /** Inter Signal Delay for L5Q. */
    private final double iscL5Q5;

    /** Elevation-Dependent User Range Accuracy. */
    private final int uraiEd;

    /** Term 0 of Non-Elevation-Dependent User Range Accuracy. */
    private final int uraiNed0;

    /** Term 1 of Non-Elevation-Dependent User Range Accuracy. */
    private final int uraiNed1;

    /** Term 2 of Non-Elevation-Dependent User Range Accuracy. */
    private final int uraiNed2;

    /** Flags.
     * @since 14.0
     */
    private final int flags;

    /**
     * Constructor.
     * @param cnv2             indicator for CNV2 messages
     * @param angularVelocity  mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle     number of weeks in the GNSS cycle
     * @param timeScales       known time scales
     * @param system           satellite system to consider for interpreting week number
     *                         (may be different from real system, for example in Rinex nav, weeks
     *                         are always according to GPS)
     * @param type             message type
     * @param prn              PRN number of the satellite
     * @param orbit            Keplerian orbit in Earth-frozen frame
     * @param aDot             change rate in semi-major axis (m/s)
     * @param deltaN0          delta of satellite mean motion
     * @param deltaN0Dot       change rate in Δn₀
     * @param iDot             inclination rate (rad/s)
     * @param omegaDot         rate of right ascension (rad/s)
     * @param cuc              amplitude of the cosine harmonic correction term to the argument of latitude
     * @param cus              amplitude of the sine harmonic correction term to the argument of latitude
     * @param crc              amplitude of the cosine harmonic correction term to the orbit radius
     * @param crs              amplitude of the sine harmonic correction term to the orbit radius
     * @param cic              amplitude of the cosine harmonic correction term to the inclination
     * @param cis              amplitude of the sine harmonic correction term to the inclination
     * @param af0              zero-th order clock correction (s)
     * @param af1              first order clock correction (s/s)
     * @param af2              second order clock correction (s/s²)
     * @param tgd              group delay differential TGD for L1-L2 correction
     * @param toc              time of clock
     * @param epochToc         time of clock epoch
     * @param transmissionTime transmission time
     * @param svAccuracy       user SV accuracy (m)
     * @param svHealth         satellite health status
     * @param iscL1CA          inter signal delay for L1 C/A
     * @param iscL1CD          inter signal delay for L1 CD
     * @param iscL1CP          inter signal delay for L1 CP
     * @param iscL2C           inter signal delay for L2 C
     * @param iscL5I5          inter signal delay for L5I
     * @param iscL5Q5          inter signal delay for L5Q
     * @param uraiEd           elevation-dependent user range accuracy
     * @param uraiNed0         term 0 of non-elevation-dependent user range accuracy
     * @param uraiNed1         term 1 of non-elevation-dependent user range accuracy
     * @param uraiNed2         term 2 of non-elevation-dependent user range accuracy
     * @param flags            flags
     */
    protected CivilianNavigationMessage(final boolean cnv2,
                                        final double angularVelocity, final int weeksInCycle,
                                        final TimeScales timeScales, final SatelliteSystem system, final String type,
                                        final int prn, final KeplerianOrbit orbit, final double aDot,
                                        final double deltaN0, final double deltaN0Dot,
                                        final double iDot, final double omegaDot,
                                        final double cuc, final double cus,
                                        final double crc, final double crs,
                                        final double cic, final double cis,
                                        final double af0, final double af1, final double af2,
                                        final double tgd, final double toc,
                                        final AbsoluteDate epochToc, final double transmissionTime,
                                        final double svAccuracy, final int svHealth,
                                        final double iscL1CA, final double iscL1CD, final double iscL1CP,
                                        final double iscL2C, final double iscL5I5, final double iscL5Q5,
                                        final int uraiEd, final int uraiNed0, final int uraiNed1, final int uraiNed2,
                                        final int flags) {
        super(angularVelocity, weeksInCycle, timeScales, system, type, prn,
              orbit, aDot, deltaN0, deltaN0Dot, iDot, omegaDot, cuc, cus, crc, crs, cic, cis,
              af0, af1, af2, tgd, toc, epochToc, transmissionTime);
        this.cnv2       = cnv2;
        this.svAccuracy = svAccuracy;
        this.svHealth   = svHealth;
        this.iscL1CA    = iscL1CA;
        this.iscL1CD    = iscL1CD;
        this.iscL1CP    = iscL1CP;
        this.iscL2C     = iscL2C;
        this.iscL5I5    = iscL5I5;
        this.iscL5Q5    = iscL5Q5;
        this.uraiEd     = uraiEd;
        this.uraiNed0   = uraiNed0;
        this.uraiNed1   = uraiNed1;
        this.uraiNed2   = uraiNed2;
        this.flags      = flags;
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param <A> type of the orbital elements (non-field version)
     * @param original regular field instance
     */
    protected <T extends CalculusFieldElement<T>,
               A extends CivilianNavigationMessage<A>> CivilianNavigationMessage(final FieldCivilianNavigationMessage<T, A, ?> original) {
        super(original);
        cnv2       = original.isCnv2();
        svAccuracy = original.getSvAccuracy().getReal();
        svHealth   = original.getSvHealth();
        iscL1CA    = original.getIscL1CA().getReal();
        iscL1CD    = original.getIscL1CD().getReal();
        iscL1CP    = original.getIscL1CP().getReal();
        iscL2C     = original.getIscL2C().getReal();
        iscL5I5    = original.getIscL5I5().getReal();
        iscL5Q5    = original.getIscL5Q5().getReal();
        uraiEd     = original.getUraiEd();
        uraiNed0   = original.getUraiNed0();
        uraiNed1   = original.getUraiNed1();
        uraiNed2   = original.getUraiNed2();
        flags      = original.getFlags();
    }

    /** Check it message is a CNV2 message.
     * @return true if message is a CNV2 message
     */
    public boolean isCnv2() {
        return cnv2;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCivilianMessage() {
        return true;
    }

    /** Get the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public double getSvAccuracy() {
        return svAccuracy;
    }

    /** Get the satellite health status.
     * @return the satellite health status
     */
    public int getSvHealth() {
        return svHealth;
    }

    /** Get inter Signal Delay for L1 C/A.
     * @return inter signal delay
     */
    public double getIscL1CA() {
        return iscL1CA;
    }

    /** Get inter Signal Delay for L1 CD.
     * @return inter signal delay
     */
    public double getIscL1CD() {
        return iscL1CD;
    }

    /** Get inter Signal Delay for L1 CP.
     * @return inter signal delay
     */
    public double getIscL1CP() {
        return iscL1CP;
    }

    /** Get inter Signal Delay for L2 C.
     * @return inter signal delay
     */
    public double getIscL2C() {
        return iscL2C;
    }

    /** Get inter Signal Delay for L5I.
     * @return inter signal delay
     */
    public double getIscL5I5() {
        return iscL5I5;
    }

    /** Get inter Signal Delay for L5Q.
     * @return inter signal delay
     */
    public double getIscL5Q5() {
        return iscL5Q5;
    }

    /** Get Elevation-Dependent User Range Accuracy.
     * @return Elevation-Dependent User Range Accuracy
     */
    public int getUraiEd() {
        return uraiEd;
    }

    /** Get term 0 of Non-Elevation-Dependent User Range Accuracy.
     * @return term 0 of Non-Elevation-Dependent User Range Accuracy
     */
    public int getUraiNed0() {
        return uraiNed0;
    }

    /** Get term 1 of Non-Elevation-Dependent User Range Accuracy.
     * @return term 1 of Non-Elevation-Dependent User Range Accuracy
     */
    public int getUraiNed1() {
        return uraiNed1;
    }

    /** Get term 2 of Non-Elevation-Dependent User Range Accuracy.
     * @return term 2 of Non-Elevation-Dependent User Range Accuracy
     */
    public int getUraiNed2() {
        return uraiNed2;
    }

    /** Get the flags.
     * @return flags
     * @since 14.0
     */
    public int getFlags() {
        return flags;
    }

}
