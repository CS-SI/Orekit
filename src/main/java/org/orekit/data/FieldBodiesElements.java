/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.data;

import java.io.Serializable;

import org.hipparchus.RealFieldElement;
import org.orekit.time.AbsoluteDate;

/** Elements of the bodies having an effect on nutation.
 * <p>This class is a simple placeholder,
 * it does not provide any processing method.</p>
 * @param <T> the type of the field elements
 * @see BodiesElements
 * @author Luc Maisonobe
 * @since 6.1
 */
public final class FieldBodiesElements<T extends RealFieldElement<T>> extends  FieldDelaunayArguments<T> implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130418L;

    /** Mean Mercury longitude. */
    private final T lMe;

    /** Mean Venus longitude. */
    private final T lVe;

    /** Mean Earth longitude. */
    private final T lE;

    /** Mean Mars longitude. */
    private final T lMa;

    /** Mean Jupiter longitude. */
    private final T lJu;

    /** Mean Saturn longitude. */
    private final T lSa;

    /** Mean Uranus longitude. */
    private final T lUr;

    /** Mean Neptune longitude. */
    private final T lNe;

    /** General accumulated precession in longitude. */
    private final T pa;

    /** Simple constructor.
     * @param date current date
     * @param tc offset in Julian centuries
     * @param gamma tide parameter γ = GMST + π
     * @param l mean anomaly of the Moon
     * @param lPrime mean anomaly of the Sun
     * @param f L - Ω where L is the mean longitude of the Moon
     * @param d mean elongation of the Moon from the Sun
     * @param omega mean longitude of the ascending node of the Moon
     * @param lMe mean Mercury longitude
     * @param lVe mean Venus longitude
     * @param lE mean Earth longitude
     * @param lMa mean Mars longitude
     * @param lJu mean Jupiter longitude
     * @param lSa mean Saturn longitude
     * @param lUr mean Uranus longitude
     * @param lNe mean Neptune longitude
     * @param pa general accumulated precession in longitude
     */
    public FieldBodiesElements(final AbsoluteDate date, final T tc, final T gamma,
                               final T l, final T lPrime, final T f, final T d, final T omega,
                               final T lMe, final T lVe, final T lE, final T lMa, final T lJu,
                               final T lSa, final T lUr, final T lNe, final T pa) {
        super(date, tc, gamma, l, lPrime, f, d, omega);
        this.lMe = lMe;
        this.lVe = lVe;
        this.lE  = lE;
        this.lMa = lMa;
        this.lJu = lJu;
        this.lSa = lSa;
        this.lUr = lUr;
        this.lNe = lNe;
        this.pa  = pa;
    }

    /** Get the mean Mercury longitude.
     * @return mean Mercury longitude.
     */
    public T getLMe() {
        return lMe;
    }

    /** Get the mean Venus longitude.
     * @return mean Venus longitude. */
    public T getLVe() {
        return lVe;
    }

    /** Get the mean Earth longitude.
     * @return mean Earth longitude. */
    public T getLE() {
        return lE;
    }

    /** Get the mean Mars longitude.
     * @return mean Mars longitude. */
    public T getLMa() {
        return lMa;
    }

    /** Get the mean Jupiter longitude.
     * @return mean Jupiter longitude. */
    public T getLJu() {
        return lJu;
    }

    /** Get the mean Saturn longitude.
     * @return mean Saturn longitude. */
    public T getLSa() {
        return lSa;
    }

    /** Get the mean Uranus longitude.
     * @return mean Uranus longitude. */
    public T getLUr() {
        return lUr;
    }

    /** Get the mean Neptune longitude.
     * @return mean Neptune longitude. */
    public T getLNe() {
        return lNe;
    }

    /** Get the general accumulated precession in longitude.
     * @return general accumulated precession in longitude. */
    public T getPa() {
        return pa;
    }

}
