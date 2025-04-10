/* Copyright 2002-2025 CS GROUP
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

package org.orekit.files.ccsds.ndm.odm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.orekit.files.ccsds.section.CommentsContainer;

/** Container for user defined data.
 * <p>
 * Beware that the Orekit getters and setters all rely on SI units. The parsers
 * and writers take care of converting these SI units into CCSDS mandatory units.
 * The {@link org.orekit.utils.units.Unit Unit} class provides useful
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} methods in case the callers
 * already use CCSDS units instead of the API SI units. The general-purpose
 * {@link org.orekit.utils.units.Unit Unit} class (without an 's') and the
 * CCSDS-specific {@link org.orekit.files.ccsds.definitions.Units Units} class
 * (with an 's') also provide some predefined units. These predefined units and the
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} conversion methods are indeed
 * what the parsers and writers use for the conversions.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class UserDefined extends CommentsContainer {

    /** Tag name for user defined parameters keys. */
    public static final String USER_DEFINED_XML_TAG = "USER_DEFINED";

    /** Attribute name for user defined parameters keys. */
    public static final String USER_DEFINED_XML_ATTRIBUTE = "parameter";

    /** Prefix for user defined parameters keys. */
    public static final String USER_DEFINED_PREFIX = "USER_DEFINED_";

    /** User defined parameters map. */
    private final Map<String, String> map;

    /** Create an empty data set.
     */
    public UserDefined() {
        // we use a LinkedHashMap so we retrieve the parameters in the same order they are put in
        map = new LinkedHashMap<>();
    }

    /** Get all user defined parameters.
     * <p>
     * The {@link #USER_DEFINED_PREFIX} has been stripped away from the keys.
     * </p>
     * @return unmodifiable view of the map containing all user defined parameters
     */
    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(map);
    }

    /** Add a key/value entry.
     * @param key parameter key, with the {@link #USER_DEFINED_PREFIX} stripped away
     * @param value parameter value
     */
    public void addEntry(final String key, final String value) {
        refuseFurtherComments();
        map.put(key, value);
    }

}

