package org.orekit.estimation.measurements;/* Copyright 2002-2022 CS GROUP
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

/**
 * Enumerate for combination of measurements types.
 *
 * @author Tommy Fryer
 * @since 10.1
 */
public enum TimeTagSpecificationType {

    /** Time tag specification at transit/bounce time. */
    TRANSIT("Transit"),

    RX("Receive"),

    TX("Transmit");

    private final String name;

    /**
     * Constructor.
     * @param name name of the combination of measurements
     */
    TimeTagSpecificationType(final String name) {
        this.name = name;
    }

    /**
     * Get the name of the combination of measurements.
     * @return the name
     */
    public String getName() {
        return name;
    }

}

