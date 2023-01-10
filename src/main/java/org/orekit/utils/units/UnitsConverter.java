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

    /** Identity converter. */
    public static final UnitsConverter IDENTITY =
                    new UnitsConverter(Unit.ONE, Unit.ONE);

    /** Percents to units converter. */
    public static final UnitsConverter PERCENTS_TO_UNIT =
                    new UnitsConverter(Unit.PERCENT, Unit.ONE);

    /** Arcseconds to radians converter. */
    public static final UnitsConverter ARC_SECONDS_TO_RADIANS =
                    new UnitsConverter(Unit.parse("as"), Unit.RADIAN);

    /** Milli arcseconds to radians converter. */
    public static final UnitsConverter MILLI_ARC_SECONDS_TO_RADIANS =
                    new UnitsConverter(Unit.parse("mas"), Unit.RADIAN);

    /** Milli seconds to seconds converter. */
    public static final UnitsConverter MILLI_SECONDS_TO_SECONDS =
                    new UnitsConverter(Unit.parse("ms"), Unit.SECOND);

    /** Nano Teslas to Tesla converter. */
    public static final UnitsConverter NANO_TESLAS_TO_TESLAS =
                    new UnitsConverter(Unit.parse("nT"), Unit.TESLA);

    /** Days to seconds converter. */
    public static final UnitsConverter DAYS_TO_SECONDS =
                    new UnitsConverter(Unit.DAY, Unit.SECOND);

    /** Kilometres to metres converter. */
    public static final UnitsConverter KILOMETRES_TO_METRES =
                    new UnitsConverter(Unit.KILOMETRE, Unit.METRE);

    /** Square kilometres to square metres converter. */
    public static final UnitsConverter KILOMETRES_2_TO_METRES_2 =
                    new UnitsConverter(Unit.parse("km²"), Unit.parse("m²"));

    /** km³/s² to m³/s² converter. */
    public static final UnitsConverter KM3_P_S2_TO_M3_P_S2 =
                    new UnitsConverter(Unit.parse("km³/s²"), Unit.parse("m³/s²"));

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
