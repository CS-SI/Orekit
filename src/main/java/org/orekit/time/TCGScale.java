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
package org.orekit.time;

/** Geocentric Coordinate Time.
 * <p>Coordinate time at the center of mass of the Earth.
 * This time scale depends linearly from {@link TTScale Terrestrial Time}.</p>
 * <p>This is intended to be accessed thanks to the {@link TimeScalesFactory} class,
 * so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class TCGScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 1967551803293794056L;

    /** LG rate. */
    private static double LG_RATE = 6.969290134e-10;

    /** Reference date for TCG.
     * <p>The reference date is such that the three following instants are equal:</p>
     * <ul>
     *   <li>1977-01-01T00:00:32.184 TT</li>
     *   <li>1977-01-01T00:00:32.184 TCG</li>
     *   <li>1977-01-01T00:00:00.000 TAI</li>
     * </ul>
     */
    private static final AbsoluteDate REFERENCE_DATE =
        new AbsoluteDate(1977, 01, 01, TimeScalesFactory.getTAI());

    /** Offset between TT and TAI scales. */
    private static final double TT_OFFSET =
        TimeScalesFactory.getTT().offsetFromTAI(REFERENCE_DATE);

    /** Package private constructor for the factory.
     */
    TCGScale() {
    }

    /** Get the unique instance of this class.
     * @return the unique instance
     * @deprecated since 4.1 replaced by {@link TimeScalesFactory#getTCG()}
     */
    @Deprecated
    public static TCGScale getInstance() {
        return TimeScalesFactory.getTCG();
    }

    /** {@inheritDoc} */
    public double offsetFromTAI(final AbsoluteDate date) {
        return TT_OFFSET + LG_RATE * date.durationFrom(REFERENCE_DATE);
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
        return "TCG";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

}
