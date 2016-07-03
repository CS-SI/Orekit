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
package org.orekit.time;

/** Terrestrial Time as defined by IAU(1991) recommendation IV.
 * <p>Coordinate time at the surface of the Earth. IT is the
 * successor of Ephemeris Time TE.</p>
 * <p>By convention, TT = TAI + 32.184 s.</p>
 * <p>This is intended to be accessed thanks to the {@link TimeScalesFactory} class,
 * so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class TTScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131209L;

    /** Package private constructor for the factory.
     */
    TTScale() {
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromTAI(final AbsoluteDate date) {
        return 32.184;
    }

    /** {@inheritDoc} */
    @Override
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        return -32.184;
    }

    /** {@inheritDoc} */
    public String getName() {
        return "TT";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

}
