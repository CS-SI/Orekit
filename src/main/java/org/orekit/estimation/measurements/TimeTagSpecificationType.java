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

package org.orekit.estimation.measurements;

/**
 * Enumeration for different time tag specification types, specifying the point at which the
 * measurement epoch is taken from.
 *
 * @author Tommy Fryer
 * @since 11.1
 */
public enum TimeTagSpecificationType {

    /** Time tag at transit/bounce time. */
    TRANSIT("Transit"),

    /** Time tag is at time of signal reception. */
    RX("Receive"),

    /** Time tag is at time of signal transmission. */
    TX("Transmit"),

    /** For Angles measurements where the time tag is at time of signal transmission but the observation is receive apparent. */
    TXRX("Transmit (Receive Apparent)");

    /** Name of time tag specification. */
    private final String name;

    /**
     * Constructor.
     * @param name name of the time tag specification
     */
    TimeTagSpecificationType(final String name) {
        this.name = name;
    }

    /**
     * Get the name of the time tag specification.
     * @return the name
     */
    public String getName() {
        return name;
    }

}

