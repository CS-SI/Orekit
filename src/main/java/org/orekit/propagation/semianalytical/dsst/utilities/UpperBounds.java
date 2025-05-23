/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.semianalytical.dsst.utilities;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;


/** Utility class to compute upper bounds for truncation algorithms.
 *
 *  @author Pascal Parraud
 */
public class UpperBounds {

    /** Private constructor as the class is a utility class. */
    private UpperBounds() {
    }

    /** Get the upper bound value D<sub>n</sub><sup>l</sup>(&Chi;).
    *
    * @param xx value of &Chi;²
    * @param xpl value of &Chi; * (&Chi;² / 2)<sup>l</sup>
    * @param n index n (power of a/R)
    * @param l index l (power of eccentricity)
    * @return the upper bound D<sub>n</sub><sup>l</sup>(&Chi;)
    */
    public static double getDnl(final double xx, final double xpl, final int n, final int l) {
        final int lp2 = l + 2;
        if (n > lp2) {
            final int ll = l * l;
            double dM = xpl;
            double dL = dM;
            double dB = (l + 1) * xx * xpl;
            for (int j = l + 3; j <= n; j++) {
                final int jm1 = j - 1;
                dL = dM;
                dM = dB;
                dB = jm1 * xx * ((2 * j - 3) * dM - (j - 2) * dL) / (jm1 * jm1 - ll);
            }
            return dB;
        } else if (n == lp2) {
            return  (l + 1) * xx * xpl;
        } else {
            return xpl;
        }
    }

    /** Get the upper bound value D<sub>n</sub><sup>l</sup>(&Chi;).
     *
     * @param xx value of &Chi;²
     * @param xpl value of &Chi; * (&Chi;² / 2)<sup>l</sup>
     * @param n index n (power of a/R)
     * @param l index l (power of eccentricity)
     * @param <T> the type of the field elements
     * @return the upper bound D<sub>n</sub><sup>l</sup>(&Chi;)
     */
    public static <T extends CalculusFieldElement<T>> T getDnl(final T xx, final T xpl, final int n, final int l) {
        final int lp2 = l + 2;
        if (n > lp2) {
            final int ll = l * l;
            T dM = xpl;
            T dL = dM;
            T dB = xx.multiply(xpl).multiply(l + 1);
            for (int j = l + 3; j <= n; j++) {
                final int jm1 = j - 1;
                dL = dM;
                dM = dB;
                dB = xx.multiply(jm1).multiply(dM.multiply(2 * j - 3).subtract(dL.multiply(j - 2))).divide(jm1 * jm1 - ll);
            }
            return dB;
        } else if (n == lp2) {
            return  xx.multiply(xpl).multiply(l + 1);
        } else {
            return xpl;
        }
    }

    /** Get the upper bound value R<sup>ε</sup><sub>n,m,l</sub>(γ).
     *
     * @param gamma value of γ
     * @param n index n
     * @param l index l
     * @param m index m
     * @param eps ε value (+1/-1)
     * @param irf retrograde factor I (+1/-1)
     * @return the upper bound R<sup>ε</sup><sub>n,m,l</sub>(γ)
     */
    public static double getRnml(final double gamma,
                                 final int n, final int l, final int m,
                                 final int eps, final int irf) {
        // Initialization
        final int mei = m * eps * irf;
        final double sinisq = 1. - gamma * gamma;
        // Set a lower bound for inclination
        final double sininc = FastMath.max(0.03, FastMath.sqrt(sinisq));
        final double onepig = 1. + gamma * irf;
        final double sinincPowLmMEI = FastMath.pow(sininc, l - mei);
        final double onepigPowLmMEI = FastMath.pow(onepig, mei);

        // Bound for index 0
        double rBound = sinincPowLmMEI * onepigPowLmMEI;

        // If index > 0
        if (n > l) {
            final int lp1 = l + 1;

            double dpnml  = lp1 * eps;
            double pnml   = dpnml * gamma - m;

            // If index > 1
            if (n > l + 1) {
                final int ll  = l * l;
                final int ml  = m * l;
                final int mm  = m * m;

                double pn1ml  = 1.;
                double dpn1ml = 0.;
                double pn2ml  = 1.;
                double dpn2ml = 0.;
                for (int in = l + 2; in <= n; in++) {
                    final int nm1   = in - 1;
                    final int tnm1  = in + nm1;
                    final int nnlnl = nm1 * (in * in - ll);
                    final int nnmnm = in * (nm1 * nm1 - mm);
                    final int c2nne = tnm1 * in * nm1 * eps;
                    final int c2nml = tnm1 * ml;
                    final double coef = c2nne * gamma - c2nml;

                    pn2ml  = pn1ml;
                    dpn2ml = dpn1ml;
                    pn1ml  = pnml;
                    dpn1ml = dpnml;
                    pnml   = (coef * pn1ml  - nnmnm * pn2ml) / nnlnl;
                    dpnml  = (coef * dpn1ml - nnmnm * dpn2ml + c2nne * pn1ml) / nnlnl;
                }
            }
            // Bound for index > 0
            rBound *= FastMath.sqrt(pnml * pnml + dpnml * dpnml * sinisq / ((n - l) * (n + lp1)));
        }

        return rBound;
    }

    /** Get the upper bound value R<sup>ε</sup><sub>n,m,l</sub>(γ).
    *
    * @param gamma value of γ
    * @param n index n
    * @param l index l
    * @param m index m
    * @param eps ε value (+1/-1)
    * @param irf retrograde factor I (+1/-1)
    * @param <T> the type of the field elements
    * @return the upper bound R<sup>ε</sup><sub>n,m,l</sub>(γ)
    */
    public static <T extends CalculusFieldElement<T>> T getRnml(final T gamma,
                                final int n, final int l, final int m,
                                final int eps, final int irf) {
        final T zero = gamma.getField().getZero();
        // Initialization
        final int mei = m * eps * irf;
        final T sinisq = gamma.square().negate().add(1.);
        // Set a lower bound for inclination
        final T sininc = FastMath.max(zero.newInstance(0.03), FastMath.sqrt(sinisq));
        final T onepig = gamma.multiply(irf).add(1.);
        final T sinincPowLmMEI = FastMath.pow(sininc, l - mei);
        final T onepigPowLmMEI = FastMath.pow(onepig, mei);

        // Bound for index 0
        T rBound = sinincPowLmMEI.multiply(onepigPowLmMEI);

        // If index > 0
        if (n > l) {
            final int lp1 = l + 1;

            T dpnml  = zero.newInstance(lp1 * eps);
            T pnml   = gamma.multiply(dpnml).subtract(m);

            // If index > 1
            if (n > l + 1) {
                final int ll  = l * l;
                final int ml  = m * l;
                final int mm  = m * m;

                T pn1ml  = zero.newInstance(1.);
                T dpn1ml = zero;
                T pn2ml  = zero.newInstance(1.);
                T dpn2ml = zero;
                for (int in = l + 2; in <= n; in++) {
                    final int nm1   = in - 1;
                    final int tnm1  = in + nm1;
                    final int nnlnl = nm1 * (in * in - ll);
                    final int nnmnm = in * (nm1 * nm1 - mm);
                    final int c2nne = tnm1 * in * nm1 * eps;
                    final int c2nml = tnm1 * ml;
                    final T coef = gamma.multiply(c2nne).subtract(c2nml);

                    pn2ml  = pn1ml;
                    dpn2ml = dpn1ml;
                    pn1ml  = pnml;
                    dpn1ml = dpnml;
                    pnml   = (coef.multiply(pn1ml).subtract(pn2ml.multiply(nnmnm))).divide(nnlnl);
                    dpnml  = (coef.multiply(dpn1ml).subtract(dpn2ml.multiply(nnmnm)).add(pn1ml.multiply(c2nne))).divide(nnlnl);
                }
            }
            // Bound for index > 0
            rBound = rBound.multiply(FastMath.sqrt(pnml.multiply(pnml).add(dpnml.multiply(dpnml).multiply(sinisq).divide((n - l) * (n + lp1)))));
        }

        return rBound;
    }

}
