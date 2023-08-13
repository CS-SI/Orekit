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
import org.orekit.time.FieldTimeStamped;

/** Delaunay arguments used for nutation or tides.
 * <p>This class is a simple placeholder,
 * it does not provide any processing method.</p>
 * @param <T> the type of the field elements
 * @see DelaunayArguments
 * @author Luc Maisonobe
 * @since 6.1
 */
public class FieldDelaunayArguments<T extends CalculusFieldElement<T>> implements FieldTimeStamped<T> {

    /** Date. */
    private final FieldAbsoluteDate<T> date;

    /** Offset in Julian centuries. */
    private final T tc;

    /** Tide parameter γ = GMST + π. */
    private final T gamma;

    /** Tide parameter γ = GMST + π time derivative. */
    private final T gammaDot;

    /** Mean anomaly of the Moon. */
    private final T l;

    /** Mean anomaly of the Moon time derivative. */
    private final T lDot;

    /** Mean anomaly of the Sun. */
    private final T lPrime;

    /** Mean anomaly of the Sun time derivative. */
    private final T lPrimeDot;

    /** L - Ω where L is the mean longitude of the Moon. */
    private final T f;

    /** L - Ω where L is the mean longitude of the Moon time derivative. */
    private final T fDot;

    /** Mean elongation of the Moon from the Sun. */
    private final T d;

    /** Mean elongation of the Moon from the Sun time derivative. */
    private final T dDot;

    /** Mean longitude of the ascending node of the Moon. */
    private final T omega;

    /** Mean longitude of the ascending node of the Moon time derivative. */
    private final T omegaDot;

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
    public FieldDelaunayArguments(final FieldAbsoluteDate<T> date, final T tc, final T gamma, final T gammaDot,
                                  final T l, final T lDot, final T lPrime, final T lPrimeDot,
                                  final T f, final T fDot, final T d, final T dDot,
                                  final T omega, final T omegaDot) {
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
    public FieldAbsoluteDate<T> getDate() {
        return date;
    }

    /** Get the offset in Julian centuries.
     * @return offset in Julian centuries
     */
    public T getTC() {
        return tc;
    }

    /** Get the tide parameter γ = GMST + π.
     * @return tide parameter γ = GMST + π
     */
    public T getGamma() {
        return gamma;
    }

    /** Get the tide parameter γ = GMST + π time derivative.
     * @return tide parameter γ = GMST + π time derivative
     */
    public T getGammaDot() {
        return gammaDot;
    }

    /** Get the mean anomaly of the Moon.
     * @return mean anomaly of the Moon
     */
    public T getL() {
        return l;
    }

    /** Get the mean anomaly of the Moon time derivative.
     * @return mean anomaly of the Moon time derivative
     */
    public T getLDot() {
        return lDot;
    }

    /** Get the mean anomaly of the Sun.
     * @return mean anomaly of the Sun.
     */
    public T getLPrime() {
        return lPrime;
    }

    /** Get the mean anomaly of the Sun time derivative.
     * @return mean anomaly of the Sun time derivative.
     */
    public T getLPrimeDot() {
        return lPrimeDot;
    }

    /** Get L - Ω where L is the mean longitude of the Moon.
     * @return L - Ω
     */
    public T getF() {
        return f;
    }

    /** Get L - Ω where L is the mean longitude of the Moon time derivative.
     * @return L - Ω time derivative
     */
    public T getFDot() {
        return fDot;
    }

    /** Get the mean elongation of the Moon from the Sun.
     * @return mean elongation of the Moon from the Sun.
     */
    public T getD() {
        return d;
    }

    /** Get the mean elongation of the Moon from the Sun time derivative.
     * @return mean elongation of the Moon from the Sun time derivative.
     */
    public T getDDot() {
        return dDot;
    }

    /** Get the mean longitude of the ascending node of the Moon.
     * @return mean longitude of the ascending node of the Moon.
     */
    public T getOmega() {
        return omega;
    }

    /** Get the mean longitude of the ascending node of the Moon time derivative.
     * @return mean longitude of the ascending node of the Moon time derivative.
     */
    public T getOmegaDot() {
        return omegaDot;
    }

}
