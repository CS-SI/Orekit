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

    /** Allowed units with SI prefixes, with various aliases for angles, year, sfu, and tecu. */
    private static final Map<String, PrefixedUnit> ALLOWED;

    static {
        final List<Unit> base = Arrays.asList(PredefinedUnit.SECOND.toUnit(),
                                              PredefinedUnit.MINUTE.toUnit(),
                                              PredefinedUnit.HOUR.toUnit(),
                                              PredefinedUnit.DAY.toUnit(),
                                              PredefinedUnit.YEAR.toUnit(),
                                              PredefinedUnit.YEAR.toUnit().alias("y"),
                                              PredefinedUnit.HERTZ.toUnit(),
                                              PredefinedUnit.METRE.toUnit(),
                                              PredefinedUnit.GRAM.toUnit(), // only case were we must use a derived unit
                                              PredefinedUnit.RADIAN.toUnit(),
                                              PredefinedUnit.DEGREE.toUnit(),
                                              PredefinedUnit.DEGREE.toUnit().alias("◦"),
                                              PredefinedUnit.DEGREE.toUnit().alias("deg"),
                                              PredefinedUnit.ARC_MINUTE.toUnit(),
                                              PredefinedUnit.ARC_MINUTE.toUnit().alias("'"),
                                              PredefinedUnit.ARC_SECOND.toUnit(),
                                              PredefinedUnit.ARC_SECOND.toUnit().alias("''"),
                                              PredefinedUnit.ARC_SECOND.toUnit().alias("\""),
                                              PredefinedUnit.ARC_SECOND.toUnit().alias("as"), // must be after second to override atto-seconds
                                              PredefinedUnit.NEWTON.toUnit(),
                                              PredefinedUnit.PASCAL.toUnit(), // must be after year to override peta-years
                                              PredefinedUnit.JOULE.toUnit(),
                                              PredefinedUnit.WATT.toUnit(),
                                              PredefinedUnit.SOLAR_FLUX_UNIT.toUnit(),
                                              PredefinedUnit.SOLAR_FLUX_UNIT.toUnit().alias("SFU"),
                                              PredefinedUnit.TOTAL_ELECTRON_CONTENT_UNIT.toUnit(),
                                              PredefinedUnit.TOTAL_ELECTRON_CONTENT_UNIT.toUnit().alias("tecu"),
                                              PredefinedUnit.PERCENT.toUnit());
        ALLOWED = new HashMap<>(base.size() * Prefix.values().length);
        for (final Unit unit : base) {
            ALLOWED.put(unit.getName(), new PrefixedUnit(null, unit));
            for (final Prefix prefix : Prefix.values()) {
                final PrefixedUnit pu = new PrefixedUnit(prefix, unit);
                ALLOWED.put(pu.getName(), pu);
            }
        }
    }

    /** Simple constructor.
     * @param prefix SI prefix (may be null)
     * @param unit base unit
     */
    PrefixedUnit(final Prefix prefix, final Unit unit) {
        super((prefix == null) ? unit.getName()  : (prefix.getSymbol() + unit.getName()),
              (prefix == null) ? unit.getScale() : (prefix.getFactor() * unit.getScale()),
              unit.getMass(), unit.getLength(), unit.getTime(), unit.getAngle());
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
