/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.files.sp3;

import java.util.Locale;

/** Orbit type indicator.
 * @author Thomas Neidhart
 * @author Evan Ward
 * @author Luc Maisonobe
 */
public enum SP3OrbitType {

    /** fitted. */
    FIT,

    /** extrapolated or predicted. */
    EXT,

    /** broadcast. */
    BCT,

    /** fitted after applying a Helmert transformation. */
    HLM,

    /** other type, defined by SP3 file producing agency.
     * @since 9.3
     */
    OTHER;

    /** Parse a string to get the type.
     * @param s string to parse
     * @return the type corresponding to the string
     */
    public static SP3OrbitType parseType(final String s) {
        final String normalizedString = s.trim().toUpperCase(Locale.US);
        if ("EST".equals(normalizedString)) {
            return FIT;
        } else if ("BHN".equals(normalizedString)) {
            // ESOC navigation team uses BHN for files produced
            // by their main parameter estimation program Bahn
            return FIT;
        } else if ("PRO".equals(normalizedString)) {
            // ESOC navigation team uses PRO for files produced
            // by their orbit propagation program Propag
            return EXT;
        } else {
            try {
                return valueOf(normalizedString);
            } catch (IllegalArgumentException iae) {
                return OTHER;
            }
        }
    }

}
