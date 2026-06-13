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
 * Container for data contained in a Galileo navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldGalileoNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldAbstractNavigationMessage<T, GalileoNavigationMessage, FieldGalileoNavigationMessage<T>> {

    /** Issue of Data of the navigation batch. */
    private final int iodNav;

    /** Data source. */
    private final int dataSource;

    /** E1/E5a broadcast group delay (s). */
    private final T bgbE1E5a;

    /** E5b/E1 broadcast group delay (s). */
    private final T bgdE5bE1;

    /** Signal in space accuracy. */
    private final T sisa;

    /** Satellite health status. */
    private final T svHealth;

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
     * @param epochToc         time of clock epoch
     * @param transmissionTime transmission time
     * @param iodNav           issue of Data of the navigation batch
     * @param dataSource       data source
     * @param bgbE1E5a         E1/E5a broadcast group delay (s)
     * @param bgdE5bE1         E5b/E1 broadcast group delay (s)
     * @param sisa             signal in space accuracy
     * @param svHealth         satellite health status
     * @since 14.0
     */
    public FieldGalileoNavigationMessage(final double angularVelocity, final int weeksInCycle,
                                         final TimeScales timeScales, final String type, final int prn,
                                         final GNSSDate gnssDate, final FieldKeplerianOrbit<T> orbit,
                                         final T[] nonKeplerian, final T tgd, final T toc,
                                         final FieldAbsoluteDate<T> epochToc, final T transmissionTime,
                                         final int iodNav, final int dataSource,
                                         final T bgbE1E5a, final T bgdE5bE1,
                                         final T sisa, final T svHealth) {
        super(angularVelocity, weeksInCycle, timeScales, type, prn, gnssDate, orbit, nonKeplerian,
              tgd, toc, epochToc, transmissionTime);
        this.iodNav     = iodNav;
        this.dataSource = dataSource;
        this.bgbE1E5a   = bgbE1E5a;
        this.bgdE5bE1   = bgdE5bE1;
        this.sisa       = sisa;
        this.svHealth   = svHealth;
    }

    /** {@inheritDoc} */
    @Override
    public GalileoNavigationMessage toNonField() {
        return new GalileoNavigationMessage(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, V extends FieldGnssOrbitalElements<U, GalileoNavigationMessage, V>>
        V toField(final FieldKeplerianOrbit<U> orbit, final U[] nonKeplerian, final Function<T, U> converter) {
        return (V) new FieldGalileoNavigationMessage<>(getAngularVelocity(), getWeeksInCycle(), getTimeScales(),
                                                       getType(), getPrn(), getGnssDate().getGnssDate(),
                                                       orbit, nonKeplerian,
                                                       converter.apply(getTgd()), converter.apply(getToc()),
                                                       new FieldAbsoluteDate<>(orbit.getMu().getField(),
                                                                               getEpochToc().toAbsoluteDate()),
                                                       converter.apply(getTransmissionTime()),
                                                       getIODNav(), getDataSource(),
                                                       converter.apply(getBGDE1E5a()),
                                                       converter.apply(getBGDE5bE1()),
                                                       converter.apply(getSisa()),
                                                       converter.apply(getSvHealth()));
    }

    /**
     * Getter for the the Issue Of Data (IOD).
     * @return the Issue Of Data (IOD)
     */
    public int getIODNav() {
        return iodNav;
    }

    /**
     * Getter for the the data source.
     * @return the data source
     */
    public int getDataSource() {
        return dataSource;
    }

    /**
     * Getter for the E1/E5a broadcast group delay.
     * @return the E1/E5a broadcast group delay (s)
     */
    public T getBGDE1E5a() {
        return bgbE1E5a;
    }

    /**
     * Getter for the the Broadcast Group Delay E5b/E1.
     * @return the Broadcast Group Delay E5b/E1 (s)
     */
    public T getBGDE5bE1() {
        return bgdE5bE1;
    }

    /**
     * Getter for the signal in space accuracy (m).
     * @return the signal in space accuracy
     */
    public T getSisa() {
        return sisa;
    }

    /**
     * Getter for the SV health status.
     * @return the SV health status
     */
    public T getSvHealth() {
        return svHealth;
    }

}
