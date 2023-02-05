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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FieldKeplerianAnomalyUtilityTest {

    @Test
    public void testEllipticMeanToTrue() {
        doTestEllipticMeanToTrue(Binary64Field.getInstance());
    }

    @Test
    public void testEllipticTrueToMean() {
        doTestEllipticTrueToMean(Binary64Field.getInstance());
    }

    @Test
    public void testEllipticEccentricToTrue() {
        doTestEllipticEccentricToTrue(Binary64Field.getInstance());
    }

    @Test
    public void testEllipticTrueToEccentric() {
        doTestEllipticTrueToEccentric(Binary64Field.getInstance());
    }

    @Test
    public void testEllipticMeanToEccentric() {
        doTestEllipticMeanToEccentric(Binary64Field.getInstance());
    }

    @Test
    public void testEllipticEccentricToMean() {
        doTestEllipticEccentricToMean(Binary64Field.getInstance());
    }

    @Test
    public void testHyperbolicMeanToTrue() {
        doTestHyperbolicMeanToTrue(Binary64Field.getInstance());
    }

    @Test
    public void testHyperbolicTrueToMean() {
        doTestHyperbolicTrueToMean(Binary64Field.getInstance());
    }

    @Test
    public void testHyperbolicEccentricToTrue() {
        doTestHyperbolicEccentricToTrue(Binary64Field.getInstance());
    }

    @Test
    public void testHyperbolicTrueToEccentric() {
        doTestHyperbolicTrueToEccentric(Binary64Field.getInstance());
    }

    @Test
    public void testHyperbolicMeanToEccentric() {
        doTestHyperbolicMeanToEccentric(Binary64Field.getInstance());
    }

    @Test
    public void testHyperbolicEccentricToMean() {
        doTestHyperbolicEccentricToMean(Binary64Field.getInstance());
    }

    @Test
    public void testIssue544() {
        doTestIssue544(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestEllipticMeanToTrue(final Field<T> field) {
        final T e = field.getZero().add(0.231);
        final T M = field.getZero().add(2.045);
        final T v = FieldKeplerianAnomalyUtility.ellipticMeanToTrue(e, M);
        Assertions.assertEquals(2.4004986679372027, v.getReal(), 1e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestEllipticTrueToMean(final Field<T> field) {
        final T e = field.getZero().add(0.487);
        final T v = field.getZero().add(1.386);
        final T M = FieldKeplerianAnomalyUtility.ellipticTrueToMean(e, v);
        Assertions.assertEquals(0.5238159114936672, M.getReal(), 1e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestEllipticEccentricToTrue(final Field<T> field) {
        final T e = field.getZero().add(0.687);
        final T E = field.getZero().add(4.639);
        final T v = FieldKeplerianAnomalyUtility.ellipticEccentricToTrue(e, E);
        Assertions.assertEquals(3.903008140176819, v.getReal(), 1e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestEllipticTrueToEccentric(final Field<T> field) {
        final T e = field.getZero().add(0.527);
        final T v = field.getZero().add(0.768);
        final T E = FieldKeplerianAnomalyUtility.ellipticTrueToEccentric(e, v);
        Assertions.assertEquals(0.44240462411915754, E.getReal(), 1e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestEllipticMeanToEccentric(final Field<T> field) {
        final T e1 = field.getZero().add(0.726);
        final T M1 = field.getZero().add(0.);
        final T E1 = FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(e1, M1);
        Assertions.assertEquals(0.0, E1.getReal(), 1e-14);

        final T e2 = field.getZero().add(0.065);
        final T M2 = field.getZero().add(4.586);
        final T E2 = FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(e2, M2);
        Assertions.assertEquals(4.522172385101093, E2.getReal(), 1e-14);

        final T e3 = field.getZero().add(0.403);
        final T M3 = field.getZero().add(0.121);
        final T E3 = FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(e3, M3);
        Assertions.assertEquals(0.20175794699115656, E3.getReal(), 1e-14);

        final T e4 = field.getZero().add(0.999);
        final T M4 = field.getZero().add(0.028);
        final T E4 = FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(e4, M4);
        Assertions.assertEquals(0.5511071508829587, E4.getReal(), 1e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestEllipticEccentricToMean(final Field<T> field) {
        final T e = field.getZero().add(0.192);
        final T E = field.getZero().add(2.052);
        final T M = FieldKeplerianAnomalyUtility.ellipticEccentricToMean(e, E);
        Assertions.assertEquals(1.881803817764882, M.getReal(), 1e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestHyperbolicMeanToTrue(final Field<T> field) {
        final T e = field.getZero().add(1.027);
        final T M = field.getZero().add(1.293);
        final T v = FieldKeplerianAnomalyUtility.hyperbolicMeanToTrue(e, M);
        Assertions.assertEquals(2.8254185280004855, v.getReal(), 1e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestHyperbolicTrueToMean(final Field<T> field) {
        final T e = field.getZero().add(1.161);
        final T v = field.getZero().add(-2.469);
        final T M = FieldKeplerianAnomalyUtility.hyperbolicTrueToMean(e, v);
        Assertions.assertEquals(-2.5499244818919915, M.getReal(), 1e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestHyperbolicEccentricToTrue(final Field<T> field) {
        final T e = field.getZero().add(2.161);
        final T E = field.getZero().add(-1.204);
        final T v = FieldKeplerianAnomalyUtility.hyperbolicEccentricToTrue(e, E);
        Assertions.assertEquals(-1.4528528149658333, v.getReal(), 1e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestHyperbolicTrueToEccentric(final Field<T> field) {
        final T e = field.getZero().add(1.595);
        final T v = field.getZero().add(0.298);
        final T E = FieldKeplerianAnomalyUtility.hyperbolicTrueToEccentric(e, v);
        Assertions.assertEquals(0.1440079208139455, E.getReal(), 1e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestHyperbolicMeanToEccentric(final Field<T> field) {
        final T e1 = field.getZero().add(1.201);
        final T M1 = field.getZero();
        final T E1 = FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(e1, M1);
        Assertions.assertEquals(0.0, E1.getReal(), 1e-14);

        final T e2 = field.getZero().add(1.127);
        final T M2 = field.getZero().add(-3.624);
        final T E2 = FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(e2, M2);
        Assertions.assertEquals(-2.3736718687722265, E2.getReal(), 1e-14);

        final T e3 = field.getZero().add(1.338);
        final T M3 = field.getZero().add(-0.290);
        final T E3 = FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(e3, M3);
        Assertions.assertEquals(-0.6621795141831807, E3.getReal(), 1e-14);

        final T e4 = field.getZero().add(1.044);
        final T M4 = field.getZero().add(3.996);
        final T E4 = FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(e4, M4);
        Assertions.assertEquals(2.532614977388778, E4.getReal(), 1e-14);

        final T e5 = field.getZero().add(2.052);
        final T M5 = field.getZero().add(4.329);
        final T E5 = FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(e5, M5);
        Assertions.assertEquals(1.816886788278918, E5.getReal(), 1e-14);

        final T e6 = field.getZero().add(2.963);
        final T M6 = field.getZero().add(-1.642);
        final T E6 = FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(e6, M6);
        Assertions.assertEquals(-0.7341946491456494, E6.getReal(), 1e-14);

        final T e7 = field.getZero().add(4.117);
        final T M7 = field.getZero().add(-0.286);
        final T E7 = FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(e7, M7);
        Assertions.assertEquals(-0.09158570899196887, E7.getReal(), 1e-14);

        // Issue 951.
        final T e8 = field.getZero().add(1.251844925917281);
        final T M8 = field.getZero().add(54.70111712786907);
        final T E8 = FieldKeplerianAnomalyUtility.hyperbolicMeanToEccentric(e8, M8);
        Assertions.assertEquals(4.550432282228856, E8.getReal(), 1e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestHyperbolicEccentricToMean(final Field<T> field) {
        final T e = field.getZero().add(1.801);
        final T E = field.getZero().add(3.287);
        final T M = FieldKeplerianAnomalyUtility.hyperbolicEccentricToMean(e, E);
        Assertions.assertEquals(20.77894350750361, M.getReal(), 1e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue544(final Field<T> field) {
        // Initial parameters
        // In order to test the issue, we voluntarily set the anomaly at T.NaN.
        T e = field.getZero().add(0.7311);
        T anomaly = field.getZero().add(Double.NaN);
        // Computes the elliptic eccentric anomaly
        T E = FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(e, anomaly);
        // Verify that an infinite loop did not occur
        Assertions.assertTrue(Double.isNaN(E.getReal()));
    }

}
