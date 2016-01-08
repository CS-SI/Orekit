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

import org.orekit.time.AbsoluteDate;

/** Elements of the bodies having an effect on nutation.
 * <p>This class is a simple placeholder,
 * it does not provide any processing method.</p>
 * @author Luc Maisonobe
 */
public final class BodiesElements extends  DelaunayArguments implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130418L;

    /** Mean Mercury longitude. */
    private final double lMe;

    /** Mean Venus longitude. */
    private final double lVe;

    /** Mean Earth longitude. */
    private final double lE;

    /** Mean Mars longitude. */
    private final double lMa;

    /** Mean Jupiter longitude. */
    private final double lJu;

    /** Mean Saturn longitude. */
    private final double lSa;

    /** Mean Uranus longitude. */
    private final double lUr;

    /** Mean Neptune longitude. */
    private final double lNe;

    /** General accumulated precession in longitude. */
    private final double pa;

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
    public BodiesElements(final AbsoluteDate date, final double tc, final double gamma,
                          final double l, final double lPrime, final double f, final double d, final double omega,
                          final double lMe, final double lVe, final double lE, final double lMa, final double lJu,
                          final double lSa, final double lUr, final double lNe, final double pa) {
        super(date, tc, gamma, l, lPrime, f, d, omega);
        this.lMe    = lMe;
        this.lVe    = lVe;
        this.lE     = lE;
        this.lMa    = lMa;
        this.lJu    = lJu;
        this.lSa    = lSa;
        this.lUr    = lUr;
        this.lNe    = lNe;
        this.pa     = pa;
    }

    /** Get the mean Mercury longitude.
     * @return mean Mercury longitude.
     */
    public double getLMe() {
        return lMe;
    }

    /** Get the mean Venus longitude.
     * @return mean Venus longitude. */
    public double getLVe() {
        return lVe;
    }

    /** Get the mean Earth longitude.
     * @return mean Earth longitude. */
    public double getLE() {
        return lE;
    }

    /** Get the mean Mars longitude.
     * @return mean Mars longitude. */
    public double getLMa() {
        return lMa;
    }

    /** Get the mean Jupiter longitude.
     * @return mean Jupiter longitude. */
    public double getLJu() {
        return lJu;
    }

    /** Get the mean Saturn longitude.
     * @return mean Saturn longitude. */
    public double getLSa() {
        return lSa;
    }

    /** Get the mean Uranus longitude.
     * @return mean Uranus longitude. */
    public double getLUr() {
        return lUr;
    }

    /** Get the mean Neptune longitude.
     * @return mean Neptune longitude. */
    public double getLNe() {
        return lNe;
    }

    /** Get the general accumulated precession in longitude.
     * @return general accumulated precession in longitude. */
    public double getPa() {
        return pa;
    }

}
