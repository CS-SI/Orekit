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
package org.orekit.forces.gravity.potential;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;

public class OceanLoadDeformationCoefficientsTest {

    @Test
    public void testIERS1996EqualsIERS2003()
        throws OrekitException {
        double[] coeff1996 = OceanLoadDeformationCoefficients.IERS_1996.getCoefficients();
        double[] coeff2003 = OceanLoadDeformationCoefficients.IERS_2003.getCoefficients();
        Assert.assertEquals(coeff1996.length, coeff2003.length);
        for (int i = 0; i < coeff1996.length; ++i) {
            Assert.assertEquals(coeff1996[i], coeff2003[i], 1.0e-15);
        }
    }

    @Test
    public void testIERS1996EqualsIERS2010()
        throws OrekitException {
        double[] coeff1996 = OceanLoadDeformationCoefficients.IERS_1996.getCoefficients();
        double[] coeff2010 = OceanLoadDeformationCoefficients.IERS_2010.getCoefficients();
        Assert.assertEquals(coeff1996.length, coeff2010.length);
        for (int i = 0; i < coeff1996.length; ++i) {
            Assert.assertEquals(coeff1996[i], coeff2010[i], 1.0e-15);
        }
    }

    @Test
    public void testGegoutHighDegree()
        throws OrekitException {
        Assert.assertEquals(251, OceanLoadDeformationCoefficients.GEGOUT.getCoefficients().length);
    }

    @Test
    public void testGegoutNotEqualToIERS()
        throws OrekitException {
        double[] coeff1996   = OceanLoadDeformationCoefficients.IERS_1996.getCoefficients();
        double[] coeffGegout = OceanLoadDeformationCoefficients.GEGOUT.getCoefficients();
        for (int i = 0; i < coeff1996.length; ++i) {
            if (coeff1996[i] == 0) {
                Assert.assertEquals(0.0, coeffGegout[i], 1.0e-15);
            } else {
                Assert.assertTrue(FastMath.abs(coeff1996[i] - coeffGegout[i]) > FastMath.abs(coeff1996[i]) / 200);
            }
        }
    }

}
