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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
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
public class DSSTZonalContext extends DSSTGravityContext {

    /** &Chi;³ = 1 / B³. */
    private final double chi3;

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
     * Constructor with central body frame potentially different than orbit frame.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param bodyFixedFrame    rotating body frame
     * @param provider          provider for spherical harmonics
     * @param parameters        values of the force model parameters
     * @since 12.2
     */
    DSSTZonalContext(final AuxiliaryElements auxiliaryElements,
                     final Frame bodyFixedFrame,
                     final UnnormalizedSphericalHarmonicsProvider provider,
                     final double[] parameters) {

        super(auxiliaryElements, bodyFixedFrame, provider, parameters);

        // Chi3
        final double chi = getChi();
        this.chi3 = chi * getChi2();

        // Short period terms
        // -----

        // h * k.
        hk = auxiliaryElements.getH() * auxiliaryElements.getK();
        // k² - h².
        k2mh2 = auxiliaryElements.getK() * auxiliaryElements.getK() - auxiliaryElements.getH() * auxiliaryElements.getH();
        // (k² - h²) / 2.
        k2mh2o2 = k2mh2 / 2.;
        // 1 / (n² * a²) = 1 / (n * A)
        oon2a2 = 1 / (getA() * getMeanMotion());
        // 1 / (n² * a) = a / (n * A)
        oon2a = auxiliaryElements.getSma() * oon2a2;
        // χ³ / (n² * a)
        x3on2a = chi3 * oon2a;
        // χ / (n² * a²)
        xon2a2 = getChi() * oon2a2;
        // (C * χ) / ( 2 * n² * a² )
        cxo2n2a2 = xon2a2 * auxiliaryElements.getC() / 2;
        // (χ²) / (n² * a² * (χ + 1 ) )
        x2on2a2xp1 = xon2a2 * chi / (chi + 1);
        // B * B
        BB = auxiliaryElements.getB() * auxiliaryElements.getB();
    }

    /** Getter for the &Chi;³.
     * @return the &Chi;³
     */
    public double getChi3() {
        return chi3;
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
