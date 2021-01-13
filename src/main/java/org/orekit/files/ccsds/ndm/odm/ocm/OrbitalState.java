/* Copyright 2002-2021 CS GROUP
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

package org.orekit.files.ccsds.ndm.odm.ocm;

import org.orekit.files.ccsds.utils.CCSDSUnit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** Orbital state entry.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OrbitalState implements TimeStamped {

    /** Entry date. */
    private final AbsoluteDate date;

    /** Orbital elements. */
    private final double[] elements;

    /** Simple constructor.
     * @param date entry date
     * @param fields orbital elements
     * @param first index of first field to consider
     * @param units units to use for parsing
     */
    public OrbitalState(final AbsoluteDate date, final String[] fields, final int first, final CCSDSUnit[] units) {
        this.date     = date;
        this.elements = new double[units.length];
        for (int i = 0; i < elements.length; ++i) {
            elements[i] = units[i].toSI(Double.parseDouble(fields[first + i]));
        }
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get orbital elements.
     * @return orbital elements
     */
    public double[] getElements() {
        return elements.clone();
    }

}
