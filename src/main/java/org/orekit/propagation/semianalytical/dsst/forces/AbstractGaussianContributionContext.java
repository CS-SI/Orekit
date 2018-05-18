/* Copyright 2002-2018 CS Systèmes d'Information
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

import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/** This class is a container for the attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.AbstractGaussianontribution AbstractGaussianContribution}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements)
 * initializeStep(AuxiliaryElements)}.
 * </p>
 */
class AbstractGaussianContributionContext extends ForceModelContext {

    // CHECKSTYLE: stop VisibilityModifierCheck

    /** 2 / (n² * a) . */
    protected double ton2a;
    /** 1 / A .*/
    protected double ooA;
    /** 1 / (A * B) .*/
    protected double ooAB;
    /** C / (2 * A * B) .*/
    protected double co2AB;
    /** 1 / (1 + B) .*/
    protected double ooBpo;
    /** 1 / μ .*/
    protected double ooMu;

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param parameters values of the force model parameters
     * @throws OrekitException if some specific error occurs
     */
    AbstractGaussianContributionContext(final AuxiliaryElements auxiliaryElements, final double[] parameters)
        throws OrekitException {

        super(auxiliaryElements);

        // 1 / A
        ooA = 1. / auxiliaryElements.getA();
        // 1 / AB
        ooAB = ooA / auxiliaryElements.getB();
        // C / 2AB
        co2AB = auxiliaryElements.getC() * ooAB / 2.;
        // 1 / (1 + B)
        ooBpo = 1. / (1. + auxiliaryElements.getB());
        // 2 / (n² * a)
        ton2a = 2. / (auxiliaryElements.getMeanMotion() * auxiliaryElements.getMeanMotion() * auxiliaryElements.getSma());
        // 1 / mu
        ooMu  = 1. / auxiliaryElements.getMu();

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

}
