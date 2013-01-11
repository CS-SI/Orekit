/* Copyright 2002-2013 CS Systèmes d'Information
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

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.util.ArithmeticUtils;
import org.apache.commons.math3.util.FastMath;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.MNSKey;

/** Compute the &Gamma;<sup>m</sup><sub>n,s</sub>(&gamma;) function from equation 2.7.1-(13).
 *
 *  @author Romain Di Costanzo
 */
public class GammaMnsFunction {

    /** Storage map. */
    private final Map<MNSKey, Double> map;

    /** &gamma;. */
    private final double gamma;

    /** I = +1 for a prograde orbit, -1 otherwise. */
    private final int    I;

    /** Simple constructor.
     *  @param gamma &gamma;
     *  @param I retrograde factor
     */
    public GammaMnsFunction(final double gamma, final int I) {
        this.gamma = gamma;
        this.I     = I;
        this.map   = new TreeMap<MNSKey, Double>();
    }

    /** Get &Gamma; function value.
     *  @param m m
     *  @param n n
     *  @param s s
     *  @return &Gamma;<sup>m</sup><sub>n, s</sub>(&gamma;)
     */
    public double getValue(final int m, final int n, final int s) {
        double res = 0.;
        if (map.containsKey(new MNSKey(m, n, s))) {
            res = map.get(new MNSKey(m, n, s));
        } else {
            if (s <= -m) {
                res = FastMath.pow(-1, m - s) * FastMath.pow(2, s) * FastMath.pow(1 + I * gamma, -I * m);
            } else if (s >= m) {
                res = FastMath.pow(2, -s) * FastMath.pow(1 + I * gamma, I * m);
            } else {
                res = FastMath.pow(-1, m - s) * FastMath.pow(2, -m) *
                      ArithmeticUtils.factorial(n + m) * ArithmeticUtils.factorial(n - m) *
                      FastMath.pow(1 + I * gamma, I * s);
                res /= ArithmeticUtils.factorial(n + s) * ArithmeticUtils.factorial(n - s);
            }
            map.put(new MNSKey(m, n, s), res);
        }
        return res;
    }

    /** Get &Gamma; function derivative.
     * @param m m
     * @param n n
     * @param s s
     * @return d&Gamma;<sup>m</sup><sub>n,s</sub>(&gamma;)/d&gamma;
     */
    public double getDerivative(final int m, final int n, final int s) {
        double res = 0.;
        if (s <= -m) {
            res = -m * getValue(m, n, s) / (1 + I * gamma);
        } else if (s >= m) {
            res =  m * getValue(m, n, s) / (1 + I * gamma);
        } else {
            res =  s * getValue(m, n, s) / (1 + I * gamma);
        }
        return res;
    }

}
