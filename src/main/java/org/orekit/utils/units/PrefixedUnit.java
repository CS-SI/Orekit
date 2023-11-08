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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** {@link Unit} with a {@link Prefix}.
 * @author Luc Maisonobe
 * @since 11.0
 */
class PrefixedUnit extends Unit {

    /** Serializable UID. */
    private static final long serialVersionUID = 20210407L;

    /** Allowed units with SI prefixes, with various aliases for angles, year, sfu, and tecu. */
    private static final Map<String, PrefixedUnit> ALLOWED;

    static {
        final List<Unit> base = Arrays.asList(Unit.SECOND,
                                              Unit.MINUTE,
                                              Unit.HOUR,
                                              Unit.DAY,
                                              Unit.DAY.alias("day"),
                                              Unit.YEAR,
                                              Unit.YEAR.alias("yr"),
                                              Unit.HERTZ,
                                              Unit.METRE,
                                              Unit.GRAM, // only case were we must use a derived unit
                                              Unit.AMPERE,
                                              Unit.RADIAN,
                                              Unit.DEGREE,
                                              Unit.DEGREE.alias("◦"),
                                              Unit.DEGREE.alias("deg"),
                                              Unit.ARC_MINUTE,
                                              Unit.ARC_MINUTE.alias("'"),
                                              Unit.ARC_SECOND,
                                              Unit.ARC_SECOND.alias("''"),
                                              Unit.ARC_SECOND.alias("\""),
                                              Unit.ARC_SECOND.alias("as"), // must be after second to override atto-seconds
                                              Unit.REVOLUTION,
                                              Unit.NEWTON,
                                              Unit.PASCAL, // must be after year to override peta-years
                                              Unit.BAR,
                                              Unit.JOULE,
                                              Unit.WATT,
                                              Unit.COULOMB,
                                              Unit.VOLT,
                                              Unit.OHM,
                                              Unit.TESLA,
                                              Unit.SOLAR_FLUX_UNIT,
                                              Unit.SOLAR_FLUX_UNIT.alias("SFU"),
                                              Unit.SOLAR_FLUX_UNIT.alias("sfu"),
                                              Unit.TOTAL_ELECTRON_CONTENT_UNIT,
                                              Unit.TOTAL_ELECTRON_CONTENT_UNIT.alias("tecu"),
                                              Unit.EARTH_RADII);
        ALLOWED = new HashMap<>(base.size() * Prefix.values().length);
        for (final Unit unit : base) {
            ALLOWED.put(unit.getName(), new PrefixedUnit(null, unit));
            for (final Prefix prefix : Prefix.values()) {
                final PrefixedUnit pu = new PrefixedUnit(prefix, unit);
                ALLOWED.put(pu.getName(), pu);
            }
        }

        // units that don't accept any prefix
        for (final Unit noPrefix : Arrays.asList(Unit.PERCENT, Unit.ONE, Unit.ONE.alias("#"))) {
            ALLOWED.put(noPrefix.getName(), new PrefixedUnit(null, noPrefix));
        }

    }

    /** Simple constructor.
     * @param prefix SI prefix (may be null)
     * @param unit base unit
     */
    PrefixedUnit(final Prefix prefix, final Unit unit) {
        super((prefix == null) ? unit.getName()  : (prefix.getSymbol() + unit.getName()),
              (prefix == null) ? unit.getScale() : (prefix.getFactor() * unit.getScale()),
              unit.getMass(), unit.getLength(), unit.getTime(), unit.getCurrent(), unit.getAngle());
    }

    /** Get one of the allowed prefixed unit.
     * @param name name of the prefixed unit
     * @return prefixed unit with that name
     */
    public static PrefixedUnit valueOf(final String name) {
        final PrefixedUnit prefixedUnit = ALLOWED.get(name);
        if (prefixedUnit == null) {
            throw new OrekitException(OrekitMessages.UNKNOWN_UNIT, name);
        }
        return prefixedUnit;
    }

}
