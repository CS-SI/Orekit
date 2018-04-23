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

import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;

/** This class is a container for the field attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.AbstractGaussianontribution AbstractGaussianContribution}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements)
 * initializeStep(AuxiliaryElements)}.
 * </p>
 */
public class FieldAbstractGaussianContributionContext<T extends RealFieldElement<T>> extends FieldForceModelContext<T> {

    // CHECKSTYLE: stop VisibilityModifierCheck

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
    protected double ooMu;

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @throws OrekitException if some specific error occurs
     */
    public FieldAbstractGaussianContributionContext(final FieldAuxiliaryElements<T> auxiliaryElements) throws OrekitException {

        super(auxiliaryElements);

        // 1 / A
        ooA = auxiliaryElements.getA().reciprocal();
        // 1 / AB
        ooAB = ooA.divide(auxiliaryElements.getB());
        // C / 2AB
        co2AB = auxiliaryElements.getC().multiply(ooAB).divide(2.);
        // 1 / (1 + B)
        ooBpo = auxiliaryElements.getB().add(1.).reciprocal();
        // 2 / (n² * a)
        ton2a = (auxiliaryElements.getMeanMotion().multiply(auxiliaryElements.getMeanMotion()).multiply(auxiliaryElements.getSma())).divide(2.).reciprocal();
        // 1 / mu
        ooMu  = 1 / auxiliaryElements.getMu();

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
    public double getOoMU() {
        return ooMu;
    }

}
