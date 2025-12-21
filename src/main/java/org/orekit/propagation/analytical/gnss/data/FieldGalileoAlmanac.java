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
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

import java.util.function.Function;

/**
 * Class for Galileo almanac.
 *
 * @see "European GNSS (Galileo) Open Service, Signal In Space,
 *      Interface Control Document, Table 75"
 *
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 *
 */
public class FieldGalileoAlmanac<T extends CalculusFieldElement<T>>
    extends FieldGnssOrbitalElements<T, GalileoAlmanac, FieldGalileoAlmanac<T>> {

    /** Satellite E5a signal health status. */
    private final int healthE5a;

    /** Satellite E5b signal health status. */
    private final int healthE5b;

    /** Satellite E1-B/C signal health status. */
    private final int healthE1;

    /** Almanac Issue Of Data. */
    private final int iod;

    /** Constructor from non-field instance.
     * @param orbit    orbit in the correct field
     * @param original regular non-field instance
     */
    public FieldGalileoAlmanac(final FieldKeplerianOrbit<T> orbit, final GalileoAlmanac original) {
        super(orbit, original);
        healthE5a = original.getHealthE5a();
        healthE5b = original.getHealthE5b();
        healthE1  = original.getHealthE1();
        iod       = original.getIOD();
    }

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
     * @param healthE5a       satellite E5a signal health status
     * @param healthE5b       satellite E5b signal health status
     * @param healthE1        satellite E1-B/C signal health status
     * @param iod             issue of data
     * @since 14.0
     */
    public FieldGalileoAlmanac(final double angularVelocity, final int weeksInCycle,
                               final TimeScales timeScales, final String type, final int prn,
                               final GNSSDate gnssDate, final FieldKeplerianOrbit<T> orbit,
                               final T[] nonKeplerian, final T tgd, final T toc,
                               final int healthE5a, final int healthE5b, final int healthE1, final int iod) {
        super(angularVelocity, weeksInCycle, timeScales, type, prn, gnssDate, orbit, nonKeplerian, tgd, toc);
        this.healthE5a = healthE5a;
        this.healthE5b = healthE5b;
        this.healthE1  = healthE1;
        this.iod       = iod;
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param orbit     orbit in the correct field
     * @param original  regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldGalileoAlmanac(final FieldKeplerianOrbit<T> orbit,
                                                                   final Function<V, T> converter,
                                                                   final FieldGalileoAlmanac<V> original) {
        super(orbit, converter, original);
        healthE5a = original.getHealthE5a();
        healthE5b = original.getHealthE5b();
        healthE1  = original.getHealthE1();
        iod       = original.getIOD();
    }

    /** {@inheritDoc} */
    @Override
    public GalileoAlmanac toNonField() {
        return new GalileoAlmanac(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, V extends FieldGnssOrbitalElements<U, GalileoAlmanac, V>>
        V toField(final FieldKeplerianOrbit<U> orbit, final Function<T, U> converter) {
        return (V) new FieldGalileoAlmanac<>(orbit, converter, this);
    }

    /**
     * Gets the E1-B/C signal health status.
     *
     * @return the E1-B/C signal health status
     */
    public int getHealthE1() {
        return healthE1;
    }

    /**
     * Gets the E5a signal health status.
     *
     * @return the E5a signal health status
     */
    public int getHealthE5a() {
        return healthE5a;
    }

    /**
     * Gets the E5b signal health status.
     *
     * @return the E5b signal health status
     */
    public int getHealthE5b() {
        return healthE5b;
    }

    /**
     * Gets the Issue of Data (IOD).
     *
     * @return the Issue Of Data
     */
    public int getIOD() {
        return iod;
    }

}
