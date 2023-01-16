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

package org.orekit.files.ccsds.ndm.odm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.orekit.files.ccsds.section.CommentsContainer;

/** Container for user defined data.
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

