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
package org.orekit.data;

import org.hipparchus.CalculusFieldElement;
import org.orekit.time.FieldAbsoluteDate;

/** Elements of the bodies having an effect on nutation.
 * <p>This class is a simple placeholder,
 * it does not provide any processing method.</p>
 * @param <T> the type of the field elements
 * @see BodiesElements
 * @author Luc Maisonobe
 * @since 6.1
 */
public final class FieldBodiesElements<T extends CalculusFieldElement<T>> extends  FieldDelaunayArguments<T> {

    /** Mean Mercury longitude. */
    private final T lMe;

    /** Mean Mercury longitude time derivative. */
    private final T lMeDot;

    /** Mean Venus longitude. */
    private final T lVe;

    /** Mean Venus longitude time derivative. */
    private final T lVeDot;

    /** Mean Earth longitude. */
    private final T lE;

    /** Mean Earth longitude time derivative. */
    private final T lEDot;

    /** Mean Mars longitude. */
    private final T lMa;

    /** Mean Mars longitude time derivative. */
    private final T lMaDot;

    /** Mean Jupiter longitude. */
    private final T lJu;

    /** Mean Jupiter longitude time derivative. */
    private final T lJuDot;

    /** Mean Saturn longitude. */
    private final T lSa;

    /** Mean Saturn longitude time derivative. */
    private final T lSaDot;

    /** Mean Uranus longitude. */
    private final T lUr;

    /** Mean Uranus longitude time derivative. */
    private final T lUrDot;

    /** Mean Neptune longitude. */
    private final T lNe;

    /** Mean Neptune longitude time derivative. */
    private final T lNeDot;

    /** General accumulated precession in longitude. */
    private final T pa;

    /** General accumulated precession in longitude time derivative. */
    private final T paDot;

    /** Simple constructor.
     * @param date current date
     * @param tc offset in Julian centuries
     * @param gamma tide parameter γ = GMST + π
     * @param gammaDot tide parameter γ = GMST + π time derivative
     * @param l mean anomaly of the Moon
     * @param lDot mean anomaly of the Moon time derivative
     * @param lPrime mean anomaly of the Sun
     * @param lPrimeDot mean anomaly of the Sun time derivative
     * @param f L - Ω where L is the mean longitude of the Moon
     * @param fDot L - Ω where L is the mean longitude of the Moon time derivative
     * @param d mean elongation of the Moon from the Sun
     * @param dDot mean elongation of the Moon from the Sun time derivative
     * @param omega mean longitude of the ascending node of the Moon
     * @param omegaDot mean longitude of the ascending node of the Moon time derivative
     * @param lMe mean Mercury longitude
     * @param lMeDot mean Mercury longitude time derivative
     * @param lVe mean Venus longitude
     * @param lVeDot mean Venus longitude time derivative
     * @param lE mean Earth longitude
     * @param lEDot mean Earth longitude time derivative
     * @param lMa mean Mars longitude
     * @param lMaDot mean Mars longitude time derivative
     * @param lJu mean Jupiter longitude
     * @param lJuDot mean Jupiter longitude time derivative
     * @param lSa mean Saturn longitude
     * @param lSaDot mean Saturn longitude time derivative
     * @param lUr mean Uranus longitude
     * @param lUrDot mean Uranus longitude time derivative
     * @param lNe mean Neptune longitude
     * @param lNeDot mean Neptune longitude time derivative
     * @param pa general accumulated precession in longitude
     * @param paDot general accumulated precession in longitude time derivative
     */
    public FieldBodiesElements(final FieldAbsoluteDate<T> date, final T tc, final T gamma, final T gammaDot,
                               final T l, final T lDot, final T lPrime, final T lPrimeDot,
                               final T f, final T fDot, final T d, final T dDot,
                               final T omega, final T omegaDot,
                               final T lMe, final T lMeDot, final T lVe, final T lVeDot,
                               final T lE, final T lEDot, final T lMa, final T lMaDot,
                               final T lJu, final T lJuDot, final T lSa, final T lSaDot,
                               final T lUr, final T lUrDot, final T lNe, final T lNeDot,
                               final T pa, final T paDot) {
        super(date, tc, gamma, gammaDot, l, lDot, lPrime, lPrimeDot, f, fDot, d, dDot, omega, omegaDot);
        this.lMe    = lMe;
        this.lMeDot = lMeDot;
        this.lVe    = lVe;
        this.lVeDot = lVeDot;
        this.lE     = lE;
        this.lEDot  = lEDot;
        this.lMa    = lMa;
        this.lMaDot = lMaDot;
        this.lJu    = lJu;
        this.lJuDot = lJuDot;
        this.lSa    = lSa;
        this.lSaDot = lSaDot;
        this.lUr    = lUr;
        this.lUrDot = lUrDot;
        this.lNe    = lNe;
        this.lNeDot = lNeDot;
        this.pa     = pa;
        this.paDot  = paDot;
    }

    /** Get the mean Mercury longitude.
     * @return mean Mercury longitude.
     */
    public T getLMe() {
        return lMe;
    }

    /** Get the mean Mercury longitude time derivative.
     * @return mean Mercury longitude time derivative.
     */
    public T getLMeDot() {
        return lMeDot;
    }

    /** Get the mean Venus longitude.
     * @return mean Venus longitude. */
    public T getLVe() {
        return lVe;
    }

    /** Get the mean Venus longitude time derivative.
     * @return mean Venus longitude time derivative. */
    public T getLVeDot() {
        return lVeDot;
    }

    /** Get the mean Earth longitude.
     * @return mean Earth longitude. */
    public T getLE() {
        return lE;
    }

    /** Get the mean Earth longitude time derivative.
     * @return mean Earth longitude time derivative. */
    public T getLEDot() {
        return lEDot;
    }

    /** Get the mean Mars longitude.
     * @return mean Mars longitude. */
    public T getLMa() {
        return lMa;
    }

    /** Get the mean Mars longitude time derivative.
     * @return mean Mars longitude time derivative. */
    public T getLMaDot() {
        return lMaDot;
    }

    /** Get the mean Jupiter longitude.
     * @return mean Jupiter longitude. */
    public T getLJu() {
        return lJu;
    }

    /** Get the mean Jupiter longitude time derivative.
     * @return mean Jupiter longitude time derivative. */
    public T getLJuDot() {
        return lJuDot;
    }

    /** Get the mean Saturn longitude.
     * @return mean Saturn longitude. */
    public T getLSa() {
        return lSa;
    }

    /** Get the mean Saturn longitude time derivative.
     * @return mean Saturn longitude time derivative. */
    public T getLSaDot() {
        return lSaDot;
    }

    /** Get the mean Uranus longitude.
     * @return mean Uranus longitude. */
    public T getLUr() {
        return lUr;
    }

    /** Get the mean Uranus longitude time derivative.
     * @return mean Uranus longitude time derivative. */
    public T getLUrDot() {
        return lUrDot;
    }

    /** Get the mean Neptune longitude.
     * @return mean Neptune longitude. */
    public T getLNe() {
        return lNe;
    }

    /** Get the mean Neptune longitude time derivative.
     * @return mean Neptune longitude time derivative. */
    public T getLNeDot() {
        return lNeDot;
    }

    /** Get the general accumulated precession in longitude.
     * @return general accumulated precession in longitude. */
    public T getPa() {
        return pa;
    }

    /** Get the general accumulated precession in longitude time derivative.
     * @return general accumulated precession in longitude time derivative. */
    public T getPaDot() {
        return paDot;
    }

}
