/* Copyright 2002-2023 CS GROUP
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
package org.orekit.forces.gravity.potential;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OceanLoadDeformationCoefficientsTest {

    @Test
    public void testIERS1996EqualsIERS2003()
        {
        double[] coeff1996 = OceanLoadDeformationCoefficients.IERS_1996.getCoefficients();
        double[] coeff2003 = OceanLoadDeformationCoefficients.IERS_2003.getCoefficients();
        Assertions.assertEquals(coeff1996.length, coeff2003.length);
        for (int i = 0; i < coeff1996.length; ++i) {
            Assertions.assertEquals(coeff1996[i], coeff2003[i], 1.0e-15);
        }
    }

    @Test
    public void testIERS1996EqualsIERS2010()
        {
        double[] coeff1996 = OceanLoadDeformationCoefficients.IERS_1996.getCoefficients();
        double[] coeff2010 = OceanLoadDeformationCoefficients.IERS_2010.getCoefficients();
        Assertions.assertEquals(coeff1996.length, coeff2010.length);
        for (int i = 0; i < coeff1996.length; ++i) {
            Assertions.assertEquals(coeff1996[i], coeff2010[i], 1.0e-15);
        }
    }

    @Test
    public void testGegoutHighDegree()
        {
        Assertions.assertEquals(251, OceanLoadDeformationCoefficients.GEGOUT.getCoefficients().length);
    }

    @Test
    public void testGegoutNotEqualToIERS()
        {
        double[] coeff1996   = OceanLoadDeformationCoefficients.IERS_1996.getCoefficients();
        double[] coeffGegout = OceanLoadDeformationCoefficients.GEGOUT.getCoefficients();
        for (int i = 0; i < coeff1996.length; ++i) {
            if (coeff1996[i] == 0) {
                Assertions.assertEquals(0.0, coeffGegout[i], 1.0e-15);
            } else {
                Assertions.assertTrue(FastMath.abs(coeff1996[i] - coeffGegout[i]) > FastMath.abs(coeff1996[i]) / 200);
            }
        }
    }

}
