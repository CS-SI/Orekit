/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.estimation.measurements.gnss;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.utils.ParameterDriver;

public class AmbiguitySolverTest {

    @Test
    public void testJoostenTiberiusFAQ() {
        // this test corresponds to the "LAMBDA: FAQs" paper by Peter Joosten and Christian Tiberius

        final List<ParameterDriver> ambiguitiesDrivers = createAmbiguities(5.450, 3.100, 2.970);
        final RealMatrix covariance = MatrixUtils.createRealMatrix(new double[][] {
            { 6.290, 5.978, 0.544 },
            { 5.978, 6.292, 2.340 },
            { 0.544, 2.340, 6.288 }
        });

        // acceptance ratio not met
        Assert.assertTrue(new AmbiguitySolver(ambiguitiesDrivers, new LambdaMethod(),
                                              new SimpleRatioAmbiguityAcceptance(0.5)).
                          fixIntegerAmbiguities(0, ambiguitiesDrivers, covariance).
                          isEmpty());

        List<ParameterDriver> fixed = new AmbiguitySolver(ambiguitiesDrivers, new LambdaMethod(),
                                                          new SimpleRatioAmbiguityAcceptance(0.8)).
                                      fixIntegerAmbiguities(0, ambiguitiesDrivers, covariance);
        Assert.assertEquals(3, fixed.size());
        Assert.assertEquals(5, fixed.get(0).getValue(), 1.0e-15);
        Assert.assertEquals(3, fixed.get(1).getValue(), 1.0e-15);
        Assert.assertEquals(4, fixed.get(2).getValue(), 1.0e-15);
    }

    private List<ParameterDriver> createAmbiguities(double...floatValues) {
        final List<ParameterDriver> ambiguitiesDrivers = new ArrayList<>(floatValues.length);
        for (int i = 0; i < floatValues.length; ++i) {
            final ParameterDriver driver = new ParameterDriver(Phase.AMBIGUITY_NAME + i, 0.0, 1.0,
                                                               Double.NEGATIVE_INFINITY,
                                                               Double.POSITIVE_INFINITY);
            driver.setValue(floatValues[i]);
            driver.setSelected(true);
            ambiguitiesDrivers.add(driver);
        }
        return ambiguitiesDrivers;
    }

}

