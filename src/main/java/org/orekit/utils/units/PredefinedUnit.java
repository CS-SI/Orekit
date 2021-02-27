/* Copyright 2002-2021 CS GROUP
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
package org.orekit.utils.units;

import org.hipparchus.fraction.Fraction;
import org.hipparchus.util.FastMath;

/** A few predefined units.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum PredefinedUnit {

    /** Dimensionless unit. */
    ONE(new Unit("1", 1.0, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO)),

    /** Second unit. */
    SECOND(new Unit("s", 1.0, Fraction.ZERO, Fraction.ZERO, Fraction.ONE, Fraction.ZERO)),

    /** Minute unit. */
    MINUTE(SECOND.unit.scale("min", 60.0)),

    /** Hour unit. */
    HOUR(MINUTE.unit.scale("h", 60)),

    /** Day unit. */
    DAY(HOUR.unit.scale("d", 24.0)),

    /** Julian year unit.
     * @see <a href="https://www.iau.org/publications/proceedings_rules/units/">SI Units</a> at IAU
     */
    YEAR(DAY.unit.scale("a", 365.25)),

    /** Hertz unit. */
    HERTZ(SECOND.unit.power("Hz", Fraction.MINUS_ONE)),

    /** Metre unit. */
    METRE(new Unit("m", 1.0, Fraction.ZERO, Fraction.ONE, Fraction.ZERO, Fraction.ZERO)),

    /** Kilometre unit. */
    KILOMETRE(METRE.unit.scale("km", 1000.0)),

    /** Kilogram unit. */
    KILOGRAM(new Unit("kg", 1.0, Fraction.ONE, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO)),

    /** Gram unit. */
    GRAM(KILOGRAM.unit.scale("g", 1.0e-3)),

    /** Radian unit. */
    RADIAN(new Unit("rad", 1.0, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ONE)),

    /** Degree unit. */
    DEGREE(RADIAN.unit.scale("°", FastMath.toRadians(1.0))),

    /** Arc minute unit. */
    ARC_MINUTE(DEGREE.unit.scale("′", 1.0 / 60.0)),

    /** Arc second unit. */
    ARC_SECOND(ARC_MINUTE.unit.scale("″", 1.0 / 60.0)),

    /** Newton unit. */
    NEWTON(KILOGRAM.unit.multiply(null, METRE.unit).divide("N", SECOND.unit.power(null, Fraction.TWO))),

    /** Pascal unit. */
    PASCAL(NEWTON.unit.divide("Pa", METRE.unit.power(null, Fraction.TWO))),

    /** Joule unit. */
    JOULE(NEWTON.unit.multiply("J", METRE.unit)),

    /** Watt unit. */
    WATT(JOULE.unit.divide("W", SECOND.unit));

    /** Underlying unit. */
    private final Unit unit;

    /** Simple constructor.
     * @param unit underlying unit
     */
    PredefinedUnit(final Unit unit) {
        this.unit = unit;
    }

    /** Get as a {@link Unit}.
     * @return unit
     */
    public Unit toUnit() {
        return unit;
    }

}
