/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.frames;


/** Predefined frames provided by {@link FramesFactory}.
 * @author Luc Maisonobe
 */
public enum Predefined {

    /** GCRF frame.*/
    GCRF(Frame.getRoot().getName()),

    /** ICRF frame.*/
    ICRF("ICRF"),

    /** EME2000 frame.*/
    EME2000("EME2000"),

    /** ITRF2008 without tidal effects. */
    ITRF_2008_WITHOUT_TIDAL_EFFECTS("ITRF2008 without tides"),

    /** ITRF2008 with tidal effects. */
    ITRF_2008_WITH_TIDAL_EFFECTS("ITRF2008 with tides"),

    /** ITRF2005 without tidal effects. */
    ITRF_2005_WITHOUT_TIDAL_EFFECTS("ITRF2005 without tides"),

    /** ITRF2005 with tidal effects. */
    ITRF_2005_WITH_TIDAL_EFFECTS("ITRF2005 with tides"),

    /** ITRF2000 without tidal effects. */
    ITRF_2000_WITHOUT_TIDAL_EFFECTS("ITRF2000 without tides"),

    /** ITRF2000 with tidal effects. */
    ITRF_2000_WITH_TIDAL_EFFECTS("ITRF2000 with tides"),

    /** ITRF97 without tidal effects. */
    ITRF_97_WITHOUT_TIDAL_EFFECTS("ITRF97 without tides"),

    /** ITRF97 with tidal effects. */
    ITRF_97_WITH_TIDAL_EFFECTS("ITRF97 with tides"),

    /** ITRF93 without tidal effects. */
    ITRF_93_WITHOUT_TIDAL_EFFECTS("ITRF93 without tides"),

    /** ITRF93 with tidal effects. */
    ITRF_93_WITH_TIDAL_EFFECTS("ITRF93 with tides"),

    /** Equinox-based ITRF, IERS 2010 conventions without tidal effects.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_2010_WITHOUT_TIDAL_EFFECTS("Equinox/2010-based ITRF without tides"),

    /** Equinox-based ITRF, IERS 2010 conventions with tidal effects.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_2010_WITH_TIDAL_EFFECTS("Equinox/2010-based ITRF with tides"),

    /** Equinox-based ITRF, IERS 2003 conventions without tidal effects.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_2003_WITHOUT_TIDAL_EFFECTS("Equinox/2003-based ITRF without tides"),

    /** Equinox-based ITRF, IERS 2003 conventions with tidal effects.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_2003_WITH_TIDAL_EFFECTS("Equinox/2003-based ITRF with tides"),

    /** Equinox-based ITRF.
     * @deprecated as of 6.1 replaced with {@link #ITRF_EQUINOX_CONV_1996_WITHOUT_TIDAL_EFFECTS}
     */
    @Deprecated
    ITRF_EQUINOX("Equinox-based ITRF"),

    /** Equinox-based ITRF, IERS 1996 conventions without tidal effects.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_1996_WITHOUT_TIDAL_EFFECTS("Equinox/1996-based ITRF without tides"),

    /** Equinox-based ITRF, IERS 1996 conventions with tidal effects.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_1996_WITH_TIDAL_EFFECTS("Equinox/1996-based ITRF with tides"),

    /** TIRF2000, IERS 2010 conventions, without tidal effects.
     * @deprecated as of 6.1 replaced with {@link #TIRF_CONVENTIONS_2010_WITHOUT_TIDAL_EFFECTS}
     */
    @Deprecated
    TIRF_2000_CONV_2010_WITHOUT_TIDAL_EFFECTS("TIRF2000/2010 without tides"),

    /** TIRF, IERS 2010 conventions, without tidal effects.
     * @since 6.1
     */
    TIRF_CONVENTIONS_2010_WITHOUT_TIDAL_EFFECTS("TIRF/2010 without tides"),

    /** TIRF2000, IERS 2010 conventions, with tidal effects.
     * @deprecated as of 6.1 replaced with {@link #TIRF_CONVENTIONS_2010_WITH_TIDAL_EFFECTS}
     */
    @Deprecated
    TIRF_2000_CONV_2010_WITH_TIDAL_EFFECTS("TIRF2000/2010 with tides"),

    /** TIRF, IERS 2010 conventions, with tidal effects.
     * @since 6.1
     */
    TIRF_CONVENTIONS_2010_WITH_TIDAL_EFFECTS("TIRF/2010 with tides"),

    /** TIRF2000, IERS 2003 conventions, without tidal effects.
     * @deprecated as of 6.1 replaced with {@link #TIRF_CONVENTIONS_2003_WITHOUT_TIDAL_EFFECTS}
     */
    @Deprecated
    TIRF_2000_CONV_2003_WITHOUT_TIDAL_EFFECTS("TIRF2000/2003 without tides"),

    /** TIRF, IERS 2003 conventions, without tidal effects.
     * @since 6.1
     */
    TIRF_CONVENTIONS_2003_WITHOUT_TIDAL_EFFECTS("TIRF/2003 without tides"),

    /** TIRF2000, IERS 2003 conventions, with tidal effects.
     * @deprecated as of 6.1 replaced with {@link #TIRF_CONVENTIONS_2003_WITH_TIDAL_EFFECTS}
     */
    @Deprecated
    TIRF_2000_CONV_2003_WITH_TIDAL_EFFECTS("TIRF2000/2003 with tides"),

    /** TIRF, IERS 2003 conventions, with tidal effects.
     * @since 6.1
     */
    TIRF_CONVENTIONS_2003_WITH_TIDAL_EFFECTS("TIRF/2003 with tides"),

    /** TIRF, IERS 1996 conventions, without tidal effects.
     * @since 6.1
     */
    TIRF_CONVENTIONS_1996_WITHOUT_TIDAL_EFFECTS("TIRF/1996 without tides"),

    /** TIRF, IERS 996 conventions, with tidal effects.
     * @since 6.1
     */
    TIRF_CONVENTIONS_1996_WITH_TIDAL_EFFECTS("TIRF/1996 with tides"),

    /** CIRF2000 frame, IERS 2010 conventions.
     * @deprecated as of 6.1 replaced with {@link #CIRF_CONVENTIONS_2010}
     */
    @Deprecated
    CIRF_2000_CONV_2010("CIRF2000/2010"),

    /** CIRF frame, IERS 2010 conventions.
     * @since 6.1
     */
    CIRF_CONVENTIONS_2010("CIRF/2010"),

    /** CIRF2000 frame, IERS 2003 conventions.
     * @deprecated as of 6.1 replaced with {@link #CIRF_CONVENTIONS_2003}
     */
    @Deprecated
    CIRF_2000_CONV_2003("CIRF2000/2003"),

    /** CIRF frame, IERS 2003 conventions.
     * @since 6.1
     */
    CIRF_CONVENTIONS_2003("CIRF/2003"),

    /** CIRF frame, IERS 1996 conventions.
     * @since 6.1
     */
    CIRF_CONVENTIONS_1996("CIRF/1996"),

    /** Veis 1950. */
    VEIS_1950("VEIS1950"),

    /** GTOD, IERS 1996 conventions without EOP corrections.
     */
    GTOD_WITHOUT_EOP_CORRECTIONS("GTOD/1996 without EOP"),

    /** GTOD, IERS 2010 conventions.
     * @since 6.1
     */
    GTOD_CONVENTIONS_2010("GTOD/2010"),

    /** GTOD, IERS 2003 conventions.
     * @since 6.1
     */
    GTOD_CONVENTIONS_2003("GTOD/2003"),

    /** GTOD, IERS 1996 conventions with EOP corrections.
     * @deprecated as of 6.1 replaced with {@link #GTOD_CONVENTIONS_1996}
     */
    @Deprecated
    GTOD_WITH_EOP_CORRECTIONS("GTOD with EOP"),

    /** GTOD, IERS 1996 conventions with EOP corrections.
     * @since 6.1
     */
    GTOD_CONVENTIONS_1996("GTOD/1996"),

    /** TOD, IERS 1996 conventions without EOP corrections.
     */
    TOD_WITHOUT_EOP_CORRECTIONS("TOD/1996 without EOP"),

    /** TOD, IERS 2010 conventions.
     * @since 6.1
     */
    TOD_CONVENTIONS_2010("TOD/2010"),

    /** TOD, IERS 2003 conventions.
     * @since 6.1
     */
    TOD_CONVENTIONS_2003("TOD/2003"),

    /** TOD, IERS 1996 conventions with EOP corrections.
     * @deprecated as of 6.1 replaced with {@link #TOD_CONVENTIONS_1996}
     */
    @Deprecated
    TOD_WITH_EOP_CORRECTIONS("TOD with EOP"),

    /** TOD, IERS 1996 conventions.
     * @since 6.1
     */
    TOD_CONVENTIONS_1996("TOD/1996"),

    /** MOD, IERS 1996 conventions without EOP corrections.
     */
    MOD_WITHOUT_EOP_CORRECTIONS("MOD/1996 without EOP"),

    /** MOD, IERS 2010 conventions.
     * @since 6.1
     */
    MOD_CONVENTIONS_2010("MOD/2010"),

    /** MOD, IERS 2003 conventions.
     * @since 6.1
     */
    MOD_CONVENTIONS_2003("MOD/2003"),

    /** MOD, IERS 1996 conventions with EOP corrections.
     * @deprecated as of 6.1 replaced with {@link #MOD_CONVENTIONS_1996}
     */
    @Deprecated
    MOD_WITH_EOP_CORRECTIONS("MOD with EOP"),

    /** MOD, IERS 1996 conventions.
     * @since 6.1
     */
    MOD_CONVENTIONS_1996("MOD/1996"),

    /** TEME frame. */
    TEME("TEME");

    /** Name fo the frame. */
    private final String name;

    /** Simple constructor.
     * @param name name of the frame
     */
    private Predefined(final String name) {
        this.name = name;
    }

    /** Get the name of the frame.
     * @return name of the frame
     */
    public String getName() {
        return name;
    }

}
