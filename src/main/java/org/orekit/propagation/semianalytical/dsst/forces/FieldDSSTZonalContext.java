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

import org.hipparchus.CalculusFieldElement;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;

/**
 * This class is a container for the common "field" parameters used in {@link DSSTZonal}.
 * <p>
 * It performs parameters initialization at each integration step for the Zonal contribution
 * to the central body gravitational perturbation.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.0
 * @param <T> type of the field elements
 */
public class FieldDSSTZonalContext<T extends CalculusFieldElement<T>> extends FieldDSSTGravityContext<T> {

    /** &Chi;³ = 1 / B³. */
    private final T chi3;

    // Short period terms
    /** h * k. */
    private T hk;
    /** k² - h². */
    private T k2mh2;
    /** (k² - h²) / 2. */
    private T k2mh2o2;
    /** 1 / (n² * a²). */
    private T oon2a2;
    /** 1 / (n² * a) . */
    private T oon2a;
    /** χ³ / (n² * a). */
    private T x3on2a;
    /** χ / (n² * a²). */
    private T xon2a2;
    /** (C * χ) / ( 2 * n² * a² ). */
    private T cxo2n2a2;
    /** (χ²) / (n² * a² * (χ + 1 ) ). */
    private T x2on2a2xp1;
    /** B * B. */
    private T BB;

    /** Constructor with central body frame equals orbit frame.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param provider          provider for spherical harmonics
     * @param parameters        values of the force model parameters (only 1 values
     * for each parameters corresponding to state date) obtained by calling the extract
     * parameter method {@link #extractParameters(double[], AbsoluteDate)}
     * to selected the right value for state date or by getting the parameters for a specific date
     * @deprecated since 12.2 and issue 1104, should be removed in 13.0
     */
    @Deprecated
    FieldDSSTZonalContext(final FieldAuxiliaryElements<T> auxiliaryElements,
                          final UnnormalizedSphericalHarmonicsProvider provider,
                          final T[] parameters) {

        this(auxiliaryElements, auxiliaryElements.getFrame(), provider, parameters);
    }

    /** Constructor with central body frame potentially different from orbit frame.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param centralBodyFrame  rotating body frame
     * @param provider          provider for spherical harmonics
     * @param parameters        values of the force model parameters (only 1 values
     * for each parameters corresponding to state date) obtained by calling the extract
     * parameter method {@link #extractParameters(double[], AbsoluteDate)}
     * to selected the right value for state date or by getting the parameters for a specific date
     * @since 12.2
     */
    FieldDSSTZonalContext(final FieldAuxiliaryElements<T> auxiliaryElements,
                          final Frame centralBodyFrame,
                          final UnnormalizedSphericalHarmonicsProvider provider,
                          final T[] parameters) {

        super(auxiliaryElements, centralBodyFrame, provider, parameters);

        // Chi3
        final T chi = getChi();
        this.chi3 = chi.multiply(getChi2());

        // Short period terms
        // -----

        // h * k.
        hk = auxiliaryElements.getH().multiply(auxiliaryElements.getK());
        // k² - h².
        k2mh2 = auxiliaryElements.getK().multiply(auxiliaryElements.getK()).subtract(auxiliaryElements.getH().multiply(auxiliaryElements.getH()));
        // (k² - h²) / 2.
        k2mh2o2 = k2mh2.divide(2.);
        // 1 / (n² * a²) = 1 / (n * A)
        oon2a2 = (getA().multiply(getMeanMotion())).reciprocal();
        // 1 / (n² * a) = a / (n * A)
        oon2a = auxiliaryElements.getSma().multiply(oon2a2);
        // χ³ / (n² * a)
        x3on2a = chi3.multiply(oon2a);
        // χ / (n² * a²)
        xon2a2 = chi.multiply(oon2a2);
        // (C * χ) / ( 2 * n² * a² )
        cxo2n2a2 = xon2a2.multiply(auxiliaryElements.getC()).divide(2.);
        // (χ²) / (n² * a² * (χ + 1 ) )
        x2on2a2xp1 = xon2a2.multiply(chi).divide(chi.add(1.));
        // B * B
        BB = auxiliaryElements.getB().multiply(auxiliaryElements.getB());
    }


    /** Getter for the &Chi;³.
     * @return the &Chi;³
     */
    public T getChi3() {
        return chi3;
    }

    /** Get h * k.
     * @return hk
     */
    public T getHK() {
        return hk;
    }

    /** Get k² - h².
     * @return k2mh2
     */
    public T getK2MH2() {
        return k2mh2;
    }

    /** Get (k² - h²) / 2.
     * @return k2mh2o2
     */
    public T getK2MH2O2() {
        return k2mh2o2;
    }

    /** Get 1 / (n² * a²).
     * @return oon2a2
     */
    public T getOON2A2() {
        return oon2a2;
    }

    /** Get χ³ / (n² * a).
     * @return x3on2a
     */
    public T getX3ON2A() {
        return x3on2a;
    }

    /** Get χ / (n² * a²).
     * @return xon2a2
     */
    public T getXON2A2() {
        return xon2a2;
    }

    /** Get (C * χ) / ( 2 * n² * a² ).
     * @return cxo2n2a2
     */
    public T getCXO2N2A2() {
        return cxo2n2a2;
    }

    /** Get (χ²) / (n² * a² * (χ + 1 ) ).
     * @return x2on2a2xp1
     */
    public T getX2ON2A2XP1() {
        return x2on2a2xp1;
    }

    /** Get B * B.
     * @return BB
     */
    public T getBB() {
        return BB;
    }

}
