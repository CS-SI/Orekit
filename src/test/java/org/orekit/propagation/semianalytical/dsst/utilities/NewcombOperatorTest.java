/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.utilities;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;

public class NewcombOperatorTest {

    @Test
    public void recursionTest() throws OrekitException {

        for (int n = 2; n < 10; n++) {
            final int mnm1 = -n - 1;
            for (int s = 0; s < 10; s++) {
                final double newcomb10 = NewcombOperators.getValue(1, 0, mnm1, s);
                final double newcalc10 = (s - mnm1 / 2.);
                Assert.assertEquals(newcalc10, newcomb10, 0.);
                final double newcomb11 = NewcombOperators.getValue(1, 1, mnm1, s);
                final double newcalc11 = (-8 * s * s + 2 * mnm1 * mnm1 + 10 * mnm1 + 12) / 8.;
                Assert.assertEquals(newcalc11, newcomb11, 0.);
                final double newcomb20 = NewcombOperators.getValue(2, 0, mnm1, s);
                final double newcalc20 = (4. * s * s + 5 * s - 4 * mnm1 * s + mnm1 * mnm1 - 3 * mnm1) / 8.;
                Assert.assertEquals(newcalc20, newcomb20, 0.);
                final double newcomb02 = NewcombOperators.getValue(0, 2, mnm1, s);
                final double newcalc02 = (4. * s * s - 5 * s + 4 * mnm1 * s + mnm1 * mnm1 - 3 * mnm1) / 8.;
                Assert.assertEquals(newcalc02, newcomb02, 0.);
                final double newcomb21 = NewcombOperators.getValue(2, 1, mnm1, s);
                final double newcalc21 = -s*s*s/2. + (mnm1*s*s)/4. - (5.*s*s)/8. + (mnm1*mnm1*s)/8. + (21.*mnm1*s)/16. + (11*s)/8. - mnm1*mnm1*mnm1/16. - (7.*mnm1*mnm1)/16. - (9.*mnm1)/16.;
                Assert.assertEquals(newcalc21, newcomb21, 1.e-14);
                final double newcomb12 = NewcombOperators.getValue(1, 2, mnm1, s);
                final double newcalc12 =  s*s*s/2. + (mnm1*s*s)/4. - (5.*s*s)/8. - (mnm1*mnm1*s)/8. - (21.*mnm1*s)/16. - (11*s)/8. - mnm1*mnm1*mnm1/16. - (7.*mnm1*mnm1)/16. - (9.*mnm1)/16.;
                Assert.assertEquals(newcalc12, newcomb12, 1.e-14);
                final double newcomb22 = NewcombOperators.getValue(2, 2, mnm1, s);
                final double newcalc22 = s*s*s*s/4. - (mnm1*mnm1*s*s)/8. - mnm1*s*s - (105.*s*s)/64. + mnm1*mnm1*mnm1*mnm1/64. + (7.*mnm1*mnm1*mnm1)/32. + (71.*mnm1*mnm1)/64. + (77.*mnm1)/32. + 15./8.;
                Assert.assertEquals(newcalc22, newcomb22, 1.e-14);
            }
        }
    }

    @Test
    public void valueRefTest() throws OrekitException {
        final int n   = -17;
        final int s   = 14;
        final int rho = 12;
        final int sig = 12;
        final double value = NewcombOperators.getValue(rho, sig, n, s);
        Assert.assertEquals(value, 90061805802.16286, 0.1);
    }

    @Before
    public void setUp() {
        Utils.clearFactories();
    }

}
