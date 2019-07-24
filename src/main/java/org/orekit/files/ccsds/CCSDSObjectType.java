/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.files.ccsds;

/** Object type used in CCSDS {@link OCMFile Orbit Comprehensive Messages}.
 * @author Luc Maisonobe
 * @since 10.1
 */
public enum CCSDSObjectType {

    /** Payload. */
    PL("Payload"),

    /** Payload mission-related. */
    PM("Payload mission-related"),

    /** Payload fragmentation debris. */
    PF("Payload fragmentation debris"),

    /** Payload debris. */
    PD("Payload debris"),

    /** Rocket body. */
    RB("Rocket body"),

    /** Rocket mission-related. */
    RM("Rocket mission-related"),

    /** Rocket fragmentation debris. */
    RF("Rocket fragmentation debris"),

    /** Rocket debris. */
    RD("Rocket debris"),

    /** Unidentified. */
    UI("Unidentified");

    /** Description. */
    private final String description;

    /** Simple constructor.
     * @param description description
     */
    CCSDSObjectType(final String description) {
        this.description = description;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return description;
    }

}
