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

/** This class holds Earth Orientation Parameters (IAU1980) data throughout a large time range.
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class EOP1980History extends AbstractEOPHistory {

    /** Serializable UID. */
    private static final long serialVersionUID = -4645479420083885164L;

    /** Id for UT1-UTC field. */
    private static final int UT1_UTC_FIELD = 2;

    /** Id for LoD field. */
    private static final int LOD_FIELD = 3;

    /** Id for Correction for nutation in obliquity field. */
    private static final int DDEPS_FIELD = 4;

    /** Id for Correction for nutation in longitude field. */
    private static final int DDPSI_FIELD = 5;

    /** Regular name for the EOPC04 files (IAU1980 compatibles). */
    private static final String EOPC04FILENAME = "^eopc04\\.(\\d\\d)$";

    /** Regular name for the BulletinB files (IAU1980 compatibles). */
    private static final String BULLETFILENAME = "^bulletinb((-\\d\\d\\d\\.txt)|(\\.\\d\\d\\d))$";

   /** Simple constructor.
     * @exception OrekitException if there is a problem while reading IERS data
     */
    public EOP1980History() throws OrekitException {
        super(EOPC04FILENAME, BULLETFILENAME);
    }

    /** Get the UT1-UTC value.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the value is desired
     * @return UT1-UTC in seconds (0 if date is outside covered range)
     */
    protected double getUT1MinusUTC(final AbsoluteDate date) {

        // interpolate UT1 - UTC
        return getInterpolatedField(date, UT1_UTC_FIELD);

    }

    /** Get the LoD (Length of Day) value.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the value is desired
     * @return LoD in seconds (0 if date is outside covered range)
     */
    protected double getLOD(final AbsoluteDate date) {

        // interpolate LOD
        return getInterpolatedField(date, LOD_FIELD);

    }

    /** Get the correction to the nutation parameters.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return nutation correction ({@link NutationCorrection#NULL_CORRECTION
     * NutationCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    protected NutationCorrection getNutationCorrection(final AbsoluteDate date) {

        // interpolate dDeps and dDpsi
        return new NutationCorrection(getInterpolatedField(date, DDEPS_FIELD),
                                      getInterpolatedField(date, DDPSI_FIELD));

    }

}
