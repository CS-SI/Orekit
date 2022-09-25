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
package org.orekit.files.ccsds.ndm.cdm;

/** Maneuvrable possibilities used in CCSDS Conjunction Data Messages.
 * @author Melina Vanel
 * @since 11.2
 */
public enum Maneuvrable {

    /** Maneuvrable. */
    YES("YES"),

    /** Non Maneuvrable. */
    NO("NO"),

    /** Don't know or not applicable. */
    N_A("N/A");

    /** Value of the enum .*/
    private String value;

    Maneuvrable(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }

    public static Maneuvrable getEnum(final String keyValue) {
        for (Maneuvrable v : values()) {
            if (v.getValue().equalsIgnoreCase(keyValue)) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

}
