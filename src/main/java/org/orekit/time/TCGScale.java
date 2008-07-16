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
 * This time scale depends linearly from {@link TTScale
 * Terrestrial Time}.</p>
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class TCGScale extends TimeScale {

    /** LG rate. */
    private static double LG_RATE = 6.969290134e-10;

    /** Inverse rate. */
    private static double INVERSE_RATE = 1.0 / (1.0 + LG_RATE);

    /** Reference time scale. */
    private static final TimeScale TT_SCALE = TTScale.getInstance();

    /** Reference time for TCG is 1977-01-01 (2557 days after 1970-01-01). */
    private static final double REFERENCE_DATE = 2557l * 86400000l;

    /** Private constructor for the singleton. */
    private TCGScale() {
        super("TCG");
    }

    /** Get the unique instance of this class.
     * @return the unique instance
     */
    public static TimeScale getInstance() {
        return LazyHolder.INSTANCE;
    }

    /** Get the offset to convert locations from {@link TAIScale}  to instance.
     * @param taiTime location of an event in the {@link TAIScale}  time scale
     * as a seconds index starting at 1970-01-01T00:00:00
     * @return offset to <em>add</em> to taiTime to get a location
     * in instance time scale
     */
    public double offsetFromTAI(final double taiTime) {
        final double ttOffset = TT_SCALE.offsetFromTAI(taiTime);
        return ttOffset + LG_RATE * (ttOffset + taiTime - REFERENCE_DATE);
    }

    /** Get the offset to convert locations from instance to {@link TAIScale} .
     * @param instanceTime location of an event in the instance time scale
     * as a seconds index starting at 1970-01-01T00:00:00
     * @return offset to <em>add</em> to instanceTime to get a location
     * in {@link TAIScale}  time scale
     */
    public double offsetToTAI(final double instanceTime) {
        final double ttTime = INVERSE_RATE * (instanceTime + LG_RATE * REFERENCE_DATE);
        return TT_SCALE.offsetToTAI(ttTime) - LG_RATE * INVERSE_RATE * (instanceTime - REFERENCE_DATE);
    }

    /** Holder for the singleton.
     * <p>We use the Initialization on demand holder idiom to store
     * the singleton, as it is both thread-safe, efficient (no
     * synchronization) and works with all version of java.</p>
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
