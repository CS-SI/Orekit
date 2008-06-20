/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.frames.series;

import java.io.Serializable;

/** Elements of the bodies having an effect on nutation.
 * <p>This class is a simple placeholder,
 * it does not provide any processing method.</p>
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public final class BodiesElements implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 9193325350743225370L;

    /** Mean anomaly of the Moon. */
    private final double l;

    /** Mean anomaly of the Sun. */
    private final double lPrime;

    /** L - &Omega; where L is the mean longitude of the Moon. */
    private final double f;

    /** Mean elongation of the Moon from the Sun. */
    private final double d;

    /** Mean longitude of the ascending node of the Moon. */
    private final double omega;

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
     * @param l mean anomaly of the Moon
     * @param lPrime mean anomaly of the Sun
     * @param f L - &Omega; where L is the mean longitude of the Moon
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
    public BodiesElements(final double l, final double lPrime, final double f, final double d, final double omega,
                          final double lMe, final double lVe, final double lE, final double lMa, final double lJu,
                          final double lSa, final double lUr, final double lNe, final double pa) {
        this.l      = l;
        this.lPrime = lPrime;
        this.f      = f;
        this.d      = d;
        this.omega  = omega;
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

    /** Get the mean anomaly of the Moon.
     * @return mean anomaly of the Moon
     */
    public double getL() {
        return l;
    }

    /** Get the mean anomaly of the Sun.
     * @return mean anomaly of the Sun.
     */
    public double getLPrime() {
        return lPrime;
    }

    /** Get L - &Omega; where L is the mean longitude of the Moon.
     * @return L - &Omega;
     */
    public double getF() {
        return f;
    }

    /** Get the mean elongation of the Moon from the Sun.
     * @return mean elongation of the Moon from the Sun.
     */
    public double getD() {
        return d;
    }

    /** Get the mean longitude of the ascending node of the Moon.
     * @return mean longitude of the ascending node of the Moon.
     */
    public double getOmega() {
        return omega;
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
