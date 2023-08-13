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

import java.io.Serializable;

import org.orekit.time.AbsoluteDate;

/** Elements of the bodies having an effect on nutation.
 * <p>This class is a simple placeholder,
 * it does not provide any processing method.</p>
 * @author Luc Maisonobe
 */
public final class BodiesElements extends  DelaunayArguments implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20170106L;

    /** Mean Mercury longitude. */
    private final double lMe;

    /** Mean Mercury longitude time derivative. */
    private final double lMeDot;

    /** Mean Venus longitude. */
    private final double lVe;

    /** Mean Venus longitude time derivative. */
    private final double lVeDot;

    /** Mean Earth longitude. */
    private final double lE;

    /** Mean Earth longitude time derivative. */
    private final double lEDot;

    /** Mean Mars longitude. */
    private final double lMa;

    /** Mean Mars longitude time derivative. */
    private final double lMaDot;

    /** Mean Jupiter longitude. */
    private final double lJu;

    /** Mean Jupiter longitude time derivative. */
    private final double lJuDot;

    /** Mean Saturn longitude. */
    private final double lSa;

    /** Mean Saturn longitude time derivative. */
    private final double lSaDot;

    /** Mean Uranus longitude. */
    private final double lUr;

    /** Mean Uranus longitude time derivative. */
    private final double lUrDot;

    /** Mean Neptune longitude. */
    private final double lNe;

    /** Mean Neptune longitude time derivative. */
    private final double lNeDot;

    /** General accumulated precession in longitude. */
    private final double pa;

    /** General accumulated precession in longitude time derivative. */
    private final double paDot;

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
    public BodiesElements(final AbsoluteDate date, final double tc, final double gamma, final double gammaDot,
                          final double l, final double lDot, final double lPrime, final double lPrimeDot,
                          final double f, final double fDot, final double d, final double dDot,
                          final double omega, final double omegaDot,
                          final double lMe, final double lMeDot, final double lVe, final double lVeDot,
                          final double lE, final double lEDot, final double lMa, final double lMaDot,
                          final double lJu, final double lJuDot, final double lSa, final double lSaDot,
                          final double lUr, final double lUrDot, final double lNe, final double lNeDot,
                          final double pa, final double paDot) {
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
    public double getLMe() {
        return lMe;
    }

    /** Get the mean Mercury longitude time derivative.
     * @return mean Mercury longitude time derivative.
     */
    public double getLMeDot() {
        return lMeDot;
    }

    /** Get the mean Venus longitude.
     * @return mean Venus longitude. */
    public double getLVe() {
        return lVe;
    }

    /** Get the mean Venus longitude time derivative.
     * @return mean Venus longitude time derivative. */
    public double getLVeDot() {
        return lVeDot;
    }

    /** Get the mean Earth longitude.
     * @return mean Earth longitude. */
    public double getLE() {
        return lE;
    }

    /** Get the mean Earth longitude time derivative.
     * @return mean Earth longitude time derivative. */
    public double getLEDot() {
        return lEDot;
    }

    /** Get the mean Mars longitude.
     * @return mean Mars longitude. */
    public double getLMa() {
        return lMa;
    }

    /** Get the mean Mars longitude time derivative.
     * @return mean Mars longitude time derivative. */
    public double getLMaDot() {
        return lMaDot;
    }

    /** Get the mean Jupiter longitude.
     * @return mean Jupiter longitude. */
    public double getLJu() {
        return lJu;
    }

    /** Get the mean Jupiter longitude time derivative.
     * @return mean Jupiter longitude time derivative. */
    public double getLJuDot() {
        return lJuDot;
    }

    /** Get the mean Saturn longitude.
     * @return mean Saturn longitude. */
    public double getLSa() {
        return lSa;
    }

    /** Get the mean Saturn longitude time derivative.
     * @return mean Saturn longitude time derivative. */
    public double getLSaDot() {
        return lSaDot;
    }

    /** Get the mean Uranus longitude.
     * @return mean Uranus longitude. */
    public double getLUr() {
        return lUr;
    }

    /** Get the mean Uranus longitude time derivative.
     * @return mean Uranus longitude time derivative. */
    public double getLUrDot() {
        return lUrDot;
    }

    /** Get the mean Neptune longitude.
     * @return mean Neptune longitude. */
    public double getLNe() {
        return lNe;
    }

    /** Get the mean Neptune longitude time derivative.
     * @return mean Neptune longitude time derivative. */
    public double getLNeDot() {
        return lNeDot;
    }

    /** Get the general accumulated precession in longitude.
     * @return general accumulated precession in longitude. */
    public double getPa() {
        return pa;
    }

    /** Get the general accumulated precession in longitude time derivative.
     * @return general accumulated precession in longitude time derivative. */
    public double getPaDot() {
        return paDot;
    }

}
