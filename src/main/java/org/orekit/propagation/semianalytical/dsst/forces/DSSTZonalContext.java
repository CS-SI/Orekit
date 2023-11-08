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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.hipparchus.util.FastMath;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/**
 * This class is a container for the common parameters used in {@link DSSTZonal}.
 * <p>
 * It performs parameters initialization at each integration step for the Zonal contribution
 * to the central body gravitational perturbation.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.0
 */
public class DSSTZonalContext extends ForceModelContext {

    // Common factors for potential computation
    /** A = sqrt(μ * a). */
    private final double A;
    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private double X;
    /** &Chi;². */
    private double XX;
    /** &Chi;³. */
    private double XXX;
    /** 1 / (A * B) . */
    private double ooAB;
    /** B / A . */
    private double BoA;
    /** B / A(1 + B) . */
    private double BoABpo;
    /** -C / (2 * A * B) . */
    private double mCo2AB;
    /** -2 * a / A . */
    private double m2aoA;
    /** μ / a . */
    private double muoa;
    /** R / a . */
    private double roa;

    /** Keplerian mean motion. */
    private final double n;

    // Short period terms
    /** h * k. */
    private double hk;
    /** k² - h². */
    private double k2mh2;
    /** (k² - h²) / 2. */
    private double k2mh2o2;
    /** 1 / (n² * a²). */
    private double oon2a2;
    /** 1 / (n² * a) . */
    private double oon2a;
    /** χ³ / (n² * a). */
    private double x3on2a;
    /** χ / (n² * a²). */
    private double xon2a2;
    /** (C * χ) / ( 2 * n² * a² ). */
    private double cxo2n2a2;
    /** (χ²) / (n² * a² * (χ + 1 ) ). */
    private double x2on2a2xp1;
    /** B * B. */
    private double BB;

    /**
     * Simple constructor.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param provider          provider for spherical harmonics
     * @param parameters        values of the force model parameters
     */
    DSSTZonalContext(final AuxiliaryElements auxiliaryElements,
                     final UnnormalizedSphericalHarmonicsProvider provider,
            final double[] parameters) {

        super(auxiliaryElements);

        final double mu = parameters[0];

        // Keplerian Mean Motion
        final double absA = FastMath.abs(auxiliaryElements.getSma());
        n = FastMath.sqrt(mu / absA) / absA;

        A = FastMath.sqrt(mu * auxiliaryElements.getSma());

        // &Chi; = 1 / B
        X = 1. / auxiliaryElements.getB();
        XX = X * X;
        XXX = X * XX;

        // 1 / AB
        ooAB = 1. / (A * auxiliaryElements.getB());
        // B / A
        BoA = auxiliaryElements.getB() / A;
        // -C / 2AB
        mCo2AB = -auxiliaryElements.getC() * ooAB / 2.;
        // B / A(1 + B)
        BoABpo = BoA / (1. + auxiliaryElements.getB());
        // -2 * a / A
        m2aoA = -2 * auxiliaryElements.getSma() / A;
        // μ / a
        muoa = mu / auxiliaryElements.getSma();
        // R / a
        roa = provider.getAe() / auxiliaryElements.getSma();

        // Short period terms

        // h * k.
        hk = auxiliaryElements.getH() * auxiliaryElements.getK();
        // k² - h².
        k2mh2 = auxiliaryElements.getK() * auxiliaryElements.getK() - auxiliaryElements.getH() * auxiliaryElements.getH();
        // (k² - h²) / 2.
        k2mh2o2 = k2mh2 / 2.;
        // 1 / (n² * a²) = 1 / (n * A)
        oon2a2 = 1 / (A * n);
        // 1 / (n² * a) = a / (n * A)
        oon2a = auxiliaryElements.getSma() * oon2a2;
        // χ³ / (n² * a)
        x3on2a = XXX * oon2a;
        // χ / (n² * a²)
        xon2a2 = X * oon2a2;
        // (C * χ) / ( 2 * n² * a² )
        cxo2n2a2 = xon2a2 * auxiliaryElements.getC() / 2;
        // (χ²) / (n² * a² * (χ + 1 ) )
        x2on2a2xp1 = xon2a2 * X / (X + 1);
        // B * B
        BB = auxiliaryElements.getB() * auxiliaryElements.getB();
    }

    /** Get &Chi; = 1 / sqrt(1 - e²) = 1 / B.
     * @return &Chi;
     */
    public double getX() {
        return X;
    }

    /** Get &Chi;².
     * @return &Chi;².
     */
    public double getXX() {
        return XX;
    }

    /** Get &Chi;³.
     * @return &Chi;³
     */
    public double getXXX() {
        return XXX;
    }

    /** Get m2aoA = -2 * a / A.
     * @return m2aoA
     */
    public double getM2aoA() {
        return m2aoA;
    }

    /** Get B / A.
     * @return BoA
     */
    public double getBoA() {
        return BoA;
    }

    /** Get ooAB = 1 / (A * B).
     * @return ooAB
     */
    public double getOoAB() {
        return ooAB;
    }

    /** Get mCo2AB = -C / 2AB.
     * @return mCo2AB
     */
    public double getMCo2AB() {
        return mCo2AB;
    }

    /** Get BoABpo = B / A(1 + B).
     * @return BoABpo
     */
    public double getBoABpo() {
        return BoABpo;
    }

    /** Get μ / a .
     * @return muoa
     */
    public double getMuoa() {
        return muoa;
    }

    /** Get roa = R / a.
     * @return roa
     */
    public double getRoa() {
        return roa;
    }

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian mean motion in radians per second
     */
    public double getMeanMotion() {
        return n;
    }

    /** Get h * k.
     * @return hk
     */
    public double getHK() {
        return hk;
    }

    /** Get k² - h².
     * @return k2mh2
     */
    public double getK2MH2() {
        return k2mh2;
    }

    /** Get (k² - h²) / 2.
     * @return k2mh2o2
     */
    public double getK2MH2O2() {
        return k2mh2o2;
    }

    /** Get 1 / (n² * a²).
     * @return oon2a2
     */
    public double getOON2A2() {
        return oon2a2;
    }

    /** Get χ³ / (n² * a).
     * @return x3on2a
     */
    public double getX3ON2A() {
        return x3on2a;
    }

    /** Get χ / (n² * a²).
     * @return xon2a2
     */
    public double getXON2A2() {
        return xon2a2;
    }

    /** Get (C * χ) / ( 2 * n² * a² ).
     * @return cxo2n2a2
     */
    public double getCXO2N2A2() {
        return cxo2n2a2;
    }

    /** Get (χ²) / (n² * a² * (χ + 1 ) ).
     * @return x2on2a2xp1
     */
    public double getX2ON2A2XP1() {
        return x2on2a2xp1;
    }

    /** Get B * B.
     * @return BB
     */
    public double getBB() {
        return BB;
    }

}
