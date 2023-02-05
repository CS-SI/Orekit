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
import org.orekit.time.TimeStamped;

/** Delaunay arguments used for nutation or tides.
 * <p>This class is a simple placeholder,
 * it does not provide any processing method.</p>
 * @author Luc Maisonobe
 * @since 6.1
 */
public class DelaunayArguments implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20170106L;

    /** Date. */
    private final AbsoluteDate date;

    /** Offset in Julian centuries. */
    private final double tc;

    /** Tide parameter γ = GMST + π. */
    private final double gamma;

    /** Tide parameter γ = GMST + π time derivative. */
    private final double gammaDot;

    /** Mean anomaly of the Moon. */
    private final double l;

    /** Mean anomaly of the Moon time derivative. */
    private final double lDot;

    /** Mean anomaly of the Sun. */
    private final double lPrime;

    /** Mean anomaly of the Sun time derivative. */
    private final double lPrimeDot;

    /** L - Ω where L is the mean longitude of the Moon. */
    private final double f;

    /** L - Ω where L is the mean longitude of the Moon time derivative. */
    private final double fDot;

    /** Mean elongation of the Moon from the Sun. */
    private final double d;

    /** Mean elongation of the Moon from the Sun time derivative. */
    private final double dDot;

    /** Mean longitude of the ascending node of the Moon. */
    private final double omega;

    /** Mean longitude of the ascending node of the Moon time derivative. */
    private final double omegaDot;

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
     */
    public DelaunayArguments(final AbsoluteDate date, final double tc, final double gamma, final double gammaDot,
                             final double l, final double lDot, final double lPrime, final double lPrimeDot,
                             final double f, final double fDot, final double d, final double dDot,
                             final double omega, final double omegaDot) {
        this.date      = date;
        this.tc        = tc;
        this.gamma     = gamma;
        this.gammaDot  = gammaDot;
        this.l         = l;
        this.lDot      = lDot;
        this.lPrime    = lPrime;
        this.lPrimeDot = lPrimeDot;
        this.f         = f;
        this.fDot      = fDot;
        this.d         = d;
        this.dDot      = dDot;
        this.omega     = omega;
        this.omegaDot  = omegaDot;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the offset in Julian centuries.
     * @return offset in Julian centuries
     */
    public double getTC() {
        return tc;
    }

    /** Get the tide parameter γ = GMST + π.
     * @return tide parameter γ = GMST + π
     */
    public double getGamma() {
        return gamma;
    }

    /** Get the tide parameter γ = GMST + π time derivative.
     * @return tide parameter γ = GMST + π time derivative
     */
    public double getGammaDot() {
        return gammaDot;
    }

    /** Get the mean anomaly of the Moon.
     * @return mean anomaly of the Moon
     */
    public double getL() {
        return l;
    }

    /** Get the mean anomaly of the Moon time derivative.
     * @return mean anomaly of the Moon time derivative
     */
    public double getLDot() {
        return lDot;
    }

    /** Get the mean anomaly of the Sun.
     * @return mean anomaly of the Sun.
     */
    public double getLPrime() {
        return lPrime;
    }

    /** Get the mean anomaly of the Sun time derivative.
     * @return mean anomaly of the Sun time derivative.
     */
    public double getLPrimeDot() {
        return lPrimeDot;
    }

    /** Get L - Ω where L is the mean longitude of the Moon.
     * @return L - Ω
     */
    public double getF() {
        return f;
    }

    /** Get L - Ω where L is the mean longitude of the Moon time derivative.
     * @return L - Ω time derivative
     */
    public double getFDot() {
        return fDot;
    }

    /** Get the mean elongation of the Moon from the Sun.
     * @return mean elongation of the Moon from the Sun.
     */
    public double getD() {
        return d;
    }

    /** Get the mean elongation of the Moon from the Sun time derivative.
     * @return mean elongation of the Moon from the Sun time derivative.
     */
    public double getDDot() {
        return dDot;
    }

    /** Get the mean longitude of the ascending node of the Moon.
     * @return mean longitude of the ascending node of the Moon.
     */
    public double getOmega() {
        return omega;
    }

    /** Get the mean longitude of the ascending node of the Moon time derivative.
     * @return mean longitude of the ascending node of the Moon time derivative.
     */
    public double getOmegaDot() {
        return omegaDot;
    }

}
