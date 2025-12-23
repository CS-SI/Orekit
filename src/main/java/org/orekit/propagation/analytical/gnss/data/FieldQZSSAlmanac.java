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
 * This class holds a QZSS almanac as read from YUMA files.
 *
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 *
 */
public class FieldQZSSAlmanac<T extends CalculusFieldElement<T>>
    extends FieldGnssOrbitalElements<T, QZSSAlmanac, FieldQZSSAlmanac<T>> {

    /** Source of the almanac. */
    private final String source;

    /** Health status. */
    private final int health;

    /** Creates a new instance.
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle    number of weeks in the GNSS cycle
     * @param timeScales      known time scales
     * @param type            type (null if not a navigation message)
     * @param prn             PRN number of the satellite
     * @param gnssDate        GNSS date (<em>must</em> be consistent with {@code orbit})
     * @param orbit           Keplerian orbit in Earth-frozen frame
     * @param nonKeplerian    15 non-Keplerian parameters (in the order given by {@link NonKeplerianDriversFactory}
     * @param tgd             group delay differential TGD for L1-L2 correction
     * @param toc             time of clock
     * @param source          source of the almanac
     * @param health          health status
     * @since 14.0
     */
    public FieldQZSSAlmanac(final double angularVelocity, final int weeksInCycle,
                            final TimeScales timeScales, final String type, final int prn,
                            final GNSSDate gnssDate, final FieldKeplerianOrbit<T> orbit,
                            final T[] nonKeplerian, final T tgd, final FieldAbsoluteDate<T> toc,
                            final String source, final int health) {
        super(angularVelocity, weeksInCycle, timeScales, type, prn, gnssDate, orbit, nonKeplerian, tgd, toc);
        this.source = source;
        this.health = health;
    }

    /** {@inheritDoc} */
    @Override
    public QZSSAlmanac toNonField() {
        return new QZSSAlmanac(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, V extends FieldGnssOrbitalElements<U, QZSSAlmanac, V>>
        V toField(final FieldKeplerianOrbit<U> orbit, final U[] nonKeplerian, final Function<T, U> converter) {
        return (V) new FieldQZSSAlmanac<>(getAngularVelocity(), getWeeksInCycle(), getTimeScales(),
                                          getType(), getPrn(), getGnssDate().getGnssDate(),
                                          orbit, nonKeplerian,
                                          converter.apply(getTgd()),
                                          new FieldAbsoluteDate<>(orbit.getDate().getField(),
                                                                  getToc().toAbsoluteDate()),
                                          getSource(), getHealth());
    }

    /**
     * Gets the source of this QZSS almanac.
     *
     * @return the source of this QZSS almanac
     */
    public String getSource() {
        return source;
    }

    /**
     * Gets the Health status.
     *
     * @return the Health status
     */
    public int getHealth() {
        return health;
    }

}
