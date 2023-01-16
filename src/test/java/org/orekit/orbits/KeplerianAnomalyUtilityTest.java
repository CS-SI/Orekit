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
package org.orekit.orbits;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class KeplerianAnomalyUtilityTest {

    @Test
    public void testEllipticMeanToTrue() {
        final double e = 0.231;
        final double M = 2.045;
        final double v = KeplerianAnomalyUtility.ellipticMeanToTrue(e, M);
        Assertions.assertEquals(2.4004986679372027, v, 1e-14);
    }

    @Test
    public void testEllipticTrueToMean() {
        final double e = 0.487;
        final double v = 1.386;
        final double M = KeplerianAnomalyUtility.ellipticTrueToMean(e, v);
        Assertions.assertEquals(0.5238159114936672, M, 1e-14);
    }

    @Test
    public void testEllipticEccentricToTrue() {
        final double e = 0.687;
        final double E = 4.639;
        final double v = KeplerianAnomalyUtility.ellipticEccentricToTrue(e, E);
        Assertions.assertEquals(3.903008140176819, v, 1e-14);
    }

    @Test
    public void testEllipticTrueToEccentric() {
        final double e = 0.527;
        final double v = 0.768;
        final double E = KeplerianAnomalyUtility.ellipticTrueToEccentric(e, v);
        Assertions.assertEquals(0.44240462411915754, E, 1e-14);
    }

    @Test
    public void testEllipticMeanToEccentric() {
        final double e1 = 0.726;
        final double M1 = 0.;
        final double E1 = KeplerianAnomalyUtility.ellipticMeanToEccentric(e1, M1);
        Assertions.assertEquals(0.0, E1, 1e-14);

        final double e2 = 0.065;
        final double M2 = 4.586;
        final double E2 = KeplerianAnomalyUtility.ellipticMeanToEccentric(e2, M2);
        Assertions.assertEquals(4.522172385101093, E2, 1e-14);

        final double e3 = 0.403;
        final double M3 = 0.121;
        final double E3 = KeplerianAnomalyUtility.ellipticMeanToEccentric(e3, M3);
        Assertions.assertEquals(0.20175794699115656, E3, 1e-14);

        final double e4 = 0.999;
        final double M4 = 0.028;
        final double E4 = KeplerianAnomalyUtility.ellipticMeanToEccentric(e4, M4);
        Assertions.assertEquals(0.5511071508829587, E4, 1e-14);
    }

    @Test
    public void testEllipticEccentricToMean() {
        final double e = 0.192;
        final double E = 2.052;
        final double M = KeplerianAnomalyUtility.ellipticEccentricToMean(e, E);
        Assertions.assertEquals(1.881803817764882, M, 1e-14);
    }

    @Test
    public void testHyperbolicMeanToTrue() {
        final double e = 1.027;
        final double M = 1.293;
        final double v = KeplerianAnomalyUtility.hyperbolicMeanToTrue(e, M);
        Assertions.assertEquals(2.8254185280004855, v, 1e-14);
    }

    @Test
    public void testHyperbolicTrueToMean() {
        final double e = 1.161;
        final double v = -2.469;
        final double M = KeplerianAnomalyUtility.hyperbolicTrueToMean(e, v);
        Assertions.assertEquals(-2.5499244818919915, M, 1e-14);
    }

    @Test
    public void testHyperbolicEccentricToTrue() {
        final double e = 2.161;
        final double E = -1.204;
        final double v = KeplerianAnomalyUtility.hyperbolicEccentricToTrue(e, E);
        Assertions.assertEquals(-1.4528528149658333, v, 1e-14);
    }

    @Test
    public void testHyperbolicTrueToEccentric() {
        final double e = 1.595;
        final double v = 0.298;
        final double E = KeplerianAnomalyUtility.hyperbolicTrueToEccentric(e, v);
        Assertions.assertEquals(0.1440079208139455, E, 1e-14);
    }

    @Test
    public void testHyperbolicMeanToEccentric() {
        final double e1 = 1.201;
        final double M1 = 0.0;
        final double E1 = KeplerianAnomalyUtility.hyperbolicMeanToEccentric(e1, M1);
        Assertions.assertEquals(0.0, E1, 1e-14);

        final double e2 = 1.127;
        final double M2 = -3.624;
        final double E2 = KeplerianAnomalyUtility.hyperbolicMeanToEccentric(e2, M2);
        Assertions.assertEquals(-2.3736718687722265, E2, 1e-14);

        final double e3 = 1.338;
        final double M3 = -0.290;
        final double E3 = KeplerianAnomalyUtility.hyperbolicMeanToEccentric(e3, M3);
        Assertions.assertEquals(-0.6621795141831807, E3, 1e-14);

        final double e4 = 1.044;
        final double M4 = 3.996;
        final double E4 = KeplerianAnomalyUtility.hyperbolicMeanToEccentric(e4, M4);
        Assertions.assertEquals(2.532614977388778, E4, 1e-14);

        final double e5 = 2.052;
        final double M5 = 4.329;
        final double E5 = KeplerianAnomalyUtility.hyperbolicMeanToEccentric(e5, M5);
        Assertions.assertEquals(1.816886788278918, E5, 1e-14);

        final double e6 = 2.963;
        final double M6 = -1.642;
        final double E6 = KeplerianAnomalyUtility.hyperbolicMeanToEccentric(e6, M6);
        Assertions.assertEquals(-0.7341946491456494, E6, 1e-14);

        final double e7 = 4.117;
        final double M7 = -0.286;
        final double E7 = KeplerianAnomalyUtility.hyperbolicMeanToEccentric(e7, M7);
        Assertions.assertEquals(-0.09158570899196887, E7, 1e-14);

        // Issue 951.
        final double e8 = 1.251844925917281;
        final double M8 = 54.70111712786907;
        final double E8 = KeplerianAnomalyUtility.hyperbolicMeanToEccentric(e8, M8);
        Assertions.assertEquals(4.550432282228856, E8, 1e-14);
    }

    @Test
    public void testHyperbolicEccentricToMean() {
        final double e = 1.801;
        final double E = 3.287;
        final double M = KeplerianAnomalyUtility.hyperbolicEccentricToMean(e, E);
        Assertions.assertEquals(20.77894350750361, M, 1e-14);
    }

    @Test
    public void testIssue544() {
        // Initial parameters
        // In order to test the issue, we voluntarily set the anomaly at Double.NaN.
        double e = 0.7311;
        double anomaly = Double.NaN;
        // Computes the elliptic eccentric anomaly
        double E = KeplerianAnomalyUtility.ellipticMeanToEccentric(e, anomaly);
        // Verify that an infinite loop did not occur
        Assertions.assertTrue(Double.isNaN(E));
    }

}
