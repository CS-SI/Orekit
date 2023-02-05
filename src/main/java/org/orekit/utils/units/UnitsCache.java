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

import java.util.HashMap;
import java.util.Map;

/** Cache for parsed units.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class UnitsCache {

    /** Parsed units cache. */
    private final Map<String, Unit> cache;

    /** Simple constructor.
     */
    public UnitsCache() {
        this.cache = new HashMap<>();
    }

    /** Get units from a string specification.
     * <p>
     * Parsing is performed only the first time a specification is
     * encountered, so the cache speeds up cases where the same
     * units is encountered many times (for example when parsing
     * CCSDS messages with many entries).
     * </p>
     * @param specification units specification (may be null)
     * @return parsed units ({@link Unit#NONE} if specification is null)
     */
    public Unit getUnits(final String specification) {

        if (specification == null) {
            return Unit.NONE;
        }

        Unit cached = cache.get(specification);
        if (cached == null) {
            cached = Unit.parse(specification);
            cache.put(specification, cached);
        }

        return cached;

    }

}
