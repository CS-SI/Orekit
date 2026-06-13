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
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

/**
 * Base class for GNSS navigation messages.
 * @param <O> type of the orbital elements
 * @author Bryan Cazabonne
 * @since 11.0
 *
 * @see GPSLegacyNavigationMessage
 * @see GalileoNavigationMessage
 * @see BeidouLegacyNavigationMessage
 * @see QZSSLegacyNavigationMessage
 * @see NavICLegacyNavigationMessage
 */
public abstract class AbstractNavigationMessage<O extends AbstractNavigationMessage<O>>
    extends GNSSOrbitalElements<O> implements NavigationMessage {

    /** Time of clock epoch. */
    private final AbsoluteDate epochToc;

    /** Transmission time.
     * @since 12.0
     */
    private final double transmissionTime;

    /**
     * Constructor.
     * @param angularVelocity  mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle     number of weeks in the GNSS cycle
     * @param timeScales       known time scales
     * @param type             message type
     * @param prn              PRN number of the satellite
     * @param gnssDate         GNSS date (<em>must</em> be consistent with {@code orbit})
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
     */
    protected AbstractNavigationMessage(final double angularVelocity, final int weeksInCycle,
                                        final TimeScales timeScales, final String type,
                                        final int prn, final GNSSDate gnssDate, final KeplerianOrbit orbit,
                                        final double aDot, final double deltaN0, final double deltaN0Dot,
                                        final double iDot, final double omegaDot,
                                        final double cuc, final double cus,
                                        final double crc, final double crs,
                                        final double cic, final double cis,
                                        final double af0, final double af1, final double af2,
                                        final double tgd, final double toc,
                                        final AbsoluteDate epochToc, final double transmissionTime) {
        super(angularVelocity, weeksInCycle, timeScales, type, prn,
              gnssDate, orbit, aDot, deltaN0, deltaN0Dot, iDot, omegaDot,
              cuc, cus, crc, crs, cic, cis, af0, af1, af2, tgd, toc);
        this.epochToc         = epochToc;
        this.transmissionTime = transmissionTime;
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param <A> type of the orbital elements (non-field version)
     * @param original regular field instance
     */
    protected <T extends CalculusFieldElement<T>,
               A extends AbstractNavigationMessage<A>> AbstractNavigationMessage(final FieldAbstractNavigationMessage<T, A, ?> original) {
        super(original);
        epochToc         = original.getEpochToc().toAbsoluteDate();
        transmissionTime = original.getTransmissionTime().getReal();
    }

    /** {@inheritDoc} */
    @Override
    public String getNavigationMessageType() {
        return getType();
    }

    /** {@inheritDoc} */
    @Override
    public String getNavigationMessageSubType() {
        return null;
    }

    /** Get the time of clock epoch.
     * @return the time of clock epoch
     */
    public AbsoluteDate getEpochToc() {
        return epochToc;
    }

    /** Get transmission time.
     * @return transmission time
     * @since 12.0
     */
    public double getTransmissionTime() {
        return transmissionTime;
    }

}
