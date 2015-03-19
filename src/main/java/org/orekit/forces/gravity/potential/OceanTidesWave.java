/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.forces.gravity.potential;

import org.apache.commons.math3.util.FastMath;
import org.orekit.data.BodiesElements;

/** Container for ocen tides coefficients for one tide wave.
 * @see org.orekit.forces.gravity.OceanTides
 * @see org.orekit.forces.gravity.OceanTidesField
 * @author Luc Maisonobe
 * @since 6.1
 * @see OceanTidesReader
 */
public class OceanTidesWave {

    /** Waves of degree 0 and 1 do not affect spacecrafts. */
    private static final int START_DEGREE = 2;

    /** Maximum supported degree. */
    private final int degree;

    /** Maximum supported order. */
    private final int order;

    /** Doodson number for the wave. */
    private final int doodson;

    /** Coefficient for γ = GMST + π tide parameter. */
    private final int cGamma;

    /** Coefficient for mean anomaly of the Moon. */
    private final int cL;

    /** Coefficient for mean anomaly of the Sun. */
    private final int cLPrime;

    /** Coefficient for L - Ω where L is the mean longitude of the Moon. */
    private final int cF;

    /** Coefficient for mean elongation of the Moon from the Sun. */
    private final int cD;

    /** Coefficient for mean longitude of the ascending node of the Moon. */
    private final int cOmega;

    /** C<sub>n,m</sub><sup>+</sup> coefficients. */
    private final double[][] cPlus;

    /** S<sub>n,m</sub><sup>+</sup> coefficients. */
    private final double[][] sPlus;

    /** C<sub>n,m</sub><sup>-</sup> coefficients. */
    private final double[][] cMinus;

    /** S<sub>n,m</sub><sup>-</sup> coefficients. */
    private final double[][] sMinus;

    /** Simple constructor.
     * @param doodson Doodson number for the wave
     * @param degree max degree present in the coefficients array
     * @param order max order present in the coefficients array
     * @param coefficients C<sub>n,m</sub><sup>+</sup>, S<sub>n,m</sub><sup>+</sup>,
     * C<sub>n,m</sub><sup>-</sup> and S<sub>n,m</sub><sup>-</sup> coefficients
     */
    public OceanTidesWave(final int doodson, final int degree, final int order,
                          final double[][][] coefficients) {

        this.doodson = doodson;

        // compute Doodson arguments from Doodson number
        final int cPs     = ( doodson           % 10) - 5;
        final int cNPrime = ((doodson / 10)     % 10) - 5;
        final int cP      = ((doodson / 100)    % 10) - 5;
        final int cH      = ((doodson / 1000)   % 10) - 5;
        final int cS      = ((doodson / 10000)  % 10) - 5;
        final int cTau    =  (doodson / 100000) % 10;

        // compute Delaunay arguments from Doodson arguments
        this.cGamma  =  cTau;
        this.cL      = -cP;
        this.cLPrime = -cPs;
        this.cF      = -cTau + cS + cH + cP + cPs;
        this.cD      = -cH - cPs;
        this.cOmega  = -cTau + cS + cH + cP - cNPrime + cPs;

        this.degree   = degree;
        this.order    = order;

        // distribute the coefficients
        this.cPlus  = new double[degree + 1][];
        this.sPlus  = new double[degree + 1][];
        this.cMinus = new double[degree + 1][];
        this.sMinus = new double[degree + 1][];
        for (int i = 0; i <= degree; ++i) {
            final int m = FastMath.min(i, order) + 1;
            final double[][] row = coefficients[i];
            cPlus[i]  = new double[m];
            sPlus[i]  = new double[m];
            cMinus[i] = new double[m];
            sMinus[i] = new double[m];
            for (int j = 0; j < m; ++j) {
                cPlus[i][j]  = row[j][0];
                sPlus[i][j]  = row[j][1];
                cMinus[i][j] = row[j][2];
                sMinus[i][j] = row[j][3];
            }
        }

    }

    /** Get the maximum supported degree.
     * @return maximum supported degree
     */
    public int getMaxDegree() {
        return degree;
    }

    /** Get the maximum supported order.
     * @return maximum supported order
     */
    public int getMaxOrder() {
        return order;
    }

    /** Get the Doodson number for the wave.
     * @return Doodson number for the wave
     */
    public int getDoodson() {
        return doodson;
    }

    /** Add the contribution of the wave to Stokes coefficients.
     * @param elements nutation elements
     * @param cnm spherical harmonic cosine coefficients table to add contribution too
     * @param snm spherical harmonic sine coefficients table to add contribution too
     */
    public void addContribution(final BodiesElements elements,
                                final double[][] cnm, final double[][] snm) {

        final double thetaF = cGamma * elements.getGamma() +
                              cL * elements.getL() + cLPrime * elements.getLPrime() + cF * elements.getF() +
                              cD * elements.getD() + cOmega * elements.getOmega();
        final double cos    = FastMath.cos(thetaF);
        final double sin    = FastMath.sin(thetaF);

        for (int i = START_DEGREE; i <= degree; ++i) {
            for (int j = 0; j <= FastMath.min(i, order); ++j) {
                // from IERS conventions 2010, section 6.3, equation 6.15
                cnm[i][j] += (cPlus[i][j] + cMinus[i][j]) * cos + (sPlus[i][j] + sMinus[i][j]) * sin;
                snm[i][j] += (sPlus[i][j] - sMinus[i][j]) * cos - (cPlus[i][j] - cMinus[i][j]) * sin;
            }
        }

    }

}
