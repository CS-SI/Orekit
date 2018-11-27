/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.models.earth;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class FieldViennaThreeModelTest {

    private static double epsilon = 1e-6;

    @BeforeClass
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

    @Test
    public void testMappingFactors() {
        doTestMappingFactors(Decimal64Field.getInstance());
    }
    
    private <T extends RealFieldElement<T>> void doTestMappingFactors(final Field<T> field) {
        
        final T zero = field.getZero();

        // Site:     latitude:  37.5°
        //           longitude: 277.5°
        //           height:    824 m
        //
        // Date:     25 November 2018 at 0h UT
        //
        // Values: ah  = 0.00123462
        //         aw  = 0.00047101
        //         zhd = 2.1993 m
        //         zwd = 0.0690 m
        //
        // Values taken from: http://vmf.geo.tuwien.ac.at/trop_products/GRID/5x5/VMF3/VMF3_OP/2018/VMF3_20181125.H00
        //
        // Expected mapping factors : hydrostatic -> 1.621024
        //                                    wet -> 1.623023
        //
        // Expected outputs are obtained by performing the Matlab script vmf3.m provided by TU WIEN:
        // http://vmf.geo.tuwien.ac.at/codes/
        //

        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2018, 11, 25, TimeScalesFactory.getUTC());
        
        final double latitude    = FastMath.toRadians(37.5);
        final double longitude   = FastMath.toRadians(277.5);
        final double height      = 824.0;

        final double elevation     = FastMath.toRadians(38.0);
        final double expectedHydro = 1.621024;
        final double expectedWet   = 1.623023;
        
        final double[] a = {0.00123462, 0.00047101};
        final double[] z = {2.1993, 0.0690};
        
        final ViennaThreeModel model = new ViennaThreeModel(a, z, latitude, longitude);
        
        final T[] computedMapping = model.mappingFactors(zero.add(height), zero.add(elevation),
                                                         date, model.getParameters(field));
        
        Assert.assertEquals(expectedHydro, computedMapping[0].getReal(), epsilon);
        Assert.assertEquals(expectedWet,   computedMapping[1].getReal(), epsilon);
    }

    @Test
    public void testLowElevation() {
        doTestLowElevation(Decimal64Field.getInstance());        
    }

    private <T extends RealFieldElement<T>> void doTestLowElevation(final Field<T> field) {
        
        final T zero = field.getZero();

        // Site:     latitude:  37.5°
        //           longitude: 277.5°
        //           height:    824 m
        //
        // Date:     25 November 2018 at 0h UT
        //
        // Values: ah  = 0.00123462
        //         aw  = 0.00047101
        //         zhd = 2.1993 m
        //         zwd = 0.0690 m
        //
        // Values taken from: http://vmf.geo.tuwien.ac.at/trop_products/GRID/5x5/VMF3/VMF3_OP/2018/VMF3_20181125.H00
        //
        // Expected mapping factors : hydrostatic -> 10.132802
        //                                    wet -> 10.879154
        //
        // Expected outputs are obtained by performing the Matlab script vmf3.m provided by TU WIEN:
        // http://vmf.geo.tuwien.ac.at/codes/
        //

        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2018, 11, 25, TimeScalesFactory.getUTC());
        
        final double latitude    = FastMath.toRadians(37.5);
        final double longitude   = FastMath.toRadians(277.5);
        final double height      = 824.0;

        final double elevation     = FastMath.toRadians(5.0);
        final double expectedHydro = 10.132802;
        final double expectedWet   = 10.879154;
        
        final double[] a = {0.00123462, 0.00047101};
        final double[] z = {2.1993, 0.0690};
        
        final ViennaThreeModel model = new ViennaThreeModel(a, z, latitude, longitude);
        
        final T[] computedMapping = model.mappingFactors(zero.add(height), zero.add(elevation),
                                                         date, model.getParameters(field));
        
        Assert.assertEquals(expectedHydro, computedMapping[0].getReal(), epsilon);
        Assert.assertEquals(expectedWet,   computedMapping[1].getReal(), epsilon);
    }

    @Test
    public void testHightElevation() {
        doTestHightElevation(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestHightElevation(final Field<T> field) {

        final T zero = field.getZero();

        // Site:     latitude:  37.5°
        //           longitude: 277.5°
        //           height:    824 m
        //
        // Date:     25 November 2018 at 0h UT
        //
        // Values: ah  = 0.00123462
        //         aw  = 0.00047101
        //         zhd = 2.1993 m
        //         zwd = 0.0690 m
        //
        // Values taken from: http://vmf.geo.tuwien.ac.at/trop_products/GRID/5x5/VMF3/VMF3_OP/2018/VMF3_20181125.H00
        //
        // Expected mapping factors : hydrostatic -> 1.003810
        //                                    wet -> 1.003816
        //
        // Expected outputs are obtained by performing the Matlab script vmf3.m provided by TU WIEN:
        // http://vmf.geo.tuwien.ac.at/codes/
        //

        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2018, 11, 25, TimeScalesFactory.getUTC());
        
        final double latitude    = FastMath.toRadians(37.5);
        final double longitude   = FastMath.toRadians(277.5);
        final double height      = 824.0;

        final double elevation     = FastMath.toRadians(85.0);
        final double expectedHydro = 1.003810;
        final double expectedWet   = 1.003816;
        
        final double[] a = {0.00123462, 0.00047101};
        final double[] z = {2.1993, 0.0690};
        
        final ViennaThreeModel model = new ViennaThreeModel(a, z, latitude, longitude);
        
        final T[] computedMapping = model.mappingFactors(zero.add(height), zero.add(elevation),
                                                         date, model.getParameters(field));
        
        Assert.assertEquals(expectedHydro, computedMapping[0].getReal(), epsilon);
        Assert.assertEquals(expectedWet,   computedMapping[1].getReal(), epsilon);
    }

    @Test
    public void testDelay() {
        doTestDelay(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestDelay(final Field<T> field) {
        final T zero = field.getZero();
        final double elevation = 10d;
        final double height = 100d;
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final double[] a = { 0.00123462, 0.00047101};
        final double[] z = {2.1993, 0.0690};
        ViennaThreeModel model = new ViennaThreeModel(a, z, FastMath.toRadians(37.5), FastMath.toRadians(277.5));
        final T path = model.pathDelay(zero.add(FastMath.toRadians(elevation)), zero.add(height),
                                       model.getParameters(field), date);
        Assert.assertTrue(Precision.compareTo(path.getReal(), 20d, epsilon) < 0);
        Assert.assertTrue(Precision.compareTo(path.getReal(), 0d, epsilon) > 0);
    }

    @Test
    public void testFixedHeight() {
        doTestFixedHeight(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFixedHeight(final Field<T> field) {
        final T zero = field.getZero();
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        final double[] a = { 0.00123462, 0.00047101};
        final double[] z = {2.1993, 0.0690};
        ViennaThreeModel model = new ViennaThreeModel(a, z, FastMath.toRadians(37.5), FastMath.toRadians(277.5));
        T lastDelay = zero.add(Double.MAX_VALUE);
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final T delay = model.pathDelay(zero.add(FastMath.toRadians(elev)), zero.add(350),
                                            model.getParameters(field), date);
            Assert.assertTrue(Precision.compareTo(delay.getReal(), lastDelay.getReal(), epsilon) < 0);
            lastDelay = delay;
        }
    }

}
