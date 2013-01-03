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

/** Compute the &Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) coefficients from equation 2.7.1-(13).
 *
 *  @author Romain Di Costanzo
 */
public class GammaMsnCoefficients {

    /** Result map. */
    private final Map<MNSKey, Double> map;

    /** &Gamma;. */
    private final double gamma;

    /** I = +1 for a prograde orbit, -1 otherwise. */
    private final int    I;

    /** simple constructor.
     * @param gamma &gamma;
     * @param I retrograde factor
     */
    public GammaMsnCoefficients(final double gamma, final int I) {
        this.gamma = gamma;
        this.I     = I;
        this.map   = new TreeMap<MNSKey, Double>();
    }

    /** &Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) coefficient from equations 2.7.1 - (13).
     * @param n n
     * @param s s
     * @param m m
     * @return &Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;)
     */
    public double getGammaMsn(final int n, final int s, final int m) {
        double res = 0d;
        if (map.containsKey(new MNSKey(m, n, s))) {
            res = map.get(new MNSKey(m, n, s));
        } else {
            if (s <= -m) {
                res = FastMath.pow(-1, m - s) * FastMath.pow(2, s) * FastMath.pow(1 + I * gamma, -I * m);
            } else if (FastMath.abs(s) <= m) {
                final double num = FastMath.pow(-1, m - s) * FastMath.pow(2, -m) * ArithmeticUtils.factorial(n + m) *
                                   ArithmeticUtils.factorial(n - m) * FastMath.pow(1 + I * gamma, I * s);
                final double den = ArithmeticUtils.factorial(n + s) * ArithmeticUtils.factorial(n - s);
                res = num / den;
            } else if (s >= m) {
                res = FastMath.pow(2, -s) * FastMath.pow(1 + I * gamma, I * m);
            }
            map.put(new MNSKey(m, n, s), res);
        }
        return res;
    }

    /** d&Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) / d&gamma; coefficient from equations 2.7.1 - (13).
     * @param n n
     * @param s s
     * @param m m
     * @return d&Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) / d&gamma;
     */
    public double getDGammaMsn(final int n, final int s, final int m) {
        double res = 0d;
        if (s <= -m) {
            res = -m * getGammaMsn(n, s, m) / (1 + I * gamma);
        } else if (FastMath.abs(s) <= m) {
            res = s * getGammaMsn(n, s, m) / (1 + I * gamma);
        } else if (s >= m) {
            res = m * getGammaMsn(n, s, m) / (1 + I * gamma);
        }
        return res;
    }

}
