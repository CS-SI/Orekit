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

import org.hipparchus.fraction.Fraction;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link Lexer}.
 *
 * @author Luc Maisonobe
 */
public class UnitTest {

    @Test
    public void testTime() {
        Assert.assertEquals(       1.0, Unit.SECOND.toSI(1.0), 1.0e-10);
        Assert.assertEquals(      60.0, Unit.MINUTE.toSI(1.0), 1.0e-10);
        Assert.assertEquals(    3600.0, Unit.HOUR.toSI(1.0),   1.0e-10);
        Assert.assertEquals(   86400.0, Unit.DAY.toSI(1.0),    1.0e-10);
        Assert.assertEquals(31557600.0, Unit.YEAR.toSI(1.0),   1.0e-10);
        Assert.assertEquals(1.0,        Unit.SECOND.fromSI(     1.0), 1.0e-10);
        Assert.assertEquals(1.0,        Unit.MINUTE.fromSI(    60.0), 1.0e-10);
        Assert.assertEquals(1.0,        Unit.HOUR.fromSI(    3600.0), 1.0e-10);
        Assert.assertEquals(1.0,        Unit.DAY.fromSI(    86400.0), 1.0e-10);
        Assert.assertEquals(1.0,        Unit.YEAR.fromSI(31557600.0), 1.0e-10);
        Assert.assertEquals(365.25,     Unit.DAY.fromSI(Unit.YEAR.toSI(1.0)), 1.0e-10);
    }

    @Test
    public void testSI() {
        Assert.assertTrue(Unit.PERCENT.sameDimensionSI().sameDimension(Unit.ONE));
        Assert.assertEquals("1", Unit.PERCENT.sameDimensionSI().getName());
        Assert.assertEquals("m³.s⁻²", Unit.parse("km**3/s**2").sameDimensionSI().getName());
        Assert.assertEquals("m⁻³.s⁻⁶.rad^(2/5)", Unit.parse("µas^(2/5)/(h**(2)×m)³").sameDimensionSI().getName());
        
    }

    @Test
    public void testReference() {                                
        checkReference(Unit.NONE,                        "n/a",                     1.0,  0,  0,  0,  0, 0);
        checkReference(Unit.ONE,                           "1",                     1.0,  0,  0,  0,  0, 0);
        checkReference(Unit.PERCENT,                       "%",                    0.01,  0,  0,  0,  0, 0);
        checkReference(Unit.SECOND,                        "s",                     1.0,  0,  0,  1,  0, 0);
        checkReference(Unit.MINUTE,                      "min",                    60.0,  0,  0,  1,  0, 0);
        checkReference(Unit.HOUR,                          "h",                  3600.0,  0,  0,  1,  0, 0);
        checkReference(Unit.DAY,                           "d",                 86400.0,  0,  0,  1,  0, 0);
        checkReference(Unit.YEAR,                          "a",              31557600.0,  0,  0,  1,  0, 0);
        checkReference(Unit.HERTZ,                        "Hz",                     1.0,  0,  0, -1,  0, 0);
        checkReference(Unit.METRE,                         "m",                     1.0,  0,  1,  0,  0, 0);
        checkReference(Unit.KILOMETRE,                    "km",                  1000.0,  0,  1,  0,  0, 0);
        checkReference(Unit.KILOGRAM,                     "kg",                     1.0,  1,  0,  0,  0, 0);
        checkReference(Unit.GRAM,                          "g",                   0.001,  1,  0,  0,  0, 0);
        checkReference(Unit.AMPERE,                        "A",                     1.0,  0,  0,  0,  1, 0);
        checkReference(Unit.RADIAN,                      "rad",                     1.0,  0,  0,  0,  0, 1);
        checkReference(Unit.DEGREE,                        "°",  FastMath.PI /    180.0,  0,  0,  0,  0, 1);
        checkReference(Unit.ARC_MINUTE,                    "′",  FastMath.PI /  10800.0,  0,  0,  0,  0, 1);
        checkReference(Unit.ARC_SECOND,                    "″",  FastMath.PI / 648000.0,  0,  0,  0,  0, 1);
        checkReference(Unit.REVOLUTION,                   "rev",      2.0 * FastMath.PI,  0,  0,  0,  0, 1);
        checkReference(Unit.NEWTON,                        "N",                     1.0,  1,  1, -2,  0, 0);
        checkReference(Unit.PASCAL,                       "Pa",                     1.0,  1, -1, -2,  0, 0);
        checkReference(Unit.BAR,                         "bar",                100000.0,  1, -1, -2,  0, 0);
        checkReference(Unit.JOULE,                         "J",                     1.0,  1,  2, -2,  0, 0);
        checkReference(Unit.WATT,                          "W",                     1.0,  1,  2, -3,  0, 0);
        checkReference(Unit.COULOMB,                       "C",                     1.0,  0,  0,  1,  1, 0);
        checkReference(Unit.VOLT,                          "V",                     1.0,  1,  2, -3, -1, 0);
        checkReference(Unit.OHM,                           "Ω",                     1.0,  1,  2, -3, -2, 0);
        checkReference(Unit.TESLA,                         "T",                     1.0,  1,  0, -2, -1, 0);
        checkReference(Unit.SOLAR_FLUX_UNIT,              "sfu",                1.0e-22,  1,  0, -2,  0, 0);
        checkReference(Unit.TOTAL_ELECTRON_CONTENT_UNIT, "TECU",                 1.0e16,  0, -2,  0,  0, 0);

    }

    private void checkReference(final Unit unit, final String name, final double scale,
                                final int mass, final int length, final int time,
                                final int current, final int angle) {
        Assert.assertEquals(name, unit.toString());
        Assert.assertEquals(scale, unit.getScale(), 1.0e-10);
        Assert.assertEquals(new Fraction(mass),     unit.getMass());
        Assert.assertEquals(new Fraction(length),   unit.getLength());
        Assert.assertEquals(new Fraction(time),     unit.getTime());
        Assert.assertEquals(new Fraction(current),  unit.getCurrent());
        Assert.assertEquals(new Fraction(angle),    unit.getAngle());
    }

}
