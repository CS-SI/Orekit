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
package org.orekit.propagation.semianalytical.dsst.utilities.hansen;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FieldHansenThirdBodyLinearTest {


    private static <T extends CalculusFieldElement<T>> T hansen(int n, int s, T chi, final Field<T> field) {
        final T zero = field.getZero();
        if (n == 0 && s == 0) {
            return zero.add(1.0);
        } else if (n == 0 && s == 1) {
            return zero.subtract(1.0);
        } else if (n == 1 && s == 0) {
            return (((chi.multiply(chi)).reciprocal()).negate().add(1.)).multiply(0.5).add(1.);
        } else if (n >= 1 && n == s - 1) {
            return hansen(s - 2, s - 1, chi, field).multiply( -(2 * (double)s - 1) / (double)s );
        } else if (n >= 1 && n == s) {
            return hansen(s - 1, s, chi, field).multiply( (2 * (double)s + 1) / ((double)s + 1) );
        } else {
            return hansen(n - 1, s, chi, field).multiply((2 * (double)n + 1) / ((double)n + 1)).
                   subtract(hansen(n - 2, s, chi, field).multiply(((chi.multiply(chi).multiply((double)n + 1).multiply((double)n)).divide(((double)n + (double)s) * ((double)n - (double)s))).reciprocal()));
        }
    }

    @Test
    public void testLinearVsRecursive00() {
        final Binary64 zero = Binary64Field.getInstance().getZero();
        doTestLinearVsRecursive(zero, zero.add(1.1e-12), Binary64Field.getInstance());
    }

    @Test
    public void testLinearVsRecursive01() {
        final Binary64 zero = Binary64Field.getInstance().getZero();
        doTestLinearVsRecursive(zero.add(0.1), zero.add(2.8e-13), Binary64Field.getInstance());
    }

    @Test
    public void testLinearVsRecursive02() {
        final Binary64 zero = Binary64Field.getInstance().getZero();
        doTestLinearVsRecursive(zero.add(0.2), zero.add(9.5e-14), Binary64Field.getInstance());
    }

    @Test
    public void testLinearVsRecursive03() {
        final Binary64 zero = Binary64Field.getInstance().getZero();
        doTestLinearVsRecursive(zero.add(0.3), zero.add(6.0e-14), Binary64Field.getInstance());
    }

    @Test
    public void testLinearVsRecursive04() {
        final Binary64 zero = Binary64Field.getInstance().getZero();
        doTestLinearVsRecursive(zero.add(0.4), zero.add(1.5e-14), Binary64Field.getInstance());
    }

    @Test
    public void testLinearVsRecursive05() {
        final Binary64 zero = Binary64Field.getInstance().getZero();
        doTestLinearVsRecursive(zero.add(0.5), zero.add(6.4e-15), Binary64Field.getInstance());
    }

    @Test
    public void testLinearVsRecursive06() {
        final Binary64 zero = Binary64Field.getInstance().getZero();
        doTestLinearVsRecursive(zero.add(0.6), zero.add(3.7e-15), Binary64Field.getInstance());
    }

    @Test
    public void testLinearVsRecursive07() {
        final Binary64 zero = Binary64Field.getInstance().getZero();
        doTestLinearVsRecursive(zero.add(0.7), zero.add(1.7e-15), Binary64Field.getInstance());
    }

    @Test
    public void testLinearVsRecursive08() {
        final Binary64 zero = Binary64Field.getInstance().getZero();
        doTestLinearVsRecursive(zero.add(0.8), zero.add(1.6e-15), Binary64Field.getInstance());
    }

    @Test
    public void testLinearVsRecursive09() {
        final Binary64 zero = Binary64Field.getInstance().getZero();
        doTestLinearVsRecursive(zero.add(0.9), zero.add(8.9e-16), Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestLinearVsRecursive(final T ecc, final T tol, final Field<T> field) {
        final T zero = field.getZero();
        final int N = 22;
        final T chi = FastMath.sqrt(ecc.multiply(ecc.negate()).add(1.)).reciprocal();

        @SuppressWarnings("unchecked")
        final FieldHansenThirdBodyLinear<T>[] htbl = new FieldHansenThirdBodyLinear[N + 1];

        for (int s = 0; s <= N; s++) {
            htbl[s] = new FieldHansenThirdBodyLinear<>(N, s, field);
            htbl[s].computeInitValues(chi.reciprocal(), (chi.multiply(chi)).reciprocal(), (chi.multiply(chi).multiply(chi)).reciprocal());
        }

        T maxRelativeError = zero;
        for (int s = 0; s <= N; s++) {
            for (int n = FastMath.max(2, s); n <= N; n++) {
                final T hansenRec = hansen(n, s, chi, field);
                final T hansenLin = htbl[s].getValue(n, chi.reciprocal());
                final T relativeError = FastMath.abs((hansenLin.subtract(hansenRec)).divide(hansenRec));
                maxRelativeError = FastMath.max(maxRelativeError, relativeError);
            }
        }
        Assertions.assertEquals(0.0, maxRelativeError.getReal(), tol.getReal());

    }


}
