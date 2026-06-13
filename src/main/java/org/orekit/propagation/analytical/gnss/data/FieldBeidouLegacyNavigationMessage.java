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
 * Container for data contained in a BeiDou navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldBeidouLegacyNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldAbstractNavigationMessage<T, BeidouLegacyNavigationMessage> {

    /** Indicator for D2 messages.
     * @since 14.0
     */
    private final boolean d2;

    /** Age of Data, Ephemeris. */
    private final int aode;

    /** Age of Data, Clock. */
    private final int aodc;

    /** Health identifier.
     * @since 14.0
     */
    private final int satH1;

    /** B1/B3 Group Delay Differential (s). */
    private final T tgd1;

    /** B2/B3 Group Delay Differential (s). */
    private final T tgd2;

    /** The user SV accuracy (m). */
    private final T svAccuracy;

    /** Creates a new instance.
     * @param d2               indicator for D2 messages
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
     * @param aode             age of data, ephemeris
     * @param aodc             age of data, clock
     * @param satH1            health identifier
     * @param tgd1             B1/B3 Group Delay Differential (s)
     * @param tgd2             B2/B3 Group Delay Differential (s)
     * @param svAccuracy       user SV accuracy (m)
     * @since 14.0
     */
    public FieldBeidouLegacyNavigationMessage(final boolean d2,
                                              final double angularVelocity, final int weeksInCycle,
                                              final TimeScales timeScales, final String type, final int prn,
                                              final GNSSDate gnssDate, final FieldKeplerianOrbit<T> orbit,
                                              final T[] nonKeplerian, final T tgd,
                                              final FieldAbsoluteDate<T> toc, final T transmissionTime,
                                              final int aode, final int aodc, final int satH1,
                                              final T tgd1, final T tgd2, final T svAccuracy) {
        super(angularVelocity, weeksInCycle, timeScales, type, prn, gnssDate, orbit, nonKeplerian,
              tgd, toc, transmissionTime);
        this.d2         = d2;
        this.aode       = aode;
        this.aodc       = aodc;
        this.satH1      = satH1;
        this.tgd1       = tgd1;
        this.tgd2       = tgd2;
        this.svAccuracy = svAccuracy;
    }

    /** {@inheritDoc} */
    @Override
    public BeidouLegacyNavigationMessage toNonField() {
        return new BeidouLegacyNavigationMessage(this);
    }

    /** {@inheritDoc} */
    @Override
    public <U extends CalculusFieldElement<U>>
        FieldBeidouLegacyNavigationMessage<U> toField(final FieldKeplerianOrbit<U> orbit,
                                                      final U[] nonKeplerian,
                                                      final Function<T, U> converter) {
        return new FieldBeidouLegacyNavigationMessage<>(isD2(),
                                                        getAngularVelocity(), getWeeksInCycle(), getTimeScales(),
                                                        getType(), getPrn(), getGnssDate().getGnssDate(),
                                                        orbit, nonKeplerian,
                                                        converter.apply(getTgd()), toFieldToc(orbit),
                                                        converter.apply(getTransmissionTime()),
                                                        getAODE(), getAODC(), getSatH1(),
                                                        converter.apply(getTGD1()),
                                                        converter.apply(getTGD2()),
                                                        converter.apply(getSvAccuracy()));
    }

    /**
     * Check if message is a D2 message.
     * @return true if message is a D2 message
     * @since 14.0
     */
    public boolean isD2() {
        return d2;
    }

    /**
     * Getter for the Age Of Data Clock (AODC).
     * @return the Age Of Data Clock (AODC)
     */
    public int getAODC() {
        return aodc;
    }

    /**
     * Getter for the Age Of Data Ephemeris (AODE).
     * @return the Age Of Data Ephemeris (AODE)
     */
    public int getAODE() {
        return aode;
    }

    /**
     * Getter for the estimated group delay differential TGD1 for B1I signal.
     * @return the estimated group delay differential TGD1 for B1I signal (s)
     */
    public T getTGD1() {
        return tgd1;
    }

    /**
     * Getter for the estimated group delay differential TGD for B2I signal.
     * @return the estimated group delay differential TGD2 for B2I signal (s)
     */
    public T getTGD2() {
        return tgd2;
    }

    /**
     * Getter for the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public T getSvAccuracy() {
        return svAccuracy;
    }

    /** Get the health identifier.
     * @return health identifier
     * @since 14.0
     */
    public int getSatH1() {
        return satH1;
    }

}
