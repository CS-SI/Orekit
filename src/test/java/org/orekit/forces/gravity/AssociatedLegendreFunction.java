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
package org.orekit.forces.gravity;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.dfp.Dfp;
import org.hipparchus.dfp.DfpField;

/** Implementation of associated Legendre functions from defining formulas.
 * <p>
 * This implementation is for test purposes only! It is limited to low degrees
 * and order and is slow.
 * </p>
 * @author Luc Maisonobe
 */
class AssociatedLegendreFunction {

    static final Map<Integer,List<Dfp[]>> LEGENDRE_POLYNOMIALS = new HashMap<Integer, List<Dfp[]>>();
    final int m;
    final Dfp[] polynomial;
    final Dfp normalization;

    private Dfp[] getLegendrePolynomial(int n, DfpField dfpField) {

        // get (or create) the list of polynomials for the specified field
        List<Dfp[]> list = LEGENDRE_POLYNOMIALS.get(dfpField.getRadixDigits());
        if (list == null) {
            list = new ArrayList<Dfp[]>();
            list.add(new Dfp[] {
                dfpField.getOne()                     // P0(X) = 1
            });
            list.add(new Dfp[] {
                dfpField.getZero(), dfpField.getOne() // P1(X) = 0 + 1 * X
            });
        }

        while (list.size() <= n) {

            // build polynomial Pk+1 using recursion formula
            // (k+1) P<sub>k+1</sub>(X) = (2k+1) X P<sub>k</sub>(X) - k P<sub>k-1</sub>(X)
            int   k       = list.size() - 1;
            Dfp   kDfp    = dfpField.newDfp(k);
            Dfp   kP1Dfp  = kDfp.add(dfpField.getOne());
            Dfp   ckDfp   = kP1Dfp.add(kDfp).divide(kP1Dfp);
            Dfp[] pk      = list.get(k);
            Dfp   ckM1Dfp = kDfp.divide(kP1Dfp).negate();
            Dfp[] pkM1    = list.get(k - 1);

            Dfp[] pkP1    = new Dfp[k + 2];
            pkP1[0] = ckM1Dfp.multiply(pkM1[0]);
            for (int i = 0; i < k; ++i) {
                if ((k - i) % 2 == 1) {
                    pkP1[i + 1] = dfpField.getZero();
                } else {
                    pkP1[i + 1] = ckDfp.multiply(pk[i]).add(ckM1Dfp.multiply(pkM1[i + 1]));
                }
            }
            pkP1[k + 1] = ckDfp.multiply(pk[k]);

            list.add(pkP1);

        }

        // retrieve degree n polynomial
        return list.get(n);

    }

    private Dfp[] differentiateLegendrePolynomial(Dfp[] p, int m, DfpField dfpField) {
        Dfp[] dp = new Dfp[p.length - m];
        for (int i = 0; i < dp.length; ++i) {
            dp[i] = p[i + m];
            for (int j = m; j > 0; --j) {
                dp[i] = dp[i].multiply(dfpField.newDfp(i + j));
            }
        }
        return dp;
    }

    public AssociatedLegendreFunction(boolean normalized, int n, int m, DfpField dfpField) {

        this.m = m;

        // store mth derivative of the degree n Legendre polynomial
        polynomial = differentiateLegendrePolynomial(getLegendrePolynomial(n, dfpField), m, dfpField);

        if (normalized) {
            // compute normalization coefficient
            Dfp c = dfpField.newDfp(((m == 0) ? 1.0 : 2.0) * (2 * n + 1));
            for (int k = 0; k < m; ++k) {
                c = c.divide(dfpField.newDfp((n + 1 + k) * (n - k)));
            }
            this.normalization = c.sqrt();
        } else {
            this.normalization = dfpField.getOne();
        }

    }

    public DerivativeStructure value(DerivativeStructure t) {
        DerivativeStructure y1 = t.getField().getOne().multiply(polynomial[polynomial.length - 1].toDouble());
        for (int j = polynomial.length - 2; j >= 0; j--) {
            y1 = y1.multiply(t).add(polynomial[j].toDouble());
        }
        DerivativeStructure oneMinusT2 = t.getField().getOne().subtract(t.multiply(t));
        DerivativeStructure y2 = oneMinusT2.pow(m).sqrt();
        return y1.multiply(y2).multiply(normalization.toDouble());
    }

    public Dfp value(Dfp t) {
        Dfp y1 = polynomial[polynomial.length - 1];
        for (int j = polynomial.length - 2; j >= 0; j--) {
            y1 = y1.multiply(t).add(polynomial[j]);
        }
        Dfp oneMinusT2 = t.getField().getOne().subtract(t.multiply(t));
        Dfp y2 = t.getField().getOne();
        for (int j = 0; j < m; ++j) {
            y2 = y2.multiply(oneMinusT2);
        }
        y2 = y2.sqrt();
        return y1.multiply(y2).multiply(normalization);
    }

}
