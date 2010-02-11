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

/** This class holds Earth Orientation Parameters (IAU2000) data throughout a large time range.
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class EOP2000History extends AbstractEOPHistory {

    /** Serializable UID. */
    private static final long serialVersionUID = 1940180123066815667L;

    /** Simple constructor.
     */
    public EOP2000History() {
    }

    /** Add an Earth Orientation Parameters entry.
     * @param entry entry to add
     */
    public void addEntry(final EOP2000Entry entry) {
        entries.add(entry);
    }

    /** Get the pole IERS Reference Pole correction.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     * PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        if (prepareInterpolation(date)) {
            synchronized (this) {

                final EOP2000Entry n = (EOP2000Entry) next;
                final EOP2000Entry p = (EOP2000Entry) previous;
                final double x = (dtP * n.getX() + dtN * p.getX()) / (dtP + dtN);
                final double y = (dtP * n.getY() + dtN * p.getY()) / (dtP + dtN);

                return new PoleCorrection(x, y);

            }
        } else {
            return PoleCorrection.NULL_CORRECTION;
        }
    }

}
