/* Copyright 2002-2021 CS GROUP
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
package org.orekit.utils.units;

import java.io.Serializable;

import org.hipparchus.fraction.Fraction;
import org.hipparchus.util.FastMath;

/** Basic handling of multiplicative units.
 * <p>
 * This class is by no means a complete handling of units. For complete
 * support, look at libraries like {@code UOM}. This class handles only
 * time, length, mass and current dimensions, as well as angles (which are
 * dimensionless).
 * </p>
 * <p>
 * Instances of this class are immutable.
 * </p>
 * @see <a href="https://github.com/netomi/uom">UOM</a>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class Unit implements Serializable {

    /** No unit. */
    public static final Unit NONE = new Unit("n/a", 1.0, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO);

    /** Dimensionless unit. */
    public static final Unit ONE = new Unit("1", 1.0, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO);

    /** Percentage unit. */
    public static final Unit PERCENT = new Unit("%", 1.0e-2, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO);

    /** Second unit. */
    public static final Unit SECOND = new Unit("s", 1.0, Fraction.ZERO, Fraction.ZERO, Fraction.ONE, Fraction.ZERO, Fraction.ZERO);

    /** Minute unit. */
    public static final Unit MINUTE = SECOND.scale("min", 60.0);

    /** Hour unit. */
    public static final Unit HOUR = MINUTE.scale("h", 60);

    /** Day unit. */
    public static final Unit DAY = HOUR.scale("d", 24.0);

    /** Julian year unit.
     * @see <a href="https://www.iau.org/publications/proceedings_rules/units/">SI Units</a> at IAU
     */
    public static final Unit YEAR = DAY.scale("a", 365.25);

    /** Hertz unit. */
    public static final Unit HERTZ = SECOND.power("Hz", Fraction.MINUS_ONE);

    /** Metre unit. */
    public static final Unit METRE = new Unit("m", 1.0, Fraction.ZERO, Fraction.ONE, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO);

    /** Kilometre unit. */
    public static final Unit KILOMETRE = METRE.scale("km", 1000.0);

    /** Kilogram unit. */
    public static final Unit KILOGRAM = new Unit("kg", 1.0, Fraction.ONE, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO);

    /** Gram unit. */
    public static final Unit GRAM = KILOGRAM.scale("g", 1.0e-3);

    /** Ampere unit. */
    public static final Unit AMPERE = new Unit("A", 1.0, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ONE, Fraction.ZERO);

    /** Radian unit. */
    public static final Unit RADIAN = new Unit("rad", 1.0, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ONE);

    /** Degree unit. */
    public static final Unit DEGREE = RADIAN.scale("°", FastMath.toRadians(1.0));

    /** Arc minute unit. */
    public static final Unit ARC_MINUTE = DEGREE.scale("′", 1.0 / 60.0);

    /** Arc second unit. */
    public static final Unit ARC_SECOND = ARC_MINUTE.scale("″", 1.0 / 60.0);

    /** Revolution unit. */
    public static final Unit REVOLUTION = RADIAN.scale("rev", 2.0 * FastMath.PI);

    /** Newton unit. */
    public static final Unit NEWTON = KILOGRAM.multiply(null, METRE).divide("N", SECOND.power(null, Fraction.TWO));

    /** Pascal unit. */
    public static final Unit PASCAL = NEWTON.divide("Pa", METRE.power(null, Fraction.TWO));

    /** Bar unit. */
    public static final Unit BAR = PASCAL.scale("bar", 100000.0);

    /** Joule unit. */
    public static final Unit JOULE = NEWTON.multiply("J", METRE);

    /** Watt unit. */
    public static final Unit WATT = JOULE.divide("W", SECOND);

    /** Coulomb unit. */
    public static final Unit COULOMB = SECOND.multiply("C", AMPERE);

    /** Volt unit. */
    public static final Unit VOLT = WATT.divide("V", AMPERE);

    /** Ohm unit. */
    public static final Unit OHM = VOLT.divide("Ω", AMPERE);

    /** tesla unit. */
    public static final Unit TESLA = VOLT.multiply(null, SECOND).divide("T", METRE.power(null, Fraction.TWO));

    /** Solar Flux Unit. */
    public static final Unit SOLAR_FLUX_UNIT = WATT.divide(null, METRE.power(null, Fraction.TWO).multiply(null, HERTZ)).scale("sfu", 1.0e-22);

    /** Total Electron Content Unit. */
    public static final Unit TOTAL_ELECTRON_CONTENT_UNIT = METRE.power(null, new Fraction(-2)).scale("TECU", 1.0e+16);

    /** Serializable UID. */
    private static final long serialVersionUID = 20210402L;

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

    /** Current exponent. */
    private final Fraction current;

    /** Angle exponent. */
    private final Fraction angle;

    /** Simple constructor.
     * @param name name of the unit
     * @param scale scaling factor to SI units
     * @param mass mass exponent
     * @param length length exponent
     * @param time time exponent
     * @param current current exponent
     * @param angle angle exponent
     */
    public Unit(final String name, final double scale,
                final Fraction mass, final Fraction length,
                final Fraction time, final Fraction current,
                final Fraction angle) {
        this.name    = name;
        this.scale   = scale;
        this.mass    = mass;
        this.length  = length;
        this.time    = time;
        this.current = current;
        this.angle   = angle;
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

    /** Get the current exponent.
     * @return current exponent
     */
    public Fraction getCurrent() {
        return current;
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
        return time.equals(other.time) && length.equals(other.length)   &&
               mass.equals(other.mass) && current.equals(other.current) &&
               angle.equals(other.angle);
    }

    /** Create the SI unit with same dimension.
     * @return a new unit, with same dimension as instance and scaling factor set to 1.0
     */
    public Unit sameDimensionSI() {
        final StringBuilder builder = new StringBuilder();
        append(builder, KILOGRAM.name, mass);
        append(builder, METRE.name,    length);
        append(builder, SECOND.name,   time);
        append(builder, AMPERE.name,   current);
        append(builder, RADIAN.name,   angle);
        if (builder.length() == 0) {
            builder.append('1');
        }
        return new Unit(builder.toString(), 1.0, mass, length, time, current, angle);
    }

    /** Append a dimension contribution to a unit name.
     * @param builder builder for unit name
     * @param dim name of the dimension
     * @param exp exponent of the dimension
     */
    private void append(final StringBuilder builder, final String dim, final Fraction exp) {
        if (!exp.isZero()) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(dim);
            if (exp.getDenominator() == 1) {
                if (exp.getNumerator() != 1) {
                    builder.append(Integer.toString(exp.getNumerator()).
                                   replace('-', '⁻').
                                   replace('0', '⁰').
                                   replace('1', '¹').
                                   replace('2', '²').
                                   replace('3', '³').
                                   replace('4', '⁴').
                                   replace('5', '⁵').
                                   replace('6', '⁶').
                                   replace('7', '⁷').
                                   replace('8', '⁸').
                                   replace('9', '⁹'));
                }
            } else {
                builder.
                    append("^(").
                    append(exp.getNumerator()).
                    append('/').
                    append(exp.getDenominator()).
                    append(')');
            }
        }
    }

    /** Create an alias for a unit.
     * @param newName name of the new unit
     * @return a new unit representing same unit as the instance but with a different name
     */
    public Unit alias(final String newName) {
        return new Unit(newName, scale, mass, length, time, current, angle);
    }

    /** Scale a unit.
     * @param newName name of the new unit
     * @param factor scaling factor
     * @return a new unit representing scale times the instance
     */
    public Unit scale(final String newName, final double factor) {
        return new Unit(newName, factor * scale, mass, length, time, current, angle);
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
                        time.multiply(exponent), current.multiply(current),
                        angle.multiply(exponent));
    }

    /** Create root of unit.
     * @param newName name of the new unit
     * @return a new unit representing the square root of the instance
     */
    public Unit sqrt(final String newName) {
        return new Unit(newName, FastMath.sqrt(scale),
                        mass.divide(2), length.divide(2),
                        time.divide(2), current.divide(2),
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
                        time.add(other.time), current.add(other.current),
                        angle.add(other.angle));
    }

    /** Create quotient of units.
     * @param newName name of the new unit
     * @param other unit to divide with
     * @return a new unit representing the this divided by the other unit
     */
    public Unit divide(final String newName, final Unit other) {
        return new Unit(newName, scale / other.scale,
                        mass.subtract(other.mass), length.subtract(other.length),
                        time.subtract(other.time), current.subtract(other.current),
                        angle.subtract(other.angle));
    }

    /** Convert a value to SI units.
     * @param value value instance unit
     * @return value in SI units
     */
    public double toSI(final double value) {
        return value * scale;
    }

    /** Convert a value to SI units.
     * @param value value instance unit
     * @return value in SI units
     */
    public double toSI(final Double value) {
        return value == null ? Double.NaN : value.doubleValue() * scale;
    }

    /** Convert a value from SI units.
     * @param value value SI unit
     * @return value in instance units
     */
    public double fromSI(final double value) {
        return value / scale;
    }

    /** Convert a value from SI units.
     * @param value value SI unit
     * @return value in instance units
     */
    public double fromSI(final Double value) {
        return value == null ? Double.NaN : value.doubleValue() / scale;
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
