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
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

import java.util.function.Function;

/**
 * Container for data contained in a QZSS navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldQZSSLegacyNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldLegacyNavigationMessage<T, QZSSLegacyNavigationMessage> {

    /** Creates a new instance.
     * @param angularVelocity  mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle     number of weeks in the GNSS cycle
     * @param timeScales       known time scales
     * @param type             type (null if not a navigation message)
     * @param prn              PRN number of the satellite
     * @param gnssDate         GNSS date (<em>must</em> be consistent with {@code orbit})
     * @param orbit            Keplerian orbit in Earth-frozen frame
     * @param nonKeplerian     15 non-Keplerian parameters (in the order given by {@link NonKeplerianDriversFactory}
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
     * @since 14.0
     */
    public FieldQZSSLegacyNavigationMessage(final double angularVelocity, final int weeksInCycle,
                                            final TimeScales timeScales, final String type, final int prn,
                                            final GNSSDate gnssDate, final FieldKeplerianOrbit<T> orbit,
                                            final T[] nonKeplerian, final T tgd,
                                            final FieldAbsoluteDate<T> toc, final T transmissionTime,
                                            final int iode, final int iodc, final T svAccuracy,
                                            final int svHealth, final int fitInterval,
                                            final int l2Codes, final int l2PFlags) {
        super(angularVelocity, weeksInCycle, timeScales, type, prn, gnssDate, orbit, nonKeplerian,
              tgd, toc, transmissionTime,
              iode, iodc, svAccuracy, svHealth, fitInterval, l2Codes, l2PFlags);
    }

    /** {@inheritDoc} */
    @Override
    public QZSSLegacyNavigationMessage toNonField() {
        return new QZSSLegacyNavigationMessage(this);
    }

    /** {@inheritDoc} */
    @Override
    public <U extends CalculusFieldElement<U>>
        FieldQZSSLegacyNavigationMessage<U> toField(final FieldKeplerianOrbit<U> orbit,
                                                    final U[] nonKeplerian,
                                                    final Function<T, U> converter) {
        return new FieldQZSSLegacyNavigationMessage<>(getAngularVelocity(), getWeeksInCycle(), getTimeScales(),
                                                      getType(), getPrn(), getGnssDate().getGnssDate(),
                                                      orbit, nonKeplerian,
                                                      converter.apply(getTgd()), toFieldToc(orbit),
                                                      converter.apply(getTransmissionTime()),
                                                      getIODE(), getIODC(),
                                                      converter.apply(getSvAccuracy()),
                                                      getSvHealth(), getFitInterval(),
                                                      getL2Codes(), getL2PFlags());
    }

}
