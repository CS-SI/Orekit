/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.frames;

import org.orekit.time.AbsoluteDate;

/** This class holds Earth Orientation Parameters (IAU1980) data throughout a large time range.
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class EOP1980History extends AbstractEOPHistory {

    /** Serializable UID. */
    private static final long serialVersionUID = -3673081177492491161L;

    /** Simple constructor.
     */
    public EOP1980History() {
    }

    /** Add an Earth Orientation Parameters entry.
     * @param entry entry to add
     */
    public void addEntry(final EOP1980Entry entry) {
        entries.add(entry);
    }

    /** Get the correction to the nutation parameters.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return nutation correction ({@link NutationCorrection#NULL_CORRECTION
     * NutationCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    public NutationCorrection getNutationCorrection(final AbsoluteDate date) {
        if (prepareInterpolation(date)) {
            synchronized (this) {

                final EOP1980Entry n = (EOP1980Entry) next;
                final EOP1980Entry p = (EOP1980Entry) previous;
                final double ddEps = (dtP * n.getDdEps() + dtN * p.getDdEps()) / (dtP + dtN);
                final double ddPsi = (dtP * n.getDdPsi() + dtN * p.getDdPsi()) / (dtP + dtN);
                return new NutationCorrection(ddEps, ddPsi);

            }
        } else {
            return NutationCorrection.NULL_CORRECTION;
        }
    }

}
