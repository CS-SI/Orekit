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
package org.orekit.propagation.numerical.cr3bp;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;

/**
 * Set of useful physical CR3BP constants using JPL data.
 * @author Vincent Mouraux
 * @since 11.0
 */
public class CR3BPConstants {

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private CR3BPConstants() {
    }

    /**
     * Get the Moon semi-major axis.
     * @return the Moon semi-major axis in meters
     */
    public static double getMoonSemiMajorAxis() {
        return 384400000.0;
    }

    /**
     * Get the Earth-Moon barycenter semi-major axis.
     * @param date date
     * @param timeScale time scale
     * @return the Earth-Moon barycenter semi-major axis in meters
     */
    public static double getEarthMoonBarycenterSemiMajorAxis(final AbsoluteDate date,
                                                             final TimeScale timeScale) {
        // Century
        final double century = getCentury(date, timeScale);
        // Return the Earth - Moon barycenter semi-major axis
        return  (1.00000261 + 0.00000562 * century) * Constants.IAU_2012_ASTRONOMICAL_UNIT;
    }

    /**
     * Get the Jupiter semi-major axis.
     * @param date date
     * @param timeScale time scale
     * @return the Jupiter semi-major axis in meters
     */
    public static double getJupiterSemiMajorAxis(final AbsoluteDate date,
                                                 final TimeScale timeScale) {
        // Century
        final double century = getCentury(date, timeScale);
        // Return the Jupiter semi-major axis
        return (5.20288700 - 0.00011607 * century) * Constants.IAU_2012_ASTRONOMICAL_UNIT;
    }

    /**
     * Get the current century as a floating value.
     * @param date date
     * @param timeScale time scale
     * @return the current century as a floating value
     */
    private static double getCentury(final AbsoluteDate date,
                                     final TimeScale timeScale) {
        // Get the date component
        final DateComponents dc = date.getComponents(timeScale).getDate();
        // Return the current century as a floating value
        return 0.01 * (dc.getYear() - 2000.0);
    }

}
