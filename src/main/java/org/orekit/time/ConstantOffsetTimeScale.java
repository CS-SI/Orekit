/* Copyright 2002-2024 Luc Maisonobe
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

/** Base class for time scales with constant offset with respect to to TAI.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class ConstantOffsetTimeScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 20240321L;

    /** Name of the time scale. */
    private final String name;

    /** Offset from TAI. */
    private final double offset;

    /** Simple constructor.
     * @param name name of the time scale
     * @param offset offset from TAI
     */
    public ConstantOffsetTimeScale(final String name, final double offset) {
        this.name   = name;
        this.offset = offset;
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        return offset;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T offsetFromTAI(final FieldAbsoluteDate<T> date) {
        return date.getField().getZero().newInstance(offset);
    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        return -offset;
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

}
