/* Copyright 2002-2020 CS GROUP
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
package org.orekit.propagation.numerical.cr3bp;

import java.util.Calendar;

import org.orekit.utils.Constants;

/**
 * Set of useful physical CR3BP constants using JPL data.
 * @author Vincent Mouraux
 */

public interface CR3BPConstants {

    /** Moon semi-major axis = 384 400 000 m. */
    double CENTURY = (Calendar.getInstance().get(Calendar.YEAR) - 2000.0) / 100.0;

    /** Moon semi-major axis in meters. */
    double MOON_SEMI_MAJOR_AXIS = 384400000.0;

    /** Earth-Moon barycenter semi-major axis in meters. */
    double EARTH_MOON_BARYCENTER_SEMI_MAJOR_AXIS = (1.00000261 + 0.00000562 * CENTURY) * Constants.IAU_2012_ASTRONOMICAL_UNIT;

    /** Jupiter semi-major axis in meters. */
    double JUPITER_SEMI_MAJOR_AXIS = (5.20288700 - 0.00011607 * CENTURY) * Constants.IAU_2012_ASTRONOMICAL_UNIT;
}
