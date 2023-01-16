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
package org.orekit.utils;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;

public class DifferentiationTest {

    @Test
    public void testScaleOne() {
        // with this step, computation is exact in IEEE754
        doTestScale(1.0, FastMath.pow(1.0, -3), Precision.SAFE_MIN);
    }

    @Test
    public void testScalePowerOfTwoStepRepresentableNumber() {
        // with this step, computation is exact in IEEE754
        doTestScale(FastMath.scalb(1.0, -10), FastMath.pow(1.0, -7), Precision.SAFE_MIN);
    }

    @Test
    public void testScalePowerOfTwoStepNonRepresentableNumber() {
        // with this step, computation has numerical noise
        doTestScale(FastMath.scalb(1.0, -10), 0.007, 1.7e-12);
    }

    private void doTestScale(final double scale, final double step, final double tolerance) {
        ParameterDriver   driver = new ParameterDriver("", -100.0, scale,
                                                       Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        ParameterFunction f0     = (d,t) -> 3 * d.getValue(t) * d.getValue(t) - 2 * d.getValue(t);
        ParameterFunction f1Diff = Differentiation.differentiate(f0, 4, step);
        ParameterFunction f1Ref  = (d,t) -> 6 * d.getValue(t) - 2;

        for (double x = -3.0; x < 3.0; x += 0.125) {
            driver.setValue(x);
            Assertions.assertEquals(f1Ref.value(driver, new AbsoluteDate()), f1Diff.value(driver, new AbsoluteDate()), tolerance);
        }

    }

}

