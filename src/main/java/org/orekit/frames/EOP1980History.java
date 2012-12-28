/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.frames;

import org.apache.commons.math3.analysis.interpolation.HermiteInterpolator;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;

/** This class holds Earth Orientation Parameters (IAU1980) data throughout a large time range.
 * @author Pascal Parraud
 */
public class EOP1980History extends AbstractEOPHistory {

    /** Serializable UID. */
    private static final long serialVersionUID = 3003752420705950441L;

    /** Simple constructor.
     */
    public EOP1980History() {
    }

    /** Get the correction to the nutation parameters.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return nutation correction ({@link NutationCorrection#NULL_CORRECTION
     * NutationCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    public NutationCorrection getNutationCorrection(final AbsoluteDate date) {
        try {
            final HermiteInterpolator interpolator = new HermiteInterpolator();
            for (final EOPEntry entry : getNeighbors(date)) {
                final EOP1980Entry e1980 = (EOP1980Entry) entry;
                interpolator.addSamplePoint(entry.getDate().durationFrom(date),
                                            new double[] {
                                                e1980.getDdEps(), e1980.getDdPsi()
                                            });
            }
            final double[] interpolated = interpolator.value(0);
            return new NutationCorrection(interpolated[0], interpolated[1]);
        } catch (TimeStampedCacheException tce) {
            // no EOP data available for this date, we use a default null correction
            return NutationCorrection.NULL_CORRECTION;
        }
    }

}
