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


/** Beidou system time scale.
 * <p>By convention, BDT = UTC on January 1st 2006.</p>
 * <p>This is intended to be accessed thanks to {@link TimeScales},
 * so there is no public constructor.</p>
 * @see AbsoluteDate
 */
public class BDTScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 20180323L;

    /** Offset from TAI. */
    private static final double OFFSET = -33;

    /** Package private constructor for the factory.
     */
    BDTScale() {
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        return OFFSET;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T offsetFromTAI(final FieldAbsoluteDate<T> date) {
        return date.getField().getZero().add(OFFSET);
    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        return -OFFSET;
    }

    /** {@inheritDoc} */
    public String getName() {
        return "BDT";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

}
