/* Copyright 2002-2021 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.utils.units;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Converter between units.
 * <p>
 * Instances of this class are immutable.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class UnitsConverter {

    /** Source unit. */
    private final Unit from;

    /** Destination unit. */
    private final Unit to;

    /** Conversion factor. */
    private final double factor;

    /** Simple constructor.
     * @param from source unit
     * @param to destination unit
     */
    public UnitsConverter(final Unit from, final Unit to) {
        this.from = from;
        this.to   = to;
        if (!from.sameDimension(to)) {
            throw new OrekitException(OrekitMessages.INCOMPATIBLE_UNITS,
                                      from.getName(), to.getName());
        }
        this.factor = from.getScale() / to.getScale();
    }

    /** Get the source unit.
     * @return source unit
     */
    public Unit getFrom() {
        return from;
    }

    /** Get the destination unit.
     * @return destination unit
     */
    public Unit getTo() {
        return to;
    }

    /** Convert a value.
     * @param value value in the {@link #getFrom() source unit}
     * @return value converted in the {@link #getTo() destination unit}
     */
    public double convert(final double value) {
        return factor * value;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return from.getName() + " → " + to.getName();
    }

}
