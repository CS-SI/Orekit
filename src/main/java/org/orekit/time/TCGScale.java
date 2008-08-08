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
package org.orekit.time;

/** Geocentric Coordinate Time.
 * <p>Coordinate time at the center of mass of the Earth.
 * This time scale depends linearly from {@link TTScale Terrestrial Time}.</p>
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class TCGScale implements TimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = -8311852881965291194L;

    /** LG rate. */
    private static double LG_RATE = 6.969290134e-10;

    /** Inverse rate. */
    private static double INVERSE_RATE = 1.0 / (1.0 + LG_RATE);

    /** Reference date for TCG.
     * <p>The reference date is such that the three following instants are equal:</p>
     * <ul>
     *   <li>1977-01-01T00:00:32.184 TT</li>
     *   <li>1977-01-01T00:00:32.184 TCG</li>
     *   <li>1977-01-01T00:00:00.000 TAI</li>
     * </ul>
     */
    private static final AbsoluteDate REFERENCE_DATE =
        new AbsoluteDate(1977, 01, 01, TAIScale.getInstance());

    /** Offset between TT and TAI scales. */
    private static final double TT_OFFSET =
        TTScale.getInstance().offsetFromTAI(REFERENCE_DATE);

    /** Private constructor for the singleton. */
    private TCGScale() {
    }

    /** Get the unique instance of this class.
     * @return the unique instance
     */
    public static TCGScale getInstance() {
        return LazyHolder.INSTANCE;
    }

    /** {@inheritDoc} */
    public double offsetFromTAI(final AbsoluteDate date) {
        return TT_OFFSET + LG_RATE * date.durationFrom(REFERENCE_DATE);
    }

    /** {@inheritDoc} */
    public double offsetToTAI(final DateComponents date, final TimeComponents time) {
        final double dt = (date.getJ2000Day() + 8400) * 86400.0 + time.getSecondsInDay();
        return -TT_OFFSET - LG_RATE * INVERSE_RATE * (dt - TT_OFFSET);
    }

    /** {@inheritDoc} */
    public String getName() {
        return "TCG";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

    /** Change object upon deserialization.
     * <p>Since {@link TimeScale} classes are serializable, they can
     * be deserialized. This class being a singleton, we always replace the
     * read object by the singleton instance at deserialization time.</p>
     * @return the singleton instance
     */
    private Object readResolve() {
        return LazyHolder.INSTANCE;
    }

    /** Holder for the singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all versions of java.</p>
     */
    private static class LazyHolder  {

        /** Unique instance. */
        private static final TCGScale INSTANCE = new TCGScale();

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private LazyHolder() {
        }

    }

}
