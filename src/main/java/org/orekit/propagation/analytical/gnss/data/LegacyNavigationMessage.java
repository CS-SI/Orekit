/* Copyright 2002-2026 CS GROUP
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
 * Container for data contained in a GPS/QZNSS legacy navigation message.
 * @param <O> type of the orbital elements
 * @author Bryan Cazabonne
 * @since 11.0
 */
public abstract class LegacyNavigationMessage<O extends LegacyNavigationMessage<O>>
    extends AbstractNavigationMessage<O> {

    /** Issue of Data, Ephemeris. */
    private final int iode;

    /** Issue of Data, Clock. */
    private final int iodc;

    /** The user SV accuracy (m). */
    private final double svAccuracy;

    /** Satellite health status. */
    private final int svHealth;

    /** Fit interval.
     * @since 12.0
     */
    private final int fitInterval;

    /** Codes on L2 channel.
     * @since 14.0
     */
    private final int l2Codes;

    /** L2 P data flags.
     * @since 14.0
     */
    private final int l2PFlags;

    /**
     * Constructor.
     * @param angularVelocity  mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle     number of weeks in the GNSS cycle
     * @param timeScales       known time scales
     * @param system           satellite system to consider for interpreting week number
     *                         (may be different from real system, for example in Rinex nav, weeks
     *                         are always according to GPS)
     * @param type             message type
     * @param prn              PRN number of the satellite
     * @param week             reference Week of the orbit
     * @param orbit            Keplerian orbit in Earth-frozen frame
     * @param time             reference time
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
     * @param iode             issue of data, ephemeris
     * @param iodc             issue of data, clock
     * @param svAccuracy       user SV accuracy (m)
     * @param svHealth         satellite health status
     * @param fitInterval      fit interval
     * @param l2Codes          codes on L2 channel
     * @param l2PFlags         L2 P data flags.
     */
    protected LegacyNavigationMessage(final double angularVelocity, final int weeksInCycle,
                                      final TimeScales timeScales, final SatelliteSystem system, final String type,
                                      final int prn, final int week, final KeplerianOrbit orbit,
                                      final double time, final double aDot,
                                      final double deltaN0, final double deltaN0Dot,
                                      final double iDot, final double omegaDot,
                                      final double cuc, final double cus,
                                      final double crc, final double crs,
                                      final double cic, final double cis,
                                      final double af0, final double af1, final double af2,
                                      final double tgd, final double toc,
                                      final AbsoluteDate epochToc, final double transmissionTime,
                                      final int iode, final int iodc, final double svAccuracy,
                                      final int svHealth, final int fitInterval,
                                      final int l2Codes, final int l2PFlags) {
        super(angularVelocity, weeksInCycle, timeScales, system, type, prn, week, orbit,
              time, aDot, deltaN0, deltaN0Dot, iDot, omegaDot, cuc, cus, crc, crs, cic, cis,
              af0, af1, af2, tgd, toc, epochToc, transmissionTime);
        this.iode        = iode;
        this.iodc        = iodc;
        this.svAccuracy  = svAccuracy;
        this.svHealth    = svHealth;
        this.fitInterval = fitInterval;
        this.l2Codes     = l2Codes;
        this.l2PFlags    = l2PFlags;
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param <A> type of the orbital elements (non-field version)
     * @param original regular field instance
     */
    protected <T extends CalculusFieldElement<T>, A extends LegacyNavigationMessage<A>>
        LegacyNavigationMessage(final FieldLegacyNavigationMessage<T, A> original) {
        super(original);
        iode        = original.getIODE();
        iodc        = original.getIODC();
        svAccuracy  = original.getSvAccuracy().getReal();
        svHealth    = original.getSvHealth();
        fitInterval = original.getFitInterval();
        l2Codes     = original.getL2Codes();
        l2PFlags    = original.getL2PFlags();
    }

    /** Get the Issue Of Data Ephemeris (IODE).
     * @return the Issue Of Data Ephemeris (IODE)
     */
    public int getIODE() {
        return iode;
    }

    /** Get the Issue Of Data Clock (IODC).
     * @return the Issue Of Data Clock (IODC)
     */
    public int getIODC() {
        return iodc;
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

    /** Get the fit interval.
     * @return the fit interval
     * @since 12.0
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
