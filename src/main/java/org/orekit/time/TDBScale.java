/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Syst�mes (CS) under one or more
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
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class TDBScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = -4138483908215161856L;

    /** Package private constructor for the factory.
     */
    TDBScale() {
    }

    /** {@inheritDoc} */
    public double offsetFromTAI(final AbsoluteDate date) {
        final double dtDays = date.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY;
        final double g = Math.toRadians(357.53 + 0.9856003 * dtDays);
        return TimeScalesFactory.getTT().offsetFromTAI(date) + (0.001658 * Math.sin(g) + 0.000014 * Math.sin(2 * g));
    }

    /** {@inheritDoc} */
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        final AbsoluteDate reference = new AbsoluteDate(date, time, TimeScalesFactory.getTAI());
        double offset = 0;
        for (int i = 0; i < 3; i++) {
            offset = -offsetFromTAI(reference.shiftedBy(offset));
        }
        return offset;
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
