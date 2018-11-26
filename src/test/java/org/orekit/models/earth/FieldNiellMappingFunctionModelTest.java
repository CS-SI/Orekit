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
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class FieldNiellMappingFunctionModelTest {

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

        // Site (Le Mans, France):      latitude:  48.0°
        //                              longitude: 0.20°
        //                              height:    68 m
        //
        // Date: 1st January 1994 at 0h UT
        //
        // Ref: Mercier F., Perosanz F., Mesures GNSS, Résolution des ambiguités.
        //
        // Expected mapping factors : hydrostatic -> 10.16 (Ref)
        //                                    wet -> 10.75 (Ref)

        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 1994, 1, 1, TimeScalesFactory.getUTC());
        
        final double latitude    = FastMath.toRadians(48.0);
        final double height      = 68.0;

        final double elevation     = FastMath.toRadians(5.0);
        final double expectedHydro = 10.16;
        final double expectedWet   = 10.75;

        final MappingFunction model = new NiellMappingFunctionModel(latitude);
        
        final T[] computedMapping = model.mappingFactors(zero.add(height), zero.add(elevation), date, model.getParameters(field));
        
        Assert.assertEquals(expectedHydro, computedMapping[0].getReal(), 1.0e-2);
        Assert.assertEquals(expectedWet,   computedMapping[1].getReal(), 1.0e-2);
    }

    @Test
    public void testFixedHeight() {
        doTestFixedHeight(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFixedHeight(final Field<T> field) {
        final T zero = field.getZero();
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        MappingFunction model = new NiellMappingFunctionModel(FastMath.toRadians(45.0));
        T[] lastFactors = MathArrays.buildArray(field, 2);
        lastFactors[0] = zero.add(Double.MAX_VALUE);
        lastFactors[1] = zero.add(Double.MAX_VALUE);
        // mapping functions shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final T[] factors = model.mappingFactors(zero.add(350), zero.add(FastMath.toRadians(elev)),
                                                     date, model.getParameters(field));
            Assert.assertTrue(Precision.compareTo(factors[0].getReal(), lastFactors[0].getReal(), 1.0e-6) < 0);
            Assert.assertTrue(Precision.compareTo(factors[1].getReal(), lastFactors[1].getReal(), 1.0e-6) < 0);
            lastFactors[0] = factors[0];
            lastFactors[1] = factors[1];
        }
    }

}
