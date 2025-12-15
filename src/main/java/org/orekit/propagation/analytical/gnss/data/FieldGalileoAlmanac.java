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
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;

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
    extends FieldGnssOrbitalElements<T, GalileoAlmanac> {

    /** Nominal inclination (Ref: Galileo ICD - Table 75). */
    private static final double I0 = FastMath.toRadians(56.0);

    /** Nominal semi-major axis in meters (Ref: Galileo ICD - Table 75). */
    private static final double A0 = 29600000;

    /** Satellite E5a signal health status. */
    private final int healthE5a;

    /** Satellite E5b signal health status. */
    private final int healthE5b;

    /** Satellite E1-B/C signal health status. */
    private final int healthE1;

    /** Almanac Issue Of Data. */
    private final int iod;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldGalileoAlmanac(final Field<T> field, final GalileoAlmanac original) {
        super(field, original);
        healthE5a = original.getHealthE5a();
        healthE5b = original.getHealthE5b();
        healthE1  = original.getHealthE1();
        iod       = original.getIOD();
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldGalileoAlmanac(final Function<V, T> converter,
                                                                   final FieldGalileoAlmanac<V> original) {
        super(converter, original);
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
    public <U extends CalculusFieldElement<U>, G extends FieldGnssOrbitalElements<U, GalileoAlmanac>>
        G changeField(final Function<T, U> converter) {
        return (G) new FieldGalileoAlmanac<>(converter, this);
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
