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
package org.orekit.gnss.metric.parser;

import org.orekit.propagation.analytical.gnss.data.GNSSConstants;
import org.orekit.utils.units.Unit;

/**
 * Units used in RTCM and IGS SSR messages.
 *
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class Units {

    /** Semi-circles units. */
    public static final Unit SEMI_CIRCLE = Unit.RADIAN.scale("sc", GNSSConstants.GNSS_PI);

    /** Nanoseconds units. */
    public static final Unit NS = Unit.parse("ns");

    /** Millimetres units. */
    public static final Unit MM = Unit.parse("mm");

    /** Millimetres per second units. */
    public static final Unit MM_PER_S = Unit.parse("mm/s");

    /** Millimetres per square second units. */
    public static final Unit MM_PER_S2 = Unit.parse("mm/s²");

    /** Kilometers par second units. */
    public static final Unit KM_PER_S = Unit.parse("km/s");

    /** Kilometers par square second units. */
    public static final Unit KM_PER_S2 = Unit.parse("km/s²");

    /** Private constructor for a utility class. */
    private Units() {
        // Nothing to do
    }

}
