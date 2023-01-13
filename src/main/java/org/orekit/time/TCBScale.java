/* Copyright 2002-2023 CS GROUP
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
package org.orekit.time;

import org.hipparchus.CalculusFieldElement;

/** Barycentric Coordinate Time.
 * <p>Coordinate time at the center of mass of the Solar System.
 * This time scale depends linearly from {@link TDBScale Barycentric Dynamical Time}.</p>
 * <p>This is intended to be accessed thanks to {@link TimeScales},
 * so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class TCBScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131209L;

    /** LG rate. */
    private static final double LB_RATE = 1.550519768e-8;

    /** Reference date for TCB.
     * <p>The reference date is such that the four following instants are equal:</p>
     * <ul>
     *   <li>1977-01-01T00:00:32.184 TT</li>
     *   <li>1977-01-01T00:00:32.184 TCG</li>
     *   <li>1977-01-01T00:00:32.184 TCB</li>
     *   <li>1977-01-01T00:00:00.000 TAI</li>
     * </ul>
     */
    private final AbsoluteDate referenceDate;

    /** Barycentric dynamic time scale. */
    private final TDBScale tdb;

    /**
     * Package private constructor for the factory.
     *
     * @param tdb Barycentric dynamic time scale
     * @param tai TAI time scale.
     */
    TCBScale(final TDBScale tdb, final TimeScale tai) {
        this.tdb = tdb;
        referenceDate = new AbsoluteDate(1977, 01, 01, tai);
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        return tdb.offsetFromTAI(date) + LB_RATE * date.durationFrom(referenceDate);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T offsetFromTAI(final FieldAbsoluteDate<T> date) {
        return tdb.offsetFromTAI(date).add(date.durationFrom(referenceDate).multiply(LB_RATE));
    }

    /** {@inheritDoc} */
    public String getName() {
        return "TCB";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

}
