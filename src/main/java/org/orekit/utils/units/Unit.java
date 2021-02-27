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

/** Basic handling of multiplicative units.
 * <p>
 * This class is by no means a complete handling of units. For complete
 * support, look at libraries like {@code UOM}. This class handles only
 * time, length and mass dimensions, as well as angles (which are
 * dimensionless).
 * </p>
 * <p>
 * Instances of this class are immutable.
 * </p>
 * @see <a href="https://github.com/netomi/uom">UOM</a>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class Unit {

    /** Name name of the unit. */
    private final String name;

    /** Scaling factor to SI units. */
    private final double scale;

    /** Mass exponent. */
    private final Fraction mass;

    /** Length exponent. */
    private final Fraction length;

    /** Time exponent. */
    private final Fraction time;

    /** Angle exponent. */
    private final Fraction angle;

    /** Simple constructor.
     * @param name name of the unit
     * @param scale scaling factor to SI units
     * @param mass mass exponent
     * @param length length exponent
     * @param time time exponent
     * @param angle angle exponent
     */
    public Unit(final String name, final double scale,
                final Fraction mass, final Fraction length,
                final Fraction time, final Fraction angle) {
        this.name   = name;
        this.scale  = scale;
        this.mass   = mass;
        this.length = length;
        this.time   = time;
        this.angle  = angle;
    }

    /** Get the name of the unit.
     * @return name of the unit
     */
    public String getName() {
        return name;
    }

    /** Get the scaling factor to SI units.
     * @return scaling factor to SI units
     */
    public double getScale() {
        return scale;
    }

    /** Get the mass exponent.
     * @return mass exponent
     */
    public Fraction getMass() {
        return mass;
    }

    /** Get the length exponent.
     * @return length exponent
     */
    public Fraction getLength() {
        return length;
    }

    /** Get the time exponent.
     * @return time exponent
     */
    public Fraction getTime() {
        return time;
    }

    /** Get the angle exponent.
     * @return angle exponent
     */
    public Fraction getAngle() {
        return angle;
    }

    /** Check if a unit has the same dimension as another unit.
     * @param other other unit to check against
     * @return true if unit has the same dimension as the other unit
     */
    public boolean sameDimension(final Unit other) {
        return time.equals(other.time) && length.equals(other.length) &&
               mass.equals(other.mass) && angle.equals(other.angle);
    }

    /** Create an alias for a unit.
     * @param newName name of the new unit
     * @return a new unit representing same unit as the instance but with a different name
     */
    public Unit alias(final String newName) {
        return new Unit(newName, scale, mass, length, time, angle);
    }

    /** Scale a unit.
     * @param newName name of the new unit
     * @param factor scaling factor
     * @return a new unit representing scale times the instance
     */
    public Unit scale(final String newName, final double factor) {
        return new Unit(newName, factor * scale, mass, length, time, angle);
    }

    /** Create power of unit.
     * @param newName name of the new unit
     * @param exponent exponent to apply
     * @return a new unit representing the power of the instance
     */
    public Unit power(final String newName, final Fraction exponent) {

        final int num = exponent.getNumerator();
        final int den = exponent.getDenominator();
        double s = (num == 1) ? scale : FastMath.pow(scale, num);
        if (den > 1) {
            if (den == 2) {
                s = FastMath.sqrt(s);
            } else if (den == 3) {
                s = FastMath.cbrt(s);
            } else {
                s = FastMath.pow(s, 1.0 / den);
            }
        }

        return new Unit(newName, s,
                        mass.multiply(exponent), length.multiply(exponent),
                        time.multiply(exponent), angle.multiply(exponent));
    }

    /** Create root of unit.
     * @param newName name of the new unit
     * @return a new unit representing the square root of the instance
     */
    public Unit sqrt(final String newName) {
        return new Unit(newName, FastMath.sqrt(scale),
                        mass.divide(2), length.divide(2), time.divide(2),
                        angle.divide(2));
    }

    /** Create product of units.
     * @param newName name of the new unit
     * @param other unit to multiply with
     * @return a new unit representing the this times the other unit
     */
    public Unit multiply(final String newName, final Unit other) {
        return new Unit(newName, scale * other.scale,
                        mass.add(other.mass), length.add(other.length),
                        time.add(other.time), angle.add(other.angle));
    }

    /** Create quotient of units.
     * @param newName name of the new unit
     * @param other unit to divide with
     * @return a new unit representing the this divided by the other unit
     */
    public Unit divide(final String newName, final Unit other) {
        return new Unit(newName, scale / other.scale,
                        mass.subtract(other.mass), length.subtract(other.length),
                        time.subtract(other.time), angle.subtract(other.angle));
    }

    /** Convert a value to SI units.
     * @param value value instance unit
     * @return value in SI units
     */
    public double toSI(final double value) {
        return value * scale;
    }

    /** Convert a value from SI units.
     * @param value value SI unit
     * @return value in instance units
     */
    public double fromSI(final double value) {
        return value / scale;
    }

    /** Parse a unit.
     * <p>
     * The grammar for unit specification allows chains units multiplication and
     * division, as well as putting powers on units.
     * </p>
     * <p>The symbols used for units are the SI units with some extensions. For
     * example the accepted non-SI unit for Julian year is "a" but we also accept
     * "y". The base symbol for mass is "g" as per the standard, despite the unit
     * is "kg" (its the only unit that has a prefix in its name, so all multiples
     * must be based on "g"). The base symbol for degrees is "°", but we also
     * accept "◦" and "deg". The base symbol for arcminute is "′" but we also
     * accept "'". The base symbol for arcsecond is "″" but we also accept "''",
     * "\"" and "as".
     * </p>
     * <p>
     * All the SI prefix (from "y", yocto, to "Y", Yotta) are accepted. Beware
     * that some combinations are forbidden, for example "Pa" is Pascal, not
     * peta-years, and "as" is arcsecond for us, not atto-seconds, because many people
     * in the space field use mas for milli-arcseconds and µas for micro-arcseconds.
     * Beware that prefixes are case-sensitive! Prefix and units must be joined
     * without blanks.
     * </p>
     * <ul>
     *   <li>multiplication can specified with either "*", "×" or "." as the operator</li>
     *   <li>division can be specified with either "/" or "⁄"</li>
     *   <li>powers can be specified either by
     *     <ul>
     *       <li>prefixing with the unicode "√" character</li>
     *       <li>postfixing with "**", "^" or implicitly using unicode superscripts</li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     * Fractional exponents are allowed, but only with regular characters (because unicode
     * does not provide a superscript '/'). Negative exponents can be used too.
     * </p>
     * <p>
     * These rules mean all the following (silly) examples are parsed properly:
     * MHz, km/√d, kg.m.s⁻¹, µas^(2/5)/(h**(2)×m)³, km/√(kg.s)
     * </p>
     * @param unitSpecification unit specification to parse
     * @return parsed unit
     */
    public static Unit parse(final String unitSpecification) {
        return Parser.parse(unitSpecification);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getName();
    }

}
