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
        Assert.assertEquals(       1.0, PredefinedUnit.SECOND.toUnit().toSI(1.0), 1.0e-10);
        Assert.assertEquals(      60.0, PredefinedUnit.MINUTE.toUnit().toSI(1.0), 1.0e-10);
        Assert.assertEquals(    3600.0, PredefinedUnit.HOUR.toUnit().toSI(1.0),   1.0e-10);
        Assert.assertEquals(   86400.0, PredefinedUnit.DAY.toUnit().toSI(1.0),    1.0e-10);
        Assert.assertEquals(31557600.0, PredefinedUnit.YEAR.toUnit().toSI(1.0),   1.0e-10);
        Assert.assertEquals(1.0,        PredefinedUnit.SECOND.toUnit().fromSI(     1.0), 1.0e-10);
        Assert.assertEquals(1.0,        PredefinedUnit.MINUTE.toUnit().fromSI(    60.0), 1.0e-10);
        Assert.assertEquals(1.0,        PredefinedUnit.HOUR.toUnit().fromSI(    3600.0), 1.0e-10);
        Assert.assertEquals(1.0,        PredefinedUnit.DAY.toUnit().fromSI(    86400.0), 1.0e-10);
        Assert.assertEquals(1.0,        PredefinedUnit.YEAR.toUnit().fromSI(31557600.0), 1.0e-10);
        Assert.assertEquals(365.25,     PredefinedUnit.DAY.toUnit().fromSI(PredefinedUnit.YEAR.toUnit().toSI(1.0)), 1.0e-10);
    }

    @Test
    public void testReference() {                                
        checkReference(PredefinedUnit.NONE.toUnit(),                        "n/a",                     1.0,  0,  0,  0,  0, 0);
        checkReference(PredefinedUnit.ONE.toUnit(),                           "1",                     1.0,  0,  0,  0,  0, 0);
        checkReference(PredefinedUnit.PERCENT.toUnit(),                       "%",                    0.01,  0,  0,  0,  0, 0);
        checkReference(PredefinedUnit.SECOND.toUnit(),                        "s",                     1.0,  0,  0,  1,  0, 0);
        checkReference(PredefinedUnit.MINUTE.toUnit(),                      "min",                    60.0,  0,  0,  1,  0, 0);
        checkReference(PredefinedUnit.HOUR.toUnit(),                          "h",                  3600.0,  0,  0,  1,  0, 0);
        checkReference(PredefinedUnit.DAY.toUnit(),                           "d",                 86400.0,  0,  0,  1,  0, 0);
        checkReference(PredefinedUnit.YEAR.toUnit(),                          "a",              31557600.0,  0,  0,  1,  0, 0);
        checkReference(PredefinedUnit.HERTZ.toUnit(),                        "Hz",                     1.0,  0,  0, -1,  0, 0);
        checkReference(PredefinedUnit.METRE.toUnit(),                         "m",                     1.0,  0,  1,  0,  0, 0);
        checkReference(PredefinedUnit.KILOMETRE.toUnit(),                    "km",                  1000.0,  0,  1,  0,  0, 0);
        checkReference(PredefinedUnit.KILOGRAM.toUnit(),                     "kg",                     1.0,  1,  0,  0,  0, 0);
        checkReference(PredefinedUnit.GRAM.toUnit(),                          "g",                   0.001,  1,  0,  0,  0, 0);
        checkReference(PredefinedUnit.AMPERE.toUnit(),                        "A",                     1.0,  0,  0,  0,  1, 0);
        checkReference(PredefinedUnit.RADIAN.toUnit(),                      "rad",                     1.0,  0,  0,  0,  0, 1);
        checkReference(PredefinedUnit.DEGREE.toUnit(),                        "°",  FastMath.PI /    180.0,  0,  0,  0,  0, 1);
        checkReference(PredefinedUnit.ARC_MINUTE.toUnit(),                    "′",  FastMath.PI /  10800.0,  0,  0,  0,  0, 1);
        checkReference(PredefinedUnit.ARC_SECOND.toUnit(),                    "″",  FastMath.PI / 648000.0,  0,  0,  0,  0, 1);
        checkReference(PredefinedUnit.NEWTON.toUnit(),                        "N",                     1.0,  1,  1, -2,  0, 0);
        checkReference(PredefinedUnit.PASCAL.toUnit(),                       "Pa",                     1.0,  1, -1, -2,  0, 0);
        checkReference(PredefinedUnit.JOULE.toUnit(),                         "J",                     1.0,  1,  2, -2,  0, 0);
        checkReference(PredefinedUnit.WATT.toUnit(),                          "W",                     1.0,  1,  2, -3,  0, 0);
        checkReference(PredefinedUnit.COULOMB.toUnit(),                       "C",                     1.0,  0,  0,  1,  1, 0);
        checkReference(PredefinedUnit.VOLT.toUnit(),                          "V",                     1.0,  1,  2, -3, -1, 0);
        checkReference(PredefinedUnit.OHM.toUnit(),                           "Ω",                     1.0,  1,  2, -3, -2, 0);
        checkReference(PredefinedUnit.TESLA.toUnit(),                         "T",                     1.0,  1,  0, -2, -1, 0);
        checkReference(PredefinedUnit.SOLAR_FLUX_UNIT.toUnit(),              "sfu",                1.0e-22,  1,  0, -2,  0, 0);
        checkReference(PredefinedUnit.TOTAL_ELECTRON_CONTENT_UNIT.toUnit(), "TECU",                 1.0e16,  0, -2,  0,  0, 0);

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
