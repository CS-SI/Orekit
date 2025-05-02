/* Copyright 2002-2025 CS GROUP
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
package org.orekit.models.earth.atmosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

class JB2006Test {

    private static double EPSILON = 1.0e-12;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere");
    }

    @Test
    public void testWithOriginalTestsCases()  {

        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        CelestialBody sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        earth.setAngularThreshold(1e-10);

        // Static inputs
        final AbsoluteDate date = new AbsoluteDate("2001-07-19T00:00:00.000Z", TimeScalesFactory.getUTC());
        final double sunRa = FastMath.toRadians(90.);
        final double sunDecli = FastMath.toRadians(20.);
        final double subLon = FastMath.toRadians(90.);
        final double subLat = FastMath.toRadians(45.);

        InputParameters inputParameters = new InputParameters();
        JB2006 atm = new JB2006(inputParameters, sun, earth);

        // alt = 400
        double computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, 400000);
        double referenceDensity = 4.066e-12;
        Assertions.assertEquals(referenceDensity * 1e12, FastMath.round(computedDensity * 1e15) / 1e3, EPSILON);

        // alt = 89.999km
        try {
            atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, 89999.0);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, oe.getSpecifier());
            Assertions.assertEquals(89999.0, (Double) oe.getParts()[0], 1.0e-15);
            Assertions.assertEquals(90000.0, (Double) oe.getParts()[1], 1.0e-15);
        }

        // alt = 90
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat,90000);
        referenceDensity = 0.3285e-05;
        Assertions.assertEquals(referenceDensity * 1e05, FastMath.round(computedDensity * 1e09) / 1e4, EPSILON);

        // alt = 110
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, 110000);
        referenceDensity = 0.7587e-07;
        Assertions.assertEquals(referenceDensity * 1e07, FastMath.round(computedDensity * 1e11) / 1e4, EPSILON);

        // alt = 180
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, 180000);
        referenceDensity = 0.5439;
        Assertions.assertEquals(referenceDensity, FastMath.round(computedDensity * 1e13) / 1e4, EPSILON);

        // alt = 230
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, 230000);
        referenceDensity = 0.1250e-09;
        Assertions.assertEquals(referenceDensity * 1e09, FastMath.round(computedDensity * 1e13) / 1e4, EPSILON);

        // alt = 270
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, 270000);
        referenceDensity = 0.4818e-10;
        Assertions.assertEquals(referenceDensity * 1e10, FastMath.round(computedDensity * 1e14) / 1e4, EPSILON);

        // alt = 660
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, 660000);
        referenceDensity = 0.9451e-13;
        Assertions.assertEquals(referenceDensity * 1e13, FastMath.round(computedDensity*1e17) / 1e4, EPSILON);

        //  alt = 890
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, 890000);
        referenceDensity = 0.8305e-14;
        Assertions.assertEquals(referenceDensity * 1e14, FastMath.round(computedDensity * 1e18) / 1e4, EPSILON);

        //  alt = 1320
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, 1320000);
        referenceDensity = 0.2004e-14;
        Assertions.assertEquals(referenceDensity * 1e14, FastMath.round(computedDensity * 1e18) / 1e4, EPSILON);

        //  alt = 1600
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, 1600000);
        referenceDensity = 0.1159e-14;
        Assertions.assertEquals(referenceDensity * 1e14, FastMath.round(computedDensity * 1e18) / 1e4, EPSILON);
    }

    @Test
    public void testWithOriginalTestsCasesField() {
        doTestWithOriginalTestsCasesField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestWithOriginalTestsCasesField(Field<T> field)  {

        final T zero = field.getZero();
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        CelestialBody sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        earth.setAngularThreshold(1e-10);

        InputParameters inputParameters = new InputParameters();
        JB2006 atm = new JB2006(inputParameters, sun, earth);

        // Static inputs
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, "2001-07-19T00:00:00.000Z", TimeScalesFactory.getUTC());
        T sunRa = zero.add(FastMath.toRadians(90.));
        T sunDecli = zero.add(FastMath.toRadians(20.));
        T subLon = zero.add(FastMath.toRadians(90.));
        T subLat = zero.add(FastMath.toRadians(45.));

        // alt = 400
        T computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, zero.add(400000));
        double referenceDensity = 4.066e-12;
        Assertions.assertEquals(referenceDensity * 1e12, FastMath.round(computedDensity.getReal() * 1e15) / 1e3, EPSILON);

        // alt = 89.999km
        try {
            atm.computeDensity(date, sunRa, sunDecli, subLon, subLat,zero.add(89999.0));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, oe.getSpecifier());
            Assertions.assertEquals(89999.0, ((Binary64) oe.getParts()[0]).getReal(), 1.0e-15);
            Assertions.assertEquals(90000.0, (Double) oe.getParts()[1], 1.0e-15);
        }

        // alt = 90
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, zero.add(90000));
        referenceDensity = 0.3285e-05;
        Assertions.assertEquals(referenceDensity * 1e05, FastMath.round(computedDensity.getReal() * 1e09) / 1e4, EPSILON);

        // alt = 110
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, zero.add(110000));
        referenceDensity = 0.7587e-07;
        Assertions.assertEquals(referenceDensity * 1e07, FastMath.round(computedDensity.getReal() * 1e11) / 1e4, EPSILON);

        // alt = 180
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, zero.add(180000));
        referenceDensity = 0.5439;
        Assertions.assertEquals(referenceDensity, FastMath.round(computedDensity.getReal() * 1e13) / 1e4, EPSILON);

        // alt = 230
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, zero.add(230000));
        referenceDensity = 0.1250e-09;
        Assertions.assertEquals(referenceDensity * 1e09, FastMath.round(computedDensity.getReal() * 1e13) / 1e4, EPSILON);

        // alt = 270
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, zero.add(270000));
        referenceDensity = 0.4818e-10;
        Assertions.assertEquals(referenceDensity * 1e10, FastMath.round(computedDensity.getReal() * 1e14) / 1e4, EPSILON);

        // alt = 660
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, zero.add(660000));
        referenceDensity = 0.9451e-13;
        Assertions.assertEquals(referenceDensity * 1e13, FastMath.round(computedDensity.getReal() * 1e17) / 1e4, EPSILON);

        //  alt = 890
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat, zero.add(890000));
        referenceDensity = 0.8305e-14;
        Assertions.assertEquals(referenceDensity * 1e14, FastMath.round(computedDensity.getReal() * 1e18) / 1e4, EPSILON);

        //  alt = 1320
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat,zero.add(1320000));
        referenceDensity = 0.2004e-14;
        Assertions.assertEquals(referenceDensity*1e14, FastMath.round(computedDensity.getReal() * 1e18) / 1e4, EPSILON);

        //  alt = 1600
        computedDensity = atm.computeDensity(date, sunRa, sunDecli, subLon, subLat,zero.add(1600000));
        referenceDensity = 0.1159e-14;
        Assertions.assertEquals(referenceDensity * 1e14, FastMath.round(computedDensity.getReal() * 1e18) / 1e4, EPSILON);
    }

    private class InputParameters implements JB2006InputParameters {

        @Override
        public double getF10(AbsoluteDate date) {
            return 135;
        }

        @Override
        public double getF10B(AbsoluteDate date) {
            return 95;
        }

        @Override
        public double getS10(AbsoluteDate date) {
            return 140;
        }

        @Override
        public double getS10B(AbsoluteDate date) {
            return 100;
        }

        @Override
        public double getXM10(AbsoluteDate date) {
            return 130;
        }

        @Override
        public double getXM10B(AbsoluteDate date) {
            return 95;
        }

        @Override
        public double getAp(AbsoluteDate date) {
            return 30;
        }

        @Override
        public AbsoluteDate getMinDate() {
            return AbsoluteDate.PAST_INFINITY;
        }

        @Override
        public AbsoluteDate getMaxDate() {
            return AbsoluteDate.FUTURE_INFINITY;
        }
    }

}