/* Copyright 2011-2012 Space Applications Services
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth.displacement;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OceanLoadingTest {

    private OneAxisEllipsoid earth;

    @Test
    public void testSemiDiurnal() {
        TimeScale                    ut1      = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        FundamentalNutationArguments fna      = IERSConventions.IERS_2010.getNutationArguments(ut1);
        BodiesElements               elements = fna.evaluateAll(new AbsoluteDate(2009, 6, 25, 0, 0, 0.0, ut1));
        for (Tide tide : getTides()) {
            if (tide.getDoodsonMultipliers()[0] == 2) {
                double f = tide.getRate(elements) * Constants.JULIAN_DAY/ (2 * FastMath.PI);
                Assertions.assertTrue(f >  1.5);
                Assertions.assertTrue(f <= 2.5);
            }
        }
    }

    @Test
    public void testDiurnal() {
        TimeScale                    ut1      = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        FundamentalNutationArguments fna      = IERSConventions.IERS_2010.getNutationArguments(ut1);
        BodiesElements               elements = fna.evaluateAll(new AbsoluteDate(2009, 6, 25, 0, 0, 0.0, ut1));
        for (Tide tide : getTides()) {
            if (tide.getDoodsonMultipliers()[0] == 1) {
                double f = tide.getRate(elements) * Constants.JULIAN_DAY/ (2 * FastMath.PI);
                Assertions.assertTrue(f >  0.5);
                Assertions.assertTrue(f <= 1.5);
            }
        }
    }

    @Test
    public void testLongPeriod() {
        TimeScale                    ut1      = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        FundamentalNutationArguments fna      = IERSConventions.IERS_2010.getNutationArguments(ut1);
        BodiesElements               elements = fna.evaluateAll(new AbsoluteDate(2009, 6, 25, 0, 0, 0.0, ut1));
        for (Tide tide : getTides()) {
            if (tide.getDoodsonMultipliers()[0] == 0) {
                double f = tide.getRate(elements) * Constants.JULIAN_DAY/ (2 * FastMath.PI);
                Assertions.assertTrue(f >  0.0);
                Assertions.assertTrue(f <= 0.5);
            }
        }
    }

    @Test
    public void testNoExtra() {
        for (Tide tide : getTides()) {
            if (tide.getDoodsonMultipliers()[0] > 2) {
                Assertions.fail("unexpected tide " + tide.getDoodsonNumber());
            }
        }
    }

    @Test
    public void testStableRates() {
        // this test checks that tides sort order is date-independent for a large time range
        // (almost 180000 years long)
        // tides sort-order is based on rate, but the rates varies slightly with dates
        // (because Delaunay nutation arguments are polynomials)
        // The variations are however small and we want to make sure
        // that if rate(tideA) < rate(tideB) at time t0, this order remains
        // the same for t1 a few millenia around t0
        final TimeScale                    ut1      = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        final FundamentalNutationArguments fna      = IERSConventions.IERS_2010.getNutationArguments(ut1);

        // initial sort at J2000.0
        final BodiesElements el2000 = fna.evaluateAll(AbsoluteDate.J2000_EPOCH);
        List<Tide> tides = getTides();
        Collections.sort(tides, (t1, t2) -> Double.compare(t1.getRate(el2000), t2.getRate(el2000)));

        for (double dt = -122000; dt < 54000; dt += 100) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_YEAR);
            final BodiesElements el = fna.evaluateAll(date);
            for (int i = 1; i < tides.size(); ++i) {
                final Tide t1 = tides.get(i - 1);
                final Tide t2 = tides.get(i);
                Assertions.assertTrue(t1.getRate(el) < t2.getRate(el));
            }
        }
    }

    @Test
    public void testTidesRatesPastInversion() {
        // on -122502-11-09, the rates for semidiurnal tides 245556 and 245635 cross over
        final TimeScale                    ut1      = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        final FundamentalNutationArguments fna      = IERSConventions.IERS_2010.getNutationArguments(ut1);
        final Tide                         tide1    = new Tide(245556);
        final Tide                         tide2    = new Tide(245635);

        final AbsoluteDate   t0530  = new AbsoluteDate(-122502, 11, 9, 5, 30, 0.0, TimeScalesFactory.getTAI());
        final BodiesElements el0530 = fna.evaluateAll(t0530);
        Assertions.assertTrue(tide1.getRate(el0530) < tide2.getRate(el0530));

        final AbsoluteDate   t0430  = t0530.shiftedBy(-3600.0);
        final BodiesElements el0430 = fna.evaluateAll(t0430);
        Assertions.assertTrue(tide1.getRate(el0430) > tide2.getRate(el0430));

    }

    @Test
    public void testTidesRatesFutureInversion() {
        // on 56824-11-02, the rates for semidiurnal tides 274554 (R₂) and 274556 cross over
        final TimeScale                    ut1      = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        final FundamentalNutationArguments fna      = IERSConventions.IERS_2010.getNutationArguments(ut1);
        final Tide                         tide1    = new Tide(274554);
        final Tide                         tide2    = new Tide(274556);

        final AbsoluteDate   t1700  = new AbsoluteDate(56824, 11, 2, 17, 0, 0.0, TimeScalesFactory.getTAI());
        final BodiesElements el1700 = fna.evaluateAll(t1700);
        Assertions.assertTrue(tide1.getRate(el1700) < tide2.getRate(el1700));

        final AbsoluteDate   t1800  = t1700.shiftedBy(3600.0);
        final BodiesElements el1800 = fna.evaluateAll(t1800);
        Assertions.assertTrue(tide1.getRate(el1800) > tide2.getRate(el1800));

    }

    @Test
    public void testOnsalaOriginalEarthRotation() {
        // this test is the first test case for HARDISP.F program
        doTestOnsala(true, 4.7e-6);
    }

    @Test
    public void testOnsalaIERSEarthRotation() {
        // this test is the first test case for HARDISP.F program
        doTestOnsala(false, 3.4e-6);
    }

    private void doTestOnsala(boolean patchEarthRotation, double tolerance) {
        OceanLoadingCoefficientsBLQFactory factory      = new OceanLoadingCoefficientsBLQFactory("^hardisp\\.blq$");
        OceanLoadingCoefficients           coefficients = factory.getCoefficients("Onsala");
        Vector3D                           refPoint     = earth.transform(coefficients.getSiteLocation());
        OceanLoading                       loading      = new OceanLoading(earth, coefficients);
        TimeScale                          ut1          = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        FundamentalNutationArguments       fna          = IERSConventions.IERS_2010.getNutationArguments(ut1);
        if (patchEarthRotation) {
            TideTest.patchEarthRotationModel(fna, ut1);
        }
        AbsoluteDate                       t0      = new AbsoluteDate("2009-06-25T01:10:45", ut1 );
        double[][] ref = new double[][] {
            {  0.003094, -0.001538, -0.000895 },
            {  0.001812, -0.000950, -0.000193 },
            {  0.000218, -0.000248,  0.000421 },
            { -0.001104,  0.000404,  0.000741 },
            { -0.001668,  0.000863,  0.000646 },
            { -0.001209,  0.001042,  0.000137 },
            {  0.000235,  0.000926, -0.000667 },
            {  0.002337,  0.000580, -0.001555 },
            {  0.004554,  0.000125, -0.002278 },
            {  0.006271, -0.000291, -0.002615 },
            {  0.006955, -0.000537, -0.002430 },
            {  0.006299, -0.000526, -0.001706 },
            {  0.004305, -0.000244, -0.000559 },
            {  0.001294,  0.000245,  0.000793 },
            { -0.002163,  0.000819,  0.002075 },
            { -0.005375,  0.001326,  0.003024 },
            { -0.007695,  0.001622,  0.003448 },
            { -0.008669,  0.001610,  0.003272 },
            { -0.008143,  0.001262,  0.002557 },
            { -0.006290,  0.000633,  0.001477 },
            { -0.003566, -0.000155,  0.000282 },
            { -0.000593, -0.000941, -0.000766 },
            {  0.001992, -0.001561, -0.001457 },
            {  0.003689, -0.001889, -0.001680 }
        };
        for (int i = 0; i < ref.length; ++i) {
            BodiesElements elements = fna.evaluateAll(t0.shiftedBy(i * 3600.0));
            final Vector3D d = loading.displacement(elements, earth.getBodyFrame(), refPoint);
            Assertions.assertEquals(ref[i][0], Vector3D.dotProduct(d, coefficients.getSiteLocation().getZenith()), tolerance);
            Assertions.assertEquals(ref[i][1], Vector3D.dotProduct(d, coefficients.getSiteLocation().getSouth()),  tolerance);
            Assertions.assertEquals(ref[i][2], Vector3D.dotProduct(d, coefficients.getSiteLocation().getWest()),   tolerance);
        }
    }

    @Test
    public void testReykjavikOriginalEarthRotation() {
        // this test is the second test case for HARDISP.F program
        doTestReykjavik(true, 9.3e-6);
    }

    @Test
    public void testReykjavikIERSEarthRotation() {
        // this test is the second test case for HARDISP.F program
        doTestReykjavik(false, 16.9e-6);
    }

    private void doTestReykjavik(boolean patchEarthRotation, double tolerance) {
        // the coordinates for Reykjavik are *known* to be wrong in this test file
        // these test data have been extracted from the HARDISP.F file in September 2017
        // and it seems longitude and latitude have been exchanged...
        // With the file coordinates, Reykjavik would be somewhere in the Indian Ocean, about 1800km East of Madagascar
        // The error has been reported to IERS conventions center.
        OceanLoadingCoefficientsBLQFactory factory      = new OceanLoadingCoefficientsBLQFactory("^hardisp\\.blq$");
        OceanLoadingCoefficients           coefficients = factory.getCoefficients("Reykjavik");
        Vector3D                           refPoint     = earth.transform(coefficients.getSiteLocation());
        OceanLoading                       loading      = new OceanLoading(earth, coefficients);
        TimeScale                          ut1          = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        FundamentalNutationArguments       fna          = IERSConventions.IERS_2010.getNutationArguments(ut1);
        if (patchEarthRotation) {
            TideTest.patchEarthRotationModel(fna, ut1);
        }
        AbsoluteDate                       t0      = new AbsoluteDate("2009-06-25T01:10:45", ut1 );
        double[][] ref = new double[][] {
            { -0.005940, -0.001245, -0.000278 },
            {  0.013516, -0.001086,  0.003212 },
            {  0.029599, -0.000353,  0.005483 },
            {  0.038468,  0.000699,  0.005997 },
            {  0.038098,  0.001721,  0.004690 },
            {  0.028780,  0.002363,  0.001974 },
            {  0.013016,  0.002371, -0.001369 },
            { -0.005124,  0.001653, -0.004390 },
            { -0.021047,  0.000310, -0.006225 },
            { -0.030799, -0.001383, -0.006313 },
            { -0.032056, -0.003048, -0.004549 },
            { -0.024698, -0.004288, -0.001314 },
            { -0.010814, -0.004794,  0.002623 },
            {  0.005849, -0.004416,  0.006291 },
            {  0.020857, -0.003208,  0.008766 },
            {  0.030226, -0.001413,  0.009402 },
            {  0.031437,  0.000594,  0.007996 },
            {  0.024079,  0.002389,  0.004844 },
            {  0.009945,  0.003606,  0.000663 },
            { -0.007426,  0.004022, -0.003581 },
            { -0.023652,  0.003601, -0.006911 },
            { -0.034618,  0.002505, -0.008585 },
            { -0.037515,  0.001044, -0.008270 },
            { -0.031544, -0.000402, -0.006125 }
        };
        for (int i = 0; i < ref.length; ++i) {
            BodiesElements elements = fna.evaluateAll(t0.shiftedBy(i * 3600.0));
            final Vector3D d = loading.displacement(elements, earth.getBodyFrame(), refPoint);
            Assertions.assertEquals(ref[i][0], Vector3D.dotProduct(d, coefficients.getSiteLocation().getZenith()), tolerance);
            Assertions.assertEquals(ref[i][1], Vector3D.dotProduct(d, coefficients.getSiteLocation().getSouth()),  tolerance);
            Assertions.assertEquals(ref[i][2], Vector3D.dotProduct(d, coefficients.getSiteLocation().getWest()),   tolerance);
        }
    }

    private List<Tide> getTides() {
        try {
            Field mapField = OceanLoading.class.getDeclaredField("CARTWRIGHT_EDDEN_AMPLITUDE_MAP");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Tide, Double> map = (Map<Tide, Double>) mapField.get(null);
            return map.entrySet().stream().map(e -> e.getKey()).collect(Collectors.toList());
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            Assertions.fail(e.getLocalizedMessage());
            return null;
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data:oso-blq");
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, false));
    }

}
