/* Copyright 2002-2014 CS Systèmes d'Information
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

    /** ITRF2008 with simple EOP interpolation.
     * @deprecated as of 6.1, replaced with {@link #ITRF_CIO_CONV_2010_SIMPLE_EOP}
     */
    @Deprecated
    ITRF_2008_WITHOUT_TIDAL_EFFECTS("ITRF2008 accurate EOP"),

    /** ITRF2008 with accurate EOP interpolation.
     * @deprecated as of 6.1, replaced with {@link #ITRF_CIO_CONV_2010_ACCURATE_EOP}
     */
    @Deprecated
    ITRF_2008_WITH_TIDAL_EFFECTS("ITRF2008 simple EOP"),

    /** ITRF2005 with simple EOP interpolation.
     * @deprecated as of 6.1, replaced with {@link #ITRF_CIO_CONV_2010_SIMPLE_EOP}
     * and {@link HelmertTransformation.Predefined#ITRF_2008_TO_ITRF_2005}
     */
    @Deprecated
    ITRF_2005_WITHOUT_TIDAL_EFFECTS("ITRF2005 accurate EOP"),

    /** ITRF2005 with accurate EOP interpolation.
     * @deprecated as of 6.1, replaced with {@link #ITRF_CIO_CONV_2010_ACCURATE_EOP}
     * and {@link HelmertTransformation.Predefined#ITRF_2008_TO_ITRF_2005}
     */
    @Deprecated
    ITRF_2005_WITH_TIDAL_EFFECTS("ITRF2005 simple EOP"),

    /** ITRF2000 with simple EOP interpolation.
     * @deprecated as of 6.1, replaced with {@link #ITRF_CIO_CONV_2010_SIMPLE_EOP}
     * and {@link HelmertTransformation.Predefined#ITRF_2008_TO_ITRF_2000}
     */
    @Deprecated
    ITRF_2000_WITHOUT_TIDAL_EFFECTS("ITRF2000 accurate EOP"),

    /** ITRF2000 with accurate EOP interpolation.
     * @deprecated as of 6.1, replaced with {@link #ITRF_CIO_CONV_2010_ACCURATE_EOP}
     * and {@link HelmertTransformation.Predefined#ITRF_2008_TO_ITRF_2000}
     */
    @Deprecated
    ITRF_2000_WITH_TIDAL_EFFECTS("ITRF2000 simple EOP"),

    /** ITRF97 with simple EOP interpolation.
     * @deprecated as of 6.1, replaced with {@link #ITRF_CIO_CONV_2010_SIMPLE_EOP}
     * and {@link HelmertTransformation.Predefined#ITRF_2008_TO_ITRF_97}
     */
    @Deprecated
    ITRF_97_WITHOUT_TIDAL_EFFECTS("ITRF97 accurate EOP"),

    /** ITRF97 with accurate EOP interpolation.
     * @deprecated as of 6.1, replaced with {@link #ITRF_CIO_CONV_2010_ACCURATE_EOP}
     * and {@link HelmertTransformation.Predefined#ITRF_2008_TO_ITRF_97}
     */
    @Deprecated
    ITRF_97_WITH_TIDAL_EFFECTS("ITRF97 simple EOP"),

    /** ITRF93 with simple EOP interpolation.
     * @deprecated as of 6.1, replaced with {@link #ITRF_CIO_CONV_2010_SIMPLE_EOP}
     * and {@link HelmertTransformation.Predefined#ITRF_2008_TO_ITRF_93}
     */
    @Deprecated
    ITRF_93_WITHOUT_TIDAL_EFFECTS("ITRF93 accurate EOP"),

    /** ITRF93 with accurate EOP interpolation.
     * @deprecated as of 6.1, replaced with {@link #ITRF_CIO_CONV_2010_ACCURATE_EOP}
     * and {@link HelmertTransformation.Predefined#ITRF_2008_TO_ITRF_93}
     */
    @Deprecated
    ITRF_93_WITH_TIDAL_EFFECTS("ITRF93 simple EOP"),

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

    /** Equinox-based ITRF.
     * @deprecated as of 6.1 replaced with {@link #ITRF_EQUINOX_CONV_1996_SIMPLE_EOP}
     */
    @Deprecated
    ITRF_EQUINOX("Equinox-based ITRF"),

    /** Equinox-based ITRF, IERS 1996 conventions with simple EOP interpolation.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_1996_SIMPLE_EOP("Equinox/1996-based ITRF simple EOP"),

    /** Equinox-based ITRF, IERS 1996 conventions with accurate EOP interpolation.
     * @since 6.1
     */
    ITRF_EQUINOX_CONV_1996_ACCURATE_EOP("Equinox/1996-based ITRF accurate EOP"),

    /** TIRF2000, IERS 2010 conventions, with simple EOP interpolation.
     * @deprecated as of 6.1 replaced with {@link #TIRF_CONVENTIONS_2010_SIMPLE_EOP}
     */
    @Deprecated
    TIRF_2000_CONV_2010_WITHOUT_TIDAL_EFFECTS("TIRF2000/2010 accurate EOP"),

    /** TIRF, IERS 2010 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    TIRF_CONVENTIONS_2010_SIMPLE_EOP("TIRF/2010 simple EOP"),

    /** TIRF2000, IERS 2010 conventions, with accurate EOP interpolation.
     * @deprecated as of 6.1 replaced with {@link #TIRF_CONVENTIONS_2010_ACCURATE_EOP}
     */
    @Deprecated
    TIRF_2000_CONV_2010_WITH_TIDAL_EFFECTS("TIRF2000/2010 simple EOP"),

    /** TIRF, IERS 2010 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    TIRF_CONVENTIONS_2010_ACCURATE_EOP("TIRF/2010 accurate EOP"),

    /** TIRF2000, IERS 2003 conventions, with simple EOP interpolation.
     * @deprecated as of 6.1 replaced with {@link #TIRF_CONVENTIONS_2003_SIMPLE_EOP}
     */
    @Deprecated
    TIRF_2000_CONV_2003_WITHOUT_TIDAL_EFFECTS("TIRF2000/2003 accurate EOP"),

    /** TIRF, IERS 2003 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    TIRF_CONVENTIONS_2003_SIMPLE_EOP("TIRF/2003 simple EOP"),

    /** TIRF2000, IERS 2003 conventions, with accurate EOP interpolation.
     * @deprecated as of 6.1 replaced with {@link #TIRF_CONVENTIONS_2003_ACCURATE_EOP}
     */
    @Deprecated
    TIRF_2000_CONV_2003_WITH_TIDAL_EFFECTS("TIRF2000/2003 simple EOP"),

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

    /** CIRF2000 frame, IERS 2010 conventions.
     * @deprecated as of 6.1 replaced with {@link #CIRF_CONVENTIONS_2010_ACCURATE_EOP}
     */
    @Deprecated
    CIRF_2000_CONV_2010("CIRF2000/2010"),

    /** CIRF frame, IERS 2010 conventions, with accurate EOP interpolation.
     * @since 6.1
     */
    CIRF_CONVENTIONS_2010_ACCURATE_EOP("CIRF/2010 accurate EOP"),

    /** CIRF frame, IERS 2010 conventions, with simple EOP interpolation.
     * @since 6.1
     */
    CIRF_CONVENTIONS_2010_SIMPLE_EOP("CIRF/2010 simple EOP"),

    /** CIRF2000 frame, IERS 2003 conventions.
     * @deprecated as of 6.1 replaced with {@link #CIRF_CONVENTIONS_2003_ACCURATE_EOP}
     */
    @Deprecated
    CIRF_2000_CONV_2003("CIRF2000/2003"),

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

    /** GTOD, IERS 1996 conventions with EOP corrections.
     * @deprecated as of 6.1 replaced with {@link #GTOD_CONVENTIONS_1996_ACCURATE_EOP}
     */
    @Deprecated
    GTOD_WITH_EOP_CORRECTIONS("GTOD with EOP"),

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

    /** TOD, IERS 1996 conventions with EOP corrections.
     * @deprecated as of 6.1 replaced with {@link #TOD_CONVENTIONS_1996_ACCURATE_EOP}
     */
    @Deprecated
    TOD_WITH_EOP_CORRECTIONS("TOD with EOP"),

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
    TEME("TEME"),

    /** B1950 frame. */
    B1950("B1950");

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
