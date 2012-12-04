/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.math.BigInteger;
import java.util.TreeMap;

import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTFactorial;
import org.orekit.propagation.semianalytical.dsst.coefficients.HansenCoefficients;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory.NSKey;
import org.orekit.time.AbsoluteDate;

/** Zonal contribution to the {@link DSSTCentralBody central body gravitational perturbation}.
 *
 *   @author Romain Di Costanzo
 *   @author Pascal Parraud
 */
class ZonalContribution implements DSSTForceModel {

    /** Truncation tolerance */
    private static final double TRUNCATION_TOLERANCE = 1e-10;

    /** Equatorial radius of the central body */
    private final double ae;

    /** Central body attraction coefficient */
    private final double mu;

    /** Geopotential coefficient Jn = -Cn0 */
    private final double[] Jn;

    /** Degree <i>n</i> of potential. */
    private final int    degree;

    /** Coefficient used to define the mean disturbing function V<sub>ns</sub> coefficient */
    private final TreeMap<NSKey, Double> Vns;

    // Equinoctial elements (according to DSST notation)
    /** a */
    private double a;
    /** ex */
    private double k;
    /** ey */
    private double h;
    /** hx */
    private double q;
    /** hy */
    private double p;
    /** Retrograde factor */
    private int    I;

    /** Eccentricity */
    private double ecc;

    /** Direction cosine &alpha */
    private double alpha;
    /** Direction cosine &beta */
    private double beta;
    /** Direction cosine &gamma */
    private double gamma;

    // Common factors for potential computation
    /** &Chi;<sup>3</sup> = (1 / B)<sup>3</sup> */
    private double X3;
    /** 1 / (A * B) */
    private double ooAB;
    /** B / A */
    private double BoA;
    /** B / A(1 + B) */
    private double BoABpo;
    /** C / (2 * A * B) */
    private double Co2AB;
    /** a / A */
    private double aoA;
    /** &mu; / a */
    private double muoa;

    /**
     * Highest power of the eccentricity to appear in the truncated analytical power series
     * expansion for the averaged central-body Zonal harmonic potential. The user can set this value
     * by using the {@link #setZonalMaximumEccentricityPower(double)} method. If he doesn't, the
     * software will compute a default value itself, through the
     * {@link #computeZonalMaxEccentricityPower()} method.
     */
    private int    maxEccentricityPower;

    /** Maximal degree of the geopotential to be used in zonal series expansion.
     *  <p>
     *  This value won't be used if the {@link #maxEccentricityPower} is set through the
     *  {@link #computeZonalMaxEccentricityPower()} method. If not, series expansion will
     *  automatically be truncated.
     *  </p>
     */
    private int    maxDegree;

    /** Truncation tolerance.
     *  <p>
     *  This value is used by the {@link truncation()} method to
     *  determine the upper bound of the zonal potential expansion.
     *  </p>
     */
    private double truncationTolerance;

    /** Hansen coefficient. */
    private HansenCoefficients hansen;

    /** Simple constructor.
     *  @param equatorialRadius equatorial radius of the central body (m)
     *  @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     *  @param Jn zonal coefficients of the potential
     */
    public ZonalContribution(final double equatorialRadius,
                             final double mu,
                             final double[] jn) {

        this.ae     = equatorialRadius;
        this.mu     = mu;
        this.Jn     = jn.clone();
        this.degree = jn.length - 1;

        // Initialize default values
        this.maxEccentricityPower = Integer.MIN_VALUE;
        this.truncationTolerance  = Double.NEGATIVE_INFINITY;

        this.Vns = DSSTCoefficientFactory.computeVnsCoefficient(degree + 1);
    }

    /** {@inheritDoc} */
    public final void initialize(final AuxiliaryElements aux) throws OrekitException {
        
        // Equinoctial elements
        a = aux.getSma();
        k = aux.getK();
        h = aux.getH();
        q = aux.getQ();
        p = aux.getP();

        // Retrograde factor
        I = aux.getRetrogradeFactor();

        // Eccentricity
        ecc = aux.getEcc();

        // Direction cosines
        alpha = aux.getAlpha();
        beta  = aux.getBeta();
        gamma = aux.getGamma();
    
        // Equinoctial coefficients
        final double A = aux.getA();
        final double B = aux.getB();
        final double C = aux.getC();

        // &Chi; = 1 / B
        X3 = 1. / (B * B * B);
        // 1 / AB
        ooAB = 1. / (A * B);
        // B / A
        BoA = B / A;
        // C / 2AB
        Co2AB = C * ooAB / 2.;
        // B / A(1 + B)
        BoABpo = BoA / (1. + B);
        // a / A
        aoA = a / A;
        // &mu; / a
        muoa = mu / a;

        // Hansen coefficients
        hansen = new HansenCoefficients(ecc);

        // Set the highest power of the eccentricity in the analytical power
        // series expansion for the averaged low order zonal harmonic perturbation
        truncation();

    }

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState spacecraftState) throws OrekitException {

        // Compute potential derivative
        final double[] dU  = computeUDerivatives();
        final double dUda  = dU[0];
        final double dUdk  = dU[1];
        final double dUdh  = dU[2];
        final double dUdAl = dU[3];
        final double dUdBe = dU[4];
        final double dUdGa = dU[5];

        // Compute cross derivatives [Eq. 2.2-(8)]
        // U(alpha,gamma) = alpha * dU / dgamma - gamma * dU / dalpha
        final double UAlphaGamma   = alpha * dUdGa - gamma * dUdAl;
        // U(beta,gamma) = beta * dU / dgamma - gamma * dU / dbeta
        final double UBetaGamma    =  beta * dUdGa - gamma * dUdBe;
        // Common factor
        final double pUAGmIqUBGoAB = (p * UAlphaGamma - I * q * UBetaGamma) * ooAB;

        // Compute mean elements rates [Eq. 3.1-(1)]
        final double da =  0.;
        final double dh =  BoA * dUdk + k * pUAGmIqUBGoAB;
        final double dk = -BoA * dUdh - h * pUAGmIqUBGoAB;
        final double dp =      -Co2AB * UBetaGamma;
        final double dq =  I * -Co2AB * UAlphaGamma;
        final double dM = -2 * aoA * dUda + BoABpo * (h * dUdh + k * dUdk) + pUAGmIqUBGoAB;

        return new double[] {da, dk, dh, dq, dp, dM};
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date,
                                               final double[] meanElements)
        throws OrekitException {
        // TODO: not implemented yet, Short Periodic Variations are set to null
        return new double[] {0., 0., 0., 0., 0., 0.};
    }

    /** Set the highest power of the eccentricity to appear in the truncated analytical
     *  power series expansion for the averaged central-body zonal harmonic potential.
     *
     * @param zonalMaxEccPower highest power of the eccentricity
     */
    public final void setZonalMaximumEccentricityPower(final int zonalMaxEccPower) {
        this.maxEccentricityPower = zonalMaxEccPower;
    }

    /** Set the Zonal truncature tolerance.
     * @param zonalTruncatureTolerance Zonal truncature tolerance
     */
    public final void setZonalTruncatureTolerance(final double zonalTruncatureTolerance) {
        this.truncationTolerance = zonalTruncatureTolerance;
    }

    /** Computes the highest power of the eccentricity and the maximal degree
     *  to appear in the truncated analytical power series expansion for the
     *  averaged central-body zonal harmonic potential.
     *  <p>
     *  This method is computing the upper value for the central body geopotential
     *  and then determine the maximal values from with upper values give geopotential
     *  terms inferior to a defined tolerance.
     *  </p>
     *  Algorithm description can be found in the D.A Danielson paper at paragraph 6.2.
     *
     * @throws OrekitException if an error occurs in Hansen coefficient computation
     */
    private void truncation() throws OrekitException {
        // Did a maximum eccentricity power has been found
        boolean maxFound = false;
        // Initialize the current spherical harmonic term to 0.
        double term = 0.;
        // Maximal degree of the geopotential expansion :
        int nMax = Integer.MIN_VALUE;
        // Maximal power of e
        int sMax = Integer.MIN_VALUE;
        // Find the truncation tolerance : set tolerance as a non dragged satellite if undefined by
        // the user. Operation stops when term > tolerance
        if (truncationTolerance == Double.NEGATIVE_INFINITY) {
            truncationTolerance = TRUNCATION_TOLERANCE;
        }
        // Check if highest power of E has been given by the user :
        if (maxEccentricityPower == Integer.MIN_VALUE) {
            // Is the degree of the zonal harmonic field too small to allow more than one power of E
            if (degree == 2) {
                maxEccentricityPower = 0;
                maxDegree = degree;
            } else {
                // Auxiliary quantities
                final double r2a = ae / (2 * a);
                double x2MuRaN = 2 * muoa * r2a;
                // Search for the highest power of E for which the computed value is greater than
                // the truncation tolerance in the power series
                // s-loop :
                for (int s = 0; s <= degree - 2; s++) {
                    // n-loop
                    for (int n = s + 2; n <= degree; n++) {
                        // (n - s) must be even
                        if ((n - s) % 2 == 0) {
                            // Local values :
                            final double gam2 = gamma * gamma;
                            // Compute factorial :
                            final BigInteger factorialNum = DSSTFactorial.fact(n - s);
                            final BigInteger factorialDen = DSSTFactorial.fact((n + s) / 2).multiply(DSSTFactorial.fact((n - s) / 2));
                            final double factorial = factorialNum.doubleValue() / factorialDen.doubleValue();
                            final double k0 = hansen.getHansenKernelValue(0, -n - 1, s);
                            // Compute the Qns(bound) upper bound :
                            final double qns = FastMath.abs(DSSTCoefficientFactory.getQnsPolynomialValue(gamma, n, s));
                            final double qns2 = qns * qns;
                            final double factor = (1 - gam2) / (n * (n + 1) - s * (s + 1));
                            // Compute dQns/dGamma
                            final double dQns = FastMath.abs(DSSTCoefficientFactory.getQnsPolynomialValue(gamma, n, s + 1));
                            final double dQns2 = dQns * dQns;
                            final double qnsBound = FastMath.sqrt(qns2 + factor * dQns2);
    
                            // Get the current potential upper bound for the current (n, s) couple.
                            final int sO2 = s / 2;
                            term = x2MuRaN * r2a * FastMath.abs(Jn[n]) * factorial * k0 * qnsBound *
                                   FastMath.pow(1 - gam2, sO2) * FastMath.pow(ecc, s) / FastMath.pow(2, n);
    
                            // Compare result with the tolerance parameter :
                            if (term <= truncationTolerance) {
                                // Stop here
                                nMax = Math.max(nMax, n);
                                sMax = Math.max(sMax, s);
                                // truncature found
                                maxFound = true;
                                // Force a premature end loop
                                n = degree;
                                s = degree;
                            }
                        }
                    }
                    // Prepare next loop :
                    x2MuRaN = 2 * mu / (a) * FastMath.pow(r2a, s + 1);
                }
                if (maxFound) {
                    maxDegree = nMax;
                    maxEccentricityPower = sMax;
                } else {
                    maxDegree = degree;
                    maxEccentricityPower = degree - 2;
                }
            }
    
        } else {
            // Value set by the user :
            maxDegree = degree;
            maxEccentricityPower = degree - 2;
        }
    
    }

    /** Compute the derivatives of the gravitational potential U [Eq. 3.1-(6)].
     *  <p>
     *  The result is the array
     *  [dU/da, dU/dk, dU/dh, dU/d&alpha;, dU/d&beta;, dU/d&gamma;]
     *  </p>
     *  @return potential derivatives
     *  @throws OrekitException if an error occurs in hansen computation
     */
    private double[] computeUDerivatives()
        throws OrekitException {

        // Initialize data
        final double[][] GsHs = DSSTCoefficientFactory.computeGsHsCoefficient(k, h, alpha, beta, maxDegree + 1);
        final double[][] Qns  = DSSTCoefficientFactory.computeQnsCoefficient(gamma, maxDegree + 1);

        final double Ra = ae / a;

        // Potential derivatives
        double dUda  = 0d;
        double dUdk  = 0d;
        double dUdh  = 0d;
        double dUdAl = 0d;
        double dUdBe = 0d;
        double dUdGa = 0d;

        for (int s = 0; s <= maxEccentricityPower; s++) {
            // Get the current gs and hs coefficient :
            final double gs = GsHs[0][s];

            // Compute partial derivatives of Gs from equ. (9) :
            // First get the G(s-1) and the H(s-1) coefficient : SET TO 0 IF < 0
            final double sxgsm1 = s > 0 ? s * GsHs[0][s - 1] : 0;
            final double sxhsm1 = s > 0 ? s * GsHs[1][s - 1] : 0;
            // Get derivatives
            final double dGsdh  = beta  * sxgsm1 - alpha * sxhsm1;
            final double dGsdk  = alpha * sxgsm1 + beta  * sxhsm1;
            final double dGsdAl = k * sxgsm1 - h * sxhsm1;
            final double dGsdBe = h * sxgsm1 + k * sxhsm1;

            // Kronecker symbol (2 - delta(0,s))
            final double delta0s = (s == 0) ? 1 : 2;

            for (int n = s + 2; n <= maxDegree; n++) {
                // Extract data from previous computation :
                final double jn   = Jn[n];
                final double vns  = Vns.get(new NSKey(n, s));
                final double kns  = hansen.getHansenKernelValue(0, -n - 1, s);
                final double qns  = Qns[n][s];
                final double rapn = FastMath.pow(Ra, n);
                final double dkns = hansen.getHansenKernelDerivative(0, -n - 1, s);
                final double coef = delta0s * rapn * jn * vns;

                // Compute dU / da :
                dUda += coef * kns * qns * (n + 1) * gs;
                // Compute dU / dEx
                dUdk += coef * qns * (kns * dGsdk + k * X3 * gs * dkns);
                // Compute dU / dEy
                dUdh += coef * qns * (kns * dGsdh + h * X3 * gs * dkns);
                // Compute dU / dAlpha
                dUdAl += coef * kns * qns * dGsdAl;
                // Compute dU / dBeta
                dUdBe += coef * kns * qns * dGsdBe;
                // Compute dU / dGamma : here dQns/dGamma = Q(n, s + 1) from Equation 3.1 - (8)
                dUdGa += coef * kns * Qns[n][s + 1] * gs;
            }
        }

        dUda  *=  muoa / a;
        dUdk  *= -muoa;
        dUdh  *= -muoa;
        dUdAl *= -muoa;
        dUdBe *= -muoa;
        dUdGa *= -muoa;

        return new double[] {dUda, dUdk, dUdh, dUdAl, dUdBe, dUdGa};
    }
}
