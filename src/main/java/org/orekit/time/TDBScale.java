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
package org.orekit.time;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.SinCos;
import org.orekit.utils.Constants;

/** Barycentric Dynamic Time.
 * <p>Time used to take account of time dilation when calculating orbits of planets,
 * asteroids, comets and interplanetary spacecraft in the Solar system. It was based
 * on a Dynamical time scale but was not well defined and not rigorously correct as
 * a relativistic time scale. It was subsequently deprecated in favour of
 * Barycentric Coordinate Time (TCB), but at the 2006 General Assembly of the
 * International Astronomical Union TDB was rehabilitated by making it a specific
 * fixed linear transformation of TCB.</p>
 * <p>By convention, TDB = TT + 0.001658 sin(g) + 0.000014 sin(2g)seconds
 * where g = 357.53 + 0.9856003 (JD - 2451545) degrees.</p>
 * @author Aude Privat
 */
public class TDBScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 20240720L;

    /** Constant term for g angle. */
    private static final double G0 = FastMath.toRadians(357.53);

    /** Slope term for g angle. */
    private static final double G1 = FastMath.toRadians(0.9856003);

    /** Factor for sin(g). */
    private static final double SIN_G_FACTOR = 0.001658;

    /** Factor for sin(2g). */
    private static final double TWO_SIN_2G_FACTOR = 2 * 0.000014;

    /** TT time scale. */
    private final TimeScale tt;

    /** Reference Epoch. */
    private final AbsoluteDate j2000Epoch;

    /**
     * Package private constructor for the factory.
     *
     * @param tt         TT time scale.
     * @param j2000Epoch reference date for this time scale.
     */
    TDBScale(final TimeScale tt, final AbsoluteDate j2000Epoch) {
        this.tt = tt;
        this.j2000Epoch = j2000Epoch;
    }

    /** {@inheritDoc} */
    @Override
    public TimeOffset offsetFromTAI(final AbsoluteDate date) {
        final double dtDays = date.durationFrom(j2000Epoch) / Constants.JULIAN_DAY;
        final SinCos sc = FastMath.sinCos(G0 + G1 * dtDays);
        return tt.offsetFromTAI(date).
               add(new TimeOffset(sc.sin() * (SIN_G_FACTOR + TWO_SIN_2G_FACTOR * sc.cos())));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T offsetFromTAI(final FieldAbsoluteDate<T> date) {
        final T dtDays = date.durationFrom(j2000Epoch).divide(Constants.JULIAN_DAY);
        final FieldSinCos<T> sc = FastMath.sinCos(dtDays.multiply(G1).add(G0));
        return tt.offsetFromTAI(date).
               add(sc.sin().multiply(sc.cos().multiply(TWO_SIN_2G_FACTOR).add(SIN_G_FACTOR)));
    }

    /** {@inheritDoc} */
    public String getName() {
        return "TDB";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

}
