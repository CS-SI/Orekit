/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.sp3;

import org.hipparchus.util.FastMath;
import org.orekit.utils.units.Unit;

/** Constants for SP3 files.
 * @since 12.0
 * @author Luc Maisonobe
 */
public class SP3Utils {

    /** Bad or absent clock values are to be set to 999999.999999. */
    public static final double DEFAULT_CLOCK_VALUE = 999999.999999;

    /** Bad or absent clock rate values are to be set to 999999.999999. */
    public static final double DEFAULT_CLOCK_RATE_VALUE = 999999.999999;

    /** Base for general position/velocity accuracy. */
    public static final double POS_VEL_BASE_ACCURACY = 2.0;

    /** Position unit. */
    public static final Unit POSITION_UNIT = Unit.parse("km");

    /** Position accuracy unit. */
    public static final Unit POSITION_ACCURACY_UNIT = Unit.parse("mm");

   /** Velocity unit. */
    public static final Unit VELOCITY_UNIT = Unit.parse("dm/s");

    /** Velocity accuracy unit. */
    public static final Unit VELOCITY_ACCURACY_UNIT = Unit.parse("mm/s").scale("10⁻⁴mm/s", 1.0e-4);

    /** Clock unit. */
    public static final Unit CLOCK_UNIT = Unit.parse("µs");

    /** Clock accuracy unit. */
    public static final Unit CLOCK_ACCURACY_UNIT = Unit.parse("ps");

    /** Clock rate unit. */
    public static final Unit CLOCK_RATE_UNIT = Unit.parse("µs/s").scale("10⁻⁴µs/s", 1.0e-4);

    /** Clock rate accuracy unit. */
    public static final Unit CLOCK_RATE_ACCURACY_UNIT = Unit.parse("ps/s").scale("10⁻⁴ps/s", 1.0e-4);

    /** Private constructor for utility class.
     */
    private SP3Utils() {
        // nothing to do
    }

    /** Convert an accuracy to SI units.
     * @param unit accuracy unit
     * @param base base
     * @param accuracyIndex index of accuracy
     * @return accuracy in SI units
     */
    public static double siAccuracy(final Unit unit, final double base, final int accuracyIndex) {
        return unit.toSI(FastMath.pow(base, accuracyIndex));
    }

    /** Convert an accuracy from SI units.
     * @param unit accuracy unit
     * @param base base
     * @param accuracy in SI units
     * @return accuracyIndex index of accuracy
     */
    public static int indexAccuracy(final Unit unit, final double base, final double accuracy) {
        return (int) FastMath.ceil(FastMath.log(unit.fromSI(accuracy)) / FastMath.log(base));
    }

}
