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

/**
 * Base class for GNSS navigation messages.
 * @param <T> type of the field elements
 * @param <O> type of the orbital elements (non-field version)
 * @author Luc Maisonobe
 * @since 13.0
 *
 * @see FieldGPSLegacyNavigationMessage
 * @see FieldGalileoNavigationMessage
 * @see FieldBeidouLegacyNavigationMessage
 * @see FieldQZSSLegacyNavigationMessage
 * @see FieldNavicLegacyNavigationMessage
 */
public abstract class FieldAbstractNavigationMessage<T extends CalculusFieldElement<T>,
                                                     O extends AbstractNavigationMessage<O>>
    extends FieldGnssOrbitalElements<T, O> {

    /** Transmission time. */
    private final T transmissionTime;

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
     * @since 14.0
     */
    public FieldAbstractNavigationMessage(final double angularVelocity, final int weeksInCycle,
                                          final TimeScales timeScales, final String type, final int prn,
                                          final GNSSDate gnssDate, final FieldKeplerianOrbit<T> orbit,
                                          final T[] nonKeplerian, final T tgd, final FieldAbsoluteDate<T> toc,
                                          final T transmissionTime) {
        super(angularVelocity, weeksInCycle, timeScales, type, prn, gnssDate, orbit, nonKeplerian, tgd, toc);
        this.transmissionTime = transmissionTime;
    }

    /**
     * Getter for transmission time.
     * @return transmission time
     */
    public T getTransmissionTime() {
        return transmissionTime;
    }

}
