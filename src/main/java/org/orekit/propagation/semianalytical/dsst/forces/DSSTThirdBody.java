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

import java.util.TreeMap;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.coefficients.HansenCoefficients;
import org.orekit.time.AbsoluteDate;

/** Third body attraction contribution to the
 *  {@link org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSSTPropagator}.
 *
 *  @author Romain Di Costanzo
 *  @author Pascal Parraud
 */
public class DSSTThirdBody extends AbstractGravitationalForces {

    /** Default N order for summation. */
    private static final int       DEFAULT_ORDER = 5;

    /** The 3rd body to consider. */
    private final CelestialBody    body;

    /** Standard gravitational parameter &mu; for the body in m<sup>3</sup>/s<sup>2</sup>. */
    private final double           gm;

    /** N order for summation. */
    private final int              order;

    /** V<sub>ns</sub> coefficients. */
    private TreeMap<NSKey, Double> Vns;

    // Equinoctial elements (according to DSST notation)
    /** a. */
    private double a;
    /** ex. */
    private double k;
    /** ey. */
    private double h;
    /** hx. */
    private double q;
    /** hy. */
    private double p;

    // Eccentricity
    private double ecc;

    // Direction cosines of the symmetry axis
    /** &alpha; */
    private double alpha;
    /** &beta; */
    private double beta;
    /** &gamma; */
    private double gamma;

    // Common factors for potential computation
    /** &Chi;<sup>3</sup> = (1 / B)<sup>3</sup> */
    private double X3;
    /** a / A. */
    private double aoA;
    /** B / A. */
    private double BoA;
    /** 1 / (A * B). */
    private double ooAB;
    /** C / (2 * A * B). */
    private double Co2AB;
    /** B / A(1 + B). */
    private double BoABpo;

    /** Retrograde factor. */
    private int    I;

    /** Distance from center of mass of the central body to the 3rd body. */
    private double R3;

    /** Simple constructor.
     * @param body the 3rd body to consider
     * @see org.orekit.bodies.CelestialBodyFactory
     */
    public DSSTThirdBody(final CelestialBody body) {
        this(body, DEFAULT_ORDER);
    }

    /** Complete constructor.
     * @param body the 3rd body to consider
     * @param order N order for summation
     * @see org.orekit.bodies.CelestialBodyFactory
     */
    public DSSTThirdBody(final CelestialBody body, final int order) {
        this.body = body;
        this.gm   = body.getGM();

        this.order = order;
        this.Vns = DSSTCoefficientFactory.computeVnsCoefficient(order + 1);
    }

    /** Get third body.
     * @return third body
     */
    public final CelestialBody getBody() {
        return body;
    }

    /** {@inheritDoc} */
    public void initialize(final AuxiliaryElements aux) throws OrekitException {
    
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

        // Distance from center of mass of the central body to the 3rd body
        final Vector3D bodyPos = body.getPVCoordinates(aux.getDate(), aux.getFrame()).getPosition();
        R3 = bodyPos.getNorm();
    
        // Direction cosines
        final Vector3D bodyDir = bodyPos.normalize();
        alpha = bodyDir.dotProduct(aux.getVectorF());
        beta  = bodyDir.dotProduct(aux.getVectorG());
        gamma = bodyDir.dotProduct(aux.getVectorW());
        
        // Equinoctial coefficients
        final double A = aux.getA();
        final double B = aux.getB();
        final double C = aux.getC();

        // &Chi; = 1 / B
        X3 = 1. / (B * B * B);
        // 1 / A
        aoA = a / A;
        // B / A
        BoA = B / A;
        // 1 / AB
        ooAB = 1. / (A * B);
        // C / 2AB
        Co2AB = C * ooAB / 2.;
        // B / A(1 + B)
        BoABpo = BoA / (1. + B);
    }

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState currentState) throws OrekitException {

        // Compute potential U derivatives
        final double[] dU  = computeUDerivatives();
        final double dUda  = dU[0];
        final double dUdk  = dU[1];
        final double dUdh  = dU[2];
        final double dUdAl = dU[3];
        final double dUdBe = dU[4];
        final double dUdGa = dU[5];

        // Compute cross derivatives from 2.2-(8)
        // U(alpha,gamma) = alpha * dU/dgamma - gamma * dU/dalpha
        final double UAlphaGamma   = alpha * dUdGa - gamma * dUdAl;
        // U(beta,gamma) = beta * dU/dgamma - gamma * dU/dbeta
        final double UBetaGamma    = beta * dUdGa - gamma * dUdBe;
        // Common factor
        final double pUAGmIqUBGoAB = (p * UAlphaGamma - I * q * UBetaGamma) * ooAB;

        // Compute mean element rates from equation 3.1-(1)
        final double da = 0.;
        final double dh =  BoA * dUdk + k * pUAGmIqUBGoAB;
        final double dk = -BoA * dUdh - h * pUAGmIqUBGoAB;
        final double dp = -Co2AB * UBetaGamma;
        final double dq = -I * Co2AB * UAlphaGamma;
        final double dM = -2 * aoA * dUda + BoABpo * (h * dUdh + k * dUdk) + pUAGmIqUBGoAB;

        return new double[] {da, dk, dh, dq, dp, dM};

    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date,
                                               final double[] meanElements) throws OrekitException {
        // TODO: not implemented yet, Short Periodic Variations are set to null
        return new double[] {0., 0., 0., 0., 0., 0.};
    }

    /** Compute potential derivatives.
     * @return derivatives of the potential with respect to orbital parameters
     * @throws OrekitException if Hansen coefficients cannot be computed
     */
    private double[] computeUDerivatives() throws OrekitException {

        // Hansen coefficients
        final HansenCoefficients hansen = new HansenCoefficients(ecc);
        // Gs coefficients
        final double[][] GsHs = DSSTCoefficientFactory.computeGsHsCoefficient(k, h, alpha, beta, order);
        // Qns coefficients
        final double[][] Qns = DSSTCoefficientFactory.computeQnsCoefficient(gamma, order);
        // mu3 / R3
        final double muoR3 = gm / R3;
        // a / R3
        final double aoR3  = a / R3;

        // Potential derivatives
        double dUda  = 0.;
        double dUdk  = 0.;
        double dUdh  = 0.;
        double dUdAl = 0.;
        double dUdBe = 0.;
        double dUdGa = 0.;

        for (int s = 0; s <= order; s++) {
            // Get the current Gs and Hs coefficient
            final double gs = GsHs[0][s];
            final double gsm1 = s > 0 ? GsHs[0][s - 1] : 0.;
            final double hsm1 = s > 0 ? GsHs[1][s - 1] : 0.;

            // Compute partial derivatives of Gs from 3.1-(9)
            final double dGsdh  = s * beta * gsm1 - s * alpha * hsm1;
            final double dGsdk  = s * alpha * gsm1 + s * beta * hsm1;
            final double dGsdAl = s * k * gsm1 - s * h * hsm1;
            final double dGsdBe = s * h * gsm1 + s * k * hsm1;

            // Kronecker symbol (2 - delta(0,s))
            final double delta0s = (s == 0) ? 1. : 2.;

            for (int n = FastMath.max(2, s); n <= order; n++) {
                // for (int n = s + 1; n <= order; n++) {
                // Extract data from previous computation :
                final double vns = Vns.get(new NSKey(n, s));
                final double kns = hansen.getHansenKernelValue(0, n, s);
                final double qns = Qns[n][s];
                final double aoR3n = FastMath.pow(aoR3, n);
                final double dkns = hansen.getHansenKernelDerivative(0, n, s);
                final double coef0 = delta0s * aoR3n * vns;
                final double coef1 = coef0 * qns;
                // dQns/dGamma = Q(n, s + 1) from Equation 3.1-(8)
                // for n = s, Q(n, n + 1) = 0. (Cefola & Broucke, 1975)
                final double dqns = (n == s) ? 0. : Qns[n][s + 1];

                // Compute dU / da :
                dUda += coef1 * n * kns * gs;
                // Compute dU / dh
                dUdh += coef1 * (kns * dGsdh + h * X3 * gs * dkns);
                // Compute dU / dk
                dUdk += coef1 * (kns * dGsdk + k * X3 * gs * dkns);
                // Compute dU / dAlpha
                dUdAl += coef1 * kns * dGsdAl;
                // Compute dU / dBeta
                dUdBe += coef1 * kns * dGsdBe;
                // Compute dU / dGamma with dQns/dGamma = Q(n, s + 1)
                dUdGa += coef0 * kns * dqns * gs;
            }
        }

        dUda  *= muoR3 / a;
        dUdk  *= muoR3;
        dUdh  *= muoR3;
        dUdAl *= muoR3;
        dUdBe *= muoR3;
        dUdGa *= muoR3;

        return new double[] {dUda, dUdk, dUdh, dUdAl, dUdBe, dUdGa};

    }

}
