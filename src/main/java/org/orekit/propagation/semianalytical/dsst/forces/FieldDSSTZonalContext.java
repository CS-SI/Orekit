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
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;

/** This class is a container for the field attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal DSSTZonal}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements)
 * initializeStep(AuxiliaryElements)}.
 * </p>
 */
public class FieldDSSTZonalContext<T extends RealFieldElement<T>> extends FieldForceModelContext<T> {

 // Common factors for potential computation
    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private T X;
    /** &Chi;². */
    private T XX;
    /** &Chi;³. */
    private T XXX;
    /** 1 / (A * B) .*/
    private T ooAB;
    /** B / A .*/
    private T BoA;
    /** B / A(1 + B) .*/
    private T BoABpo;
    /** -C / (2 * A * B) .*/
    private T mCo2AB;
    /** -2 * a / A .*/
    private T m2aoA;
    /** μ / a .*/
    private T muoa;
    /** R / a .*/
    private T roa;

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param fieldAuxiliaryElements auxiliary elements related to the current orbit
     * @param provider provider for spherical harmonics
     * @throws OrekitException if some specific error occurs
     */
    public FieldDSSTZonalContext(final FieldAuxiliaryElements<T> fieldAuxiliaryElements, final UnnormalizedSphericalHarmonicsProvider provider) {

        super(fieldAuxiliaryElements);

        // &Chi; = 1 / B
        X = fieldAuxiliaryElements.getB().reciprocal();
        XX = X.multiply(X);
        XXX = X.multiply(XX);
        // 1 / AB
        ooAB = (fieldAuxiliaryElements.getA().multiply(fieldAuxiliaryElements.getB())).reciprocal();
        // B / A
        BoA = fieldAuxiliaryElements.getB().divide(fieldAuxiliaryElements.getA());
        // -C / 2AB
        mCo2AB = fieldAuxiliaryElements.getC().multiply(ooAB).divide(2.).negate();
        // B / A(1 + B)
        BoABpo = BoA.divide(fieldAuxiliaryElements.getB().add(1.));
        // -2 * a / A
        m2aoA = fieldAuxiliaryElements.getSma().divide(fieldAuxiliaryElements.getA()).multiply(-2.);
        // μ / a
        muoa = fieldAuxiliaryElements.getSma().divide(provider.getMu()).reciprocal();
        // R / a
        roa = fieldAuxiliaryElements.getSma().divide(provider.getAe()).reciprocal();

    }

    /** Get &Chi; = 1 / sqrt(1 - e²) = 1 / B.
     * @return &Chi;
     */
    public T getX() {
        return X;
    }

    /** Get &Chi;².
     * @return &Chi;².
     */
    public T getXX() {
        return XX;
    }

    /** Get &Chi;³.
     * @return &Chi;³
     */
    public T getXXX() {
        return XXX;
    }

    /** Get m2aoA = -2 * a / A.
     * @return m2aoA
     */
    public T getM2aoA() {
        return m2aoA;
    }

    /** Get B / A.
     * @return BoA
     */
    public T getBoA() {
        return BoA;
    }

    /** Get ooAB = 1 / (A * B).
     * @return ooAB
     */
    public T getOoAB() {
        return ooAB;
    }

    /** Get mCo2AB = -C / 2AB.
     * @return mCo2AB
     */
    public T getMCo2AB() {
        return mCo2AB;
    }

    /** Get BoABpo = B / A(1 + B).
     * @return BoABpo
     */
    public T getBoABpo() {
        return BoABpo;
    }

    /** Get μ / a .
     * @return muoa
     */
    public T getMuoa() {
        return muoa;
    }

    /** Get roa = R / a.
     * @return roa
     */
    public T getRoa() {
        return roa;
    }

}
