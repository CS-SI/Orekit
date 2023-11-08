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
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/**
 * This class is a container for the common parameters used in {@link AbstractGaussianContribution}.
 * <p>
 * It performs parameters initialization at each integration step for the Gaussian contributions
 * </p>
 * @author Bryan Cazabonne
 * @since 10.0
 */
public class AbstractGaussianContributionContext extends ForceModelContext {

    // CHECKSTYLE: stop VisibilityModifier check

    /** 2 / (n² * a) . */
    protected double ton2a;

    /** 1 / A . */
    protected double ooA;

    /** 1 / (A * B) . */
    protected double ooAB;

    /** C / (2 * A * B) . */
    protected double co2AB;

    /** 1 / (1 + B) . */
    protected double ooBpo;

    /** 1 / μ . */
    protected double ooMu;

    /** A = sqrt(μ * a). */
    private final double A;

    /** Keplerian mean motion. */
    private final double n;

    /** Central attraction coefficient. */
    private double mu;

    // CHECKSTYLE: resume VisibilityModifier check

    /**
     * Simple constructor.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param parameters        parameters values of the force model parameters
     *                          only 1 value for each parameterDriver
     */
    AbstractGaussianContributionContext(final AuxiliaryElements auxiliaryElements, final double[] parameters) {

        super(auxiliaryElements);

        // mu driver corresponds to the last term of parameters driver array
        mu = parameters[parameters.length - 1];

        // Keplerian Mean Motion
        final double absA = FastMath.abs(auxiliaryElements.getSma());
        n = FastMath.sqrt(mu / absA) / absA;

        // sqrt(μ * a)
        A = FastMath.sqrt(mu * auxiliaryElements.getSma());
        // 1 / A
        ooA = 1. / A;
        // 1 / AB
        ooAB = ooA / auxiliaryElements.getB();
        // C / 2AB
        co2AB = auxiliaryElements.getC() * ooAB / 2.;
        // 1 / (1 + B)
        ooBpo = 1. / (1. + auxiliaryElements.getB());
        // 2 / (n² * a)
        ton2a = 2. / (n * n * auxiliaryElements.getSma());
        // 1 / mu
        ooMu = 1. / mu;

    }

    /** Get central attraction coefficient.
     * @return mu
     */
    public double getMu() {
        return mu;
    }

    /** Get ooA = 1 / A.
     * @return ooA
     */
    public double getOOA() {
        return ooA;
    }

    /** Get ooAB = 1 / (A * B).
     * @return ooAB
     */
    public double getOOAB() {
        return ooAB;
    }

    /** Get co2AB = C / 2AB.
     * @return co2AB
     */
    public double getCo2AB() {
        return co2AB;
    }

    /** Get ooBpo = 1 / (B + 1).
     * @return ooBpo
     */
    public double getOoBpo() {
        return ooBpo;
    }

    /** Get ton2a = 2 / (n² * a).
     * @return ton2a
     */
    public double getTon2a() {
        return ton2a;
    }

    /** Get ooMu = 1 / mu.
     * @return ooMu
     */
    public double getOoMU() {
        return ooMu;
    }

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian mean motion in radians per second
     */
    public double getMeanMotion() {
        return n;
    }
}
