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
 * Container for data contained in a GPS navigation message.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class GPSCivilianNavigationMessage extends CivilianNavigationMessage<GPSCivilianNavigationMessage> {

    /** Message type.
     * @since 14.0
     */
    public static final String CNAV = "CNAV";

    /** Message type.
     * @since 14.0
     */
    public static final String CNV2 = "CNV2";

    /** Constructor.
     * @param cnv2             indicator for CNV2 messages
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
    public GPSCivilianNavigationMessage(final boolean cnv2,
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
        super(cnv2, GNSSConstants.GPS_AV, GNSSConstants.GPS_WEEK_NB,
              timeScales, system, type, prn, orbit,
              aDot, deltaN0, deltaN0Dot, iDot, omegaDot, cuc, cus, crc, crs, cic, cis,
              af0, af1, af2, tgd, toc, epochToc, transmissionTime,
              svAccuracy, svHealth, iscL1CA, iscL1CD, iscL1CP, iscL2C, iscL5I5, iscL5Q5,
              uraiEd, uraiNed0, uraiNed1, uraiNed2, flags);
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param original regular field instance
     */
    public <T extends CalculusFieldElement<T>> GPSCivilianNavigationMessage(final FieldGPSCivilianNavigationMessage<T> original) {
        super(original);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>, F extends FieldGnssOrbitalElements<T, GPSCivilianNavigationMessage, F>>
        F toField(final FieldKeplerianOrbit<T> orbit) {
        return (F) new FieldGPSCivilianNavigationMessage<>(orbit, this);
    }

    /** {@inheritDoc} */
    @Override
    public GPSCivilianNavigationMessageFactory baseFactory(final Frame inertial, final Frame bodyFixed) {
        return new GPSCivilianNavigationMessageFactory(getTimeScales(), getSystem(), getType(),
                                                       inertial, bodyFixed, isCnv2());
    }

}
