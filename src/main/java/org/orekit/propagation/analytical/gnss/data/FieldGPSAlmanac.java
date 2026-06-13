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

import java.util.function.Function;

/**
 * This class holds a GPS almanac as read from SEM or YUMA files.
 *
 * <p>Depending on the source (SEM or YUMA), some fields may be filled in or not.
 * An almanac read from a YUMA file doesn't hold SVN number, average URA and satellite
 * configuration.</p>
 *
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 *
 */
public class FieldGPSAlmanac<T extends CalculusFieldElement<T>>
    extends FieldGnssOrbitalElements<T, GPSAlmanac, FieldGPSAlmanac<T>> {

    /** Source of the almanac. */
    private final String source;

    /** SVN number. */
    private final int svn;

    /** Health status. */
    private final int health;

    /** Average URA. */
    private final int ura;

    /** Satellite configuration. */
    private final int satConfiguration;

    /** Constructor from non-field instance.
     * @param orbit    orbit in the correct field
     * @param original regular non-field instance
     */
    public FieldGPSAlmanac(final FieldKeplerianOrbit<T> orbit, final GPSAlmanac original) {
        super(orbit, original);
        source           = original.getSource();
        svn              = original.getSVN();
        health           = original.getHealth();
        ura              = original.getURA();
        satConfiguration = original.getSatConfiguration();
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param orbit     orbit in the correct field
     * @param original  regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldGPSAlmanac(final FieldKeplerianOrbit<T> orbit,
                                                               final Function<V, T> converter,
                                                               final FieldGPSAlmanac<V> original) {
        super(orbit, converter, original);
        source           = original.getSource();
        svn              = original.getSVN();
        health           = original.getHealth();
        ura              = original.getURA();
        satConfiguration = original.getSatConfiguration();
    }

    /** {@inheritDoc} */
    @Override
    public GPSAlmanac toNonField() {
        return new GPSAlmanac(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, V extends FieldGnssOrbitalElements<U, GPSAlmanac, V>>
        V toField(final FieldKeplerianOrbit<U> orbit, final Function<T, U> converter) {
        return (V) new FieldGPSAlmanac<>(orbit, converter, this);
    }

    /**
     * Gets the source of this GPS almanac.
     * <p>Sources can be SEM or YUMA, when the almanac is read from a file.</p>
     *
     * @return the source of this GPS almanac
     */
    public String getSource() {
        return source;
    }

    /**
     * Gets the satellite "SVN" reference number.
     *
     * @return the satellite "SVN" reference number
     */
    public int getSVN() {
        return svn;
    }

    /**
     * Gets the Health status.
     *
     * @return the Health status
     */
    public int getHealth() {
        return health;
    }

    /**
     * Gets the average URA number.
     *
     * @return the average URA number
     */
    public int getURA() {
        return ura;
    }

    /**
     * Gets the satellite configuration.
     *
     * @return the satellite configuration
     */
    public int getSatConfiguration() {
        return satConfiguration;
    }

}
