/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;

/**
 * This class is a container for the common "field" parameters used in {@link AbstractGaussianContribution}.
 * <p>
 * It performs parameters initialization at each integration step for the Gaussian contributions
 * </p>
 * @author Bryan Cazabonne
 * @since 10.0
 */
class FieldAbstractGaussianContributionContext<T extends RealFieldElement<T>> extends FieldForceModelContext<T> {

    // CHECKSTYLE: stop VisibilityModifier check

    /** 2 / (n² * a) . */
    protected T ton2a;

    /** 1 / A .*/
    protected T ooA;

    /** 1 / (A * B) .*/
    protected T ooAB;

    /** C / (2 * A * B) .*/
    protected T co2AB;

    /** 1 / (1 + B) .*/
    protected T ooBpo;

    /** 1 / μ .*/
    protected T ooMu;

    /** A = sqrt(μ * a). */
    private final T A;

    /** Keplerian mean motion. */
    private final T n;

    /** Central attraction coefficient. */
    private T mu;

    // CHECKSTYLE: resume VisibilityModifier check

    /**
     * Simple constructor.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param parameters parameters values of the force model parameters
     */
    FieldAbstractGaussianContributionContext(final FieldAuxiliaryElements<T> auxiliaryElements, final T[] parameters) {

        super(auxiliaryElements);

        // mu driver corresponds to the last term of parameters driver array
        mu = parameters[parameters.length - 1];

        // Keplerian mean motion
        final T absA = FastMath.abs(auxiliaryElements.getSma());
        n = FastMath.sqrt(mu.divide(absA)).divide(absA);
        // sqrt(μ * a)
        A = FastMath.sqrt(mu.multiply(auxiliaryElements.getSma()));
        // 1 / A
        ooA = A.reciprocal();
        // 1 / AB
        ooAB = ooA.divide(auxiliaryElements.getB());
        // C / 2AB
        co2AB = auxiliaryElements.getC().multiply(ooAB).divide(2.);
        // 1 / (1 + B)
        ooBpo = auxiliaryElements.getB().add(1.).reciprocal();
        // 2 / (n² * a)
        ton2a = (n.multiply(n).multiply(auxiliaryElements.getSma())).divide(2.).reciprocal();
        // 1 / mu
        ooMu  = mu.reciprocal();

    }

    /** Get central attraction coefficient.
     * @return mu
     */
    public T getMu() {
        return mu;
    }

    /** Get A = sqrt(μ * a).
     * @return A
     */
    public T getA() {
        return A;
    }

    /** Get ooA = 1 / A.
     * @return ooA
     */
    public T getOOA() {
        return ooA;
    }

    /** Get ooAB = 1 / (A * B).
     * @return ooAB
     */
    public T getOOAB() {
        return ooAB;
    }

    /** Get co2AB = C / 2AB.
     * @return co2AB
     */
    public T getCo2AB() {
        return co2AB;
    }

    /** Get ooBpo = 1 / (B + 1).
     * @return ooBpo
     */
    public T getOoBpo() {
        return ooBpo;
    }

    /** Get ton2a = 2 / (n² * a).
     * @return ton2a
     */
    public T getTon2a() {
        return ton2a;
    }

    /** Get ooMu = 1 / mu.
     * @return ooMu
     */
    public T getOoMU() {
        return ooMu;
    }

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian mean motion in radians per second
     */
    public T getMeanMotion() {
        return n;
    }

}
