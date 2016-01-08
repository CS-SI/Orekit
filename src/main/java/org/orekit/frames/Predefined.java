/* Copyright 2002-2016 CS Systèmes d'Information
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

    /** Ecliptic framen IERS 1996 conventions. */
    ECLIPTIC_CONVENTIONS_1996("Ecliptic/1996"),

    /** Ecliptic framen IERS 2003 conventions. */
    ECLIPTIC_CONVENTIONS_2003("Ecliptic/2003"),

    /** Ecliptic framen IERS 2010 conventions. */
    ECLIPTIC_CONVENTIONS_2010("Ecliptic/2010"),

    /** EME2000 frame.*/
    EME2000("EME2000"),

    /** CIO-based ITRF, IERS 2010 conventions with simple EOP interpolation.
     * @since 6.1
     */
    ITRF_CIO_CONV_2010_SIMPLE_EOP("CIO/2010-based ITRF simple EOP"),

    /** CIO-based ITRF, IERS 2010 conventions with accurate EOP interpolation.
     * @since 6.1
     */
    ITRF_CIO_CONV_2010_ACCURATE_EOP("CIO/2010-based ITRF accurate EOP"),

    /** CIO-based ITRF, IERS 2003 conventions with simple EOP interpolation.
     * @since 6.1
     */
    ITRF_CIO_CONV_2003_SIMPLE_EOP("CIO/2003-based ITRF simple EOP"),

    /** CIO-based ITRF, IERS 2003 conventions with accurate EOP interpolation.
     * @since 6.1
     */
    ITRF_CIO_CONV_2003_ACCURATE_EOP("CIO/2003-based ITRF accurate EOP"),

    /** CIO-based ITRF, IERS 1996 conventions with simple EOP interpolation.
     * @since 6.1
     */
    ITRF_CIO_CONV_1996_SIMPLE_EOP("CIO/1996-based ITRF simple EOP"),

    /** CIO-based ITRF, IERS 1996 conventions with accurate EOP interpolation.
     * @since 6.1
     */
    ITRF_CIO_CONV_1996_ACCURATE_EOP("CIO/1996-based ITRF accurate EOP"),

    /** Equinox-based ITRF, IERS 2010 conventions with simple EOP interpolation.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_2010_SIMPLE_EOP("Equinox/2010-based ITRF simple EOP"),

    /** Equinox-based ITRF, IERS 2010 conventions with accurate EOP interpolation.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_2010_ACCURATE_EOP("Equinox/2010-based ITRF accurate EOP"),

    /** Equinox-based ITRF, IERS 2003 conventions with simple EOP interpolation.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_2003_SIMPLE_EOP("Equinox/2003-based ITRF simple EOP"),

    /** Equinox-based ITRF, IERS 2003 conventions with accurate EOP interpolation.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_2003_ACCURATE_EOP("Equinox/2003-based ITRF accurate EOP"),

    /** Equinox-based ITRF, IERS 1996 conventions with simple EOP interpolation.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_1996_SIMPLE_EOP("Equinox/1996-based ITRF simple EOP"),

    /** Equinox-based ITRF, IERS 1996 conventions with accurate EOP interpolation.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_1996_ACCURATE_EOP("Equinox/1996-based ITRF accurate EOP"),

    /** TIRF, IERS 2010 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    TIRF_CONVENTIONS_2010_SIMPLE_EOP("TIRF/2010 simple EOP"),

    /** TIRF, IERS 2010 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    TIRF_CONVENTIONS_2010_ACCURATE_EOP("TIRF/2010 accurate EOP"),

    /** TIRF, IERS 2003 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    TIRF_CONVENTIONS_2003_SIMPLE_EOP("TIRF/2003 simple EOP"),

    /** TIRF, IERS 2003 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    TIRF_CONVENTIONS_2003_ACCURATE_EOP("TIRF/2003 accurate EOP"),

    /** TIRF, IERS 1996 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    TIRF_CONVENTIONS_1996_SIMPLE_EOP("TIRF/1996 simple EOP"),

    /** TIRF, IERS 996 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    TIRF_CONVENTIONS_1996_ACCURATE_EOP("TIRF/1996 accurate EOP"),

    /** CIRF frame, IERS 2010 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    CIRF_CONVENTIONS_2010_ACCURATE_EOP("CIRF/2010 accurate EOP"),

    /** CIRF frame, IERS 2010 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    CIRF_CONVENTIONS_2010_SIMPLE_EOP("CIRF/2010 simple EOP"),

    /** CIRF frame, IERS 2003 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    CIRF_CONVENTIONS_2003_ACCURATE_EOP("CIRF/2003 accurate EOP"),

    /** CIRF frame, IERS 2003 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    CIRF_CONVENTIONS_2003_SIMPLE_EOP("CIRF/2003 simple EOP"),

    /** CIRF frame, IERS 1996 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    CIRF_CONVENTIONS_1996_ACCURATE_EOP("CIRF/1996 accurate EOP"),

    /** CIRF frame, IERS 1996 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    CIRF_CONVENTIONS_1996_SIMPLE_EOP("CIRF/1996 simple EOP"),

    /** Veis 1950. */
    VEIS_1950("VEIS1950"),

    /** GTOD, IERS 1996 conventions without EOP corrections.
     */
    GTOD_WITHOUT_EOP_CORRECTIONS("GTOD/1996 without EOP"),

    /** GTOD, IERS 2010 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    GTOD_CONVENTIONS_2010_ACCURATE_EOP("GTOD/2010 accurate EOP"),

    /** GTOD, IERS 2010 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    GTOD_CONVENTIONS_2010_SIMPLE_EOP("GTOD/2010 simple EOP"),

    /** GTOD, IERS 2003 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    GTOD_CONVENTIONS_2003_ACCURATE_EOP("GTOD/2003 accurate EOP"),

    /** GTOD, IERS 2003 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    GTOD_CONVENTIONS_2003_SIMPLE_EOP("GTOD/2003 simple EOP"),

    /** GTOD, IERS 1996 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    GTOD_CONVENTIONS_1996_ACCURATE_EOP("GTOD/1996 accurate EOP"),

    /** GTOD, IERS 1996 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    GTOD_CONVENTIONS_1996_SIMPLE_EOP("GTOD/1996 simple EOP"),

    /** TOD, IERS 1996 conventions without EOP corrections.
     */
    TOD_WITHOUT_EOP_CORRECTIONS("TOD/1996 without EOP"),

    /** TOD, IERS 2010 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    TOD_CONVENTIONS_2010_ACCURATE_EOP("TOD/2010 accurate EOP"),

    /** TOD, IERS 2010 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    TOD_CONVENTIONS_2010_SIMPLE_EOP("TOD/2010 simple EOP"),

    /** TOD, IERS 2003 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    TOD_CONVENTIONS_2003_ACCURATE_EOP("TOD/2003 accurate EOP"),

    /** TOD, IERS 2003 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    TOD_CONVENTIONS_2003_SIMPLE_EOP("TOD/2003 simple EOP"),

    /** TOD, IERS 1996 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    TOD_CONVENTIONS_1996_ACCURATE_EOP("TOD/1996 accurate EOP"),

    /** TOD, IERS 1996 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    TOD_CONVENTIONS_1996_SIMPLE_EOP("TOD/1996 simple EOP"),


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
    Predefined(final String name) {
        this.name = name;
    }

    /** Get the name of the frame.
     * @return name of the frame
     */
    public String getName() {
        return name;
    }

}
