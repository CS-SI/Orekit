/* Copyright 2002-2025 CS GROUP
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
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

import java.util.function.DoubleFunction;

/**
 * Container for data contained in a QZSS navigation message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class QZSSLegacyNavigationMessage extends LegacyNavigationMessage<QZSSLegacyNavigationMessage> {

    /** Message type.
     * @since 14.0
     */
    public static final String LNAV = "LNAV";

    /** Constructor.
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
     * @param transmissionTime transmission time
     * @param iode             issue of data, ephemeris
     * @param iodc             issue of data, clock
     * @param svAccuracy       user SV accuracy (m)
     * @param svHealth         satellite health status
     * @param fitInterval      fit interval
     * @param l2Codes          codes on L2 channel
     * @param l2PFlags         L2 P data flags.
     */
    public QZSSLegacyNavigationMessage(final TimeScales timeScales, final String type,
                                       final int prn, final GNSSDate gnssDate, final KeplerianOrbit orbit,
                                       final double aDot, final double deltaN0, final double deltaN0Dot,
                                       final double iDot, final double omegaDot,
                                       final double cuc, final double cus,
                                       final double crc, final double crs,
                                       final double cic, final double cis,
                                       final double af0, final double af1, final double af2,
                                       final double tgd, final AbsoluteDate toc, final double transmissionTime,
                                       final int iode, final int iodc, final double svAccuracy,
                                       final int svHealth, final int fitInterval,
                                       final int l2Codes, final int l2PFlags) {
        super(GNSSConstants.QZSS_AV, GNSSConstants.QZSS_WEEK_NB,
              timeScales, type, prn, gnssDate, orbit, aDot, deltaN0, deltaN0Dot, iDot, omegaDot,
              cuc, cus, crc, crs, cic, cis, af0, af1, af2, tgd, toc, transmissionTime,
              iode, iodc, svAccuracy, svHealth, fitInterval, l2Codes, l2PFlags);
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param original regular field instance
     */
    public <T extends CalculusFieldElement<T>> QZSSLegacyNavigationMessage(final FieldQZSSLegacyNavigationMessage<T> original) {
        super(original);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>>
    FieldQZSSLegacyNavigationMessage<T> toField(final FieldKeplerianOrbit<T> orbit,
                                                final T[] nonKeplerian,
                                                final DoubleFunction<T> converter) {
        return new FieldQZSSLegacyNavigationMessage<>(getAngularVelocity(), getWeeksInCycle(), getTimeScales(),
                                                      getType(), getPrn(), getGnssDate(), orbit, nonKeplerian,
                                                      converter.apply(getTgd()), toFieldToc(orbit),
                                                      converter.apply(getTransmissionTime()),
                                                      getIODE(), getIODC(),
                                                      converter.apply(getSvAccuracy()),
                                                      getSvHealth(), getFitInterval(),
                                                      getL2Codes(), getL2PFlags());
    }

    /** {@inheritDoc} */
    @Override
    public QZSSLegacyNavigationMessageFactory baseFactory(final Frame inertial, final Frame bodyFixed) {
        return new QZSSLegacyNavigationMessageFactory(getTimeScales(), getGnssDate().getSystem(), getType(),
                                                      inertial, bodyFixed);
    }

}
