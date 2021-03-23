/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.definitions;

import org.orekit.utils.units.Unit;

/**
 * Units used in CCSDS messages.
 *
 * @author Luc Maisonobe
 */
public class Units {

    /** Seconds reciprocal unit. */
    public static final Unit ONE_PER_S = Unit.parse("s⁻¹");

    /** kg/m² unit. */
    public static final Unit KG_PER_M2 = Unit.parse("kg/m²");

    /** km³/s² unit. */
    public static final Unit KM3_PER_S2 = Unit.parse("km³/s²");

    /** m² unit. */
    public static final Unit M2 = Unit.parse("m²");

    /** #/yr unit. */
    public static final Unit NB_PER_Y = Unit.parse("y⁻¹");

    /** Square kilometers units. */
    public static final Unit KM2 = Unit.parse("km²");

    /** Kilometers par second units. */
    public static final Unit KM_PER_S = Unit.parse("km/s");

    /** Square kilometers par second units. */
    public static final Unit KM2_PER_S = Unit.parse("km²/s");

    /** Square kilometers per square second units. */
    public static final Unit KM2_PER_S2 = Unit.parse("km²/s²");

    /** Revolutions per day unit. */
    public static final Unit REV_PER_DAY = Unit.parse("rev/d");

    /** Scaled revolutions per square day unit. */
    public static final Unit REV_PER_DAY2_SCALED = Unit.parse("rev/d²").scale("2rev/d²", 2.0);

    /** Scaled revolutions per cubic day divieded by 6 unit. */
    public static final Unit REV_PER_DAY3_SCALED = Unit.parse("rev/d³").scale("6rev/d³", 6.0);

    /** Degree per second unit. */
    public static final Unit DEG_PER_S = Unit.parse("°/s");

    /** Private constructor for a utility class.
     */
    private Units() {
        // nothing to do
    }

}
