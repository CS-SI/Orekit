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
package org.orekit.frames;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;

/** This class holds Earth Orientation Parameters (IAU2000) data throughout a large time range.
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
class EOP2000History extends AbstractEOPHistory {

    /** Serializable UID. */
    private static final long serialVersionUID = 9141543606409905199L;

    /** Id for X component of pole motion field. */
    private static final int POLE_X_FIELD = 0;

    /** Id for Y component of pole motion field. */
    private static final int POLE_Y_FIELD = 1;

    /** Id for UT1-UTC field. */
    private static final int UT1_UTC_FIELD = 2;

    /** Id for LoD field. */
    private static final int LOD_FIELD = 3;

    /** Regular name for the EOPC04 files (IAU2000 compatibles). */
    private static final String EOPC04FILENAME = "^eopc04_IAU2000\\.(\\d\\d)$";

    /** Regular name for the BulletinB files (IAU2000 compatibles). */
    private static final String BULLETFILENAME = "^bulletinb_IAU2000((-\\d\\d\\d\\.txt)|(\\.\\d\\d\\d))$";

   /** Simple constructor.
     * @exception OrekitException if there is a problem while reading IERS data
     */
    public EOP2000History() throws OrekitException {
        super(EOPC04FILENAME, BULLETFILENAME);
    }

    /** Get the UT1-UTC value.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the value is desired
     * @return UT1-UTC in seconds (0 if date is outside covered range)
     */
    public double getUT1MinusUTC(final AbsoluteDate date) {

        // interpolate UT1 - UTC
        return getInterpolatedField(date, UT1_UTC_FIELD);

    }

    /** Get the LoD (Length of Day) value.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the value is desired
     * @return LoD in seconds (0 if date is outside covered range)
     */
    public double getLOD(final AbsoluteDate date) {

        // interpolate LOD
        return getInterpolatedField(date, LOD_FIELD);

    }

    /** Get the pole IERS Reference Pole correction.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     * PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {

        // interpolate XP and Yp
        return new PoleCorrection(getInterpolatedField(date, POLE_X_FIELD),
                                  getInterpolatedField(date, POLE_Y_FIELD));

    }

}
