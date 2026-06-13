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
public class FieldQZSSCivilianNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldCivilianNavigationMessage<T, QZSSCivilianNavigationMessage, FieldQZSSCivilianNavigationMessage<T>> {

    /** Constructor from non-field instance.
     * @param orbit    orbit in the correct field
     * @param original regular non-field instance
     */
    public FieldQZSSCivilianNavigationMessage(final FieldKeplerianOrbit<T> orbit, final QZSSCivilianNavigationMessage original) {
        super(orbit, original);
    }

    /** Creates a new instance.
     * @param cnv2             indicator for CNV2 messages
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
     * @since 14.0
     */
    public FieldQZSSCivilianNavigationMessage(final boolean cnv2,
                                              final double angularVelocity, final int weeksInCycle,
                                              final TimeScales timeScales, final String type, final int prn,
                                              final GNSSDate gnssDate, final FieldKeplerianOrbit<T> orbit,
                                              final T[] nonKeplerian, final T tgd, final T toc,
                                              final FieldAbsoluteDate<T> epochToc, final T transmissionTime,
                                              final T svAccuracy, final int svHealth,
                                              final T iscL1CA, final T iscL1CD, final T iscL1CP,
                                              final T iscL2C, final T iscL5I5, final T iscL5Q5,
                                              final int uraiEd, final int uraiNed0, final int uraiNed1, final int uraiNed2,
                                              final int flags) {
        super(cnv2, angularVelocity, weeksInCycle, timeScales, type, prn, gnssDate, orbit, nonKeplerian,
              tgd, toc, epochToc, transmissionTime, svAccuracy, svHealth,
              iscL1CA, iscL1CD, iscL1CP, iscL2C, iscL5I5, iscL5Q5,
              uraiEd, uraiNed0, uraiNed1, uraiNed2, flags);
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param orbit     orbit in the correct field
     * @param original  regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldQZSSCivilianNavigationMessage(final FieldKeplerianOrbit<T> orbit,
                                                                                  final Function<V, T> converter,
                                                                                  final FieldQZSSCivilianNavigationMessage<V> original) {
        super(orbit, converter, original);
    }

    /** {@inheritDoc} */
    @Override
    public QZSSCivilianNavigationMessage toNonField() {
        return new QZSSCivilianNavigationMessage(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, V extends FieldGnssOrbitalElements<U, QZSSCivilianNavigationMessage, V>>
        V toField(final FieldKeplerianOrbit<U> orbit, final Function<T, U> converter) {
        return (V) new FieldQZSSCivilianNavigationMessage<>(orbit, converter, this);
    }

}
