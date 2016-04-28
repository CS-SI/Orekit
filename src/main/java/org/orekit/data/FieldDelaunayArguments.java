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
import org.orekit.time.TimeStamped;

/** Delaunay arguments used for nutation or tides.
 * <p>This class is a simple placeholder,
 * it does not provide any processing method.</p>
 * @param <T> the type of the field elements
 * @see DelaunayArguments
 * @author Luc Maisonobe
 * @since 6.1
 */
public class FieldDelaunayArguments<T extends RealFieldElement<T>> implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131097L;

    /** Date. */
    private final AbsoluteDate date;

    /** Offset in Julian centuries. */
    private final T tc;

    /** Tide parameter γ = GMST + π. */
    private final T gamma;

    /** Mean anomaly of the Moon. */
    private final T l;

    /** Mean anomaly of the Sun. */
    private final T lPrime;

    /** L - Ω where L is the mean longitude of the Moon. */
    private final T f;

    /** Mean elongation of the Moon from the Sun. */
    private final T d;

    /** Mean longitude of the ascending node of the Moon. */
    private final T omega;

    /** Simple constructor.
     * @param date current date
     * @param tc offset in Julian centuries
     * @param gamma tide parameter γ = GMST + π
     * @param l mean anomaly of the Moon
     * @param lPrime mean anomaly of the Sun
     * @param f L - Ω where L is the mean longitude of the Moon
     * @param d mean elongation of the Moon from the Sun
     * @param omega mean longitude of the ascending node of the Moon
     */
    public FieldDelaunayArguments(final AbsoluteDate date, final T tc, final T gamma,
                                  final T l, final T lPrime,
                                  final T f, final T d, final T omega) {
        this.date   = date;
        this.tc     = tc;
        this.gamma  = gamma;
        this.l      = l;
        this.lPrime = lPrime;
        this.f      = f;
        this.d      = d;
        this.omega  = omega;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
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

    /** Get the mean anomaly of the Moon.
     * @return mean anomaly of the Moon
     */
    public T getL() {
        return l;
    }

    /** Get the mean anomaly of the Sun.
     * @return mean anomaly of the Sun.
     */
    public T getLPrime() {
        return lPrime;
    }

    /** Get L - Ω where L is the mean longitude of the Moon.
     * @return L - Ω
     */
    public T getF() {
        return f;
    }

    /** Get the mean elongation of the Moon from the Sun.
     * @return mean elongation of the Moon from the Sun.
     */
    public T getD() {
        return d;
    }

    /** Get the mean longitude of the ascending node of the Moon.
     * @return mean longitude of the ascending node of the Moon.
     */
    public T getOmega() {
        return omega;
    }

}
