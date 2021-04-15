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
package org.orekit.propagation.analytical.tle;

import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Test;

public class TLEConstantsTest {

    @Test
    public void testCoverage() {
        // this test is specially awkward
        // it doesn't really test anything…
        // its *sole* purpose is to work around a false positive coverage in SonarQube
        Assert.assertTrue(Precision.equals(1.0 / 3.0,                            TLEConstants.ONE_THIRD,                    1));
        Assert.assertTrue(Precision.equals(2.0 / 3.0,                            TLEConstants.TWO_THIRD,                    1));
        Assert.assertTrue(Precision.equals(6378.135,                             TLEConstants.EARTH_RADIUS,                 1));
        Assert.assertTrue(Precision.equals(1.0,                                  TLEConstants.NORMALIZED_EQUATORIAL_RADIUS, 1));
        Assert.assertTrue(Precision.equals(1440.0,                               TLEConstants.MINUTES_PER_DAY,              1));
        Assert.assertTrue(Precision.equals(0.0743669161331734132,                TLEConstants.XKE,                          1));
        Assert.assertTrue(Precision.equals(-2.53881e-6,                          TLEConstants.XJ3,                          1));
        Assert.assertTrue(Precision.equals(1.082616e-3,                          TLEConstants.XJ2,                          1));
        Assert.assertTrue(Precision.equals(-1.65597e-6,                          TLEConstants.XJ4,                          1));
        Assert.assertTrue(Precision.equals(0.5 * TLEConstants.XJ2,               TLEConstants.CK2,                          1));
        Assert.assertTrue(Precision.equals(-0.375 * TLEConstants.XJ4,            TLEConstants.CK4,                          1));
        Assert.assertTrue(Precision.equals(1. + 78. / TLEConstants.EARTH_RADIUS, TLEConstants.S,                            1));
        Assert.assertTrue(Precision.equals(1.880279159015270643865e-9,           TLEConstants.QOMS2T,                       1));
        Assert.assertTrue(Precision.equals(-TLEConstants.XJ3 / TLEConstants.CK2, TLEConstants.A3OVK2,                       1));
        Assert.assertTrue(Precision.equals(1.19459E-5,                           TLEConstants.ZNS,                          1));
        Assert.assertTrue(Precision.equals(0.01675,                              TLEConstants.ZES,                          1));
        Assert.assertTrue(Precision.equals(1.5835218E-4,                         TLEConstants.ZNL,                          1));
        Assert.assertTrue(Precision.equals(0.05490,                              TLEConstants.ZEL,                          1));
        Assert.assertTrue(Precision.equals(4.3752691E-3,                         TLEConstants.THDT,                         1));
        Assert.assertTrue(Precision.equals(2.9864797E-6,                         TLEConstants.C1SS,                         1));
        Assert.assertTrue(Precision.equals(4.7968065E-7,                         TLEConstants.C1L,                          1));
        Assert.assertTrue(Precision.equals(1.7891679E-6,                         TLEConstants.ROOT22,                       1));
        Assert.assertTrue(Precision.equals(3.7393792E-7,                         TLEConstants.ROOT32,                       1));
        Assert.assertTrue(Precision.equals(7.3636953E-9,                         TLEConstants.ROOT44,                       1));
        Assert.assertTrue(Precision.equals(1.1428639E-7,                         TLEConstants.ROOT52,                       1));
        Assert.assertTrue(Precision.equals(2.1765803E-9,                         TLEConstants.ROOT54,                       1));
        Assert.assertTrue(Precision.equals(1.7891679E-6,                         TLEConstants.Q22,                          1));
        Assert.assertTrue(Precision.equals(2.1460748E-6,                         TLEConstants.Q31,                          1));
        Assert.assertTrue(Precision.equals(2.2123015E-7,                         TLEConstants.Q33,                          1));
        Assert.assertTrue(Precision.equals(0.99139134268488593,                  TLEConstants.C_FASX2,                      1));
        Assert.assertTrue(Precision.equals(0.13093206501640101,                  TLEConstants.S_FASX2,                      1));
        Assert.assertTrue(Precision.equals(0.87051638752972937,                  TLEConstants.C_2FASX4,                     1));
        Assert.assertTrue(Precision.equals(-0.49213943048915526,                 TLEConstants.S_2FASX4,                     1));
        Assert.assertTrue(Precision.equals(0.43258117585763334,                  TLEConstants.C_3FASX6,                     1));
        Assert.assertTrue(Precision.equals(0.90159499016666422,                  TLEConstants.S_3FASX6,                     1));
        Assert.assertTrue(Precision.equals(0.87051638752972937,                  TLEConstants.C_G22,                        1));
        Assert.assertTrue(Precision.equals(-0.49213943048915526,                 TLEConstants.S_G22,                        1));
        Assert.assertTrue(Precision.equals(0.57972190187001149,                  TLEConstants.C_G32,                        1));
        Assert.assertTrue(Precision.equals(0.81481440616389245,                  TLEConstants.S_G32,                        1));
        Assert.assertTrue(Precision.equals(-0.22866241528815548,                 TLEConstants.C_G44,                        1));
        Assert.assertTrue(Precision.equals(0.97350577801807991,                  TLEConstants.S_G44,                        1));
        Assert.assertTrue(Precision.equals(0.49684831179884198,                  TLEConstants.C_G52,                        1));
        Assert.assertTrue(Precision.equals(0.86783740128127729,                  TLEConstants.S_G52,                        1));
        Assert.assertTrue(Precision.equals(-0.29695209575316894,                 TLEConstants.C_G54,                        1));
        Assert.assertTrue(Precision.equals(-0.95489237761529999,                 TLEConstants.S_G54,                        1));
    }

}