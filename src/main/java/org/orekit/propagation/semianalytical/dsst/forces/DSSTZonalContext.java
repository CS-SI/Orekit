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
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/** This class is a container for the attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal DSSTZonal}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements)
 * initializeStep(AuxiliaryElements)}.
 * </p>
 */
public class DSSTZonalContext extends ForceModelContext {

    // Common factors for potential computation
    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private double X;
    /** &Chi;². */
    private double XX;
    /** &Chi;³. */
    private double XXX;
    /** 1 / (A * B) .*/
    private double ooAB;
    /** B / A .*/
    private double BoA;
    /** B / A(1 + B) .*/
    private double BoABpo;
    /** -C / (2 * A * B) .*/
    private double mCo2AB;
    /** -2 * a / A .*/
    private double m2aoA;
    /** μ / a .*/
    private double muoa;
    /** R / a .*/
    private double roa;

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param provider provider for spherical harmonics
     * @throws OrekitException if some specific error occurs
     */
    public DSSTZonalContext(final AuxiliaryElements auxiliaryElements, final UnnormalizedSphericalHarmonicsProvider provider) {

        super(auxiliaryElements);

        // &Chi; = 1 / B
        X = 1. / auxiliaryElements.getB();
        XX = X * X;
        XXX = X * XX;
        // 1 / AB
        ooAB = 1. / (auxiliaryElements.getA() * auxiliaryElements.getB());
        // B / A
        BoA = auxiliaryElements.getB() / auxiliaryElements.getA();
        // -C / 2AB
        mCo2AB = -auxiliaryElements.getC() * ooAB / 2.;
        // B / A(1 + B)
        BoABpo = BoA / (1. + auxiliaryElements.getB());
        // -2 * a / A
        m2aoA = -2 * auxiliaryElements.getSma() / auxiliaryElements.getA();
        // μ / a
        muoa = provider.getMu() / auxiliaryElements.getSma();
        // R / a
        roa = provider.getAe() / auxiliaryElements.getSma();

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

}
