/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

    /** Equinox-based ITRF. */
    ITRF_EQUINOX("Equinox-based ITRF"),

    /** TIRF2000 without tidal effects. */
    TIRF_2000_WITHOUT_TIDAL_EFFECTS("TIRF2000 without tides"),

    /** TIRF2000 with tidal effects. */
    TIRF_2000_WITH_TIDAL_EFFECTS("TIRF2000 with tides"),

    /** CIRF2000 frame. */
    CIRF_2000("CIRF2000"),

    /** Veis 1950 with tidal effects. */
    VEIS_1950("VEIS1950"),

    /** GTOD without EOP corrections. */
    GTOD_WITHOUT_EOP_CORRECTIONS("GTOD without EOP"),

    /** GTOD with EOP corrections. */
    GTOD_WITH_EOP_CORRECTIONS("GTOD with EOP"),

    /** TOD without EOP corrections. */
    TOD_WITHOUT_EOP_CORRECTIONS("TOD without EOP"),

    /** TOD with EOP corrections. */
    TOD_WITH_EOP_CORRECTIONS("TOD with EOP"),

    /** MOD without EOP corrections. */
    MOD_WITHOUT_EOP_CORRECTIONS("MOD without EOP"),

    /** MOD with EOP corrections. */
    MOD_WITH_EOP_CORRECTIONS("MOD with EOP"),

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
