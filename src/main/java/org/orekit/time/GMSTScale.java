/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.time;

import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;

/** Greenwich Mean Sidereal Time.
 * <p>The Greenwich Mean Sidereal Time is the hour angle between the meridian of Greenwich
 * and mean equinox of date at 0h UT1.</p>
 * <p>This is intended to be accessed thanks to the {@link TimeScalesFactory} class,
 * so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @since 5.1
 */
public class GMSTScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131209L;

    /** Duration of one julian day. */
    private static double FULL_DAY = Constants.JULIAN_DAY;

    /** Duration of an half julian day. */
    private static double HALF_DAY = Constants.JULIAN_DAY / 2.0;

    /** Coefficient for degree 0. */
    private static double C0 = 24110.54841;

    /** Coefficient for degree 1. */
    private static double C1 = 8640184.812866;

    /** Coefficient for degree 2. */
    private static double C2 = 0.093104;

    /** Coefficient for degree 3. */
    private static double C3 = -0.0000062;

    /** Universal Time 1 time scale. */
    private final UT1Scale     ut1;

    /** Reference date for GMST. */
    private final AbsoluteDate referenceDate;

    // GST 1982: 24110.54841 + 8640184.812866 t + 0.093104 t2 - 6.2e-6 t3
    // GST 2000: 24110.5493771 + 8639877.3173760 tu + 307.4771600 te + 0.0931118 te2 - 0.0000062 te3 + 0.0000013 te4

    /** Package private constructor for the factory.
     * @param ut1 Universal Time 1 scale
     */
    GMSTScale(final UT1Scale ut1) {
        this.ut1           = ut1;
        this.referenceDate = new AbsoluteDate(2000, 1, 1, 12, 0, 0.0, ut1);
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {

        // julian seconds since reference date
        final double ts = date.durationFrom(referenceDate);

        // julian centuries since reference date
        final double tc = ts / Constants.JULIAN_CENTURY;

        // GMST at 0h00 UT1 in seconds = offset with respect to UT1
        final double gmst0h = C0 + tc * (C1 + tc * (C2 + tc * C3));

        // offset with respect to TAI
        final double offset = gmst0h + ut1.offsetFromTAI(date);

        // normalize offset between -43200 and +43200 seconds
        return offset - FULL_DAY * FastMath.floor((offset + HALF_DAY) / FULL_DAY);

    }

    /** {@inheritDoc} */
    public String getName() {
        return "GMST";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

}
