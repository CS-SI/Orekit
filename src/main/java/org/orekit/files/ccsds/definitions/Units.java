/* Copyright 2002-2022 CS GROUP
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
    public static final Unit ONE_PER_S = Unit.parse("1/s");

    /** kg.m² unit. */
    public static final Unit KG_M2 = Unit.parse("kg.m²");

    /** km³/s² unit. */
    public static final Unit KM3_PER_S2 = Unit.parse("km³/s²");

    /** m² unit. */
    public static final Unit M2 = Unit.parse("m²");

    /** #/year unit. */
    public static final Unit NB_PER_Y = Unit.parse("#/yr");

    /** Square kilometers units. */
    public static final Unit KM2 = Unit.parse("km²");

    /** Kilometers par second units. */
    public static final Unit KM_PER_S = Unit.parse("km/s");

    /** Kilometers par square second units. */
    public static final Unit KM_PER_S2 = Unit.parse("km/s²");

    /** Square kilometers par second units. */
    public static final Unit KM2_PER_S = Unit.parse("km²/s");

    /** Square kilometers per square second units. */
    public static final Unit KM2_PER_S2 = Unit.parse("km²/s²");

    /** Revolutions per day unit. */
    public static final Unit REV_PER_DAY = Unit.parse("rev/d");

    /** Scaled revolutions per square day unit. */
    public static final Unit REV_PER_DAY2_SCALED = Unit.parse("2rev/d²");

    /** Scaled revolutions per cubic day divieded by 6 unit. */
    public static final Unit REV_PER_DAY3_SCALED = Unit.parse("6rev/d³");

    /** Degree per second unit. */
    public static final Unit DEG_PER_S = Unit.parse("°/s");

    /** Newton metre unit. */
    public static final Unit N_M = Unit.parse("N.m");

    /** Nano Tesla unit. */
    public static final Unit NANO_TESLA = Unit.parse("nT");

    /** HectoPascal unit. */
    public static final Unit HECTO_PASCAL = Unit.parse("hPa");

    /** Hertz per second unit. */
    public static final Unit HZ_PER_S = Unit.parse("Hz/s");

    /** Private constructor for a utility class.
     */
    private Units() {
        // nothing to do
    }

}
